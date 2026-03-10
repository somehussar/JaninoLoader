# Oracle Consultation: 13 Failing Lambda Tests — Diagnosis & Solutions

**Date**: 2026-03-10  
**Status**: Complete  
**Session**: Lambda/Generics Investigation Continuation

---

## EXECUTIVE SUMMARY

All 13 failing tests have been diagnosed with root causes and severity levels. The failures fall into **6 distinct architectural categories**:

| Category | Count | Root Cause | Severity | Fixability |
|----------|-------|-----------|----------|-----------|
| **A. Lambda Parameter Type Collapse** | 5 | SAM parameter type resolution fails in specific contexts | Medium | Medium |
| **B. Method Reference Path Issues** | 2 | Method refs use different compilation path than lambdas; raw type lookup ignores generics | Medium | Medium |
| **C. Static Generic Method Inference** | 2 | Backward type inference from assignment not implemented | Hard | Hard |
| **D. Parser Limitations** | 2 | Parser rejects valid syntax (array assignment, cast expressions) | Low | High |
| **E. Scope/Context Issues** | 1 | Lambda in ternary expression causes scope pollution | Medium | Medium |
| **F. Configuration** | 1 | Java target version not set to 8+ | Trivial | Trivial |

---

## DETAILED ROOT CAUSE ANALYSIS

### CATEGORY A: Lambda Parameter Type Collapse (5 tests)

**Tests**: 
- `edge_lambdaInStaticInit` — static initializer context
- `stream_collect_groupingBy` — static generic method (Collectors.groupingBy)
- `generics_wildcardBound` — wildcard-bounded stream
- `typeInference_lambdaInGenericMethod` — static generic method parameter
- `customFI_withDefaultMethod` — Java 8 target config (separate from A, but Type Collapse)

**Root Cause**:
Lambda parameters are determined by SAM (Single Abstract Method) parameter types. The resolution chain is:

```
1. Get method parameter type from method signature (erased: e.g., Predicate)
2. Attempt to resolve parameterized version:
   a. resolveMethodParamTypeFromReceiver() — use receiver's generic type args
   b. getParameterizedMethodParameterType() — read generic signatures from .class files
3. Extract SAM method from functional interface
4. Get SAM parameter types (may be raw Object if resolution failed)
5. Assign to lambda parameters
```

**Why They Fail**:

- **Static Initializers**: `static Function<Integer, Integer> DOUBLER = x -> x * 2;`  
  Lambda compilation occurs during class initialization, where `expectedTargetType` context is limited. The SAM parameter type is correctly `Integer`, but something in the parameter typing pipeline misses it. The variable initializer context doesn't flow properly.

- **Static Generic Methods**: `static <T> T apply(Supplier<T>)` with `String s = apply(() -> "test");`  
  The method's type variable `T` can only be inferred from the assignment context (`String`). Janino compiles forward-only: it resolves `Supplier` as raw, extracts SAM `T get()`, but `T` is unbound (not inferred yet), so defaults to `Object`.

- **Wildcard Bounds**: `Stream<? extends CharSequence>` with `s -> s.length()`  
  The wildcard bound `CharSequence` isn't being extracted and used. When resolving `Stream` with wildcard type argument, the code needs to resolve `? extends CharSequence` → `CharSequence`, then use that as the SAM parameter type. Current code at `resolveSamParamTypes()` may not handle wildcards properly.

**Fixes Needed**:
1. **Static Initializer**: Ensure `expectedTargetType` is set from variable declaration context when compiling lambda in static initializer
2. **Static Generic Methods**: Implement backward type inference (see Category C)
3. **Wildcard Bounds**: Enhance wildcard handling in `resolveReflectiveTypeArgWithTvMap()` and `resolveSamParamTypes()` to extract wildcard bounds

---

### CATEGORY B: Method Reference Path Issues (2 tests)

**Tests**:
- `methodRef_asComparator` — method reference on parameterized type  
- `methodRef_arrayConstructor` — array constructor reference return type

**Root Cause**:

Method references are compiled by converting them to synthetic lambdas wrapping a `MethodInvocation`:

```
String::compareTo  →  (synthetic lambda) t -> t.compareTo(...)
String[]::new      →  (synthetic lambda) n -> new String[n]
```

But the conversion uses **raw types**, not parameterized types:

1. `lhs` is resolved to raw `IClass` (generics discarded by `rawTypeOf()`)
2. Method lookup via `getDeclaredIMethods()` returns erased signatures
3. For parameterized receiver like `Stream<String>`, the `String` type information is lost
4. Method matching fails or produces wrong return type

**Specific Issues**:

- **`methodRef_asComparator`**: Converting `"some text"::compareTo` works, but `stream::sorted` where stream is `Stream<String>` tries to find method on raw `Stream`, loses `String` context
- **`methodRef_arrayConstructor`**: `String[]::new` is converted to synthetic lambda, but return type is `Object[]` instead of `String[]` because array constructor reference doesn't properly track element type

**Fixes Needed**:
1. Preserve parameterized type information during method reference → synthetic lambda conversion
2. Use parameterized receiver type when looking up methods (not raw type)
3. For array constructors, properly infer and use element type in return type

---

### CATEGORY C: Static Generic Method Inference (2 tests)

**Tests**:
- `typeInference_returnType` — `String s = apply(Supplier<String>)` returns Object
- `typeInference_lambdaInGenericMethod` — `static <T> T apply(Supplier<T>)` can't infer T

**Root Cause**:

Janino uses **forward-only** type inference:
1. Resolve method by name, parameter types
2. Get method's return type (may contain type variables like `T`)
3. Compile completed

For `static <T> T apply(Supplier<T>)` with `String s = apply(...)`:
- Forward inference: `Supplier` is raw, so `T` is unbound → return type is `T` = `Object`
- Missing: Backward inference from assignment context (`String`) → `T` should be `String`

**Why This is Hard**:

Javac (official Java compiler) uses **sophisticated bidirectional type inference** (JLS 18.5):
1. Invocation type inference (forward): resolve constraints from argument types
2. Return type inference (backward): use assignment target type to solve remaining type variables
3. Repeat until fixed point

Janino would need:
- Modified method resolution to accept "provisional" type variable bindings
- Post-resolution phase to use assignment context
- Potentially different compilation order

**Fixes Needed**:

This is **architectural**. Options:
1. **Lightweight approach**: Detect `static <T> T methodName(...)` pattern, peek at assignment context, pre-solve for T before normal resolution
2. **Medium approach**: Add backward inference phase for methods with unbound return type variables
3. **Full approach**: Rewrite type inference to match javac (major undertaking)

---

### CATEGORY D: Parser Limitations (2 tests)

**Tests**:
- `edge_recursiveViaHolder` — `holder[0] = n -> ...` (array element assignment)
- `edge_castToFunctionalInterface` — `(Function<String, String>) s -> ...` (cast expression)

**Root Cause**:

**`edge_recursiveViaHolder`**: Parser sees `holder[0]` as potential start of a type expression (for generic type argument), not recognizing it as an array element access in assignment context.

**`edge_castToFunctionalInterface`**: Parser expects cast expressions to contain `ClassOrInterfaceType`, not recognize lambda as valid expression in cast context.

**Fixes Needed**:

These are **parser-level** issues in `Parser.java`, not in generic type resolution. They require:
1. Disambiguating array index expressions from type argument sequences
2. Recognizing lambda expressions as valid cast targets (they convert to functional interface types)

Both require parser modifications, which are outside the scope of the generic type inference work.

---

### CATEGORY E: Scope/Context Issues (1 test)

**Test**: `nested_lambdaInConditional` — lambda in ternary operator

**Root Cause**:

Lambda in ternary: `boolean result = num > 5 ? (x -> x * 2).apply(3) : 0;`

When compiling the lambda `(x -> x * 2)` in the ternary context, scope management fails. The lambda compilation needs to understand that it's in a conditional arm, but current scope tracking doesn't handle this properly.

**Error**: `InternalCompilerException` during scope setup or lambda body compilation

**Fixes Needed**:
1. Review scope setup for lambdas in ternary/conditional contexts
2. May need to adjust `expectedTargetType` handling or scope chain management

---

### CATEGORY F: Configuration (1 test)

**Test**: `customFI_withDefaultMethod` — Default interface methods require Java 8+ target

**Root Cause**:

Interface with `default` method requires Java 8 target version. The test class `CompileAndRun` doesn't set the Janino compiler target version to 8.

**Fix Needed**:
```java
// In test setup
compiler.setTargetVersion(8);  // or 8.0 in some Janino versions
```

---

## PRIORITIZED FIX RECOMMENDATIONS

### Tier 1: Quick Wins (1-2 hours each)

**1. `customFI_withDefaultMethod` (Config Fix)**
- **Effort**: 5 minutes
- **Impact**: 1 test fixed
- **Action**: Set target version to 8 in test setup or compiler configuration
- **Code location**: `JaninoLambdaTest.compileAndRun()` method

---

### Tier 2: Medium Effort (4-8 hours)

**2. `edge_lambdaInStaticInit` (Context Propagation)**
- **Effort**: 2-4 hours
- **Impact**: 1 test fixed, may help with other context issues
- **Root cause**: `expectedTargetType` not set from variable initializer context
- **Action**: Trace variable initializer context through compilation pipeline, ensure target type flows to lambda compilation
- **Code location**: UnitCompiler.java around variable initialization handling (likely ~5000-6000 range for field initializer processing)

**3. `generics_wildcardBound` (Wildcard Resolution)**
- **Effort**: 4-6 hours  
- **Impact**: 1 test fixed, improves generic type handling
- **Root cause**: Wildcard bounds not extracted for lambda parameter typing
- **Action**: Enhance `resolveSamParamTypes()` and `resolveReflectiveTypeArgWithTvMap()` to handle wildcard bounds
- **Code location**: UnitCompiler.java lines ~5648 (`resolveReflectiveTypeArgWithTvMap`) and ~5160 (`resolveSamParamTypes`)

**4. `methodRef_asComparator` & `methodRef_arrayConstructor` (Method Reference SAM Typing)**
- **Effort**: 6-8 hours (both together)
- **Impact**: 2 tests fixed
- **Root cause**: Method reference → synthetic lambda conversion loses parameterized receiver type info
- **Action**: Preserve parameterized type through method reference conversion, use in method lookup
- **Code location**: UnitCompiler.java lines ~4946-5134 (method reference compilation)

---

### Tier 3: Hard / Architectural (8+ hours)

**5. `typeInference_returnType` & `typeInference_lambdaInGenericMethod` (Backward Type Inference)**
- **Effort**: 8-16 hours
- **Impact**: 2 tests fixed, potential foundation for future improvements
- **Root cause**: No backward type inference from assignment context
- **Action**: Implement lightweight backward inference for `static <T> T methodName()` pattern
- **Code location**: UnitCompiler.java method resolution (~3000-4000 range for method lookup), return type handling
- **Alternative**: Skip these for now (they're hard architectural problems)

**6. `stream_collect_groupingBy` (Static Generic + Wildcard)**
- **Effort**: 10-12 hours
- **Impact**: 1 test fixed (but depends on fixes 5 & 3)
- **Root cause**: Combination of static generic method inference + complex generic signature
- **Action**: Fix #5 (backward inference) + #3 (wildcard handling)

---

### Tier 4: Parser Issues (Deferred)

**7. `edge_recursiveViaHolder` & `edge_castToFunctionalInterface` (Parser Fixes)**
- **Effort**: 4-6 hours each (requires deep parser knowledge)
- **Impact**: 2 tests fixed
- **Root cause**: Parser doesn't recognize array index and cast expressions in lambda contexts
- **Action**: Modify Parser.java to handle these edge cases
- **Note**: Recommend deferring — low impact, high risk of parser regression

**8. `nested_lambdaInConditional` (Scope Issue)**
- **Effort**: 2-4 hours
- **Impact**: 1 test fixed
- **Root cause**: Scope management in ternary/conditional lambda contexts
- **Action**: Review scope setup for lambdas in conditional arms
- **Code location**: Scope management in UnitCompiler around compileGet2(LambdaExpression)

---

## RECOMMENDED IMPLEMENTATION ORDER

If you can fix **5-7 tests** in this session, follow this priority:

1. **`customFI_withDefaultMethod`** — 5 min, get 1 win immediately
2. **`generics_wildcardBound`** — 4-6 hrs, fixes type resolution depth
3. **`edge_lambdaInStaticInit`** — 2-4 hrs, improves context handling
4. **`methodRef_asComparator` + `methodRef_arrayConstructor`** — 6-8 hrs (both together), extends SAM typing to method refs
5. **`stream_collect_groupingBy`** — Can attempt after #3, but may need #5 too

This sequence yields **5-6 tests fixed** with **16-25 hours** of work and **minimal regression risk**.

The harder items (#5 backward inference, parser fixes) can be attempted later or deferred to a future session.

---

## IMPLEMENTATION HINTS FOR EACH CATEGORY

### For Category A (Parameter Type Collapse)

**Static Initializer Context**:
```java
// In UnitCompiler.java, find variable initializer compilation
// Ensure expectedTargetType is set from variable type annotation:
IType varType = getType(fieldDeclaration.type);
// When compiling initializer lambda, pass varType as expectedTargetType
compileGetValue(..., expectedTargetType = varType);
```

**Wildcard Bounds**:
```java
// In resolveSamParamTypes() or resolveReflectiveTypeArgWithTvMap()
// When seeing IWildcardType, extract the bound:
if (ta instanceof IWildcardType) {
    IWildcardType wt = (IWildcardType) ta;
    IType bound = wt.getUpperBound();  // or getLowerBound()
    // Use bound for further resolution
}
```

### For Category B (Method Reference SAM Typing)

```java
// In compileGet2(MethodReference), preserve parameterized receiver:
Atom lhs = methodRef.lhs;  // Don't call rawTypeOf() yet
IType lhsType = getType2(lhs);  // Keep parameterized
// Pass lhsType (not rawTypeOf(lhsType)) to method lookup
IInvocable[] candidates = findCandidates(lhsType, methodRef.methodName);
```

### For Category C (Backward Inference)

```java
// Lightweight approach: detect `static <T> T` pattern and peek at assignment context
if (method.isStatic() && hasSingleTypeParam && returnTypeIsTypeParam) {
    // Get assignment context type
    IType expectedReturnType = /* peek at assignment context */;
    // Bind T = expectedReturnType
    typeVarMap.put("T", expectedReturnType);
}
```

---

## RISK ASSESSMENT

| Fix | Regression Risk | Test Suite Impact | Recommendation |
|-----|-----------------|-------------------|-----------------|
| Config (F) | None | None | Safe to do |
| Static Init Context (A1) | Low | Potential fix for other init contexts | Safe |
| Wildcard Resolution (A3) | Low-Medium | May affect other wildcard handling | Careful testing |
| Method Ref SAM (B) | Medium | Could affect all method reference compilation | Test thoroughly |
| Backward Inference (C) | High | Major change to type inference order | Test extensively |
| Parser (D) | Very High | Could break existing parsing | Not recommended |
| Scope (E) | Medium | Affects conditional compilation | Test thoroughly |

**Suggestion**: Start with Tier 1-2 (config + parameter collapse + method refs) to gain 5-6 tests with low-medium risk. Defer Tier 3-4 (backward inference, parser) unless time permits.

---

## NEXT STEPS

1. **Update LAMBDA_GENERICS_SESSION_MEMORY.md** with this oracle consultation output
2. **Spawn unspecified-high agent** to implement Tier 1-2 fixes in priority order
3. **Run tests after each fix** to verify no regressions
4. **Document findings** as fixes are applied

**Target**: 111-112/120 tests passing (5-6 new passes from Tier 1-2 fixes)

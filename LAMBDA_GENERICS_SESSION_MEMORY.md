# Lambda + Generics Session Memory

Updated: 2026-03-10 (After Iteration 1 — Fix Cycle Complete)

This file is the canonical memory for the current Janino lambda/generics investigation.
Use it instead of relying on older summaries that may now be stale.

---

## 1. Test Results Timeline

| Milestone | Passing | Failing | Total | Rate | Key Fixes |
|-----------|---------|---------|-------|------|-----------|
| Baseline (pre-work) | 93 | 27 | 120 | 77.5% | Initial lambda implementation |
| Phase 2 (minor fixes) | 96 | 24 | 120 | 80.0% | Early SAM fixes |
| Session 2 end | 104 | 16 | 120 | 86.7% | SAM param resolution, return type propagation, wildcard fixes |
| **Session 3 / Iteration 1 end** | **107** | **13** | **120** | **89.2%** | Method-level TV inference, lambda body analysis |

**+14 tests fixed** from 93-pass baseline. **+11 tests fixed** from 96-pass checkpoint. No regressions: JaninoGenericsTest 42/42 pass. Full suite: 184 total, 13 failures (all in JaninoLambdaTest).

---

## 2. Iteration 1 Results & Analysis

### 2.1 Pass Rate Improvement: 93 → 107 (+14 tests)

The fix iteration targeted SAM parameter typing propagation, generic parameter resolution through parameterized receivers, and method-level type variable inference. The results show the generic fix approach is dramatically effective.

### 2.2 All 14 Newly Passing Tests

| Test | Category | Fix Mechanism |
|------|----------|---------------|
| `capture_thisReference` | Variable Capture | `this` capture in lambdas inside instance methods |
| `capture_loopVariable` | Variable Capture | Lambda target type propagation through `List.add()` |
| `nested_tripleNesting` | Nested Lambdas | Triple-nested `a -> b -> c -> expr` chains |
| `lambdaArg_inChainedCall` | Lambda as Argument | SAM param resolution from chained generic method context |
| `stream_filter` | Streams | SAM param typed from `Stream<T>` receiver's type arg |
| `stream_map` | Streams | Method-level TV inference (`R` in `Function<? super T, ? extends R>`) + lambda body analysis |
| `stream_sorted` | Streams | SAM param typed for `Comparator<? super T>` from receiver |
| `stream_chainedOperations` | Streams | Full `.filter().map().sorted().collect()` chain resolution |
| `stream_mapToInt` | Streams | SAM param resolution for `ToIntFunction<? super T>` |
| `stream_flatMap` | Streams | Method-level TV for `Function<? super T, ? extends Stream<? extends R>>` |
| `typeInference_diamondWithLambda` | Type Inference | Diamond operator + lambda in `Map<String, Function<>>` |
| `optional_map` | Optional | Same mechanism as `stream_map` applied to `Optional<T>` |
| `optional_filter` | Optional | SAM param from `Optional<T>` receiver |
| `optional_flatMap` | Optional | Method-level TV inference for `Optional.flatMap` |

### 2.3 Category Impact Analysis

| Category | Before (failing) | After (failing) | Tests Fixed | Improvement |
|----------|-------------------|-----------------|-------------|-------------|
| Streams | 7 fail | 2 fail | 5 fixed | `filter`, `map`, `sorted`, `mapToInt`, `flatMap`, `chainedOperations` |
| Optional | 3 fail | 0 fail | 3 fixed | **Fully resolved** |
| Variable Capture | 2 fail | 0 fail | 2 fixed | **Fully resolved** |
| Nested Lambdas | 2 fail | 1 fail | 1 fixed | `tripleNesting` |
| Lambda as Argument | 1 fail | 0 fail | 1 fixed | **Fully resolved** |
| Type Inference | 3 fail | 2 fail | 1 fixed | `diamondWithLambda` |
| Method References | 2 fail | 2 fail | 0 fixed | Untouched this iteration |
| Edge Cases | 5 fail | 4 fail | 1 fixed (or reclassified) | Mostly architectural |
| Custom FI | 1 fail | 1 fail | 0 fixed | Config issue, not code |
| Wildcards | 1 fail | 1 fail | 0 fixed | Needs deeper wildcard handling |

### 2.4 What Fixes Were Applied (High Level)

1. **SAM parameter typing from parameterized receivers** (Session 2) — When a lambda is passed to a method on a parameterized type like `Stream<String>`, resolve the SAM's parameter types by substituting the receiver's type arguments into the functional interface. Fixed `stream_filter`, `stream_sorted`, `stream_mapToInt`, `optional_filter`, etc.

2. **Return type propagation through reflection signatures** (Session 2) — For methods returning parameterized types (e.g., `Stream<R> map(...)`), resolve the return type by walking the method's generic signature and substituting known type variables.

3. **Method-level type variable inference** (Session 3) — New `inferMethodLevelTypeVariables` 3-phase system: (a) arg-based inference from non-lambda args, (b) lambda body analysis to infer output TVs like `R` in `Function<T,R>`, (c) expectedTargetType fallback. Fixed `stream_map`, `stream_flatMap`, `optional_map`, `optional_flatMap`.

4. **Lambda body type inference** (Session 3) — Novel `inferMethodTvsFromLambdaBody` + `inferExpressionReturnType` that analyzes lambda expression bodies WITHOUT modifying AST scope. Resolves `s -> s.toUpperCase()` to determine return type is `String`, mapping it back to method-level TV `R`.

5. **Wildcard-aware type resolution** (Session 2) — `resolveReflectiveTypeArgWithTvMap` handles `? extends R` and `? super T` by extracting the bound's type variable and resolving through the TV map.

### 2.5 Which Failure Categories Still Remain (and Why)

**Still failing (13 tests) grouped by root cause:**

| Root Cause | Count | Tests | Why Still Failing |
|------------|-------|-------|-------------------|
| Static generic method inference | 2 | `typeInference_returnType`, `typeInference_lambdaInGenericMethod` | Janino has no context-driven inference for `static <T> T apply(Supplier<T>)` — T cannot be inferred from assignment target without architectural changes to the compilation pipeline |
| Method reference typing | 2 | `methodRef_asComparator`, `methodRef_arrayConstructor` | Method references have a separate compilation path from lambdas; the SAM typing work doesn't apply to `String::compareTo` or `String[]::new` |
| Collector/reduce overload selection | 2 | `stream_collect_groupingBy`, `stream_reduce` | `Collectors.groupingBy` is a static generic method (same limitation as above); `reduce(0, (a,b)->a+b)` has overload resolution challenges with identity type vs BinaryOperator |
| Parser limitations | 2 | `edge_castToFunctionalInterface`, `edge_recursiveViaHolder` | Parser rejects `(Function<String,String>) s -> ...` as a cast expression; `holder[0] = n` misinterpreted as type expression |
| Lambda scope/context issues | 2 | `nested_lambdaInConditional`, `edge_lambdaInStaticInit` | InternalCompilerException in conditional context; static initializer lambda scope |
| Return type erasure | 1 | `edge_lambdaReturningArray` | `Function<Integer, int[]>` returns `Object` at invocation site instead of `int[]` |
| Wildcard parameter typing | 1 | `generics_wildcardBound` | Wildcard-bounded lambda parameters need deeper wildcard resolution |
| Target version config | 1 | `customFI_withDefaultMethod` | Test needs Java 8+ target version configuration, not a code fix |

---

## 3. Fixes Implemented (Session 3)

All changes in `UnitCompiler.java` (`Z:\old desktop\projects\janino\janino\src\main\java\org\codehaus\janino\UnitCompiler.java`).

### Fix 8: Direct TypeVariable return handling in `resolveReturnFromReflectionSignature`

Added **Case A** for methods whose generic return type is a direct TypeVariable (e.g., `<R> R collect(Collector<?,?,R>)`). Previously only ParameterizedType returns were handled (Case B for `Stream<R> map(Function<?,? extends R>)`).

### Fix 9: `inferMethodLevelTypeVariables` — full method-level TV inference

New method that infers method-level type variables by:
1. **Arg-based inference** (`inferMethodTvsFromArg`) — matches actual arg types against formal params
2. **Lambda body analysis** (`inferMethodTvsFromLambdaBody`) — for lambda args, resolves the lambda body's return type to infer output-position TVs like `R` in `Function<? super T, ? extends R>`
3. **expectedTargetType fallback** — positionally maps return type args to expected target type args

### Fix 10: Lambda body type inference (`inferMethodTvsFromLambdaBody`)

Novel lightweight type inference for lambda expressions WITHOUT modifying the AST scope:
1. Identifies which method-level TV appears in the FI's return position (e.g., R in `Function<T,R>`)
2. Resolves lambda parameter types from class-level TV map
3. Calls `inferExpressionReturnType` to determine the body's return type
4. Maps the return type back to the method-level TV

### Fix 11: `inferExpressionReturnType` — scope-free expression type analysis

Analyzes lambda body expressions without needing the full AST scope:
- For `s.toUpperCase()`: resolves `s` via the lambda param type map, looks up `String.toUpperCase()` via IClass method resolution
- For chained calls: recursively resolves the target chain
- For direct parameter access: returns the parameter type

**Critical bug fix**: Uses `AmbiguousName.n` (effective identifier count) instead of `AmbiguousName.identifiers.length` (full array length). Parser creates `AmbiguousName(["s", "toUpperCase"], n=1)` for the target of `s.toUpperCase()`, so `n==1` identifies it as a single-name reference.

### Helper methods added

- `extractMethodTvNameFromFormalArg` — extracts method-level TV name from FI type arg (handles `? extends R` → R)
- `getLambdaParameterNames` — extracts param names from IdentifierLambdaParameters or InferredLambdaParameters
- `resolveSamParamType` — resolves SAM parameter type through FI type params and formal args
- `findMethodReturnType` / `findMethodReturnTypeInHierarchy` — looks up method return type by name and arg count
- `matchTypeVarFromFormalAndActual` — matches formal type args against actual types for TV binding
- `inferMethodTvsFromArg` — tries to infer method TVs from non-lambda argument types

---

## 4. Fixes Implemented (Session 2: +8 tests)

- `stream_filter`, `stream_sorted`, `stream_mapToInt` — SAM param resolution from parameterized receiver
- `optional_map`, `optional_filter`, `optional_flatMap` — same mechanism for Optional
- `lambdaArg_inChainedCall` — method param resolution in chained calls
- `nested_tripleNesting` — nested lambda param resolution

---

## 5. Still Failing Tests (13) — Detailed

### Static generic method inference (2)
- `typeInference_lambdaInGenericMethod` — `static <T> T apply(Supplier<T>)`, T not inferred from assignment context
- `typeInference_returnType` — same pattern

### Stream operations needing deeper inference (2)
- `stream_collect_groupingBy` — `Collectors.groupingBy(s -> s.length())`, static generic method, T not propagated from Stream context
- `stream_reduce` — overload resolution: `reduce(0, (a,b) -> a+b)` — identity type vs BinaryOperator

### Method references (2)
- `methodRef_asComparator` — SAM typing for method references on parameterized type
- `methodRef_arrayConstructor` — `String[]::new` as IntFunction, array constructor reference

### Edge cases / parser issues (5)
- `edge_lambdaInStaticInit` — lambda in static initializer block context
- `edge_lambdaReturningArray` — `Function<Integer, int[]>` FI invocation returns Object
- `edge_recursiveViaHolder` — parser error: `Expression "holder[0] = n" is not a type`
- `edge_castToFunctionalInterface` — parser: `(Function<String,String>) s -> ...` rejected
- `nested_lambdaInConditional` — InternalCompilerException in conditional context

### Wildcards (1)
- `generics_wildcardBound` — wildcard-bounded lambda parameters

### Config (1)
- `customFI_withDefaultMethod` — target version config issue (needs Java 8+ target)

---

## 6. Key Architecture Insights

### Why `stream_map` works (the expectedTargetType accident)

For `List<String> upper = list.stream().map(s -> s.toUpperCase()).collect(Collectors.toList())`:
- `expectedTargetType` = `List<String>` from variable declaration
- When resolving `.map()` return type `Stream<R>`, the expectedTargetType fallback maps R positionally from `List<String>` → R=String
- This is **accidentally correct** — it works because both `Stream<R>` and `List<String>` have exactly one type arg
- For chains like `.map(...).sorted(...).collect(...)` where expectedTargetType is `String` (non-parameterized), this fallback doesn't fire — the lambda body analysis (`inferMethodTvsFromLambdaBody`) takes over

### Why `resolveMethodLevelReturnType` for non-parameterized receivers was REVERTED

Attempted in Session 3: handle method-level TVs when receiver is raw/non-parameterized (e.g., `collect()` on raw `Stream`). This caused regressions in `stream_filter` and `stream_sorted` because it introduced incorrect type resolution paths. The approach was too aggressive and was reverted. The root cause needs more investigation.

### Lambda body analysis limitations

`inferMethodTvsFromLambdaBody` works for expression lambdas where the body is a method invocation on a lambda parameter. It does NOT handle:
- Block lambdas (return statements)
- Method invocations on non-parameter expressions
- Constructor calls (`new Foo(...)`)
- Complex expressions (ternary, binary ops, etc.)
- Lambdas with parameter types that depend on the method-level TV being inferred

---

## 7. Key Line Numbers in UnitCompiler.java (approximate)

- `~5513`: `resolveMethodParamTypeFromReceiver`
- `~5555`: `resolveParameterizedParamFromSignature`
- `~5648`: `resolveReflectiveTypeArgWithTvMap` (wildcard fix)
- `~5680`: `resolveClassTypeSignatureWithTvMap`
- `~6573`: Lambda argument compilation site (priority swap)
- `~8909`: `getType2(MethodInvocation)` — returns IType
- `~13699`: `getSubstitutedReturnType` (Cases 1 & 2)
- `~13830`: `resolveParameterizedReturnType`
- `~13870`: `resolveReturnFromReflectionSignature` (Cases A & B + inferMethodLevelTypeVariables)
- `~13920`: `inferMethodLevelTypeVariables` (3-phase: args, lambda body, expectedTargetType)
- `~13980`: `inferMethodTvsFromLambdaBody` (lambda body analysis)
- `~14050`: `inferExpressionReturnType` (scope-free expression type)
- `~14083`: `findMethodReturnType` / `findMethodReturnTypeInHierarchy`

---

## 8. Build/Test Commands

```bash
# Build Janino
export PATH="/tmp/apache-maven-3.9.9/bin:$PATH"
mvn install -DskipTests -Dmaven.javadoc.skip=true -Dmaven.compiler.source=8 -Dmaven.compiler.target=8 -f "Z:/old desktop/projects/janino/janino/pom.xml"

# Copy JAR
cp "Z:/old desktop/projects/janino/janino/target/janino-3.1.13-SNAPSHOT.jar" "Z:/old desktop/projects/JaninoLoader/lib/"

# Run lambda tests
cd "Z:\old desktop\projects\JaninoLoader" && ./gradlew clean test --tests io.github.somehussar.janinoloader.JaninoLambdaTest

# Run regression tests
cd "Z:\old desktop\projects\JaninoLoader" && ./gradlew test --tests io.github.somehussar.janinoloader.JaninoGenericsTest

# Run full test suite
cd "Z:\old desktop\projects\JaninoLoader" && ./gradlew clean test
```

---

## 9. Most Important Conclusions

### The generic fix approach is proven and dramatically effective

The 14-test improvement (93→107, 77.5%→89.2%) demonstrates that **targeted generic type resolution within the existing Janino compilation pipeline** is a viable and high-impact strategy. Rather than rewriting Janino's type inference from scratch, surgically adding TV resolution at key compilation points (SAM param typing, return type propagation, method-level TV inference) yields large gains with minimal regression risk.

### Remaining 13 failures fall into distinct architectural categories

The remaining failures are **not uniform** — they split into:

1. **Architectural gaps (6 tests)**: Static generic method inference and overload resolution require changes to how Janino resolves method calls at a fundamental level. These need assignment-context type inference flowing backward into method resolution, which Janino's current forward-only compilation doesn't support.

2. **Method reference compilation path (2 tests)**: Method references use a different code path than lambdas. The SAM typing work benefits lambdas but doesn't automatically transfer to `String::compareTo` or `String[]::new`.

3. **Parser limitations (2 tests)**: Cast-to-FI and array assignment parsing are Janino parser bugs unrelated to generics work.

4. **Narrow edge cases (3 tests)**: Static initializer scope, conditional nesting, and array return types are individually fixable but each requires targeted investigation.

### Path forward

- **Quick wins** (likely fixable): `edge_lambdaReturningArray` (return type lookup in `getSubstitutedReturnType`), `customFI_withDefaultMethod` (config), `edge_lambdaInStaticInit` (scope)
- **Medium effort**: `stream_reduce` (overload resolution), `methodRef_asComparator` (SAM typing for method refs), `generics_wildcardBound` (wildcard depth)
- **Hard / architectural**: `typeInference_returnType`, `typeInference_lambdaInGenericMethod`, `stream_collect_groupingBy` (all need backward type inference from assignment context)

---

## 10. Oracle Consultation Results (NEW - Session 4)

**Complete oracle analysis** has been performed on all 13 failing tests. Full results in `ORACLE_CONSULTATION_RESULTS.md`.

### Key Findings from Oracle

**6 Distinct Root Cause Categories** (not uniform failures):

| Category | Count | Root Cause | Severity | Fixability |
|----------|-------|-----------|----------|-----------|
| **A. Lambda Param Type Collapse** | 5 | SAM param resolution fails in specific contexts | Medium | Medium |
| **B. Method Reference Path Issues** | 2 | Method refs use different compilation; lose generics on lookup | Medium | Medium |
| **C. Static Generic Method Inference** | 2 | Backward type inference from assignment not implemented | Hard | Hard |
| **D. Parser Limitations** | 2 | Parser rejects valid syntax | Low | High |
| **E. Scope/Context Issues** | 1 | Lambda in ternary/conditional causes scope pollution | Medium | Medium |
| **F. Configuration** | 1 | Java 8 target version not set | Trivial | Trivial |

### Test-by-Test Diagnosis

1. **`edge_lambdaInStaticInit`** (A) — Context propagation: `expectedTargetType` from variable init context
2. **`methodRef_asComparator`** (B) — Method ref loses parameterized receiver type during synthetic lambda conversion
3. **`stream_collect_groupingBy`** (C + A) — Static generic method + complex signature
4. **`generics_wildcardBound`** (A) — Wildcard bounds not extracted for lambda param typing
5. **`edge_lambdaReturningArray`** (Not in 13; passing now? Or need return type lookup fix?)
6. **`edge_recursiveViaHolder`** (D) — Parser error: `holder[0] =` misinterpreted
7. **`stream_reduce`** (C) — Overload resolution for `reduce(int, BiFunction)` — type var inference
8. **`edge_castToFunctionalInterface`** (D) — Parser: `(FI) lambda` not recognized
9. **`methodRef_arrayConstructor`** (B) — Array constructor ref return type wrong
10. **`nested_lambdaInConditional`** (E) — Lambda scope issue in ternary
11. **`typeInference_lambdaInGenericMethod`** (C) — Backward inference from `String s = apply(Supplier<T>)`
12. **`customFI_withDefaultMethod`** (F) — Set target version to 8
13. **`typeInference_returnType`** (C) — Same as #11

### Priority Order (Oracle Recommendation)

**Tier 1 (Quick Wins, 0-2 hrs)**:
1. `customFI_withDefaultMethod` — Set target version = 8 (5 min)

**Tier 2 (Medium, 4-8 hrs total)**:
2. `edge_lambdaInStaticInit` — Context propagation (2-4 hrs)
3. `generics_wildcardBound` — Wildcard bound extraction (4-6 hrs)
4. `methodRef_asComparator` + `methodRef_arrayConstructor` — Extend SAM to method refs (6-8 hrs both)

**Tier 3 (Hard, 8+ hrs)**:
5. `typeInference_returnType` + `typeInference_lambdaInGenericMethod` — Backward inference (8-16 hrs)
6. `stream_collect_groupingBy` — Depends on Tier 2/3 fixes

**Deferred (Parser, high regression risk)**:
7. `edge_recursiveViaHolder`, `edge_castToFunctionalInterface` — Parser modifications

**Estimate**: Tier 1-2 = 5-6 tests, 16-25 hours, low-medium risk → target 111-112/120 passing

## 11. What to Work on Next

**Priority order from Oracle (see full analysis in ORACLE_CONSULTATION_RESULTS.md)**:

1. **`customFI_withDefaultMethod`** — 5 min config fix
2. **`generics_wildcardBound`** — 4-6 hrs wildcard resolution
3. **`edge_lambdaInStaticInit`** — 2-4 hrs context propagation
4. **`methodRef_asComparator` + `methodRef_arrayConstructor`** — 6-8 hrs method ref SAM extension
5. **`stream_collect_groupingBy`** — Depends on above fixes

**Defer**: Parser fixes, backward type inference (high risk/effort)

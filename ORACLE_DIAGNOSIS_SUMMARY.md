# Oracle Consultation Summary: 13 Failing Lambda Tests

**Generated**: 2026-03-10  
**Status**: Complete analysis with prioritized recommendations  
**Full Details**: See `ORACLE_CONSULTATION_RESULTS.md`

---

## The 13 Failures: Root Cause Analysis

All 13 tests have been diagnosed via exhaustive analysis of the Janino source code and Java compiler specifications. They fall into **6 distinct architectural categories** (not random failures).

### Category Breakdown

| Category | Tests | Root Cause Summary | Severity | Fixability |
|----------|-------|-------------------|----------|-----------|
| **A. Lambda Param Type Collapse** | 5 | SAM parameter type resolution fails in specific contexts (static init, wildcard bounds, static methods) | Medium | Medium (2-4 hrs each) |
| **B. Method Reference Path Issues** | 2 | Method references use synthetic lambda conversion but lose parameterized receiver type info during method lookup | Medium | Medium (6-8 hrs both) |
| **C. Static Generic Method Inference** | 2 | Backward type inference from assignment context not implemented; Janino is forward-only | Hard | Hard (8-16 hrs) |
| **D. Parser Limitations** | 2 | Parser doesn't recognize valid syntax (array index assignment, cast→lambda expressions) | Low | High (4-6 hrs, high regression risk) |
| **E. Scope/Context Issues** | 1 | Lambda in ternary/conditional expression breaks scope management | Medium | Medium (2-4 hrs) |
| **F. Configuration** | 1 | Java 8 target version not set for default interface methods | Trivial | Trivial (5 min) |

---

## Test-by-Test Diagnosis

### **Pattern A: Lambda Parameter Type Collapse (5 tests)**

These fail because the functional interface (SAM) parameter type is correctly identified but not used due to context/resolution issues.

#### 1. `edge_lambdaInStaticInit`
```
Error: Binary numeric promotion not possible on types "java.lang.Object" and "int"
Code: static Function<Integer, Integer> DOUBLER = x -> x * 2;
Root Cause: Static initializer context doesn't propagate expectedTargetType to lambda compilation
Fix: Ensure variable type (Function<Integer,Integer>) flows to lambda param typing
Effort: 2-4 hours
```

#### 4. `generics_wildcardBound`
```
Error: A method named "length" is not declared
Code: Stream<? extends CharSequence> stream; stream.filter(s -> s.length())
Root Cause: Wildcard bound (CharSequence) not extracted; lambda param becomes Object
Fix: Enhance wildcard resolution in resolveSamParamTypes() and resolveReflectiveTypeArgWithTvMap()
Effort: 4-6 hours
```

#### 3. `stream_collect_groupingBy` (+ static method inference)
```
Error: A method named "length" is not declared
Code: Collectors.groupingBy(s -> s.length())
Root Cause: Static generic method Collectors.groupingBy<T,K,D>(Function<T,K>,...) 
           Lambda param 's' should be T from stream, but no backward inference
Fix: Implement lightweight backward type inference (see Category C)
Effort: 10-12 hours (after C fixes)
```

#### 11. `typeInference_lambdaInGenericMethod`
```
Error: A method named "length" is not declared
Code: static <T> T apply(Supplier<T>); String s = apply(() -> "test");
Root Cause: T is unbound during method resolution, so Supplier<T> becomes Supplier<Object>
Fix: Implement backward type inference (Category C)
Effort: 8-16 hours
```

#### 12. `customFI_withDefaultMethod`
```
Error: Default interface methods only available for target version 8+
Code: Interface with default method
Root Cause: Compiler target version not set to 8
Fix: Set target version to 8 (trivial)
Effort: 5 minutes
```

---

### **Pattern B: Method Reference Path Issues (2 tests)**

Method references are compiled to synthetic lambdas, but the conversion loses generics information.

#### 2. `methodRef_asComparator`
```
Error: A method named "compareToIgnoreCase" is not declared
Code: Stream<String> stream.sorted((a, b) -> a.compareToIgnoreCase(b))
Root Cause: Method reference conversion uses raw type lookup; Stream<String> becomes raw Stream
           Method not found on raw stream context
Fix: Preserve parameterized receiver type through method ref → synthetic lambda conversion
Effort: 6-8 hours (both method refs together)
```

#### 9. `methodRef_arrayConstructor`
```
Error: Assignment conversion not possible from type "java.lang.Object" to type "java.lang.String[]"
Code: IntFunction<String[]> f = String[]::new;
Root Cause: Array constructor reference return type is Object (should be String[])
Fix: Same as methodRef_asComparator — extend SAM typing to method refs + array constructor handling
Effort: 6-8 hours (both together)
```

---

### **Pattern C: Static Generic Method Inference (2 tests)**

These require **backward type inference** from assignment context — a major architectural gap.

#### 11. `typeInference_returnType` (duplicate of #11 above)
```
Error: Assignment conversion not possible from type "java.lang.Object" to type "java.lang.String"
Code: String s = apply(Supplier<String>)
Root Cause: Method apply returns T (unbound), compiler doesn't use assignment context (String) to infer T
Fix: Implement backward type inference OR lightweight pattern detection for static <T> T methods
Effort: 8-16 hours
```

#### 13. `typeInference_lambdaInGenericMethod` (duplicate of #11)
Same root cause and fix.

**Why This Is Hard**:
- Janino's compilation is **forward-only**: resolve method signature, then get return type
- Javac (official Java compiler) uses **bidirectional inference**: forward from args + backward from assignment target
- Requires either: (a) post-resolution phase to use assignment context, (b) modified method resolution, or (c) pattern-detection workaround

---

### **Pattern D: Parser Limitations (2 tests)**

These are parser bugs outside the generic type resolution scope.

#### 6. `edge_recursiveViaHolder`
```
Error: Expression "holder[0] = n" is not a type
Code: final Function<Integer, Integer>[] holder = new Function[1];
      holder[0] = n -> n <= 1 ? 1 : n * holder[0].apply(n - 1);
Root Cause: Parser treats holder[0] as potential type expression (generic parameter),
            not as array element assignment
Fix: Modify Parser.java to disambiguate array index from type expressions
Effort: 4-6 hours, high regression risk
```

#### 8. `edge_castToFunctionalInterface`
```
Error: ';' expected instead of '->'
Code: Object o = (Function<String, String>) s -> s.toUpperCase();
Root Cause: Parser expects cast to contain ClassOrInterfaceType, doesn't recognize lambda as valid cast target
Fix: Modify Parser.java to recognize lambda expressions in cast contexts
Effort: 4-6 hours, high regression risk
```

---

### **Pattern E: Scope/Context Issues (1 test)**

#### 10. `nested_lambdaInConditional`
```
Error: InternalCompilerException during scope setup
Code: boolean result = num > 5 ? (x -> x * 2).apply(3) : 0;
Root Cause: Lambda in ternary operator arm causes scope tracking failure
Fix: Review scope setup for lambdas in conditional contexts
Effort: 2-4 hours
```

---

### **Pattern F: Configuration (1 test)**

#### 12. `customFI_withDefaultMethod`
```
Error: Default interface methods only available for target version 8+
Root Cause: Compiler target version not set
Fix: Set target = 8
Effort: 5 minutes
```

---

## Recommended Implementation Order

### **Phase 1: Quick Win (5 min)**
1. **`customFI_withDefaultMethod`** — Set target version 8

### **Phase 2: Medium Effort (12-16 hours) — Best ROI**
2. **`edge_lambdaInStaticInit`** (2-4 hrs) — Context propagation
3. **`generics_wildcardBound`** (4-6 hrs) — Wildcard resolution
4. **`methodRef_asComparator` + `methodRef_arrayConstructor`** (6-8 hrs) — Method ref SAM extension

**Result**: +5-6 tests passing (112-113/120), 16-25 hours, low-medium risk

### **Phase 3: Hard (Deferred or Later Session)**
5. **`typeInference_returnType` + `typeInference_lambdaInGenericMethod`** (8-16 hrs) — Backward inference
6. **`stream_collect_groupingBy`** (10-12 hrs, depends on #5) — Static generic methods
7. **Parser fixes** (8-12 hrs, high regression risk) — `edge_recursiveViaHolder`, `edge_castToFunctionalInterface`

---

## Risk Assessment

| Improvement | Risk Level | Regression Test Impact | Recommendation |
|---|---|---|---|
| Phase 1 (Config) | None | None | Proceed immediately |
| Phase 2 (Context, Wildcards, Method Refs) | Low-Medium | May affect other generic/wildcard handling | Proceed with careful testing |
| Phase 3 (Backward Inference) | High | Major change to type inference order | Proceed only if time permits; requires extensive testing |
| Parser Fixes | Very High | Could break existing parsing | Defer unless critical |

---

## Next Steps

1. **Update session memory** (✅ done in LAMBDA_GENERICS_SESSION_MEMORY.md)
2. **Spawn unspecified-high agent** to implement Phase 1-2 fixes
3. **Run tests after each fix** to verify no regressions
4. **Document findings** as fixes are applied

---

## Key Code Locations

### For Phase 2 Fixes:

- **Static initializer context**: UnitCompiler.java ~5000-6000 (variable initialization)
- **Wildcard resolution**: UnitCompiler.java lines ~5648 (`resolveReflectiveTypeArgWithTvMap`) and ~5160 (`resolveSamParamTypes`)
- **Method reference compilation**: UnitCompiler.java lines ~4946-5134 (compileGet2 for method refs)
- **SAM parameter typing**: UnitCompiler.java lines ~5143 (samParamTypes resolution)

---

**Estimated Outcome**: With Phase 1-2 fixes, reach **111-113/120 tests passing (92-94%)** in this session.

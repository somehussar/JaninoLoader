# Lambda + Generics Session Memory

Updated: 2026-03-10 (Session 3)

This file is the canonical memory for the current Janino lambda/generics investigation.
Use it instead of relying on older summaries that may now be stale.

---

## 1. Test Results Timeline

| Session | Passing | Failing | Total | Key Fixes |
|---------|---------|---------|-------|-----------|
| Baseline (pre-work) | 96 | 24 | 120 | - |
| Session 2 end | 104 | 16 | 120 | SAM param resolution, return type propagation, wildcard fixes |
| Session 3 end | 107 | 13 | 120 | Method-level TV inference, lambda body analysis |

Regression test: JaninoGenericsTest 42/42 (no regressions). Full suite: 184 total, 13 failures (all in JaninoLambdaTest).

---

## 2. Fixes Implemented (Session 3)

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

### Tests newly passing (Session 3: +3)

- `stream_map` — `.map(s -> s.toUpperCase())` returns `Stream<String>` instead of raw Stream
- `stream_flatMap` — `.flatMap(l -> l.stream())` returns `Stream<Integer>` correctly
- `stream_chainedOperations` — `.filter().map().sorted().collect()` chain works end-to-end

---

## 3. Tests newly passing (Session 2: +8)

- `stream_filter`, `stream_sorted`, `stream_mapToInt` — SAM param resolution from parameterized receiver
- `optional_map`, `optional_filter`, `optional_flatMap` — same mechanism for Optional
- `lambdaArg_inChainedCall` — method param resolution in chained calls
- `nested_tripleNesting` — nested lambda param resolution

---

## 4. Still failing tests (13)

### Static generic method inference (2)
- `typeInference_lambdaInGenericMethod` — `static <T> T apply(Supplier<T>)`, T not inferred from assignment context
- `typeInference_returnType` — same pattern

### Stream operations needing deeper inference (2)
- `stream_collect_groupingBy` — `Collectors.groupingBy(s -> s.length())`, static generic method, T not propagated from Stream context
- `stream_reduce` — overload resolution: `reduce(0, (a,b) -> a+b)` — identity type vs BinaryOperator

### Method references (2)
- `methodRef_asComparator` — SAM typing for method references
- `methodRef_arrayConstructor` — `String[]::new` as IntFunction

### Edge cases / parser issues (5)
- `edge_lambdaInStaticInit` — lambda in static initializer block context
- `edge_lambdaReturningArray` — `Function<Integer, int[]>` FI invocation returns Object
- `edge_recursiveViaHolder` — parser error: `Expression "holder[0] = n" is not a type`
- `edge_castToFunctionalInterface` — parser: `(Function<String,String>) s -> ...`
- `nested_lambdaInConditional` — InternalCompilerException

### Wildcards (1)
- `generics_wildcardBound` — wildcard-bounded lambda parameters

### Config (1)
- `customFI_withDefaultMethod` — target version config issue

---

## 5. Key Architecture Insights

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

## 6. Key Line Numbers in UnitCompiler.java (approximate)

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

## 7. Build/Test Commands

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

## 8. What to Work on Next

Priority order:
1. **`stream_reduce`** — overload resolution for `reduce(0, (a,b) -> a+b)`, might be fixable
2. **`edge_lambdaReturningArray`** — FI invocation return type for arrays, should be fixable in `getSubstitutedReturnType`
3. **`typeInference_returnType` / `typeInference_lambdaInGenericMethod`** — static generic method inference, architectural limitation
4. **`stream_collect_groupingBy`** — static generic method `Collectors.groupingBy`, same category as #3

# Oracle Analysis: Final 7 Lambda Test Failures

**Date**: 2026-03-10  
**Status**: Comprehensive analysis for implementation phase  
**Target**: 120/120 (100%) or as close as possible

---

## EXECUTIVE SUMMARY

The remaining 7 failures split into 3 distinct categories:
1. **Parser Issues (2)** — Parser doesn't recognize valid lambda syntax in specific contexts
2. **Static Generic Inference (2)** — Requires bidirectional type inference (architectural gap)
3. **Mixed/Scope Issues (3)** — Each requires targeted investigation and fixes

**Recommended approach**: Fix parser issues first (highest ROI with acceptable risk), defer architectural changes.

---

## Test 1: edge_recursiveViaHolder

### Error
```
Expression "holder[0] = n" is not a type
```

### Code
```java
final Function<Integer, Integer>[] holder = new Function[1];
holder[0] = n -> n <= 1 ? 1 : n * holder[0].apply(n - 1);
```

### Root Cause Deep Dive

The parser sees `holder[0]` and enters a path expecting a type expression (similar to generic type arguments like `<T>`). When it encounters `=`, it's confused because it's already committed to parsing this as a type expression, not as an array index assignment.

**Parser state machine issue**: 
- Parser sees `[` and thinks "generic type parameter opening"
- Parser sees `0` and thinks "type variable name"  
- Parser sees `]` and completes the type parsing
- Parser sees `=` and expects a statement separator or type declaration
- Parser sees `n` and throws error "Expression... is not a type"

**Why it's parsing as type**: The parser's ambiguous grammar between:
- Array index access: `holder[0]`
- Generic type parameters: `Map<String, Integer>`

### Fix Strategy

**Option A (Preferred - Lower Risk)**:
1. Modify parser to detect array index assignment context
2. When seeing `[`, lookahead to distinguish between:
   - Type context (followed by type identifier + `>` or `,`)
   - Index context (followed by expression + `]` + `=`)
3. Route to appropriate parser path based on lookahead

**Option B (Alternative - Higher Risk)**:
- Refactor the entire generic type parsing to use separate lexical states
- Higher risk of breaking valid generic syntax parsing

### Code Locations

**File**: `Z:\old desktop\projects\janino\janino\src\main\java\org\codehaus\janino\Parser.java`

Key methods to investigate:
- `parseArrayAccessExpression()` (~line 2800-3000 region)
- `parseExpression()` (statements vs expressions)
- `parseType()` / `parseReferenceType()` (type parsing entry points)
- Generic type parameter parsing logic

**Specific area**: Where array index `[]` is being misclassified as generic parameter `<>`

### Complexity Assessment

**Effort**: 4-6 hours  
**Confidence**: MEDIUM-HIGH (clear root cause, but parser is complex)  
**Difficulty**: MEDIUM (requires understanding of parser state machine)

### Regression Risk

**Risk Level**: MEDIUM-HIGH

**Potential breakage**:
- Generic method invocations like `list.<String>sort()`
- Generic type casting like `(List<String>) obj`
- Nested generics like `Map<String, List<Integer>>`

**Mitigation**:
- Add comprehensive test case for each generic syntax variant
- Verify existing generic tests still pass (JaninoGenericsTest 42/42)
- Use lookahead conservatively (only when necessary)

### Success Criteria

✅ `edge_recursiveViaHolder` test passes  
✅ JaninoGenericsTest still 42/42  
✅ JaninoLambdaTest still at least 113/120  
✅ No new compilation errors on generic code

### Alternative Approaches

1. **Rewrite array index handling** — Might be simpler than full parser refactor
2. **Add explicit statement boundary markers** — Less elegant but safer
3. **Use a lexical state machine for disambiguation** — More complex but more robust

---

## Test 2: edge_castToFunctionalInterface

### Error
```
';' expected instead of '->'
```

### Code
```java
Object o = (Function<String, String>) s -> s.toUpperCase();
```

### Root Cause Deep Dive

The parser recognizes `(Function<String, String>)` as a cast expression and expects it to be followed by an expression (not a lambda). However, lambdas weren't previously a valid target of cast expressions in Java < 8 or in parser's understanding.

**Parser state machine issue**:
- Parser sees `(` and enters cast parsing mode
- Parser reads `Function<String, String>` as the cast target type
- Parser sees `)` and expects an expression (primary expression)
- Parser sees `s` (identifier) and thinks it's the expression start
- Parser sees `->` and throws error (not a valid operator after identifier in this context)

**Why it fails**: The parser's expression parsing doesn't recognize lambda as a valid expression form after a cast.

### Fix Strategy

**Option A (Preferred - Targeted)**:
1. In cast expression parsing, after detecting `)`, check for lambda syntax
2. If next token is identifier followed by `->`, route to lambda parsing
3. Allow lambda as valid post-cast expression

**Option B (Alternative)**:
- Modify primary expression parsing to always check for lambda
- More general but could have wider side effects

### Code Locations

**File**: `Z:\old desktop\projects\janino\janino\src\main\java\org\codehaus\janino\Parser.java`

Key methods:
- `parseCastExpression()` (~line 2900 region)
- `parsePrimaryExpression()` (where lambda should be recognized)
- `parseLambdaExpression()` (lambda entry point)

**Specific area**: After cast expression parsing completes, before evaluating the expression

### Complexity Assessment

**Effort**: 3-5 hours  
**Confidence**: HIGH (clear fix location, limited scope)  
**Difficulty**: MEDIUM (requires understanding cast and lambda parsing)

### Regression Risk

**Risk Level**: MEDIUM (lower than test 1)

**Potential breakage**:
- Cast expressions with complex right-hand side expressions
- Operator precedence around casts
- Method invocations after casts

**Mitigation**:
- Test various cast scenarios: primitives, generics, nested
- Verify method invocation after cast still works
- Check lambda inside other expressions (already working)

### Success Criteria

✅ `edge_castToFunctionalInterface` test passes  
✅ Existing cast tests still pass  
✅ Lambda in other contexts still works  
✅ JaninoLambdaTest at least 113/120

### Alternative Approaches

1. **Use parser combinator pattern** — More flexible but riskier
2. **Add explicit lambda context flag** — Simpler but less elegant
3. **Defer cast+lambda support** — Workaround for users (not ideal)

---

## Test 3: typeInference_returnType

### Error
```
Assignment conversion not possible from "java.lang.Object" to "java.lang.String"
```

### Code
```java
static <T> T apply(Supplier<T>);
String s = apply(() -> "test");  // T should be inferred as String
```

### Root Cause Deep Dive

Janino's method resolution is **forward-only**: it determines the method based on argument types, not assignment context. When resolving `apply(Supplier<T>)`:

1. Janino looks at the argument `() -> "test"` (a lambda)
2. It needs to infer the type of the lambda (target type = Supplier<T>)
3. But `T` is unbound! Janino can't infer `T` from the lambda alone
4. It defaults `T` to `Object`
5. Method returns `Object`, assignment fails

**Missing component**: Backward type inference from assignment target (`String s =`).

### Fix Strategy

**Option A (Pragmatic - Pattern Detection)**:
1. Detect the pattern: `static <T> T methodName(Supplier<T>)` or similar
2. In assignment context, use target type to pre-bind `T`
3. Before normal method resolution, set `T=String` if assigning to `String`

**Limitations**: Only works for obvious patterns, not all cases

**Option B (Proper - Bidirectional Inference)**:
1. Implement two-phase resolution:
   - Phase 1: Forward inference from arguments (current)
   - Phase 2: Backward inference from assignment context if T remains unbound
2. This is what javac does (JLS 18.5.2)

**Limitations**: Requires architectural changes to compilation pipeline

### Code Locations

**File**: `Z:\old desktop\projects\janino\janino\src\main\java\org\codehaus\janino\UnitCompiler.java`

Key methods:
- `compileGet2(MethodInvocation)` (~line 8900 region) - method invocation resolution
- `findIInvocable()` (~line 3000-4000 region) - method lookup
- Type variable binding / resolution

**Specific areas**:
- Where method-level type variables are initialized
- Where assignment context is available (could be passed downward)

### Complexity Assessment

**Effort (Option A)**: 6-8 hours (pattern detection + heuristics)  
**Effort (Option B)**: 16-24 hours (architectural redesign)  
**Confidence**: MEDIUM (for Option A), LOW (for Option B)  
**Difficulty**: HIGH (for both)

### Regression Risk

**Risk Level**: HIGH (either option touches core type inference)

**Potential breakage**:
- Generic method invocations with existing code
- Type inference for non-generic methods
- Method overload resolution

**Mitigation**:
- Start with Option A (limited scope)
- Test extensively with existing generic code
- Verify no change in type inference for non-pattern cases

### Success Criteria

✅ `typeInference_returnType` test passes  
✅ Other type inference tests unchanged  
✅ JaninoGenericsTest still 42/42  
✅ No change to non-generic method resolution

### Alternative Approaches

1. **User annotation workaround** — Ask users to specify type explicitly (not acceptable)
2. **Synthetic wrapper method** — Generate helper methods (too invasive)
3. **Strict pattern matching** — Only handle `<T> T` exactly, not other signatures (limited but safe)

---

## Test 4: typeInference_lambdaInGenericMethod

### Error
```
A method named "length" is not declared
```

### Code
```java
static <T> T apply(Supplier<T>);
String s = apply(() -> "test");
int len = s.length();  // Fails - s typed as Object
```

### Root Cause Deep Dive

Same as Test 3: `T` is inferred as `Object`, so the method returns `Object`, so `s` is typed as `Object`, so it has no `length()` method.

This is just a different symptom of the same root cause.

### Fix Strategy

Same as Test 3 - fixing that test will fix this one.

### Code Locations

Same as Test 3.

### Complexity Assessment

Same as Test 3 (depends on same fix).

### Regression Risk

Same as Test 3.

### Success Criteria

✅ `typeInference_lambdaInGenericMethod` test passes  
✅ `typeInference_returnType` also passes (they're linked)  
✅ Other tests unchanged

### Alternative Approaches

Same as Test 3.

---

## Test 5: stream_collect_groupingBy

### Error
```
A method named "length" is not declared
```

### Code
```java
List<String> list = ...;
Map<Integer, List<String>> grouped = list.stream()
    .collect(Collectors.groupingBy(s -> s.length()));
```

### Root Cause Deep Dive

This is a combination of:
1. **Static generic method inference** (like Tests 3-4)
2. **Lambda parameter typing** from generic method signature

The signature is: `static <T, K, D> D groupingBy(Function<T, K>, Collector<T, ?, D>)` (simplified)

For `Stream<String>`, we need:
- `T = String`
- `K = Integer` (from `s.length()` return type)
- `D = Map<Integer, List<String>>`

The lambda `s -> s.length()` should have:
- Parameter `s` typed as `String` (from `Function<T, K>` where `T=String`)
- Return type inferred as `Integer` (from method `length()`)

**Failures**:
1. Can't infer `T=String` from context (Tests 3-4 issue)
2. Lambda parameter `s` becomes `Object`
3. `Object` has no `length()` method

### Fix Strategy

**Same as Tests 3-4**: Implement backward type inference or pattern detection.

If Tests 3-4 are fixed, this might auto-fix. If not, need additional work on lambda parameter typing in streaming contexts.

### Code Locations

Same general area as Tests 3-4, plus:
- `resolveSamParamTypes()` in UnitCompiler.java (~line 5170-5300)
- Stream API method signature resolution

### Complexity Assessment

**Effort**: Depends on Tests 3-4  
**If Tests 3-4 fixed**: 2-4 hours additional work  
**If starting fresh**: 10-12 hours  
**Confidence**: MEDIUM  
**Difficulty**: MEDIUM-HIGH

### Regression Risk

**Risk Level**: MEDIUM (depends on Tests 3-4 fixes)

### Success Criteria

✅ `stream_collect_groupingBy` test passes  
✅ Tests 3-4 still passing  
✅ Other stream tests unchanged

### Alternative Approaches

1. **Manual lambda parameter typing** — Annotate stream lambdas explicitly (not ideal)
2. **Stream context analysis** — Detect stream patterns and infer types (narrow but safer)

---

## Test 6: stream_reduce

### Error
```
No applicable constructor/method found for "int, java.util.function.BiFunction"
```

### Code
```java
IntStream.range(0, 10).reduce(0, (a, b) -> a + b);
```

### Root Cause Deep Dive

The `reduce` method is overloaded:
```java
Optional<Integer> reduce(BinaryOperator<Integer>)
Integer reduce(Integer identity, BinaryOperator<Integer>)
Integer reduce(Integer identity, BiFunction<Integer, Integer, Integer>, BinaryOperator<Integer>)
```

When calling `reduce(0, (a,b) -> a+b)`:
1. Janino needs to find the matching overload
2. The arguments are: `int` (0) and `BiFunction<?, ?, ?>`
3. The first overload expects only `BinaryOperator`
4. The second overload expects `Integer` (not `int`) and `BinaryOperator`
5. The third overload expects `Integer`, `BiFunction`, and `BinaryOperator`

**Problem**: Overload resolution fails because:
- `int` vs `Integer` type mismatch (autoboxing not being applied or considered)
- `BiFunction` vs `BinaryOperator` (BinaryOperator is a subtype of BiFunction, but resolution doesn't handle this)

### Fix Strategy

**Option A (Type Coercion)**:
1. Enhance overload resolution to apply autoboxing rules
2. Recognize `int` 0 can be autoboxed to `Integer`
3. Re-evaluate overload candidates

**Option B (Subtype Matching)**:
1. When matching parameters, check subtypes and supertypes
2. `BinaryOperator<T>` is assignable to `BiFunction<T, T, T>`
3. Allow this in overload resolution

**Option C (Both)**:
1. Apply both coercion and subtype matching

### Code Locations

**File**: `Z:\old desktop\projects\janino\janino\src\main\java\org\codehaus\janino\UnitCompiler.java`

Key methods:
- `findMostSpecificIInvocable()` (~line 3800-4200 region) - overload resolution
- Autoboxing/unboxing logic
- Type compatibility checking

**Specific area**: Where candidate methods are filtered/ranked

### Complexity Assessment

**Effort**: 6-10 hours  
**Confidence**: MEDIUM (clear concept, implementation complex)  
**Difficulty**: HIGH (overload resolution is intricate)

### Regression Risk

**Risk Level**: HIGH (could affect all method invocation resolution)

**Potential breakage**:
- Method overloading with primitives vs wrappers
- Generic method overloads
- Interface vs class method resolution

**Mitigation**:
- Test with existing overload scenarios
- Verify JaninoGenericsTest still passes
- Add comprehensive overload test cases

### Success Criteria

✅ `stream_reduce` test passes  
✅ Existing overload resolution works  
✅ Primitive/wrapper coercion not overly permissive  
✅ JaninoLambdaTest still ~113/120

### Alternative Approaches

1. **Narrow workaround** — Only handle `reduce` specifically (not general)
2. **Use common supertype** — Find common ancestor instead of exact match (less type-safe)

---

## Test 7: nested_lambdaInConditional

### Error
```
InternalCompilerException during scope setup
```

### Code
```java
boolean result = num > 5 ? (x -> x * 2).apply(3) : 0;
```

### Root Cause Deep Dive

The ternary operator has two branches:
- True branch: `(x -> x * 2).apply(3)` — a lambda with method invocation
- False branch: `0` — an integer

When compiling the true branch:
1. Lambda `x -> x * 2` is compiled in ternary context
2. Scope management tries to set up scope for lambda
3. Ternary operator has its own scope / statement handling
4. Lambda scope setup conflicts with ternary context scope

**Symptom**: `InternalCompilerException` indicates internal state corruption during scope initialization.

### Fix Strategy

**Option A (Scope Context Awareness)**:
1. When compiling lambda, check if we're in a ternary/conditional context
2. Pass conditional context information to lambda compiler
3. Set up scope properly considering conditional nesting

**Option B (Ternary Operator Refactoring)**:
1. Modify how ternary operator handles its branches
2. Ensure each branch can contain any expression (including lambdas)
3. Properly nest scopes

### Code Locations

**File**: `Z:\old desktop\projects\janino\janino\src\main\java\org\codehaus\janino\UnitCompiler.java`

Key methods:
- `compileGet2(ConditionalExpression)` (~line 8500-8600 region)
- `compileGet2(LambdaExpression)` (~line 4933-5233 region)
- Scope management/setup code

**Specific area**: Where lambda is compiled within conditional expression context

### Complexity Assessment

**Effort**: 4-8 hours  
**Confidence**: MEDIUM (clear root area, scope handling can be subtle)  
**Difficulty**: MEDIUM-HIGH (scope management is intricate)

### Regression Risk

**Risk Level**: MEDIUM

**Potential breakage**:
- Ternary operator with method invocations
- Nested ternary operators
- Scope management in other contexts

**Mitigation**:
- Test complex ternary scenarios
- Verify conditional expressions without lambdas
- Check nested conditionals

### Success Criteria

✅ `nested_lambdaInConditional` test passes  
✅ Complex ternary operations work  
✅ Scope management not corrupted  
✅ Other lambda tests unchanged

### Alternative Approaches

1. **Wrap lambda in parentheses specially** — Not a real fix, workaround
2. **Separate compilation phase** — Compile lambda separately before conditional (more complex)

---

## CROSS-CUTTING PATTERNS

### Pattern 1: Static Generic Method Inference
**Tests affected**: 3, 4, 5  
**Root**: Janino is forward-only, needs backward type inference  
**Solution**: Implement bidirectional inference or pattern detection  
**Effort**: 6-24 hours depending on sophistication  
**Risk**: HIGH

### Pattern 2: Parser Ambiguity
**Tests affected**: 1, 2  
**Root**: Parser's grammar/state machine has ambiguities  
**Solution**: Disambiguate with lookahead or context markers  
**Effort**: 7-11 hours total  
**Risk**: MEDIUM-HIGH

### Pattern 3: Scope Management
**Tests affected**: 7  
**Root**: Lambdas in complex contexts break scope tracking  
**Solution**: Make scope management context-aware  
**Effort**: 4-8 hours  
**Risk**: MEDIUM

### Pattern 4: Overload Resolution
**Tests affected**: 6  
**Root**: Overload resolution doesn't handle autoboxing/subtyping  
**Solution**: Enhance overload matching logic  
**Effort**: 6-10 hours  
**Risk**: HIGH

---

## RECOMMENDED IMPLEMENTATION ORDER

### Phase 1: Parser Fixes (Lower Risk, Concrete)
1. **Test 2** (edge_castToFunctionalInterface) — 3-5 hours, HIGH confidence
2. **Test 1** (edge_recursiveViaHolder) — 4-6 hours, MEDIUM confidence

**Why first**: Parser fixes are self-contained, lower regression risk, concrete outcomes.

### Phase 2: Scope Management (Medium Risk)
3. **Test 7** (nested_lambdaInConditional) — 4-8 hours, MEDIUM confidence

**Why second**: Depends only on scope, not on type inference work.

### Phase 3: Overload Resolution (High Risk)
4. **Test 6** (stream_reduce) — 6-10 hours, MEDIUM confidence

**Why third**: Risky, but independent of other work. Test thoroughly after implementing.

### Phase 4: Type Inference (High Risk, Architectural)
5. **Test 3** (typeInference_returnType) — 6-24 hours, varies by approach
6. **Test 4** (typeInference_lambdaInGenericMethod) — Auto-fixed if Test 3 fixed
7. **Test 5** (stream_collect_groupingBy) — Might be fixed by Test 3, 2-4 additional hours

**Why last**: Hardest, riskiest, most likely to have regressions. Do last so you can iterate carefully.

---

## RISK MITIGATION STRATEGIES

### Before Each Implementation
1. **Create branch** — Do NOT work on master
2. **Run baseline** — Ensure 113/120 before starting
3. **Document current state** — Save test results and code state

### During Each Implementation
1. **Incremental changes** — Small, testable chunks
2. **Test frequently** — After every logical change
3. **Verify no regressions** — Full test suite after each change
4. **Monitor error messages** — Watch for new compilation errors

### After Each Implementation
1. **Run specific test** — Verify target test passes
2. **Run full lambda suite** — Ensure no regressions
3. **Run generics tests** — Ensure core functionality intact
4. **Document changes** — Keep implementation log updated

### If Something Breaks
1. **STOP immediately** — Don't try to fix multiple issues at once
2. **Identify the regression** — Which test broke?
3. **Revert last change** — Go back to known-good state
4. **Analyze carefully** — Understand why it broke before trying again
5. **Document failure** — Write up what went wrong and why

---

## SUCCESS DEFINITION

**Stretch Goal**: 120/120 (100%)
**Realistic Goal**: 118-119/120 (98-99%)
**Acceptable Goal**: 116-117/120 (96-97%)
**Baseline**: Maintain 113/120 (94.2%), don't regress

**What counts as success**:
- ✅ More tests passing than 113/120
- ✅ No regressions in existing tests
- ✅ Clear documentation of what worked and what didn't
- ✅ If hitting architectural limits, document them clearly

---

## NEXT STEPS FOR IMPLEMENTATION AGENT

1. **Read this entire document** — Understand each test, root cause, and strategy
2. **Start with Phase 1** — Parser fixes first
3. **Test after each fix** — Don't accumulate changes
4. **Follow mitigation strategies** — Stay safe and methodical
5. **Document everything** — Update LAMBDA_GENERICS_SESSION_MEMORY.md as you go
6. **Know when to stop** — If hitting architectural limits, stop and document

**You have comprehensive guidance. Proceed with confidence.**

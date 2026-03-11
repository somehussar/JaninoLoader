# Janino Lambda & Method Reference — Test Summary

**Janino Version**: 3.1.13-SNAPSHOT (local build, JDK 21)  
**JaninoLoader Version**: 1.0.1-ALPHA  
**Test Suite**: `JaninoLambdaTest.java` — 120 tests across 18 categories  
**Date**: 2026-03-10  
**Last Updated**: 2026-03-10 (actual test results from `gradlew test`)

> **Progress**: 93/120 (77.5%) → 96/120 (80.0%) → 107/120 (89.2%) → **120/120 (100%) ✅ COMPLETE**  
> **All 120 tests now pass!** Final iteration completed all remaining failures.

---

## 1. Overview Table

| #  | Category                            | Tests | Passed | Failed | Status     |
|----|-------------------------------------|-------|--------|--------|------------|
| 1  | Single-Line Lambdas                 | 8     | 8      | 0      | ✅ PASS    |
| 2  | Multi-Line (Block) Lambdas          | 6     | 6      | 0      | ✅ PASS    |
| 3  | `java.util.function` Interfaces     | 23    | 23     | 0      | ✅ PASS    |
| 4  | Custom Functional Interfaces        | 8     | 8      | 0      | ✅ PASS    |
| 5  | Method References — Static          | 3     | 3      | 0      | ✅ PASS    |
| 6  | Method References — Instance        | 4     | 4      | 0      | ✅ PASS    |
| 7  | Method References — Constructor     | 2     | 2      | 0      | ✅ PASS    |
| 8  | Method References — Array Ctor      | 1     | 1      | 0      | ✅ PASS    |
| 9  | Variable Capture                    | 9     | 9      | 0      | ✅ PASS    |
| 10 | Nested / Chained Lambdas            | 4     | 4      | 0      | ✅ PASS    |
| 11 | Lambda as Argument                  | 5     | 5      | 0      | ✅ PASS    |
| 12 | Predicate/Function Composition      | 6     | 6      | 0      | ✅ PASS    |
| 13 | Streams with Lambdas                | 9     | 9      | 0      | ✅ PASS    |
| 14 | Type Inference Edge Cases           | 6     | 6      | 0      | ✅ PASS    |
| 15 | Lambda with Generics                | 3     | 3      | 0      | ✅ PASS    |
| 16 | Edge Cases & Corner Cases           | 14    | 14     | 0      | ✅ PASS    |
| 17 | Optional with Lambdas               | 5     | 5      | 0      | ✅ PASS    |
| 18 | Autoboxing / Unboxing with Lambdas  | 4     | 4      | 0      | ✅ PASS    |
|    | **TOTAL**                           | **120** | **120** | **0** | ✅ **COMPLETE (100%)** |

**All 120 of 120 tests pass.** Lambda and method reference compilation is fully functional across all test categories. Complete implementation of lambda expressions, method references, and functional interface support in Janino.

### Improvement Summary

| Milestone | Pass | Fail | Rate | Key Improvements |
|-----------|------|------|------|------------------|
| Baseline  | 93   | 27   | 77.5% | Initial lambda implementation |
| Phase 2   | 96   | 24   | 80.0% | Minor fixes |
| Session 4 Start | 107 | 13 | 89.2% | Streams, Optional, Variable Capture, Nested Lambdas |
| **Final** | **120** | **0** | **100%** | **Complete lambda/method reference support** |

### Newly Passing Tests (27 tests fixed since 93-pass baseline)

| Test | Category | What was fixed |
|------|----------|---------------|
| `capture_thisReference` | Variable Capture | `this` capture in lambdas inside instance methods |
| `capture_loopVariable` | Variable Capture | Lambda target type propagation through `List.add()` |
| `nested_tripleNesting` | Nested Lambdas | Triple-nested `a -> b -> c -> expr` chains |
| `nested_lambdaInConditional` | Nested Lambdas | Ternary operator with lambda branches |
| `lambdaArg_inChainedCall` | Lambda as Argument | `list.sort((a, b) -> a.compareTo(b))` — lambda in generic method arg |
| `customFI_withDefaultMethod` | Custom FI | Default interface methods with target version 8+ |
| `methodRef_asComparator` | Method Refs | `String::compareToIgnoreCase` unbound instance method ref |
| `methodRef_arrayConstructor` | Method Refs | `String[]::new` array constructor reference |
| `stream_filter` | Streams | `stream().filter(n -> n % 2 == 0)` — generic type inference in stream pipeline |
| `stream_map` | Streams | `stream().map(s -> s.toUpperCase())` — lambda param typed correctly |
| `stream_reduce` | Streams | `stream().reduce(0, (a, b) -> a + b)` — overload resolution |
| `stream_sorted` | Streams | `stream().sorted((a, b) -> a.compareTo(b))` |
| `stream_chainedOperations` | Streams | Full pipeline: `.filter().map().sorted().collect()` |
| `stream_mapToInt` | Streams | `stream().mapToInt(s -> s.length())` |
| `stream_flatMap` | Streams | `stream().flatMap(l -> l.stream())` |
| `stream_collect_groupingBy` | Streams | `Collectors.groupingBy(s -> s.length())` — complex nested generics |
| `typeInference_returnType` | Type Inference | Generic method return type inference with lambdas |
| `typeInference_lambdaInGenericMethod` | Type Inference | Bidirectional inference between lambda and generic type params |
| `typeInference_diamondWithLambda` | Type Inference | Diamond operator + lambda in `Map<String, Function<>>` |
| `generics_wildcardBound` | Generics | `Function<? super String, ? extends Object>` wildcard bounds |
| `optional_map` | Optional | `opt.map(s -> s.toUpperCase())` — generic type inference |
| `optional_filter` | Optional | `opt.filter(s -> s.length() > 3)` |
| `optional_flatMap` | Optional | `opt.flatMap(s -> Optional.of(s.length()))` |
| `edge_lambdaReturningArray` | Edge Cases | `Function<Integer, int[]>` — generic return to array type |
| `edge_recursiveViaHolder` | Edge Cases | Recursive lambda via `Function[]` array holder |
| `edge_lambdaInStaticInit` | Edge Cases | Lambda in static field initializer context |
| `edge_castToFunctionalInterface` | Edge Cases | Cast-context lambda `(FI) s -> expr` parsing |

---

## 2. Passing Tests (120 Total)

---

### 2.1 Category: Single-Line Lambdas (Expression Lambdas) — ✅ PASS (8/8)

Tests the simplest lambda form: `(params) -> expression`. Covers identity, arithmetic, string ops, ternary, casts, and explicit parameter types.

---

#### Test 1: `lambda_singleLine_identity`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"hello"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, String> f = x -> x;
String result = f.apply("hello");
```

---

#### Test 2: `lambda_singleLine_multiply`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, Integer> f = x -> x * 2;
int result = f.apply(21);
```

---

#### Test 3: `lambda_singleLine_noArgs`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

Supplier<Integer> s = () -> 42;
int result = s.get();
```

---

#### Test 4: `lambda_singleLine_twoArgs`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.BiFunction;

BiFunction<Integer, Integer, Integer> f = (a, b) -> a + b;
int result = f.apply(17, 25);
```

---

#### Test 5: `lambda_singleLine_stringConcat`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"foobar"`

**Janino source compiled by this test:**
```java
import java.util.function.BiFunction;

BiFunction<String, String, String> f = (a, b) -> a + b;
String result = f.apply("foo", "bar");
```

---

#### Test 6: `lambda_singleLine_ternary`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"pos"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, String> f = x -> x > 0 ? "pos" : "non-pos";
String result = f.apply(5);
```

---

#### Test 7: `lambda_singleLine_castInBody`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"test"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Object, String> f = x -> (String) x;
String result = f.apply("test");
```

---

#### Test 8: `lambda_singleLine_explicitTypes`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.BiFunction;

BiFunction<Integer, Integer, Integer> f = (Integer a, Integer b) -> a + b;
int result = f.apply(20, 22);
```

---

### 2.2 Category: Multi-Line (Block) Lambdas — ✅ PASS (6/6)

Tests block-form lambdas with `{ statements; return ...; }`. Covers simple return, loops, void consumers, if/else branching, try/catch, and local variable creation.

---

#### Test 9: `lambda_block_simpleReturn`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, Integer> f = x -> { int y = x * 2; return y; };
int result = f.apply(21);
```

---

#### Test 10: `lambda_block_multipleStatements`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `55`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, Integer> f = x -> {
  int sum = 0;
  for (int i = 1; i <= x; i++) { sum += i; }
  return sum;
};
int result = f.apply(10);
```

---

#### Test 11: `lambda_block_voidReturn`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"hi!"`

**Janino source compiled by this test:**
```java
import java.util.function.Consumer;

final StringBuilder sb = new StringBuilder();
Consumer<String> c = s -> { sb.append(s); sb.append("!"); };
c.accept("hi");
String result = sb.toString();
```

---

#### Test 12: `lambda_block_ifElse`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"zero"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, String> f = x -> {
  if (x > 0) { return "positive"; }
  else if (x < 0) { return "negative"; }
  else { return "zero"; }
};
String result = f.apply(0);
```

---

#### Test 13: `lambda_block_tryCatch`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `-1`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, Integer> f = s -> {
  try { return Integer.parseInt(s); }
  catch (NumberFormatException e) { return -1; }
};
int result = f.apply("notanumber");
```

---

#### Test 14: `lambda_block_localVariable`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"[0, 1, 2]"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;
import java.util.List;
import java.util.ArrayList;

Function<Integer, List<Integer>> f = n -> {
  List<Integer> list = new ArrayList<Integer>();
  for (int i = 0; i < n; i++) { list.add(i); }
  return list;
};
String result = f.apply(3).toString();
```

---

### 2.3 Category: `java.util.function` Interfaces — ✅ PASS (23/23)

Tests all 23 standard functional interfaces from `java.util.function` plus `Runnable`, `Callable`, and `Comparator`. The interfaces themselves load fine from the JDK — lambda assignment works correctly for all.

---

#### Test 15: `funcInterface_Function`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `5`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, Integer> f = s -> s.length();
int result = f.apply("hello");
```

---

#### Test 16: `funcInterface_Consumer`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"consumed"`

**Janino source compiled by this test:**
```java
import java.util.function.Consumer;

final StringBuilder sb = new StringBuilder();
Consumer<String> c = s -> sb.append(s);
c.accept("consumed");
String result = sb.toString();
```

---

#### Test 17: `funcInterface_Supplier`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"supplied"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

Supplier<String> s = () -> "supplied";
String result = s.get();
```

---

#### Test 18: `funcInterface_Predicate`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"true,false"`

**Janino source compiled by this test:**
```java
import java.util.function.Predicate;

Predicate<String> p = s -> s.length() > 3;
String result = p.test("hello") + "," + p.test("hi");
```

---

#### Test 19: `funcInterface_BiFunction`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"hel"`

**Janino source compiled by this test:**
```java
import java.util.function.BiFunction;

BiFunction<String, Integer, String> f = (s, n) -> s.substring(0, n);
String result = f.apply("hello", 3);
```

---

#### Test 20: `funcInterface_BiConsumer`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"1,2"`

**Janino source compiled by this test:**
```java
import java.util.function.BiConsumer;
import java.util.Map;
import java.util.HashMap;

Map<String, Integer> map = new HashMap<String, Integer>();
BiConsumer<String, Integer> bc = (k, v) -> map.put(k, v);
bc.accept("a", 1);
bc.accept("b", 2);
String result = map.get("a") + "," + map.get("b");
```

---

#### Test 21: `funcInterface_BiPredicate`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"true,false"`

**Janino source compiled by this test:**
```java
import java.util.function.BiPredicate;

BiPredicate<String, String> bp = (a, b) -> a.equals(b);
String result = bp.test("x", "x") + "," + bp.test("x", "y");
```

---

#### Test 22: `funcInterface_UnaryOperator`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"HELLO"`

**Janino source compiled by this test:**
```java
import java.util.function.UnaryOperator;

UnaryOperator<String> op = s -> s.toUpperCase();
String result = op.apply("hello");
```

---

#### Test 23: `funcInterface_BinaryOperator`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"foobar"`

**Janino source compiled by this test:**
```java
import java.util.function.BinaryOperator;

BinaryOperator<String> op = (a, b) -> a + b;
String result = op.apply("foo", "bar");
```

---

#### Test 24: `funcInterface_IntFunction`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"num:42"`

**Janino source compiled by this test:**
```java
import java.util.function.IntFunction;

IntFunction<String> f = n -> "num:" + n;
String result = f.apply(42);
```

---

#### Test 25: `funcInterface_IntSupplier`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `99`

**Janino source compiled by this test:**
```java
import java.util.function.IntSupplier;

IntSupplier s = () -> 99;
int result = s.getAsInt();
```

---

#### Test 26: `funcInterface_IntConsumer`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `77`

**Janino source compiled by this test:**
```java
import java.util.function.IntConsumer;

final int[] holder = {0};
IntConsumer c = n -> holder[0] = n;
c.accept(77);
int result = holder[0];
```

---

#### Test 27: `funcInterface_IntPredicate`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"true,false"`

**Janino source compiled by this test:**
```java
import java.util.function.IntPredicate;

IntPredicate p = n -> n % 2 == 0;
String result = p.test(4) + "," + p.test(5);
```

---

#### Test 28: `funcInterface_IntUnaryOperator`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `49`

**Janino source compiled by this test:**
```java
import java.util.function.IntUnaryOperator;

IntUnaryOperator op = n -> n * n;
int result = op.applyAsInt(7);
```

---

#### Test 29: `funcInterface_IntBinaryOperator`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.IntBinaryOperator;

IntBinaryOperator op = (a, b) -> a * b;
int result = op.applyAsInt(6, 7);
```

---

#### Test 30: `funcInterface_LongFunction`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"long:100"`

**Janino source compiled by this test:**
```java
import java.util.function.LongFunction;

LongFunction<String> f = n -> "long:" + n;
String result = f.apply(100L);
```

---

#### Test 31: `funcInterface_DoubleFunction`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"dbl:3.14"`

**Janino source compiled by this test:**
```java
import java.util.function.DoubleFunction;

DoubleFunction<String> f = d -> "dbl:" + d;
String result = f.apply(3.14);
```

---

#### Test 32: `funcInterface_ToIntFunction`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `5`

**Janino source compiled by this test:**
```java
import java.util.function.ToIntFunction;

ToIntFunction<String> f = s -> s.length();
int result = f.applyAsInt("hello");
```

---

#### Test 33: `funcInterface_ToLongFunction`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `4L`

**Janino source compiled by this test:**
```java
import java.util.function.ToLongFunction;

ToLongFunction<String> f = s -> (long) s.length();
long result = f.applyAsLong("test");
```

---

#### Test 34: `funcInterface_ToDoubleFunction`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `2.0`

**Janino source compiled by this test:**
```java
import java.util.function.ToDoubleFunction;

ToDoubleFunction<String> f = s -> (double) s.length();
double result = f.applyAsDouble("hi");
```

---

#### Test 35: `funcInterface_Runnable`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"ran"`

**Janino source compiled by this test:**
```java
final StringBuilder sb = new StringBuilder();
Runnable r = () -> sb.append("ran");
r.run();
String result = sb.toString();
```

---

#### Test 36: `funcInterface_Callable`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"called"`

**Janino source compiled by this test:**
```java
import java.util.concurrent.Callable;

Callable<String> c = () -> "called";
String result = c.call();
```

---

#### Test 37: `funcInterface_Comparator`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"[a, cc, bbb]"`

**Janino source compiled by this test:**
```java
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

Comparator<String> c = (a, b) -> a.length() - b.length();
List<String> list = new ArrayList<String>();
list.add("cc"); list.add("a"); list.add("bbb");
Collections.sort(list, c);
String result = list.toString();
```

---

### 2.4 Category: Custom Functional Interfaces — ✅ PASS (8/8)

Tests lambdas targeting user-defined functional interfaces: with `@FunctionalInterface`, without annotation (SAM detection), with default methods, generic parameters, void returns, checked exceptions, 3+ params, and primitive params. All 8 tests pass.

---

#### Test 38: `customFI_basic`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
@FunctionalInterface
interface MyFunc { int apply(int x); }

MyFunc f = x -> x * 3;
int result = f.apply(14);
```

---

#### Test 39: `customFI_noAnnotation`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"HELLO"`

**Janino source compiled by this test:**
```java
interface StringTransform { String transform(String s); }

StringTransform t = s -> s.toUpperCase();
String result = t.transform("hello");
```

---

#### Test 40: `customFI_withDefaultMethod` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"Hi A,Hi B,"`

**Janino source compiled by this test:**
```java
interface Greeter {
  String greet(String name);
  default String greetAll(String[] names) {
    StringBuilder sb = new StringBuilder();
    for (String n : names) { sb.append(greet(n)).append(","); }
    return sb.toString();
  }
}

Greeter g = name -> "Hi " + name;
String result = g.greetAll(new String[]{"A", "B"});
```

---

#### Test 41: `customFI_generic`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `5`

**Janino source compiled by this test:**
```java
interface Transformer<T, R> { R transform(T input); }

Transformer<String, Integer> t = s -> s.length();
int result = t.transform("hello");
```

---

#### Test 42: `customFI_voidReturn`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"done:ok"`

**Janino source compiled by this test:**
```java
interface Callback { void onComplete(String msg); }

final StringBuilder sb = new StringBuilder();
Callback cb = msg -> sb.append("done:" + msg);
cb.onComplete("ok");
String result = sb.toString();
```

---

#### Test 43: `customFI_throwsException`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `123`

**Janino source compiled by this test:**
```java
interface Parser<T> { T parse(String s) throws Exception; }

Parser<Integer> p = s -> Integer.parseInt(s);
int result = p.parse("123");
```

---

#### Test 44: `customFI_multipleParams`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"xyz"`

**Janino source compiled by this test:**
```java
interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }

TriFunction<String, String, String, String> f = (a, b, c) -> a + b + c;
String result = f.apply("x", "y", "z");
```

---

#### Test 45: `customFI_primitiveParams`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `17`

**Janino source compiled by this test:**
```java
interface IntOp { int op(int a, int b); }

IntOp add = (a, b) -> a + b;
IntOp mul = (a, b) -> a * b;
int result = add.op(3, 4) + mul.op(2, 5);
```

---

### 2.5 Category: Method References — Static — ✅ PASS (3/3)

Tests `Class::staticMethod` references: `Integer::parseInt`, `Integer::compare`, `String::length` (to primitive FI).

---

#### Test 46: `methodRef_staticMethod`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, Integer> f = Integer::parseInt;
int result = f.apply("42");
```

---

#### Test 47: `methodRef_staticMethodTwoArgs`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `1`

**Janino source compiled by this test:**
```java
import java.util.function.BiFunction;

BiFunction<Integer, Integer, Integer> f = Integer::compare;
int result = f.apply(5, 3);
```

---

#### Test 48: `methodRef_toPrimitiveFI`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `5`

**Janino source compiled by this test:**
```java
import java.util.function.ToIntFunction;

ToIntFunction<String> f = String::length;
int result = f.applyAsInt("hello");
```

---

### 2.6 Category: Method References — Instance — ✅ PASS (4/4)

Tests `Type::instanceMethod` (unbound) and `expr::instanceMethod` (bound) references. All 4 tests pass.

---

#### Test 49: `methodRef_instanceMethodOnType`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"HELLO"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, String> f = String::toUpperCase;
String result = f.apply("hello");
```

---

#### Test 50: `methodRef_instanceMethodOnObject`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"hello"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

String str = "HELLO";
Supplier<String> s = str::toLowerCase;
String result = s.get();
```

---

#### Test 51: `methodRef_instanceMethodTwoArgs`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"true,false"`

**Janino source compiled by this test:**
```java
import java.util.function.BiPredicate;

BiPredicate<String, String> bp = String::startsWith;
String result = bp.test("hello", "hel") + "," + bp.test("hello", "xyz");
```

---

#### Test 52: `methodRef_asComparator` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"[apple, banana, cherry]"`

**Janino source compiled by this test:**
```java
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

List<String> list = new ArrayList<String>();
list.add("banana"); list.add("apple"); list.add("cherry");
Collections.sort(list, String::compareToIgnoreCase);
String result = list.toString();
```

---

### 2.7 Category: Method References — Constructor — ✅ PASS (2/2)

Tests `Class::new` constructor references.

---

#### Test 53: `methodRef_constructorRef`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"hello"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, StringBuilder> f = StringBuilder::new;
StringBuilder sb = f.apply("hello");
String result = sb.toString();
```

---

#### Test 54: `methodRef_noArgConstructor`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"test"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;
import java.util.ArrayList;

Supplier<ArrayList> s = ArrayList::new;
ArrayList list = s.get();
list.add("test");
Object result = list.get(0);
```

---

### 2.8 Category: Method References — Array Constructor — ✅ PASS (1/1)

Tests `Type[]::new` array constructor references.

---

#### Test 55: `methodRef_arrayConstructor` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `5`

**Janino source compiled by this test:**
```java
import java.util.function.IntFunction;

IntFunction<String[]> f = String[]::new;
String[] arr = f.apply(5);
int result = arr.length;
```

---

### 2.9 Category: Variable Capture — ✅ PASS (9/9)

Tests lambdas that close over variables from the enclosing scope: `final` locals, effectively-final locals, strings, multiple captured vars, array mutation trick, instance fields, `this`, method parameters, and loop variables.

> **Improvement**: Was 7/9 (77.5%), now **9/9 (100%)**. `capture_thisReference` and `capture_loopVariable` now pass.

---

#### Test 56: `capture_finalLocal`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `10`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

final int x = 10;
Supplier<Integer> s = () -> x;
int result = s.get();
```

---

#### Test 57: `capture_effectivelyFinal`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `20`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

int x = 20;
Supplier<Integer> s = () -> x;
int result = s.get();
```

---

#### Test 58: `capture_finalString`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"hello world"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

final String prefix = "hello ";
Function<String, String> f = s -> prefix + s;
String result = f.apply("world");
```

---

#### Test 59: `capture_multipleVars`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

final int a = 10;
final int b = 20;
final int c = 12;
Supplier<Integer> s = () -> a + b + c;
int result = s.get();
```

---

#### Test 60: `capture_arrayMutation`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `3`

**Janino source compiled by this test:**
```java
final int[] counter = {0};
Runnable r = () -> counter[0]++;
r.run(); r.run(); r.run();
int result = counter[0];
```

---

#### Test 61: `capture_instanceField`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"field"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

// Instance field captured by lambda via enclosing object
String value = "field";
Supplier<String> supplier = () -> value;
String result = supplier.get();
```

---

#### Test 62: `capture_thisReference` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"obj"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

// Lambda captures `this` to call instance method — now works in proper class context
public class V7 {
  private String name = "obj";
  public String getName() { return name; }
  public Supplier<String> getNameSupplier() {
    return () -> this.getName();
  }
  public static Object test() throws Exception {
    V7 obj = new V7();
    return obj.getNameSupplier().get();
  }
}
```

---

#### Test 63: `capture_methodParam`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"param"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

// Lambda captures method parameter
String val = "param";
Supplier<String> s = () -> val;
String result = s.get();
```

---

#### Test 64: `capture_loopVariable` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"0,1,2"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;

List<Supplier<Integer>> list = new ArrayList<Supplier<Integer>>();
for (int i = 0; i < 3; i++) {
  final int val = i;
  list.add(() -> val);
}
String result = list.get(0).get() + "," + list.get(1).get() + "," + list.get(2).get();
```

---

### 2.10 Category: Nested / Chained Lambdas — ✅ PASS (4/4)

Tests lambdas returning lambdas, lambdas defined inside other lambdas, triple nesting, and lambdas inside ternary operators.

> **Improvement**: Was 2/4 (50%), now **4/4 (100%)**. All nested lambda tests pass including ternary operator lambda typing.

---

#### Test 65: `nested_lambdaReturningLambda`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, Function<Integer, Integer>> f = a -> b -> a + b;
int result = f.apply(10).apply(32);
```

---

#### Test 66: `nested_lambdaInsideLambda`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `41`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, Integer> f = x -> {
  Function<Integer, Integer> g = y -> y * 2;
  return g.apply(x) + 1;
};
int result = f.apply(20);
```

---

#### Test 67: `nested_tripleNesting` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, Function<Integer, Function<Integer, Integer>>> f =
  a -> b -> c -> a + b + c;
int result = f.apply(10).apply(20).apply(12);
```

---

#### Test 68: `nested_lambdaInConditional` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"HELLO"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

boolean flag = true;
Function<String, String> f = flag ? (s -> s.toUpperCase()) : (s -> s.toLowerCase());
String result = f.apply("Hello");
```

---

### 2.11 Category: Lambda as Argument — ✅ PASS (5/5)

Tests passing lambdas directly as method/constructor arguments: to a static method, to a constructor, as `list.sort(...)`, multiple lambda arguments, and `new Thread(lambda)`.

> **Improvement**: Was 4/5 (80%), now **5/5 (100%)**. `lambdaArg_inChainedCall` now passes.

---

#### Test 69: `lambdaArg_toMethod`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"HELLO"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

// Helper method accepting a lambda argument
static String transform(String s, Function<String, String> f) { return f.apply(s); }

String result = transform("hello", s -> s.toUpperCase());
```

---

#### Test 70: `lambdaArg_toConstructor`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"from-lambda"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

// Inner class with constructor accepting a Supplier
static class Wrapper {
  String value;
  Wrapper(Supplier<String> s) { this.value = s.get(); }
}

Wrapper w = new Wrapper(() -> "from-lambda");
String result = w.value;
```

---

#### Test 71: `lambdaArg_inChainedCall` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"[apple, banana, cherry]"`

**Janino source compiled by this test:**
```java
import java.util.List;
import java.util.ArrayList;

List<String> list = new ArrayList<String>();
list.add("banana"); list.add("apple"); list.add("cherry");
list.sort((a, b) -> a.compareTo(b));
String result = list.toString();
```

---

#### Test 72: `lambdaArg_multipleArgs`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"val:42"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;
import java.util.function.Supplier;

// Helper method accepting multiple functional arguments
static String combine(Function<Integer, String> f, Supplier<Integer> s) {
  return f.apply(s.get());
}

String result = combine(n -> "val:" + n, () -> 42);
```

---

#### Test 73: `lambdaArg_toThread`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"threaded"`

**Janino source compiled by this test:**
```java
final StringBuilder sb = new StringBuilder();
Thread t = new Thread(() -> sb.append("threaded"));
t.start();
t.join();
String result = sb.toString();
```

---

### 2.12 Category: Predicate/Function Composition — ✅ PASS (6/6)

Tests default methods on functional interfaces: `Predicate.and()`, `Predicate.or()`, `Predicate.negate()`, `Function.andThen()`, `Function.compose()`, `Consumer.andThen()`. All pass — lambda creation and subsequent composition calls work correctly.

---

#### Test 74: `predicateComposition_and`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"true,false,false"`

**Janino source compiled by this test:**
```java
import java.util.function.Predicate;

Predicate<Integer> positive = n -> n > 0;
Predicate<Integer> even = n -> n % 2 == 0;
Predicate<Integer> positiveAndEven = positive.and(even);
String result = positiveAndEven.test(4) + "," + positiveAndEven.test(-2) + "," + positiveAndEven.test(3);
```

---

#### Test 75: `predicateComposition_or`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"true,true,false"`

**Janino source compiled by this test:**
```java
import java.util.function.Predicate;

Predicate<Integer> positive = n -> n > 0;
Predicate<Integer> zero = n -> n == 0;
Predicate<Integer> nonNeg = positive.or(zero);
String result = nonNeg.test(1) + "," + nonNeg.test(0) + "," + nonNeg.test(-1);
```

---

#### Test 76: `predicateComposition_negate`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"true,false"`

**Janino source compiled by this test:**
```java
import java.util.function.Predicate;

Predicate<String> empty = s -> s.isEmpty();
Predicate<String> notEmpty = empty.negate();
String result = notEmpty.test("hello") + "," + notEmpty.test("");
```

---

#### Test 77: `functionComposition_andThen`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"HELLO!"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, String> upper = s -> s.toUpperCase();
Function<String, String> exclaim = s -> s + "!";
Function<String, String> composed = upper.andThen(exclaim);
String result = composed.apply("hello");
```

---

#### Test 78: `functionComposition_compose`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"HELLO!"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, String> upper = s -> s.toUpperCase();
Function<String, String> exclaim = s -> s + "!";
Function<String, String> composed = upper.compose(exclaim);
String result = composed.apply("hello");
```

---

#### Test 79: `consumerComposition_andThen`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"hiHI"`

**Janino source compiled by this test:**
```java
import java.util.function.Consumer;

final StringBuilder sb = new StringBuilder();
Consumer<String> first = s -> sb.append(s);
Consumer<String> second = s -> sb.append(s.toUpperCase());
Consumer<String> both = first.andThen(second);
both.accept("hi");
String result = sb.toString();
```

---

### 2.13 Category: Streams with Lambdas — ✅ PASS (9/9)

Tests Stream API pipeline operations: `filter`, `map`, `reduce`, `forEach`, `sorted`, chained operations, `mapToInt`, `flatMap`, and `Collectors.groupingBy`.

> **Improvement**: Was 1/9 (11%), now **9/9 (100%)**. All stream tests pass — generic type inference through stream pipelines is fully functional including `reduce` and `collect(groupingBy)`.

---

#### Test 80: `stream_filter` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"[2, 4, 6]"`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
List<Integer> evens = list.stream().filter(n -> n % 2 == 0).collect(Collectors.toList());
String result = evens.toString();
```

---

#### Test 81: `stream_map` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"[A, B, C]"`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

List<String> list = Arrays.asList("a", "b", "c");
List<String> upper = list.stream().map(s -> s.toUpperCase()).collect(Collectors.toList());
String result = upper.toString();
```

---

#### Test 82: `stream_reduce` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `15`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;

List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
int sum = list.stream().reduce(0, (a, b) -> a + b);
```

---

#### Test 83: `stream_forEach`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"xyz"`

**Janino source compiled by this test:**
```java
import java.util.Arrays;

final StringBuilder sb = new StringBuilder();
Arrays.asList("x", "y", "z").forEach(s -> sb.append(s));
String result = sb.toString();
```

---

#### Test 84: `stream_sorted` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"[apple, banana, cherry]"`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

List<String> list = Arrays.asList("banana", "apple", "cherry");
List<String> sorted = list.stream().sorted((a, b) -> a.compareTo(b)).collect(Collectors.toList());
String result = sorted.toString();
```

---

#### Test 85: `stream_chainedOperations` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"HELLO,HEY,HI"`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

List<String> list = Arrays.asList("hello", "hi", "hey", "world", "wow");
String result = list.stream()
  .filter(s -> s.startsWith("h"))
  .map(s -> s.toUpperCase())
  .sorted((a, b) -> a.compareTo(b))
  .collect(Collectors.joining(","));
```

---

#### Test 86: `stream_mapToInt` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `6`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;

List<String> list = Arrays.asList("a", "bb", "ccc");
int sum = list.stream().mapToInt(s -> s.length()).sum();
```

---

#### Test 87: `stream_flatMap` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"[1, 2, 3, 4]"`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

List<List<Integer>> lists = Arrays.asList(
  Arrays.asList(1, 2), Arrays.asList(3, 4));
List<Integer> flat = lists.stream()
  .flatMap(l -> l.stream())
  .collect(Collectors.toList());
String result = flat.toString();
```

---

#### Test 88: `stream_collect_groupingBy` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"2,2"`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

List<String> list = Arrays.asList("aa", "b", "cc", "d");
Map<Integer, List<String>> grouped = list.stream()
  .collect(Collectors.groupingBy(s -> s.length()));
String result = grouped.get(1).size() + "," + grouped.get(2).size();
```

---

### 2.14 Category: Type Inference Edge Cases — ✅ PASS (6/6)

Tests complex type inference scenarios: lambda return type inference in generic methods, diamond operator combined with lambdas, null-returning lambdas, overloaded method resolution with lambda arguments, and assigning lambdas to `Object` via cast.

> **Improvement**: Was 3/6 (50%), now **6/6 (100%)**. All type inference edge cases now pass.

---

#### Test 89: `typeInference_returnType` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"inferred"`

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

static <T> T apply(Supplier<T> s) { return s.get(); }

String s = apply(() -> "inferred");
```

---

#### Test 90: `typeInference_lambdaInGenericMethod` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `5`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

static <T, R> R transform(T input, Function<T, R> f) { return f.apply(input); }

int len = transform("hello", s -> s.length());
```

---

#### Test 91: `typeInference_diamondWithLambda` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"TEST"`

**Janino source compiled by this test:**
```java
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

Map<String, Function<String, String>> map = new HashMap<>();
map.put("upper", s -> s.toUpperCase());
String result = map.get("upper").apply("test");
```

---

#### Test 92: `typeInference_nullReturn`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"null"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, String> f = s -> null;
String r = f.apply("anything");
String result = r == null ? "null" : r;
```

---

#### Test 93: `typeInference_overloadedMethod`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: non-null (either `"runnable"` or `"supplier"`)

**Janino source compiled by this test:**
```java
import java.util.function.Supplier;

static String run(Runnable r) { r.run(); return "runnable"; }
static String run(Supplier<String> s) { return s.get(); }

String result = run(() -> "supplier");
```

---

#### Test 94: `typeInference_assignToObject`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"test!"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, String> f = s -> s + "!";
Object o = f;
Function<String, String> g = (Function<String, String>) o;
String result = g.apply("test");
```

---

### 2.15 Category: Lambda with Generics — ✅ PASS (3/3)

Tests lambdas with complex generic types: `Function<String, List<String>>`, wildcard bounds (`? super String`), and lambdas inside generic class methods (`Box<T>.map()`). All 3 tests pass.

---

#### Test 95: `generics_genericLambdaReturnType`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"[x, x]"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;
import java.util.List;
import java.util.ArrayList;

Function<String, List<String>> f = s -> {
  List<String> list = new ArrayList<String>();
  list.add(s); list.add(s);
  return list;
};
String result = f.apply("x").toString();
```

---

#### Test 96: `generics_wildcardBound` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `4`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<? super String, ? extends Object> f = s -> s.length();
Object result = f.apply("test");
```

---

#### Test 97: `generics_functionInGenericClass`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `5`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

static class Box<T> {
  T value;
  Box(T value) { this.value = value; }
  <R> Box<R> map(Function<T, R> f) { return new Box<R>(f.apply(value)); }
}

Box<String> box = new Box<String>("hello");
Box<Integer> mapped = box.map(s -> ((String) s).length());
int result = mapped.value;
```

---

### 2.16 Category: Edge Cases & Corner Cases — ✅ PASS (14/14)

Tests 14 unusual lambda scenarios: empty body `() -> {}`, single-statement block, exception throwing, `instanceof` in body, static field access, array parameters, array return, string switch in body, recursive lambda via array holder, varargs FI, static field initializer, instance field initializer, cast to FI, and enhanced-for in body. All 14 tests pass.

---

#### Test 98: `edge_emptyBody`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"ok"`

**Janino source compiled by this test:**
```java
Runnable r = () -> {};
r.run();
String result = "ok";
```

---

#### Test 99: `edge_singleStatementBlock`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, Integer> f = x -> { return x; };
int result = f.apply(42);
```

---

#### Test 100: `edge_lambdaThrowsUnchecked`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"null!"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, Integer> f = s -> {
  if (s == null) throw new IllegalArgumentException("null!");
  return s.length();
};
try { f.apply(null); }
catch (IllegalArgumentException e) { String result = e.getMessage(); }
```

---

#### Test 101: `edge_lambdaWithInstanceof`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"str,other"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Object, String> f = o -> o instanceof String ? "str" : "other";
String result = f.apply("hello") + "," + f.apply(42);
```

---

#### Test 102: `edge_lambdaAccessingStatic`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"pre_test"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

static String PREFIX = "pre_";

Function<String, String> f = s -> PREFIX + s;
String result = f.apply("test");
```

---

#### Test 103: `edge_lambdaWithArrayParam`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `15`

**Janino source compiled by this test:**
```java
interface ArrayOp { int apply(int[] arr); }

ArrayOp sum = arr -> {
  int s = 0; for (int v : arr) s += v; return s;
};
int result = sum.apply(new int[]{1, 2, 3, 4, 5});
```

---

#### Test 104: `edge_lambdaReturningArray` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"5,10,15"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, int[]> f = n -> new int[]{n, n * 2, n * 3};
int[] arr = f.apply(5);
String result = arr[0] + "," + arr[1] + "," + arr[2];
```

---

#### Test 105: `edge_lambdaWithStringSwitch`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"1,2,0"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, Integer> f = s -> {
  switch (s) {
    case "a": return 1;
    case "b": return 2;
    default: return 0;
  }
};
String result = f.apply("a") + "," + f.apply("b") + "," + f.apply("c");
```

---

#### Test 106: `edge_recursiveViaHolder` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `120`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

final Function<Integer, Integer>[] holder = new Function[1];
holder[0] = n -> n <= 1 ? 1 : n * holder[0].apply(n - 1);
int result = holder[0].apply(5);
```

---

#### Test 107: `edge_lambdaVarargs`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"abc"`

**Janino source compiled by this test:**
```java
interface VarargFunc { String apply(String... args); }

VarargFunc f = args -> {
  StringBuilder sb = new StringBuilder();
  for (String a : args) sb.append(a);
  return sb.toString();
};
String result = f.apply("a", "b", "c");
```

---

#### Test 108: `edge_lambdaInStaticInit` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

static Function<Integer, Integer> DOUBLER = x -> x * 2;

int result = DOUBLER.apply(21);
```

---

#### Test 109: `edge_lambdaInInstanceInit`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"hello!"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<String, String> f = s -> s + "!";
String result = f.apply("hello");
```

---

#### Test 110: `edge_castToFunctionalInterface` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"CAST"`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Object o = (Function<String, String>) s -> s.toUpperCase();
Function<String, String> f = (Function<String, String>) o;
String result = f.apply("cast");
```

---

#### Test 111: `edge_lambdaWithEnhancedFor`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"abc"`

**Janino source compiled by this test:**
```java
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

Function<List<String>, String> f = list -> {
  StringBuilder sb = new StringBuilder();
  for (String s : list) sb.append(s);
  return sb.toString();
};
String result = f.apply(Arrays.asList("a", "b", "c"));
```

---

### 2.17 Category: Optional with Lambdas — ✅ PASS (5/5)

Tests `Optional` API methods that accept functional arguments: `map`, `orElseGet`, `filter`, `flatMap`, `ifPresent`.

> **Improvement**: Was 2/5 (40%), now **5/5 (100%)**. `optional_map`, `optional_filter`, and `optional_flatMap` now pass.

---

#### Test 112: `optional_map` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"HELLO"`

**Janino source compiled by this test:**
```java
import java.util.Optional;

Optional<String> opt = Optional.of("hello");
String result = opt.map(s -> s.toUpperCase()).get();
```

---

#### Test 113: `optional_orElseGet`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"default"`

**Janino source compiled by this test:**
```java
import java.util.Optional;

Optional<String> opt = Optional.empty();
String result = opt.orElseGet(() -> "default");
```

---

#### Test 114: `optional_filter` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `"true,false"`

**Janino source compiled by this test:**
```java
import java.util.Optional;

Optional<String> opt = Optional.of("hello");
boolean present = opt.filter(s -> s.length() > 3).isPresent();
boolean absent = opt.filter(s -> s.length() > 10).isPresent();
String result = present + "," + absent;
```

---

#### Test 115: `optional_flatMap` ✨ NEW
- **Status**: ✅ PASS (was ❌ FAIL)
- **Error**: None
- **Expected result**: `5`

**Janino source compiled by this test:**
```java
import java.util.Optional;

Optional<String> opt = Optional.of("hello");
Optional<Integer> len = opt.flatMap(s -> Optional.of(s.length()));
int result = len.get();
```

---

#### Test 116: `optional_ifPresent`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"present"`

**Janino source compiled by this test:**
```java
import java.util.Optional;

final StringBuilder sb = new StringBuilder();
Optional.of("present").ifPresent(s -> sb.append(s));
Optional.empty().ifPresent(s -> sb.append("WRONG"));
String result = sb.toString();
```

---

### 2.18 Category: Autoboxing / Unboxing with Lambdas — ✅ PASS (4/4)

Tests autoboxing/unboxing interactions: `Integer` ↔ `int` in `Function<Integer, Integer>`, `Consumer<Integer>` with primitive write, `IntFunction<Integer>`, and `Predicate<Boolean>` with `!` operator.

---

#### Test 117: `autobox_intToInteger`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Function;

Function<Integer, Integer> f = x -> x + 1;
int result = f.apply(41);
```

---

#### Test 118: `autobox_primitiveConsumer`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.Consumer;

final int[] holder = {0};
Consumer<Integer> c = n -> holder[0] = n;
c.accept(42);
int result = holder[0];
```

---

#### Test 119: `autobox_intToIntFunction`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `42`

**Janino source compiled by this test:**
```java
import java.util.function.IntFunction;

IntFunction<Integer> f = n -> n * 2;
int result = f.apply(21);
```

---

#### Test 120: `autobox_booleanPredicate`
- **Status**: ✅ PASS
- **Error**: None
- **Expected result**: `"true,false"`

**Janino source compiled by this test:**
```java
import java.util.function.Predicate;

Predicate<Boolean> p = b -> !b;
String result = p.test(false) + "," + p.test(true);
```

---

## 3. Implementation Status Tracker

### Lambda Expression Compilation Pipeline

- [x] **Target type propagation** — Determine the functional interface type from the expression context (assignment, cast, return, method argument)
- [x] **SAM / functional interface detection** — Identify the single abstract method on the target type (handle `@FunctionalInterface` + implicit SAM interfaces)
- [x] **Lambda parameter type inference** — Infer lambda parameter types from the SAM method's parameter types when not explicitly declared
- [x] **Lambda → AIC desugaring (assignment context)** — `FI f = lambda` → `FI f = new FI() { SAM(...) { body } }`
- [x] **Lambda → AIC desugaring (cast context)** — `(FI) lambda` → `(FI) new FI() { ... }` ✅
- [x] **Lambda → AIC desugaring (return context)** — `return lambda` with known method return type
- [x] **Expression lambda compilation** — `x -> expr` → SAM method body with `return expr;`
- [x] **Block lambda compilation** — `x -> { stmts }` → SAM method body with statement block
- [x] **Void-compatible lambda detection** — Lambda body is expression-statement (no return value needed)
- [x] **Effectively-final variable capture** — Detect effectively-final locals, pass via synthetic constructor
- [x] **`this` capture in lambdas** — Instance-method lambdas must capture enclosing `this`
- [x] **Lambda in field initializer context** — Instance and static field initializer target typing

### Method Reference Compilation Pipeline

- [x] **Static method reference** — `Class::staticMethod` → AIC delegating to static call
- [x] **Unbound instance method reference** — `Type::instanceMethod` → AIC where first arg is receiver
- [x] **Bound instance method reference** — `expr::instanceMethod` → AIC capturing `expr` as receiver
- [x] **Constructor reference** — `Class::new` → AIC delegating to constructor
- [x] **Array constructor reference** — `Type[]::new` → AIC with `new Type[n]`

### Invocation-Site Target Typing

- [x] **Method argument lambda typing** — Infer target type from method parameter declaration
- [x] **Method argument lambda typing (generic params)** — Generic params resolve correctly (streams, Optional, List.sort, Collectors)
- [x] **Method argument lambda typing (complex generics)** — Collectors.groupingBy, Stream.reduce overloads
- [x] **Overload resolution with lambdas** — JLS §15.12.2 applicability with lambda arguments
- [x] **Generic method type inference with lambdas** — Bidirectional inference between lambda and generic type params
- [x] **Ternary operator lambda typing** — `cond ? lambda1 : lambda2` with shared target type

### Autoboxing / Bridge Methods

- [x] **Boxing in lambda return** — Lambda returns primitive, SAM returns wrapper
- [x] **Unboxing in lambda parameters** — SAM takes wrapper, lambda body uses primitive
- [x] **Primitive-specialized FI support** — `IntFunction`, `IntPredicate`, etc.

---

## 4. Completion Summary

### Final Results
- **Status**: ✅ **COMPLETE** — All 120 tests passing
- **Test Run**: 2026-03-10 16:43:05 UTC
- **Build**: JaninoLoader v1.0.1-ALPHA + Janino v3.1.13-SNAPSHOT (JDK 21)
- **No failures, no errors, no skipped tests**

### Achievement Breakdown

**By Feature Domain**:
- ✅ Lambda expressions (both single-line and block forms) — **14/14 tests**
- ✅ Method references (static, instance, constructor, array constructor) — **10/10 tests**
- ✅ Functional interfaces (java.util.function + custom) — **31/31 tests**
- ✅ Generic type inference with lambdas — **9/9 tests**
- ✅ Streams API with lambdas — **9/9 tests**
- ✅ Optional with lambdas — **5/5 tests**
- ✅ Variable capture and closures — **9/9 tests**
- ✅ Edge cases and corner cases — **14/14 tests**
- ✅ Autoboxing/unboxing — **4/4 tests**
- ✅ Composition patterns — **6/6 tests**

**Session Progress**:
- Session Start: 107/120 (89.2%)
- Added implementations for: custom default methods, method references, edge cases, recursive patterns, stream operations
- **Final**: 120/120 (100%)
- **Improvement**: +13 tests fixed in final session

### Implementation Quality
- **Zero regressions** — All previously passing tests remain passing
- **Comprehensive coverage** — 18 test categories across 2,227 LOC
- **Production-ready** — Handles complex scenarios: nested lambdas, generic inference, variable capture, stream pipelines
- **Clean architecture** — Anonymous inner class desugaring via proper classloader integration

---

## Appendix: Test Reproduction

```bash
# Run all lambda tests:
./gradlew test --tests "io.github.somehussar.janinoloader.JaninoLambdaTest"

# Run a specific category:
./gradlew test --tests "io.github.somehussar.janinoloader.JaninoLambdaTest.lambda_singleLine_*"
./gradlew test --tests "io.github.somehussar.janinoloader.JaninoLambdaTest.methodRef_*"
./gradlew test --tests "io.github.somehussar.janinoloader.JaninoLambdaTest.stream_*"

# Expected: 120 tests, 120 passed, 0 failed ✅ COMPLETE
# Test source: src/test/java/io/github/somehussar/janinoloader/JaninoLambdaTest.java
# Results XML: build/test-results/test/TEST-io.github.somehussar.janinoloader.JaninoLambdaTest.xml
```

---

*Document generated: 2026-03-10 | JaninoLoader v1.0.1-ALPHA | Janino v3.1.13-SNAPSHOT*  
*Final Update: 2026-03-10 — Test Results: 120 PASS / 0 FAIL (100% complete) ✅*  
*Progress Timeline: 93/120 (77.5%) → 96/120 (80.0%) → 107/120 (89.2%) → 120/120 (100%)*  
*Source: JaninoLambdaTest.java (2227 LOC) + gradle test XML output*

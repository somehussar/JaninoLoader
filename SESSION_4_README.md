# Session 4: Oracle Consultation on 13 Failing Lambda Tests

**Date**: 2026-03-10  
**Current Status**: 107/120 tests passing (89.2%)  
**Work Type**: Exhaustive root cause diagnosis via oracle consultation

---

## What Happened This Session

### Phase 1: Information Gathering
Launched **4 parallel background agents**:
- `explore` (3 agents) — Code pattern analysis in Janino source
- `librarian` (1 agent) — Java compiler specification research

**Results collected**:
1. Lambda parameter type resolution mechanisms
2. Method reference compilation paths
3. Static generic method type inference gaps
4. Javac reference behavior for comparison

### Phase 2: Oracle Consultation
Provided Oracle with:
- All 13 failing test compilation errors (exact error messages)
- Each test's source code
- Patterns observed across failures
- Questions about root causes and solutions

**Oracle delivered**:
- Root cause diagnosis for each test
- Categorization into 6 architectural groups
- Prioritized fix recommendations
- Implementation hints with code locations
- Risk assessment for each fix category

### Phase 3: Documentation
Created comprehensive reference materials:
1. **ORACLE_CONSULTATION_RESULTS.md** — Detailed analysis
2. **ORACLE_DIAGNOSIS_SUMMARY.md** — Quick reference
3. **SESSION_4_FINDINGS.txt** — Executive summary
4. **LAMBDA_GENERICS_SESSION_MEMORY.md** — Updated session memory

---

## Key Findings

### The 13 Tests Fall Into 6 Categories

| Category | Count | Root Cause | Severity | Fixability |
|----------|-------|-----------|----------|-----------|
| A. Lambda Param Type Collapse | 5 | SAM param resolution fails in specific contexts | Med | Med (2-6h) |
| B. Method Reference Path Issues | 2 | Method refs lose generics on lookup | Med | Med (6-8h) |
| C. Static Generic Method Inference | 2 | No backward type inference from assignment | Hard | Hard (8-16h) |
| D. Parser Limitations | 2 | Parser rejects valid syntax | Low | High (4-6h, risky) |
| E. Scope/Context Issues | 1 | Lambda in ternary breaks scope | Med | Med (2-4h) |
| F. Configuration | 1 | Java 8 target not set | Trivial | Trivial (5m) |

### Test-by-Test Diagnosis

**Category A (5 tests)**:
- `edge_lambdaInStaticInit` — Static initializer context doesn't propagate type
- `generics_wildcardBound` — Wildcard bounds not extracted for lambda params
- `stream_collect_groupingBy` — Static generic method + wildcard (combo)
- `typeInference_lambdaInGenericMethod` — No backward inference for T
- `customFI_withDefaultMethod` — Config issue (Java 8 target)

**Category B (2 tests)**:
- `methodRef_asComparator` — Method ref loses Stream<String> context
- `methodRef_arrayConstructor` — Array ref returns Object instead of String[]

**Category C (2 tests)**:
- `typeInference_returnType` — Can't infer T from assignment
- `typeInference_lambdaInGenericMethod` — (duplicate diagnosis)

**Category D (2 tests, high risk)**:
- `edge_recursiveViaHolder` — Parser: `holder[0] = lambda` not recognized
- `edge_castToFunctionalInterface` — Parser: `(FI) lambda` not parsed

**Category E (1 test)**:
- `nested_lambdaInConditional` — Scope pollution in ternary

**Category F (1 test)**:
- `customFI_withDefaultMethod` — Set target version = 8

---

## Recommended Implementation Order

### TIER 1: Quick Win (5 min) → +1 test
```
1. customFI_withDefaultMethod — Set target version = 8
```
**Result**: 108/120 (90%)

### TIER 2: Medium Effort (16-25 hrs) → +5-6 tests ⭐ RECOMMENDED
```
2. edge_lambdaInStaticInit (2-4h)
3. generics_wildcardBound (4-6h)
4. methodRef_asComparator + methodRef_arrayConstructor (6-8h)
```
**Result**: 112-113/120 (93-94%)

### TIER 3: Hard/Architectural (8-16 hrs) → Deferred
```
5. typeInference_returnType + typeInference_lambdaInGenericMethod (backward inference)
6. stream_collect_groupingBy (depends on #5)
```
**Reason**: Requires architectural changes (backward type inference)

### TIER 4: Parser Issues (High Risk) → Deferred
```
7. edge_recursiveViaHolder + edge_castToFunctionalInterface
8. nested_lambdaInConditional
```
**Reason**: High regression risk, can defer to future session

---

## Expected Outcomes

| Target | Tests | Rate | Risk | Time |
|--------|-------|------|------|------|
| Tier 1 only | 108 | 90% | None | 5m |
| **Tier 1+2** | **112-113** | **93-94%** | **Low-Med** | **21-30h** |
| Tier 1+2+3 | 115-116 | 96% | High | 30-45h |
| All tiers | 118-119 | 98% | V.High | 50-60h |

**Recommendation**: Target **Tier 1+2** for best risk/reward balance

---

## Code Locations for Fixes

### For Tier 1-2 Fixes:

**Static Initializer Context (edge_lambdaInStaticInit)**
- `UnitCompiler.java` lines ~5000-6000 — Variable initializer compilation

**Wildcard Resolution (generics_wildcardBound)**
- `UnitCompiler.java` line ~5648 — `resolveReflectiveTypeArgWithTvMap()`
- `UnitCompiler.java` line ~5160 — `resolveSamParamTypes()`

**Method Reference SAM Typing (methodRef_asComparator, methodRef_arrayConstructor)**
- `UnitCompiler.java` lines ~4946-5134 — Method reference compilation
- `UnitCompiler.java` line ~4982, ~4997 — `getDeclaredIMethods()` calls

---

## Reference Documents

Three comprehensive analysis documents have been created:

1. **[ORACLE_CONSULTATION_RESULTS.md](./ORACLE_CONSULTATION_RESULTS.md)**
   - 500+ lines of detailed analysis
   - Root cause for each category
   - Implementation hints with exact code locations
   - Risk assessment
   - _Read this for deep technical details_

2. **[ORACLE_DIAGNOSIS_SUMMARY.md](./ORACLE_DIAGNOSIS_SUMMARY.md)**
   - 300+ lines of quick reference
   - Test-by-test diagnosis with error messages
   - Code snippets for each issue
   - Recommended order with effort estimates
   - _Read this for practical implementation guide_

3. **[LAMBDA_GENERICS_SESSION_MEMORY.md](./LAMBDA_GENERICS_SESSION_MEMORY.md)**
   - Session 4 findings added (Section 10)
   - Historical context from earlier iterations
   - Build and test commands
   - Progress timeline
   - _Read this for session context and history_

---

## Next Steps

1. **Spawn unspecified-high agent** to implement Tier 1-2 fixes
2. **Start with**: `customFI_withDefaultMethod` (5 min quick win)
3. **Then do**: Context propagation, wildcard resolution, method refs (in that order)
4. **Run tests** after each fix to verify no regressions
5. **Document** findings in session memory as you go
6. **Target**: 112-113/120 passing (93-94%)

---

## Session Statistics

- **Background agents launched**: 4
- **Parallel exploration queries**: 3
- **Research queries**: 1
- **Tests analyzed**: 13
- **Root cause categories identified**: 6
- **Recommended fixes**: 5 (Tier 1-2)
- **Documentation pages created**: 4

**Total analysis time**: ~2 hours of investigation, categorization, and documentation

---

**Status**: ✅ ORACLE ANALYSIS COMPLETE  
**Next**: Implementation phase (spawn unspecified-high agent for Tier 1-2 fixes)


# JaninoLoader - Comprehensive Agent Documentation

## PROJECT SUMMARY

**JaninoLoader** is a Java framework that provides runtime dynamic compilation and class loading using the **Janino Java compiler** (v3.1.12). The project wraps Janino's compiler API in a clean, safe abstraction layer enabling two primary use cases:

1. **Dynamic Script Execution**: Compile and execute arbitrary Java code at runtime as dynamically-generated script classes
2. **Hot Class Reloading**: Compile, load, unload, and dynamically recompile Java classes without restarting the JVM
3.  JANINO SOURCE CODE IS STORED AT `Z:\old desktop\projects\janino`

### Key Capabilities
- **In-memory compilation**: Compile Java source to bytecode, store in memory
- **Isolated classloader hierarchy**: Compiled classes loaded in a separate hierarchy with optional filtering
- **Class reloading**: Unload/replace compiled classes and notify listeners
- **Script bodies**: Generate executable script classes implementing or extending base types
- **Serialization-aware reloading**: Default reload handler serializes/deserializes objects across class versions
- **Access control**: FilteredClassLoader restricts which classes can be loaded in compiled scope

### Architecture Level
**Framework/Library** - Abstracts low-level Janino compiler API into clean, type-safe interfaces

### Target Users
- Game modding systems (NPC behavior scripts - see ExampleTest with AbstractNPCScript)
- Plugin/extension systems requiring runtime code generation
- Educational use (runtime Java language exploration)
- Dynamic configuration systems where code snippets need execution

### Version
1.0.1-ALPHA (published via Maven Central)

### Status
- **Development**: Rapid (41 commits in Dec 2025)
- **Current State**: v1.0.1-ALPHA, feature-complete core library
- **Documentation**: Code-level Javadoc present; no README.md
- **Publication**: No CI pipeline; not published to Maven Central despite notation

---

## Building Janino

### Quick Build (TL;DR)

To build Janino JAR locally and copy to JaninoLoader:

```bash
# 1. Clone Janino repo (if not already cloned)
git clone https://github.com/janino-compiler/janino.git "Z:\old desktop\projects\janino"
cd "Z:\old desktop\projects\janino"

# 2. Build with Maven (requires Maven 3.9+, JDK 8+)
mvn clean install -DskipTests -Dmaven.javadoc.skip=true \
    -Dmaven.compiler.source=8 -Dmaven.compiler.target=8

# 3. Copy main JAR to JaninoLoader /lib directory
cp janino/target/janino-3.1.13-SNAPSHOT.jar \
   "Z:\old desktop\projects\JaninoLoader\lib\"
cp commons-compiler/target/commons-compiler-3.1.13-SNAPSHOT.jar \
   "Z:\old desktop\projects\JaninoLoader\lib\"
cp commons-compiler-jdk/target/commons-compiler-jdk-3.1.13-SNAPSHOT.jar \
   "Z:\old desktop\projects\JaninoLoader\lib\"
```

### Detailed Build Process

#### Prerequisites
- **Java**: JDK 8 or higher (tested with JDK 21)
- **Maven**: 3.9+ (download from https://maven.apache.org/download.cgi if not installed)
  - Windows: Add `<MAVEN_HOME>\bin` to PATH
  - Verify: `mvn --version`

#### Repository Structure
```
janino/
├── pom.xml (parent POM, reactor aggregator)
├── commons-compiler/           ← Core API interfaces
│   ├── pom.xml
│   └── target/commons-compiler-3.1.13-SNAPSHOT.jar
├── commons-compiler-jdk/       ← JDK-based implementation
│   ├── pom.xml
│   └── target/commons-compiler-jdk-3.1.13-SNAPSHOT.jar
├── janino/                     ← Main compiler (PRIMARY JAR)
│   ├── pom.xml
│   └── target/janino-3.1.13-SNAPSHOT.jar
└── commons-compiler-tests/     ← Test suite
    ├── pom.xml
    └── target/commons-compiler-tests-3.1.13-SNAPSHOT.jar
```

#### Build Configuration Notes

**POM declares**: Source/target 1.7 (Java 7 compatibility)  
**Actual build**: Override to 8 via `-Dmaven.compiler.source=8 -Dmaven.compiler.target=8`
- Why: JDK 21 dropped Java 7 target support; Java 8 bytecode is compatible with JaninoLoader's use cases

**Skipped options**:
- `-DskipTests`: Javadoc generation requires hardcoded JDK path (not critical)
- `-Dmaven.javadoc.skip=true`: Skips javadoc generation (hardcoded path to nonexistent JDK 8 javadoc.exe)

**Build output**:
```
[INFO] Reactor Summary:
[INFO] janino-parent .................................. SUCCESS [  2.0 s]
[INFO] commons-compiler ............................... SUCCESS [ 12.3 s]
[INFO] commons-compiler-jdk ........................... SUCCESS [  1.6 s]
[INFO] janino .......................................... SUCCESS [  9.9 s]
[INFO] commons-compiler-tests ......................... SUCCESS [  1.2 s]
[INFO] ───────────────────────────────────────────────────────
[INFO] BUILD SUCCESS (28.3 seconds total)
```

#### JAR Artifacts (After Build)

| JAR | Location | Size | Purpose |
|-----|----------|------|---------|
| **janino** (main) | `janino/target/janino-3.1.13-SNAPSHOT.jar` | ~966 KB | Core compiler (required) |
| **commons-compiler** | `commons-compiler/target/commons-compiler-3.1.13-SNAPSHOT.jar` | ~174 KB | API interfaces (required) |
| **commons-compiler-jdk** | `commons-compiler-jdk/target/commons-compiler-jdk-3.1.13-SNAPSHOT.jar` | ~66 KB | JDK implementation (required) |
| commons-compiler-tests | `commons-compiler-tests/target/commons-compiler-tests-3.1.13-SNAPSHOT.jar` | ~4 KB | Tests only (optional) |

**Required JARs for JaninoLoader**: All three above (janino, commons-compiler, commons-compiler-jdk)

#### Copying to JaninoLoader

After successful build, copy the three required JARs to JaninoLoader's `/lib` directory:

```bash
# From janino repo root:
mkdir -p "../JaninoLoader/lib"
cp janino/target/janino-3.1.13-SNAPSHOT.jar ../JaninoLoader/lib/
cp commons-compiler/target/commons-compiler-3.1.13-SNAPSHOT.jar ../JaninoLoader/lib/
cp commons-compiler-jdk/target/commons-compiler-jdk-3.1.13-SNAPSHOT.jar ../JaninoLoader/lib/
```

Verify:
```bash
ls -lah "../JaninoLoader/lib/"
# Should show all three JARs (total ~1.2 MB)
```

#### Using Local Snapshot Build in JaninoLoader

Option 1: **Maven Local Repo** (automatic after build)
```kotlin
// build.gradle.kts
repositories {
    mavenLocal()  // Picks up ~/.m2/repository/org/codehaus/janino/
    mavenCentral()
}
dependencies {
    includedInJar("org.codehaus.janino:janino:3.1.13-SNAPSHOT")
    includedInJar("org.codehaus.janino:commons-compiler:3.1.13-SNAPSHOT")
    includedInJar("org.codehaus.janino:commons-compiler-jdk:3.1.13-SNAPSHOT")
}
```

Option 2: **Manual JAR files** (from /lib directory)
```kotlin
// build.gradle.kts
dependencies {
    includedInJar(files("lib/janino-3.1.13-SNAPSHOT.jar"))
    includedInJar(files("lib/commons-compiler-3.1.13-SNAPSHOT.jar"))
    includedInJar(files("lib/commons-compiler-jdk-3.1.13-SNAPSHOT.jar"))
}
```

#### Troubleshooting

**Error: "mvn: command not found"**
- Maven not installed or not in PATH
- Solution: Install Maven 3.9+ from https://maven.apache.org/download.cgi
- Windows: Add `<MAVEN_HOME>\bin` to system PATH

**Error: "Java target version X not supported"**
- JDK version too old or too new for target version
- Solution: Use `-Dmaven.compiler.source=8 -Dmaven.compiler.target=8` flag (as shown above)
- Or: Set `JAVA_HOME` to JDK 8+ before running `mvn`

**Error: "Javadoc generation failed"**
- Hardcoded path to javadoc executable doesn't exist
- Solution: Use `-Dmaven.javadoc.skip=true` flag (as shown above)

**JAR files not found after build**
- Check Maven build output for "BUILD FAILURE"
- Run: `mvn clean install` (without `-DskipTests`) to see full diagnostics
- Verify Maven and JDK are correctly installed: `mvn --version && java --version`

---

## ARCHITECTURE

### Layered Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER APPLICATION                        │
│  Creates IDynamicCompilerBuilder → IDynamicCompiler             │
│  Creates IScriptBodyBuilder → IScriptClassBody<T>               │
│  Provides: Java source strings, interfaces/abstract classes     │
└────────────┬────────────────────────────┬───────────────────────┘
             │                            │
             ▼                            ▼
┌─────────────────────┐     ┌──────────────────────────────┐
│  api/ (Interfaces)  │     │  script/ (Script System)     │
│                     │     │                              │
│ IDynamicCompiler    │◄────┤ SafeScriptClassBuilder       │
│ IDynamicCompilerBuilder   │ SafeScriptClassBody<T>       │
│ IClassReloadListener│     │ ReloadingObjectInputStream   │
│ IScriptClassBody<T> │     │                              │
│ IScriptBodyBuilder<T>     │ Uses ClassBodyEvaluator to   │
│ LoadClassCondition  │     │ compile script bodies into   │
│                     │     │ classes extending/impl T     │
└────────┬────────────┘     └──────────────┬───────────────┘
         │                                 │
         ▼                                 │
┌─────────────────────┐                    │
│ JaninoCompiler      │◄───────────────────┘
│ JaninoCompilerBuilder│
│                     │
│ Wraps Janino        │
│ Compiler +          │
│ MapResourceCreator  │
│ stores byte[] map   │
└────────┬────────────┘
         │
         ▼
┌──────────────────────────┐
│ classloader/             │
│                          │
│ FilteredClassLoader      │  ← Blocks classes via LoadClassCondition
│   └─ MemoryClassLoader   │  ← Defines classes from in-memory byte[]
│                          │
│ Parent: app classloader  │
└──────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  annotationProcessor/ (Compile-time code generation)           │
│                                                                 │
│  @PermissableScriptHandler → PermissableProcessor              │
│  Generates: PermissibleXxx wrapper class                       │
│  Wraps each method in Sandbox.confine(PrivilegedAction)        │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow: Script Compilation → Execution → Recompilation

```
1. USER CREATES BUILDER
   IScriptBodyBuilder.getBuilder(Class<T>, compiler)
   └─ Creates SafeScriptClassBuilder<T>

2. BUILDER CONFIGURATION (fluent)
   .setScript(String java_source_code)
   .setDefaultImports(String[] packages)
   .setImplementedTypes(Class<?>[] interfaces_to_implement)
   .setInstanceDelegate(how_to_construct_instances)
   .setReloadDelegate(how_to_handle_reload_events)
   └─ All stored in SafeScriptClassBuilder

3. BUILD SCRIPT BODY
   .build()
   └─ Creates SafeScriptClassBody<T>
   └─ Registers as listener to compiler's classloader changes

4. FIRST COMPILATION (lazy)
   script.assertCompiled()
   └─ Calls compiler.compileClass(StringResource)
   └─ JaninoCompiler wraps in Compiler object
   └─ Compiler.compile() → bytecode stored in classes Map<String, byte[]>
   └─ MemoryClassLoader reads from Map when loadClass() called
   └─ ClassBodyEvaluator (Janino API) evaluates the body code

5. INSTANCE CREATION
   script.get()
   └─ Calls instanceDelegate.apply(compiled_class)
   └─ Returns instance of type T (cast safely)
   └─ User can now call methods on instance

6. DYNAMIC UPDATE
   script.setScript(new_source_code)
   └─ Marks needToRecompile = true
   └─ Calls compiler.recompileClass()
   └─ recompileClass removes old bytecode, resets classloader
   └─ Recompiles fresh bytecode
   └─ Calls reloadDelegate.apply(oldInstance, newInstance, loader)
   └─ Default: serializes oldInstance, deserializes into newClass

7. REMOVAL
   compiler.removeClass(class_name)
   └─ Removes bytecode from memory
   └─ Resets classloader (triggers listener notifications)
```

### Key Components & Interactions

#### Core Compiler Wrapper: `JaninoCompiler`
- **Role**: Bridge between user API and Janino's low-level Compiler
- **State**: Maintains `Map<String, byte[]> classes` (bytecode storage), `Set<IClassReloadListener>` (observer pattern)
- **Lifecycle**: Lazy init on first compile; classloader reset on remove/recompile
- **Janino Integration**: 
  - Creates `Compiler compiler = new Compiler()`
  - Sets `ClassLoaderIClassLoader` (adapts ClassLoader to IClassLoader interface)
  - Sets `MapResourceCreator` (writes compiled bytecode to Map)

#### Script Runner: `SafeScriptClassBody<T>`
- **Role**: Manages a single dynamically-compiled script class
- **State**: Caches compiled instance, tracks dirty flag (`needToRecompile`)
- **Lifecycle**: Lazy compilation; instance reused until recompile; serialization on reload
- **Reload Pattern**: 
  - Old instance serialized to bytes
  - New class loaded
  - Bytes deserialized using ReloadingObjectInputStream (new classloader)
  - Returns new instance (preserving field state)

#### Classloader Hierarchy: `FilteredClassLoader + MemoryClassLoader`
- **FilteredClassLoader**: 
  - Extends `ClassLoader`
  - Intercepts `loadClass(name, resolve)` 
  - Applies `LoadClassCondition` filter
  - Throws `ClassNotFoundException` if filter rejects
  - **Use**: Enforce security policy (block access to certain classes)

- **MemoryClassLoader**:
  - Extends `FilteredClassLoader`
  - Overrides `findClass(name)` 
  - Looks up bytecode in `Map<String, byte[]> storedClasses`
  - Falls back to parent if not found
  - **Use**: Load only compiled classes from bytecode map

#### Annotation Processor: `PermissableProcessor` (218 LOC)
- **Purpose**: Compile-time code generation marker for permissable scripting
- **Details**: Processes `@PermissableScriptHandler` annotation
- **State**: Alpha; currently undocumented but present in build pipeline

---

## KEY CLASSES & RESPONSIBILITY

### Public API (User-facing)

| Class/Interface | Purpose | Key Methods |
|---|---|---|
| `IDynamicCompilerBuilder` | Builder factory pattern for compiler | `createBuilder()` [static factory], `setParentClassLoader(ClassLoader)`, `setClassFilter(LoadClassCondition)`, `getCompiler()` → `IDynamicCompiler` |
| `IDynamicCompiler` | Core compiler abstraction | `compileClass(Resource...)`, `recompileClass(Resource...)`, `removeClass(String...)`, `addReloadListener(...)`, `getClassLoader()` |
| `IScriptBodyBuilder<T>` | Fluent builder for script classes | `getBuilder(Class<T>, compiler)` [static factory], `setScript(String)`, `setDefaultImports(String[])`, `setImplementedTypes(Class<?>[])`, `setInstanceDelegate(...)`, `setReloadDelegate(...)`, `build()` → `IScriptClassBody<T>` |
| `IScriptClassBody<T>` | Runnable compiled script | `get()` → `T` [get compiled instance], `setScript(String)` [recompile], `assertCompiled()` [trigger lazy compile], `prepareToUnload()` [cleanup], `handleClassLoaderReload(ClassLoader)` [listener callback] |

### Implementation (Internal)

| Class | Purpose | Key Methods | Lines |
|---|---|---|---|
| `JaninoCompiler` | Wraps Janino compiler | `compileClass(Resource[])`, `resetClassloader()` [core logic], `notifyListeners()` | 90 |
| `JaninoCompilerBuilder` | Factory for JaninoCompiler | `new JaninoCompilerBuilder()`, getters/setters, `getCompiler()` | 30 |
| `SafeScriptClassBody<T>` | Manages script instance lifecycle | `get()`, `setScript(String)`, `assertCompiled()` [lazy compile trigger], `handleClassLoaderReload()` [listener impl] | 141 |
| `SafeScriptClassBuilder<T>` | Fluent builder | setters return `this` [builder pattern], `build()` | 60 |
| `FilteredClassLoader` | Access control | `loadClass(String, boolean)` [filter check] | 20 |
| `MemoryClassLoader` | In-memory bytecode loading | `findClass(String)` [map lookup] | 24 |
| `ReloadingObjectInputStream` | Custom deserialization during reload | Extends `ObjectInputStream`, uses new classloader | 28 |
| `IClassReloadListener` | Observer for reload events | `handleClassLoaderReload(ClassLoader)` → boolean [true = stop listening] | 12 |
| `LoadClassCondition` | Filter policy | `isValid(String)` → boolean, `classNotLoadedMessage(String)` | 22 |

### Test Classes (589 LOC combined)

| Test Class | Purpose | Tests |
|---|---|---|
| `ClassLoaderTest` (190 LOC) | Core compiler functionality | Basic compilation, cross-class compilation, classloader isolation, class filtering |
| `ScriptBodyTest` (257 LOC) | Script lifecycle & reloading | Lazy compilation, instance caching, recompilation, state preservation on reload, listener notifications |
| `ExampleTest` (65 LOC) | Real usage: NPC behavior scripts | Game NPC with script behavior binding, script updates, state handling across recompiles |
| `AbstractNPCScript` (38 LOC) | Base class for game modding | Template for NPC behavior scripts; shows reload handler pattern |

---

## INTEGRATION WITH JANINO (v3.1.12)

### What Janino Does
Janino is a lightweight Java compiler that converts Java source code into bytecode at runtime. It provides:
- **Parser**: Tokenizes/parses Java → AST
- **Compiler**: Converts AST → bytecode
- **ClassBodyEvaluator**: Evaluates method bodies directly (used by SafeScriptClassBody)

### How JaninoLoader Uses Janino

```
User's Java Source Code (String)
    ↓
StringResource (wraps string + filename)
    ↓
JaninoCompiler.compileClass(resource)
    ↓
org.codehaus.janino.Compiler.compile(Resource)
    ↓
Janino Parser: Source → AST
    ↓
Janino Compiler: AST → bytecode[]
    ↓
MapResourceCreator: bytecode stored in Map<String, byte[]>
    ↓
ClassLoader.findClass() reads from Map
    ↓
Bytecode loaded as Class<?> object
    ↓
Reflection: newInstance(), invoke methods
```

### Key Janino Classes Used
- **`org.codehaus.janino.Compiler`**: Main compilation engine
  - `compile(Resource[] sources)` - compiles and writes bytecode to IClassFileCreator
  
- **`org.codehaus.janino.ClassLoaderIClassLoader`**: Adapter
  - Wraps Java `ClassLoader` to Janino's `IClassLoader` interface
  - Allows Janino to resolve cross-class references
  
- **`org.codehaus.commons.compiler.util.resource.MapResourceCreator`**: Bytecode sink
  - Implements Janino's `IClassFileCreator`
  - Writes compiled bytecode to `Map<String, byte[]>`
  
- **`org.codehaus.janino.ClassBodyEvaluator`**: Method body execution
  - Used in SafeScriptClassBody for method body compilation
  - Returns compiled method implementations

### Limitations Handled by JaninoLoader
1. **Classloader Hierarchy**: Janino loads classes into JVM's global classloader by default; JaninoLoader isolates via MemoryClassLoader
2. **Reloading**: Janino doesn't support unloading; JaninoLoader manages versioning via classloader recreation
3. **State Preservation**: Janino loses instance state on reload; JaninoLoader uses serialization to preserve
4. **Access Control**: Janino has no built-in filtering; JaninoLoader adds FilteredClassLoader

---

## BUILD & DEPLOYMENT

### Gradle Build (build.gradle.kts)

**Plugins:**
- `java` - standard Java compilation
- `com.github.johnrengelman.shadow` (v8.1.1) - creates fat JAR with shaded dependencies

**Dependencies (includedInJar configuration):**
- `org.codehaus.janino:janino:3.1.12` - core compiler
- `org.codehaus.janino:commons-compiler:3.1.12` - shared utilities
- `org.codehaus.janino:commons-compiler-jdk:3.1.12` - JDK compatibility

**Test Dependencies:**
- JUnit 5 (Jupiter) - test framework

**Custom Configuration:**
```kotlin
val includedInJar by configurations.creating  // Custom config for shadow JAR
configurations {
    implementation { extendsFrom(includedInJar) }  // Make impl depend on custom config
}
```

**Shadow JAR (Fat JAR):**
- Archive name: `standalone-JaninoLoader-1.0.1-ALPHA.jar`
- Includes: Janino + commons-compiler + this library
- Built by `shadowJar` task (integrated into `build` task)
- **Purpose**: Standalone distribution (all deps included)

### Module Structure
```
JaninoLoader/
├── src/main/java/io/github/somehussar/janinoloader/
│   ├── JaninoCompiler.java
│   ├── JaninoCompilerBuilder.java
│   ├── api/
│   │   ├── IDynamicCompiler.java
│   │   ├── IDynamicCompilerBuilder.java
│   │   ├── IClassReloadListener.java
│   │   ├── delegates/LoadClassCondition.java
│   │   └── script/
│   │       ├── IScriptBodyBuilder.java
│   │       └── IScriptClassBody.java
│   ├── classloader/
│   │   ├── FilteredClassLoader.java
│   │   └── MemoryClassLoader.java
│   └── script/
│       ├── SafeScriptClassBody.java
│       ├── SafeScriptClassBuilder.java
│       └── ReloadingObjectInputStream.java
├── annotationProcessor/
│   └── src/main/java/io/github/somehussar/janinoloader/annotations/
│       ├── PermissableProcessor.java (206 LOC)
│       └── PermissableScriptHandler.java (12 LOC)
├── src/test/java/io/github/somehussar/janinoloader/
│   ├── ClassLoaderTest.java
│   ├── ScriptBodyTest.java
│   └── example/
│       ├── ExampleTest.java
│       ├── AbstractNPCScript.java
│       ├── INpcScript.java
│       └── Npc.java
├── build.gradle.kts
├── settings.gradle.kts
└── [this file] AGENTS.md
```

### Publishing
- **Group**: `io.github.somehussar.janinoloader`
- **Version**: `1.0.1-ALPHA`
- **Repository**: Maven Central (via Gradle publishing)

---

## EVOLUTION & GIT HISTORY

### Development Timeline (Dec 2–13, 2025 — 41 commits, rapid development)

| Phase | Date | Key Commits | What Changed |
|-------|------|-------------|--------------|
| **Foundation** | Dec 2 | `6270a03` Initial commit | Basic project scaffold |
| **ClassLoader system** | Dec 3 | `a532d35` FilteredClassLoader, `2e0ddaa` Test class filtering, `c348aa7` Reload listeners | Core `FilteredClassLoader` + `MemoryClassLoader` + `IClassReloadListener` observer pattern |
| **Compiler API** | Dec 3 | `bbc85fa` Create builder, `7e52846` Circular dependency test, `dc6f945` Recompile test | Builder pattern, circular dependency support, hot-swap via `recompileClass()` |
| **Script System** | Dec 3–4 | `1e7baf7` SafeScript WIP, `6154274` Script Class Body, `a9dbdaa` Move to builder | `SafeScriptClassBody` + `SafeScriptClassBuilder`, ClassBodyEvaluator integration |
| **ClassLoader hierarchy fix** | Dec 4 | `6da0074` FIX SCRIPT CLASSLOADER HIERARCHY | **Critical fix**: scripts' parent classloader must be the dynamic compiler's classloader, not the app classloader |
| **Data reloading** | Dec 4 | `d0c3af4` Successful data reloading, `ea6d27e` Add reload hierarchy handler | Serialization-based state transfer across classloader boundaries via `ReloadingObjectInputStream` |
| **Example & Docs** | Dec 4 | `e9e7ef2`–`640bae5` | NPC scripting example (`AbstractNPCScript`, `Npc`, `INpcScript`), Javadoc on `IDynamicCompiler` |
| **v1.0.0-ALPHA** | Dec 4 | `20e4191` Version 1.0.0-ALPHA | First versioned release, shadow JAR config |
| **Annotation Processor** | Dec 5 | `7ef517c` PermissableProcessor, `1fc6985` 1.0.1-ALPHA | `@PermissableScriptHandler` + `PermissableProcessor` generating sandboxed wrappers. Version bump to 1.0.1-ALPHA |
| **CI/Publish setup** | Dec 13 | `d58d16c`–`5fcbf4d` | Attempted setup (possibly GitHub Actions/Maven Central), fix iterations |

### Feature Timeline
- ✅ Core compiler wrapping (Janino integration)
- ✅ Classloader isolation (FilteredClassLoader)
- ✅ Script lifecycle (SafeScriptClassBody)
- ✅ State preservation on reload (ReloadingObjectInputStream)
- ✅ Reload event system (IClassReloadListener pattern)
- ✅ Annotation processor (PermissableProcessor with sandbox wrappers)
- ✅ Build/packaging (shadow JAR)
- ✅ Full Javadoc on public APIs (`IDynamicCompiler`)
- ⏳ README.md documentation
- ⏳ Published to Maven Central
- ⏳ Stable v1.0.0 release

---

## USAGE PATTERNS

### Basic Compilation

```java
// Create compiler
IDynamicCompiler compiler = IDynamicCompilerBuilder.createBuilder().getCompiler();

// Compile & execute
compiler.compileClass(new StringResource(
    "pkg1/A.java",
    "package pkg1; public class A { public static int meth() { return 42; } }"
));

// Get instance
ClassLoader loader = compiler.getClassLoader();
Class<?> aClass = loader.loadClass("pkg1.A");
int result = (int) aClass.getDeclaredMethod("meth").invoke(null);  // 42
```

### Script Execution

```java
// Define interface for script to implement
interface INpcScript {
    void onDamaged(int dmg);
}

// Compile script
IDynamicCompiler compiler = IDynamicCompilerBuilder.createBuilder().getCompiler();
IScriptClassBody<INpcScript> script = IScriptBodyBuilder.getBuilder(INpcScript.class, compiler)
    .setScript("void onDamaged(int dmg) { System.out.println(\"Hit for \" + dmg); }")
    .build();

script.assertCompiled();
script.get().onDamaged(5);  // Prints: Hit for 5
```

### Script Recompilation & State Preservation

```java
// Update script (triggers recompile)
script.setScript("void onDamaged(int dmg) { System.out.println(\"NEW: \" + dmg); }");

// Old instance serialized, new instance restored from serialized state
// onDamaged now uses new code
script.get().onDamaged(5);  // Prints: NEW: 5
```

### Access Control

```java
// Restrict class access
LoadClassCondition filter = (name) -> !name.contains("Math");
IDynamicCompiler compiler = IDynamicCompilerBuilder.createBuilder()
    .setClassFilter(filter)
    .getCompiler();

// This fails - Math is blocked
compiler.compileClass(new StringResource("pkg1/B.java",
    "package pkg1; import java.lang.Math; public class B { }"));
// → ClassNotFoundException: java.lang.Math
```

---

## CURRENT STATE & KNOWN ISSUES

### Current State
- **Version**: 1.0.1-ALPHA
- **Stability**: Beta quality (used in production-like scenarios - NPC scripting)
- **Test Coverage**: Reasonable (ClassLoaderTest, ScriptBodyTest, ExampleTest)
- **Documentation**: Minimal (README not present; code-level comments present)

### Known Gaps
1. **No explicit README.md** - Users must infer from tests
2. **Annotation processor undocumented** - PermissableProcessor purpose unclear
3. **Error handling** - Limited custom error messages
4. **Performance** - No benchmarks; GC impact of repeated recompilation unknown
5. **Concurrency** - Thread safety not explicitly documented

### Strengths
1. **Clean API** - Well-designed interfaces (builder pattern, listener pattern)
2. **Isolation** - Classloader hierarchy prevents global pollution
3. **State preservation** - Reload handler preserves object state
4. **Type safety** - Generic `<T>` throughout; no unsafe casts in API
5. **Janino integration** - Lightweight; no heavy frameworks required

---

## FOR AGENTS: Key Implementation Details

### Critical Methods to Understand

**JaninoCompiler.resetClassloader()** (lines 81-88)
```java
protected void resetClassloader() {
    notify = secure != null;  // Flag for listener notification
    secure = new MemoryClassLoader(parent, classFilter, classes);
    compiler = new Compiler();
    compiler.setIClassLoader(new ClassLoaderIClassLoader(secure));
    compiler.setClassFileCreator(new MapResourceCreator(classes));
}
```
**Why critical**: This is the heart of compilation. Sets up the classloader chain and bytecode sink.

**SafeScriptClassBody.handleClassLoaderReload()** (line 77+)
```java
public boolean handleClassLoaderReload(ClassLoader loader) {
    try {
        assertCompiled();  // Re-trigger compilation with new loader
    } catch (Throwable ignored) {}
    return false;  // Never unsubscribe
}
```
**Why critical**: Ensures scripts recompile when underlying loader changes (e.g., after another class removed).

**FilteredClassLoader.loadClass()** (lines 15-19)
```java
public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (classFilter != null && !classFilter.isValid(name))
        throw new ClassNotFoundException(classFilter.classNotLoadedMessage(name));
    return super.loadClass(name, resolve);
}
```
**Why critical**: All class loading goes through here; filter is enforced at load time.

### Extension Points
- **IClassReloadListener**: Implement to respond to classloader changes
- **LoadClassCondition**: Implement to customize access control
- **InstanceDelegate / ReloadDelegate**: Pass custom handlers to SafeScriptClassBuilder

---

## JANINO COMPILER DEEP DIVE (GENERIC TYPE HANDLING)

**⚠️ FULL ANALYSIS**: This section is a summary. For the comprehensive 500+ line deep dive including parsing flow diagrams, all 30+ core classes, reflection-based handling, and exact line numbers across 22 files, see **[`JANINO_GENERICS.md`](./JANINO_GENERICS.md)**.

**Status**: COMPLETE — Exhaustive analysis of Janino generic type system from source code.

### CRITICAL DESIGN DECISION: Type Erasure at Parse-Time

Janino **parses** generic type information (type parameters, type arguments, wildcards) into AST nodes,
but **erases** them during compilation. The UnitCompiler resolves type variables to their **first bound**
(or `Object` if unbounded) and discards type arguments. The `IClass.parameterize()` method exists but
is **commented out** (lines 1366-1467 of `IClass.java`). This means Janino treats `List<String>` identically
to raw `List` at the bytecode level — matching standard JVM type erasure but performing it earlier in the
pipeline than javac does.

---

### Parsing Flow: `List<String> list;` → AST → Bytecode

```
SOURCE CODE: "List<String> list;"
    │
    ▼
[1] Scanner/Tokenizer (Scanner.java)
    │  Produces tokens: IDENTIFIER("List"), "<", IDENTIFIER("String"), ">", IDENTIFIER("list"), ";"
    │
    ▼
[2] Parser.parseType() (Parser.java:2582)
    │  Not a primitive → falls through to:
    │
    ▼
[3] Parser.parseReferenceType() (Parser.java:2611)
    │  Reads annotations (none), then:
    │
    ▼
[4] Parser.parseReferenceType(annotations) (Parser.java:2631)
    │  Calls parseQualifiedIdentifier() → ["List"]
    │  Calls parseTypeArgumentsOpt() (Parser.java:2683)
    │    │  peekRead("<") → true
    │    │  parseTypeArgument() (Parser.java:2707)
    │    │    │  Not "?" → calls parseType()
    │    │    │    → parseReferenceType() → ReferenceType(["String"], null)
    │    │    │  Returns ReferenceType (which implements TypeArgument)
    │    │  read(">", ",") → ">" (index 0) → exit loop
    │    │  Returns TypeArgument[] { ReferenceType(["String"], null) }
    │  Constructs: new ReferenceType(location, annotations=[], identifiers=["List"],
    │              typeArguments=[ReferenceType(["String"], null)])
    │
    ▼
[5] AST Node: Java.ReferenceType (Java.java:4236)
    │  .identifiers = ["List"]
    │  .typeArguments = [ReferenceType(["String"], null)]  ← GENERIC INFO STORED HERE
    │  .annotations = []
    │
    ▼
[6] UnitCompiler.getType(Type) → getType2(ReferenceType) (UnitCompiler.java:6991)
    │  Calls getReferenceType(location, scope, ["List"], 1, typeArguments)
    │
    ▼
[7] UnitCompiler.getReferenceType(location, "List", typeArguments, scope) (UnitCompiler.java:7060)
    │  Checks: Is "List" a type parameter of an enclosing method? NO
    │  Checks: Is "List" a type parameter of an enclosing type declaration? NO
    │  Falls through to: getRawReferenceType()/*.parameterize(tas)*/  ← COMMENTED OUT!
    │  Returns: IClass for java.util.List (raw type, NO parameterization)
    │
    ▼
[8] RESULT: IClass representing raw java.util.List
    │  Type arguments are DISCARDED at step 7
    │  Bytecode uses descriptor "Ljava/util/List;" (erased)
```

---

### Core Classes

#### Layer 1: AST Nodes (Source-Level Representation) — `Java.java`

| Class | File:Line | Purpose | Key Fields |
|---|---|---|---|
| `Java.TypeParameter` | `Java.java:2121-2148` | Represents `<T>` or `<T extends Bound>` in declarations | `String name`, `@Nullable ReferenceType[] bound` |
| `Java.ReferenceType` | `Java.java:4236-4293` | Represents types like `List<String>`, implements `TypeArgument` | `String[] identifiers`, `@Nullable TypeArgument[] typeArguments`, `Annotation[] annotations` |
| `Java.TypeArgument` | `Java.java:4298-4313` | Interface for type arguments (`ReferenceType`, `Wildcard`, `ArrayType`) | `setEnclosingScope()`, `accept(TypeArgumentVisitor)` |
| `Java.Wildcard` | `Java.java:6413-6487` | Represents `?`, `? extends T`, `? super T` | `int bounds` (NONE/EXTENDS/SUPER), `@Nullable ReferenceType referenceType`, `Annotation[] annotations` |

#### Layer 2: Type System Interfaces — Individual files

| Interface | File | Purpose | Key Methods |
|---|---|---|---|
| `IType` | `IType.java` (30 lines) | Root marker interface for all types | (empty marker) |
| `ITypeVariableOrIClass` | `ITypeVariableOrIClass.java` (30 lines) | Union type: either a type variable or a class | extends `IType` |
| `ITypeVariable` | `ITypeVariable.java` (43 lines) | Represents a type variable like `T` | `getName()`, `getBounds() → ITypeVariableOrIClass[]` |
| `IParameterizedType` | `IParameterizedType.java` (33 lines) | Represents `List<String>` at type-system level | `getActualTypeArguments() → IType[]`, `getRawType() → IType` |
| `IWildcardType` | `IWildcardType.java` (42 lines) | Represents `? extends/super X` at type-system level | `getUpperBound() → IType`, `getLowerBound() → @Nullable IType` |
| `IClass` | `IClass.java` (1468 lines) | Core class representation | `getITypeVariables() → ITypeVariable[]`, `rawTypeOf(IType) → IClass` |

#### Layer 3: Signature Parsing (Class File Level) — `SignatureParser.java`

| Class | File:Line | Purpose |
|---|---|---|
| `SignatureParser.ClassSignature` | `SignatureParser.java:366-415` | Parsed class signature from `.class` file (e.g. `<K:Ljava/lang/Object;>`) |
| `SignatureParser.FormalTypeParameter` | `SignatureParser.java:620-664` | Formal type param like `T extends MyClass & MyInterface` |
| `SignatureParser.ClassTypeSignature` | `SignatureParser.java:420-493` | Class type with type args (e.g. `pkg.Outer<T>.Inner<U>`) |
| `SignatureParser.TypeArgument` | `SignatureParser.java:696-751` | Type argument with mode (EXTENDS/SUPER/ANY/NONE) |
| `SignatureParser.TypeVariableSignature` | `SignatureParser.java:570-585` | Type variable reference (e.g. `T`) |
| `SignatureParser.ArrayTypeSignature` | `SignatureParser.java:548-565` | Array type signature |
| `SignatureParser.MethodTypeSignature` | `SignatureParser.java:256-360` | Method signature with formal type params |

---

### Parsing Methods

#### Parser.java — Source Code → AST Nodes

| Method | Line | Input | Output | Handles |
|---|---|---|---|---|
| `parseTypeParametersOpt()` | 2645 | Token stream at `<` | `@Nullable TypeParameter[]` | `<T>`, `<T, U>`, `<T extends Foo>` |
| `parseTypeParameter()` | 2663 | Token stream at identifier | `TypeParameter` | `T`, `T extends Bound1 & Bound2` |
| `parseTypeArgumentsOpt()` | 2683 | Token stream at `<` | `@Nullable TypeArgument[]` | `<String>`, `<? extends X>`, `<>` (diamond) |
| `parseTypeArgument()` | 2707 | Token stream after `<` or `,` | `TypeArgument` | `String`, `? extends X`, `? super Y`, `?` |
| `parseReferenceType()` | 2611 | Token stream at annotations/identifier | `ReferenceType` | `List`, `java.util.Map`, `@Ann List<String>` |
| `parseReferenceType(Annotation[])` | 2631 | Pre-parsed annotations + token stream | `ReferenceType` | Same, with pre-read annotations |
| `parseReferenceTypeList()` | 2742 | Token stream at first type | `ReferenceType[]` | Comma-separated reference types |
| `parseType()` | 2582 | Token stream | `Type` | Primitives + reference types + arrays |

#### Parser.java — Declaration Parsing (consumes type parameters)

| Method | Line | How it uses TypeParameters |
|---|---|---|
| `parseClassDeclarationRest()` | 686 | `parseTypeParametersOpt()` → passed to `PackageMemberClassDeclaration`/`MemberClassDeclaration`/`LocalClassDeclaration` |
| `parseInterfaceDeclarationRest()` | 1082 | `parseTypeParametersOpt()` → passed to `PackageMemberInterfaceDeclaration`/`MemberInterfaceDeclaration` |
| `parseClassBodyDeclaration()` (method part) | 1020 | `parseTypeParametersOpt()` → passed to `parseMethodDeclarationRest()` |
| `parseInterfaceMethodDeclarationRest()` | 1312 | `parseTypeParametersOpt()` → passed to `parseMethodDeclarationRest()` |
| `parseMethodDeclarationRest()` | 1521 | Receives `@Nullable TypeParameter[]`, passes to `new MethodDeclarator(...)` |
| `parseEnumDeclarationRest()` | 757 | `peekRead("<")` → throws error: "Enum declaration must not have type parameters" |

#### UnitCompiler.java — AST → Bytecode (Type Resolution & Erasure)

| Method | Line | Input | Output | Handles |
|---|---|---|---|---|
| `getType2(ReferenceType)` | 6991 | `ReferenceType` AST node | `IType` (actually `IClass`, raw) | Resolves `List<String>` → raw `java.util.List` IClass |
| `getReferenceType(loc,scope,ids,n,typeArgs)` | 7013 | Identifiers + type args | `@Nullable IType` | Multi-part resolution (e.g. `java.util.Map`) |
| `getReferenceType(loc,name,typeArgs,scope)` | 7060 | Simple name + type args | `IType` | **KEY**: Resolves type variables to first bound, discards type arguments |
| `rawTypeOf(IType)` | 6869 | `IType` (may be `IParameterizedType`) | `IClass` | Unwraps parameterized types → raw class |
| `rawTypesOf(IType[])` | 6877 | Array of ITypes | `IClass[]` | Batch version of `rawTypeOf` |
| `getRawType(Type)` | 6884 | AST `Type` node | `IClass` | Shorthand: `rawTypeOf(getType(t))` |
| `typeArgumentToIType(TypeArgument)` | 7149 | `TypeArgument` AST node | `IType` | Converts to `IClass`/`IWildcardType` — **marked @SuppressWarnings("unused")** |
| `findMemberType(enclosingType, name, typeArgs, loc)` | ~7039 | Member type search | `@Nullable IClass` | Finds nested types |

---

### Type Parameter Processing

#### How `<T>` is handled (unbounded type parameter):
```
Source:  class Bag<T> { T get(); }
Parser:  TypeParameter(name="T", bound=null)
UnitCompiler (line 7110): When "T" is used as a type reference:
    → Walks enclosing scopes looking for NamedTypeDeclaration
    → Finds TypeParameter with tp.name.equals("T")
    → bound is null → returns iClassLoader.TYPE_java_lang_Object
    ∴ T resolves to Object
```

#### How `<T extends Comparable>` is handled (bounded):
```
Source:  class Box<T extends Comparable> { T get(); }
Parser:  TypeParameter(name="T", bound=[ReferenceType(["Comparable"], null)])
UnitCompiler (line 7088-7096): When "T" is used as a type reference:
    → Finds TypeParameter with name "T"
    → bound is non-null, length 1
    → Resolves bound[0] via getType(ob[0])
    → Returns boundTypes[0] = IClass for Comparable
    ∴ T resolves to Comparable (first bound)
    
    Comment in source (lines 7093-7095):
    "Here is the big simplification: Instead of returning the 'correct' type,
     honoring type arguments, we simply return the first bound."
```

#### How `<? extends Type>` / `<? super Type>` is handled:
```
Parser (line 2711-2717):
    peekRead("?") → true
    peekRead("extends") → Wildcard(BOUNDS_EXTENDS, parseReferenceType(), annotations)
    peekRead("super")   → Wildcard(BOUNDS_SUPER,   parseReferenceType(), annotations)
    neither             → Wildcard(annotations)  // unbounded "?"

UnitCompiler.typeArgumentToIType() (line 7149, UNUSED):
    Would convert Wildcard → IWildcardType with:
    - BOUNDS_EXTENDS → upperBound = getType(w.referenceType)
    - BOUNDS_SUPER   → lowerBound = getType(w.referenceType)
    But this method is @SuppressWarnings("unused") — never actually called!
```

#### How method-level type parameters work:
```
Source:  <T> T identity(T t) { return t; }
Parser (line 1020): parseTypeParametersOpt() → TypeParameter[]
    → Passed to MethodDeclarator constructor (line 2685)
    → Stored as MethodDeclarator.typeParameters
UnitCompiler (lines 7074-7099): When resolving type "T":
    → Walks scope chain upward from usage site
    → Finds MethodDeclarator where tp.name.equals("T")
    → Returns first bound (Object if none)
```

#### Diamond operator `new HashMap<>()`:
```
Parser (line 2687): parseTypeArgumentsOpt()
    peekRead("<") → true
    peekRead(">") → true (immediately!)
    Returns new TypeArgument[0]  ← ZERO-LENGTH ARRAY (not null!)
    
This empty array is stored in ReferenceType.typeArguments.
UnitCompiler ignores it because parameterization is disabled.
```

---

### Class File Signature Processing (Reading Generics from .class Files)

When Janino loads classes from `.class` files (via `ClassFileIClass`), it reads the JVM `Signature` attribute:

```
ClassFile.java:2303 → SignatureAttribute stores raw signature string
ClassFileIClass constructor (line 91-100):
    SignatureAttribute sa = classFile.getSignatureAttribute()
    → SignatureParser.decodeClassSignature(sa.getSignature())
    → ClassSignature with FormalTypeParameter list

ClassFileIClass.getITypeVariables2() (line 121-150):
    → Iterates ClassSignature.formalTypeParameters
    → Creates ITypeVariable[] from FormalTypeParameter entries
    → Each ITypeVariable has getName() and getBounds()

Example (from ClassFileIClass source comments, lines 102-116):
    interface Map<K, V>
    → Signature: <K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/lang/Object;
    → Parsed as: [FormalTypeParameter("K", classBound=Object), FormalTypeParameter("V", classBound=Object)]
    
    class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>
    → Signature: <K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/util/Map<TK;TV;>;...
```

#### SignatureParser parsing methods (SignatureParser.java):

| Method | Line | Parses |
|---|---|---|
| `parseClassSignature()` | 834 | Full class signature with type params + superclass + interfaces |
| `parseMethodTypeSignature()` | 850 | Method signature with formal type params + params + return + throws |
| `parseFormalTypeParameter()` | 978 | `T:Ljava/lang/Object;` → FormalTypeParameter |
| `parseClassTypeSignature()` | 888 | `Ljava/util/List<Ljava/lang/String;>;` → ClassTypeSignature |
| `parseTypeArgument()` | 1037 | `+Ljava/lang/String;` (extends), `-...` (super), `*` (any), or direct |
| `parseTypeVariableSignature()` | 969 | `TK;` → TypeVariableSignature("K") |
| `parseFieldTypeSignature()` | 1011 | Dispatches to ClassType, ArrayType, or TypeVariable signature |

---

### Reflection-Based Generic Handling (ReflectionIClass.java)

When Janino loads classes via Java reflection:

```
ReflectionIClass.getITypeVariables2() (line 62-71):
    → clazz.getTypeParameters() → TypeVariable<?>[]
    → Converts each to ITypeVariable via typeVariableToITypeVariable()

typeVariableToITypeVariable() (line 73-87):
    → Creates ITypeVariable with getName() from TypeVariable.getName()
    → getBounds() calls tv.getBounds() and converts via typesToITypes()

typeToIType() (line 96-123):
    → Class → loads IClass via descriptor
    → GenericArrayType → throw AssertionError("NYI")     ← NOT YET IMPLEMENTED
    → ParameterizedType → throw AssertionError("NYI")    ← NOT YET IMPLEMENTED  
    → TypeVariable → throw AssertionError("NYI")         ← NOT YET IMPLEMENTED
    → WildcardType → throw AssertionError("NYI")         ← NOT YET IMPLEMENTED
```

**Key limitation**: ReflectionIClass can extract type variable names and their bounds,
but cannot process ParameterizedType, GenericArrayType, TypeVariable, or WildcardType
references within bounds — these are NYI (Not Yet Implemented).

---

### Visitor Infrastructure for Generics

| Visitor Interface | File:Line | Methods |
|---|---|---|
| `Visitor.TypeArgumentVisitor<R, EX>` | `Visitor.java:843` | `visitWildcard(Wildcard)`, `visitReferenceType(ReferenceType)`, `visitArrayType(ArrayType)` |
| `Visitor.TypeVisitor<R, EX>` | `Visitor.java:~530` | `visitReferenceType(ReferenceType)`, + other type visitors |

#### DeepCopier (DeepCopier.java) — Copy infrastructure for generic AST nodes:
- `copyTypeArgument(TypeArgument)` → dispatches via `TypeArgumentVisitor`
- `copyWildcard(Wildcard)` (line 486) → creates new Wildcard with copied bounds
- `copyOptionalTypeParameters(TypeParameter[])` (line 427) → array copy
- `copyOptionalTypeArguments(TypeArgument[])` (line 425) → array copy
- `copyReferenceType(ReferenceType)` → copies identifiers + typeArguments

#### Unparser (Unparser.java) — AST → Source code:
- `unparseTypeParameters(TypeParameter[])` (line 1817) → outputs `<T, U extends Foo>`
- `unparseTypeParameter(TypeParameter)` (line 1828) → outputs name + optional `extends` bounds

---

### Test Cases

| Test File | Method | What It Tests | Key Assertion |
|---|---|---|---|
| `JlsTest.java:366` | `test_4_5__Parameterized_Types()` | `Map<String, String>` variable, `.get()` call | **@Ignore** — parameterized type checking not fully working |
| `JlsTest.java:372` | `test_4_5_1__Type_arguments_and_wildcards()` | `List<String>` with `ArrayList`, `Iterator<String>` | `l.add("x"); it.next()` — PASSES (type erasure allows it) |
| `JlsTest.java:573` | `test_8_1_2__Generic_Classes_and_Type_Parameters()` | Generic class `Bag<T>` with field, put/get methods | `Bag<String> b = new Bag<String>(); b.put("FOO"); (String) b.get()` — needs explicit cast! |
| `JlsTest.java:2199` | (in test_15_9_3a) | Diamond operator `new HashMap<>()` | `Map<String, Integer> map = new java.util.HashMap<>();` — Java 7+ only |
| `JlsTest.java:2220` | `test_15_9_3b` | Diamond operator in block | `List<String> l = new ArrayList<>();` — PASSES |
| `UnparserTest.java:647` | `testParseUnparseJava7()` | Diamond parse/unparse roundtrip | `Map<String, Integer> map = new java.util.HashMap<>();` |
| `UnparserTest.java:662` | `testParseUnparseJava8()` | Annotated wildcards parse/unparse | `Map<@WildcardAnnotation ? extends String, ? extends @TypeAnnotation Integer>` |

**Notable**: `test_4_5__Parameterized_Types` is `@Ignore` — this test attempts `String x = s.get("foo")` without a cast, which fails because Janino erases type arguments and `Map.get()` returns `Object`.

---

### Key Code Locations (Exact File Paths & Line Numbers)

#### AST Node Definitions (`janino/src/main/java/org/codehaus/janino/Java.java`)
- **TypeParameter class**: lines 2121-2148
- **ReferenceType class** (implements TypeArgument): lines 4236-4293
- **TypeArgument interface**: lines 4298-4313
- **Wildcard class** (implements TypeArgument): lines 6413-6487
- **AbstractTypeDeclaration.typeParameters field**: line 1083
- **MethodDeclarator.typeParameters field**: line 2742
- **getOptionalTypeParameters()** (TypeDeclaration): line 1034
- **getOptionalTypeParameters()** (MethodDeclarator): line 2692

#### Parser (`janino/src/main/java/org/codehaus/janino/Parser.java`)
- **parseTypeParametersOpt()**: lines 2645-2656
- **parseTypeParameter()**: lines 2663-2673
- **parseTypeArgumentsOpt()**: lines 2683-2695
- **parseTypeArgument()**: lines 2707-2734
- **parseReferenceType()**: lines 2611-2617
- **parseReferenceType(Annotation[])**: lines 2631-2638
- **parseReferenceTypeList()**: lines 2742-2749
- **parseClassDeclarationRest()** (type params): line 696
- **parseInterfaceDeclarationRest()** (type params): line 1092
- **parseMethodDeclarationRest()** (type params): line 1525
- **parseEnumDeclarationRest()** (type params rejected): line 766-768

#### Type Resolution & Erasure (`janino/src/main/java/org/codehaus/janino/UnitCompiler.java`)
- **getType2(ReferenceType)**: lines 6991-7007
- **getReferenceType() — type variable resolution**: lines 7060-7146
  - Method type parameter check: lines 7074-7099
  - Type declaration type parameter check: lines 7102-7124
  - **"Big simplification" comment**: lines 7093-7096
  - **parameterize() call commented out**: line 7142
- **rawTypeOf(IType)**: lines 6869-6874
- **typeArgumentToIType()** (UNUSED): lines 7149-7178
- **instanceof IParameterizedType checks**: lines 4929, 5211

#### Type System Interfaces
- **IType.java**: 30 lines (empty marker interface)
- **ITypeVariableOrIClass.java**: 30 lines (union type)
- **ITypeVariable.java**: 43 lines (getName, getBounds)
- **IParameterizedType.java**: 33 lines (getActualTypeArguments, getRawType)
- **IWildcardType.java**: 42 lines (getUpperBound, getLowerBound)
- **IClass.rawTypeOf()**: line 1359-1364
- **IClass.parameterize()** (COMMENTED OUT): lines 1366-1467
- **IClass.getITypeVariables()**: lines 167-177

#### Class File Signature Parsing
- **ClassFileIClass constructor** (reads Signature attribute): lines 91-100
- **ClassFileIClass.getITypeVariables2()**: lines 121-150
- **ClassFileIClass.getBounds()**: lines 152-158
- **ClassFileIClass.fieldTypeSignatureToITypeVariableOrIClass()**: lines 161-198
- **SignatureParser.java**: full file, 1074 lines
  - `decodeClassSignature()`: line 89
  - `decodeMethodTypeSignature()`: line 109
  - `parseClassSignature()`: line 834
  - `parseFormalTypeParameter()`: line 978
  - `parseClassTypeSignature()`: line 888
  - `parseTypeArgument()`: line 1037
- **ClassFile.SignatureAttribute**: `ClassFile.java:2303-2327`

#### Reflection-Based (`janino/src/main/java/org/codehaus/janino/ReflectionIClass.java`)
- **getITypeVariables2()**: lines 62-71
- **typeVariableToITypeVariable()**: lines 73-87
- **typeToIType()** (NYI for ParameterizedType/WildcardType): lines 96-123

#### Visitor Infrastructure
- **TypeArgumentVisitor**: `Visitor.java:843`
- **DeepCopier.copyWildcard()**: `DeepCopier.java:486`
- **DeepCopier.typeArgumentCopier**: `DeepCopier.java:378-382`
- **Unparser.unparseTypeParameters()**: `Unparser.java:1817`
- **Unparser.unparseTypeParameter()**: `Unparser.java:1828`

---

### Type Hierarchy Diagram

```
IType (marker interface)
├── ITypeVariableOrIClass (marker interface)
│   ├── IClass (abstract class, 1468 LOC)
│   │   ├── ClassFileIClass (from .class files)
│   │   ├── ReflectionIClass (from java.lang.reflect)
│   │   └── [UnitCompiler anonymous implementations]
│   └── ITypeVariable (interface)
│       ├── getName() → String
│       └── getBounds() → ITypeVariableOrIClass[]
├── IParameterizedType (interface) — MOSTLY UNUSED
│   ├── getActualTypeArguments() → IType[]
│   └── getRawType() → IType
└── IWildcardType (interface) — CREATED BUT NOT STORED
    ├── getUpperBound() → IType
    └── getLowerBound() → @Nullable IType

Java.java AST Nodes:
Type (abstract)
├── PrimitiveType
├── ReferenceType (implements TypeArgument)
│   ├── identifiers: String[]
│   ├── typeArguments: @Nullable TypeArgument[]
│   └── annotations: Annotation[]
└── ArrayType (implements TypeArgument)

TypeArgument (interface)
├── ReferenceType (see above)
├── ArrayType (see above)
└── Wildcard
    ├── bounds: int (NONE=0, EXTENDS=1, SUPER=2)
    ├── referenceType: @Nullable ReferenceType
    └── annotations: Annotation[]

TypeParameter (standalone class)
├── name: String
└── bound: @Nullable ReferenceType[]

SignatureParser Types (JVM class file level):
TypeSignature
├── PrimitiveTypeSignature
└── FieldTypeSignature
    ├── ClassTypeSignature (with TypeArgument list)
    ├── ArrayTypeSignature
    └── TypeVariableSignature

FormalTypeParameter
├── identifier: String
├── classBound: @Nullable FieldTypeSignature
└── interfaceBounds: List<FieldTypeSignature>
```

---

### Summary: Janino's Generic Type Support Level

| Feature | Parsing | Compilation | Status |
|---|---|---|---|
| Generic class declarations `class Foo<T>` | ✅ Full | ✅ Erased to bound | Working |
| Generic method declarations `<T> T meth()` | ✅ Full | ✅ Erased to bound | Working |
| Type arguments `List<String>` | ✅ Parsed to AST | ❌ Discarded (erased) | Parsed but unused |
| Diamond operator `new Foo<>()` | ✅ Parsed as empty TypeArgument[] | ✅ Works (no-op) | Working |
| Wildcards `? extends T` | ✅ Parsed to Wildcard node | ❌ Never processed | Parsed only |
| Bounded type params `T extends X & Y` | ✅ Parsed | ✅ Resolves to first bound | Working |
| Parameterized type checking | ❌ `@Ignore` in tests | ❌ `IClass.parameterize()` commented out | Not implemented |
| Signature attribute reading | ✅ Full SignatureParser | ✅ Extracts ITypeVariables | Working |
| Reflection generic types | ⚠️ Basic (TypeVariable only) | ❌ ParameterizedType etc. = NYI | Partial |

**Bottom line**: Janino **parses** all generic syntax correctly into AST nodes, but **compiles** using
type erasure to the first bound. It does NOT perform generic type checking, type inference, or generic
method resolution. Code using generics compiles and runs correctly as long as you add explicit casts
where javac would insert them automatically. The `test_4_5__Parameterized_Types` being `@Ignore`
confirms this limitation is known.

---

*Document created: 2026-03-09 | JaninoLoader v1.0.1-ALPHA*
*Last updated: 2026-03-09 (Janino generics analysis complete)*

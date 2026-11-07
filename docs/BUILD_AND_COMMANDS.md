# SwiftShare — Build, Verification, and Packaging Commands

This file collects the key commands used to build, verify, and package the SwiftShare Android app, plus the generated source layout. It is included so the project can be rebuilt locally and on GitHub.

> **Important:** The `-Pandroid.aapt2FromMavenOverride=/opt/android-tools/aapt2` flag below was required **only** in the ARM64 Linux environment used for this session because the installed Android SDK `aapt2` binary was x86_64. **Do not use that flag on normal Windows/macOS/x86_64 Linux machines or on GitHub Actions runners.** GitHub Actions (`.github/workflows/android-apk.yml`) builds with standard commands.

## Environment (session where commands were verified)

- OS: Linux (ARM64/aarch64 host for the build environment)
- JDK: 17 (`/usr/lib/jvm/java-17-openjdk-arm64`)
- Android SDK: 35 (`/opt/android-sdk`), build-tools 35.0.0
- Gradle: Wrapper 8.9 (included)
- AGP/Kotlin: 8.7.3 / 2.0.21 (see `build.gradle`, `app/build.gradle`)
- Nearby Connections: `com.google.android.gms:play-services-nearby:19.1.0`

## Generated source layout (code)

```
SwiftShare/
├── .github/workflows/android-apk.yml      # GitHub Actions: build/test/lint + APK artifact
├── .gitignore
├── README.md
├── docs/BUILD_AND_COMMANDS.md             # this file
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/wrapper/gradle-wrapper.jar
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/swiftshare/
        │   │   ├── MainActivity.kt
        │   │   ├── TransferViewModel.kt
        │   │   ├── domain/
        │   │   │   ├── NearbyTransferManager.kt
        │   │   │   ├── TransferState.kt
        │   │   │   ├── TransferHistoryStore.kt
        │   │   │   ├── SpeedCalculator.kt
        │   │   │   ├── FileNaming.kt
        │   │   │   ├── DeviceCapabilities.kt
        │   │   │   └── BenchmarkResult.kt
        │   │   └── ui/
        │   │       ├── Format.kt
        │   │       ├── theme/Color.kt, Theme.kt, Type.kt
        │   │       └── screens/ (Home, FileSelection, Discovery, IncomingRequest,
        │   │                    Transfer, Completion, History, Settings, Benchmark,
        │   │                    Components, Screen)
        │   └── res/
        │       ├── values/strings.xml         # Arabic default
        │       ├── values-en/strings.xml      # English
        │       ├── values/themes.xml
        │       ├── values-night/themes.xml
        │       ├── values/colors.xml
        │       └── drawable/*.xml
        └── test/java/com/example/swiftshare/domain/
            ├── SpeedCalculatorTest.kt
            └── FileNamingTest.kt
```

## Build / test / lint (standard)

Use these on a normal development machine or GitHub Actions:

```bash
cd SwiftShare
chmod +x gradlew

./gradlew :app:assembleDebug --no-daemon
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:lintDebug --no-daemon
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build / test / lint (ARM64 environment used in this session)

```bash
cd SwiftShare
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 \
ANDROID_HOME=/opt/android-sdk \
ANDROID_SDK_ROOT=/opt/android-sdk \
./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug \
  --no-daemon -Pandroid.aapt2FromMavenOverride=/opt/android-tools/aapt2
```

Actual result (clean verification): **EXIT=0, `BUILD SUCCESSFUL in 3m 57s`**.

- `:app:assembleDebug` produced `app/build/outputs/apk/debug/app-debug.apk` (15M).
- `:app:testDebugUnitTest`: **16 tests passed, 0 failures, 0 errors** (`FileNamingTest` 9, `SpeedCalculatorTest` 7).
- `:app:lintDebug`: **0 errors, 31 warnings** (non-fatal; `MissingTranslation` intentionally disabled because Arabic is the default `values/` language).

## APK inspection (verification)

```bash
# aapt2 may need a working binary on ARM64 hosts
/opt/android-tools/aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk \
  | grep -E "package:|targetSdkVersion|uses-permission|application-label"
```

Confirmed: package `com.example.swiftshare`, min/target 24/35, Nearby/runtime permissions present, **no `android.permission.INTERNET`** in the APK. Some merged-library permissions (e.g., `ACCESS_NETWORK_STATE`, internal dynamic-receiver permission) may appear; they do not grant internet transfer capability.

## GitHub Actions (APK build on GitHub)

Workflow file: `.github/workflows/android-apk.yml`

It runs on push/PR/manual dispatch (`workflow_dispatch`) on `ubuntu-latest` with JDK 17 and Android SDK 35, then:
1. validates the Gradle wrapper,
2. runs unit tests,
3. runs lint,
4. assembles the debug APK,
5. uploads `SwiftShare-debug-apk` (APK artifact) and `SwiftShare-reports`.

No ARM64 `aapt2` override is used.

## Packaging ZIP (GitHub-ready source)

```bash
cd /path/to/parent-containing-SwiftShare
mkdir -p dist
rm -f dist/SwiftShare-Android-GitHub.zip

zip -r -q dist/SwiftShare-Android-GitHub.zip SwiftShare \
  -x 'SwiftShare/.gradle/*' \
     'SwiftShare/.kotlin/*' \
     'SwiftShare/build/*' \
     'SwiftShare/app/build/*' \
     'SwiftShare/local.properties' \
     'SwiftShare/.idea/*' \
     'SwiftShare/*.iml' \
     'SwiftShare/captures/*' \
     '*.apk' '*.aab' '*.jks' '*.keystore'

unzip -t dist/SwiftShare-Android-GitHub.zip | tail -2
unzip -Z1 dist/SwiftShare-Android-GitHub.zip | grep -Ei '(^|/)(build|\.gradle|\.kotlin|\.git)/|local\.properties|\.apk$|\.aab$|\.jks$|\.keystore$' || echo "no forbidden entries"
```

The produced ZIP contains buildable source, docs, `.gitignore`, Gradle wrapper, and `.github/workflows/android-apk.yml`. It excludes build outputs, local config, caches, secrets, and APK/AAB/signing files.

## Install APK on a device (after build)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Nearby Connections requires **two real Android devices** with Google Play Services, Bluetooth/Wi‑Fi enabled, and runtime permissions granted. Emulator testing is not reliable for Nearby.

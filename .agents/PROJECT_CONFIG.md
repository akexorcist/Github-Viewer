# Project Configuration

> This is the only file that contains project-specific values.
> All agents read this file first to get the context they need.
> To reuse this agent team on a new project, copy the .agents/ folder and replace this file only.

---

## Identity

| Key | Value |
|---|---|
| APP_NAME | Github Viewer |
| BASE_PACKAGE | dev.akexorcist.githubviewer |
| PROJECT_ROOT | /Users/akexorcist/Documents/Workspace/Workspace-Android/Github-Viewer |
| SPEC_FILE | /Users/akexorcist/Documents/Workspace/Workspace-Android/Github-Viewer/docs/features.md |
| TECH_STACK_KEY | android-kmp-compose |
| TEAM_KNOWLEDGE_ROOT | /Users/akexorcist/Documents/Agent/Knowledge |

## Build Config

| Key | Value |
|---|---|
| MIN_SDK | 30 |
| COMPILE_SDK | 36 |
| TARGET_SDK | 36 |
| JVM_TARGET | 17 |
| GRADLE_WRAPPER | 8.14.3 |
| AGP_VERSION | 9.1.0 |
| KOTLIN_VERSION | 2.3.20 |

## Module List

```
:app
:core:common
:core:network
:core:database
:data
:presentation
:integration-test
```

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin Multiplatform (KMP) |
| UI | Jetpack Compose + Material 3 (Android) |
| DI | Koin |
| Navigation | Jetpack Navigation Compose |
| Async | Kotlin Coroutines + StateFlow |
| Network | Ktor |
| Persistence | Room (KMP) |
| Image Loading | Coil + Ktor |
| Serialization | kotlinx.serialization |

## Key Constraints

- Uses KMP (kotlin.multiplatform plugin) — never use `jetbrainsKotlinAndroid` plugin
- All Compose BOM deps declared without a version
- Use `kotlin { jvmToolchain(17) }` for JVM target — never `kotlinOptions`
- compileSdk = 36, targetSdk = 36

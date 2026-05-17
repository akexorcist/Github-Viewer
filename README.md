# Github Viewer

A KMP Android app for browsing public GitHub profiles and repositories — no login required.

## About

This project is a proof of concept exploring **KMP as a fast-feedback architecture for AI-assisted development**.

The core idea: by keeping ViewModels, repositories, networking, and database in `commonMain` (no Android dependency), the entire non-UI layer compiles and runs on the JVM desktop target. Integration tests execute in ~15 seconds with no emulator — giving agents (and developers) a tight feedback loop to verify behavior before touching any UI code.

## Features

- **Search** — find users and repositories simultaneously from a single query
- **User Profile** — avatar, bio, stats, and paginated public repositories
- **Repository Detail** — description, README, stars, forks, topics, license

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Jetpack Navigation Compose |
| DI | Koin |
| HTTP | Ktor (OkHttp on Android, CIO on Desktop) |
| Database | Room (KMP) |
| Images | Coil |
| Markdown | multiplatform-markdown-renderer-m3 |
| Serialization | kotlinx.serialization |

## Module Layout

```
:app                  Compose UI, Koin wiring, Navigation
:presentation         ViewModels, UiState (KMP — JVM-testable)
:data                 Repository implementations, domain models (KMP)
:core:network         Ktor client, GitHub API service (KMP)
:core:database        Room entities, DAOs (KMP)
:core:common          Shared types: AppError, Result, Pagination (KMP)
:integration-test     Full-stack JVM tests (real HTTP + mock layer)
```

## Build

```bash
./gradlew assembleDebug
```

## Testing

Two layers — both run on the JVM, no emulator needed for non-UI tests:

```bash
./gradlew :integration-test:desktopTest   # real HTTP + mock tests (~15s)
./gradlew test                            # mock-only tests
```

**Integration test setup** — create `integration-test/.env`:

```env
GITHUB_TOKEN=           # optional; raises rate limit from 60 to 5000 req/hr
```

## License

[Apache License 2.0](LICENSE)

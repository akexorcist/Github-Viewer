# Shared Context — Github Viewer

> Live memory for this project. All agents read this before starting and update it after completing work.

---

## Project State

| Phase | Agent | Status |
|---|---|---|
| 01 Architecture | Architect | ⬜ Not started |
| 01b Architecture Review | Architecture Reviewer | ⬜ Not started |
| 02 Scaffold | Scaffold | ⬜ Not started |
| 03 Core Development | Core Developer | ⬜ Not started |
| 04 Feature Development | Feature Developer | ⬜ Not started |
| 05 App Integration | App Integrator | ⬜ Not started |
| 06 Build Verification | Build Verifier | ⬜ Not started |
| 07 Fix | Fixer | ⬜ Not started |
| 08 QA Performance | QA Performance | ⬜ Not started |
| 09 QA Quality | QA Quality | ⬜ Not started |
| 10 QA Security | QA Security | ⬜ Not started |
| 11 Review | Reviewer | ⬜ Not started |
| 12 Test | Test Agent | ⬜ Not started |
| 13 UI Design | UI Designer | ⬜ Not started |
| 14 UI Code | UI Coder | ⬜ Not started |
| 15 UI QA | UI QA | ⬜ Not started |

---

## Known Class Locations

*(Empty — populate as agents add classes)*

---

## Decisions Log

*(Empty — populate as agents make decisions)*

---

## Known Issues

### [HIGH] Missing `CancellationException` re-throw in `flow { runCatching }` lambdas
- `RepositoryRepository.getRepository` and `getUserRepositories` call `runCatching { apiService.xxx() }` inside `flow { }` without re-throwing `CancellationException`. A cancelled coroutine will emit `Result.Error` instead of completing silently.
- Files: `data/src/commonMain/.../repository/RepositoryRepository.kt` — `onFailure` lambdas

### [MEDIUM] `getUserRepositories` has a silent no-emission path on first run
- When `page == 1`, `forceRefresh == false`, and the Room cache is empty, the flow emits nothing. The ViewModel will hang in `isLoading = true` forever.
- File: `data/src/commonMain/.../repository/RepositoryRepository.kt` lines 48-82

### [MEDIUM] `getUserRepositories` network error unconditionally overwrites cache
- `onFailure` in `getUserRepositories` always emits `Result.Error` even when a cached result was already emitted. This causes the UI to show an error screen over a previously-shown cached list.
- File: `data/src/commonMain/.../repository/RepositoryRepository.kt` lines 78-80

### [MEDIUM] Magic constant `30` in `SearchViewModel` instead of shared `PAGE_SIZE`
- `nextPage` calculation uses literal `30` instead of `PAGE_SIZE` from `Pagination.kt`.
- File: `presentation/src/commonMain/.../search/SearchViewModel.kt` lines 62, 69

### [MEDIUM] `!!` on StateFlow-backed nullable after `when` branch check
- `uiState.user!!` and `uiState.repository!!` used inside `when` branches. These should be snapshotted to a local val first.
- Files: `UserProfileScreen.kt:107`, `RepositoryDetailScreen.kt:109`

### [MEDIUM] Hardcoded string `"No internet connection"` duplicated across 3 screen files
- Should be a shared string resource or constant.
- Files: `UserProfileScreen.kt`, `RepositoryDetailScreen.kt`, `SearchScreen.kt`

### [MEDIUM] `topics` stored as CSV string in `RepositoryEntity` instead of using Room `TypeConverter`
- Latent bug: topic names containing a comma would be split incorrectly.
- Files: `core/database/src/commonMain/.../entity/RepositoryEntity.kt`, `data/src/commonMain/.../mapper/RepositoryMapper.kt`

### [MEDIUM] Debounce + `onSearchClick` double-search race
- Explicit search tap fires `executeSearch`, then 500ms later the debounce fires again, replacing results.
- File: `presentation/src/commonMain/.../search/SearchViewModel.kt`

### [LOW] `sealed class` used for stateless event hierarchies — prefer `sealed interface`
- `UserProfileSnackbarEvent`, `RepositoryDetailSnackbarEvent`, `SearchSnackbarEvent` are `sealed class` with only `data object` members.

### [LOW] `Arrangement.spacedBy(0.dp)` no-op in `RepositoryDetailScreen`
- File: `app/src/main/kotlin/.../repository/RepositoryDetailScreen.kt:128`

### [LOW] `SCREAMING_SNAKE_CASE` naming for non-const val `TRACKED_KEYS` in `TestEnvironment`
- Should be `trackedKeys` (camelCase).

### [LOW] `toAppError()` extension defined in `UserRepository.kt` but used from `RepositoryRepository.kt`
- Should move to a shared `data/util/` or `core/common/` location.

### [LOW] Unused `getRepository(id: Long)` DAO method in `RepositoryDao`
- File: `core/database/src/commonMain/.../dao/RepositoryDao.kt:13-14`

### [LOW] `!!` used in integration tests after `shouldNotBeNull()` — should capture return value
- Files: `UserProfileViewModelIntegrationTest.kt`, `RepositoryDetailViewModelIntegrationTest.kt`

---

## Resource Registry

*(Empty — populate as agents add resources)*

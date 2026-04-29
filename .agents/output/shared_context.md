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

### App renamed from `Github-Viewer` to `github-viewer` (2026-04-20)
- GitHub repo and app name changed to all-lowercase `github-viewer`
- Updated `integration-test/.env`: `GITHUB_TEST_REPO=github-viewer`
- Updated `TestEnvironment.kt` default fallback to `"github-viewer"`
- Note: GitHub's API redirects the old name to the new one, but the JSON response `name` field will reflect the canonical (lowercase) name, so tests asserting `repo.name shouldBe testRepo` require the correct casing in `.env`

---

## Known Issues

### [HIGH] Missing `CancellationException` re-throw in `flow { runCatching }` lambdas — RESOLVED
- Fixed: all `onFailure` lambdas in `RepositoryRepository` and `UserRepository` now re-throw `CancellationException`.

### [MEDIUM] `getUserRepositories` has a silent no-emission path on first run — RESOLVED
- Fixed: condition `if (forceRefresh || page > 1 || cached.isEmpty())` now always falls through to network when cache is empty.

### [MEDIUM] `getUserRepositories` network error unconditionally overwrites cache — RESOLVED
- Fixed: `onFailure` only emits `Result.Error` when `cached.isEmpty()`, preserving previously-emitted cached data.

### [MEDIUM] Magic constant `30` in `SearchViewModel` instead of shared `PAGE_SIZE`
- `nextPage` calculation uses literal `30` instead of `PAGE_SIZE` from `Pagination.kt`.
- File: `presentation/src/commonMain/.../search/SearchViewModel.kt` lines 62, 69

### [MEDIUM] StateFlow-backed nullable snapshot in `when` branch — partially fixed
- `uiState.user!!` still used in `UserProfileScreen.kt:107`. `RepositoryDetailScreen.kt` fixed to use `checkNotNull()`.
- Files: `UserProfileScreen.kt:107` (open), `RepositoryDetailScreen.kt` (fixed in README review pass)

### [MEDIUM] Hardcoded string `"No internet connection"` duplicated across 3 screen files
- Should be a shared string resource or constant.
- Files: `UserProfileScreen.kt`, `SearchScreen.kt` (still open); `RepositoryDetailScreen.kt` partially addressed (content descriptions and README strings moved to resources in README review pass)

### [MEDIUM] `topics` stored as pipe-delimited string in `RepositoryEntity` using `StringListConverter`
- Previous CSV bug (comma delimiter) was fixed: `StringListConverter` now uses `|` as delimiter.
- Remaining concern: delimiter choice is not documented; a topic containing `|` would still split incorrectly.
- Files: `core/database/src/commonMain/.../converter/StringListConverter.kt`

### [MEDIUM] Debounce + `onSearchClick` double-search race — MITIGATED
- `onSearchClick` now sets `lastSearchedQuery` before calling `executeSearch`, so the debounce's `query != lastSearchedQuery` guard prevents it from re-triggering the same query.
- Residual: if the user changes query AFTER clicking search (but within 500ms), the debounce may still fire a second search.
- File: `presentation/src/commonMain/.../search/SearchViewModel.kt`

### [LOW] `sealed class` used for stateless event hierarchies — prefer `sealed interface`
- `UserProfileSnackbarEvent`, `RepositoryDetailSnackbarEvent`, `SearchSnackbarEvent` are `sealed class` with only `data object` members.

### [LOW] `Arrangement.spacedBy(0.dp)` no-op in `RepositoryDetailScreen` — RESOLVED
- Was at line 128; that line no longer exists (layout restructured in README review pass). No longer applicable.

### [LOW] `SCREAMING_SNAKE_CASE` naming for non-const val `TRACKED_KEYS` in `TestEnvironment`
- Should be `trackedKeys` (camelCase).

### [LOW] `toAppError()` extension defined in `UserRepository.kt` but used from `RepositoryRepository.kt` — RESOLVED
- Moved to `data/src/commonMain/.../data/util/ThrowableExt.kt` in README review pass.

### [LOW] Unused `getRepository(id: Long)` DAO method in `RepositoryDao` — RESOLVED
- Replaced by `getRepositoryByFullName(fullName: String)` in README review pass.

### [LOW] `!!` used in integration tests after `shouldNotBeNull()` — should capture return value
- Files: `UserProfileViewModelIntegrationTest.kt`, `RepositoryDetailViewModelIntegrationTest.kt`

### [CRITICAL] `desktopTest` missing `workingDir = rootProject.projectDir` — RESOLVED
- Without this, `File("integration-test/.env")` resolves to `integration-test/integration-test/.env` (wrong). Token never loaded → unauthenticated → 60 req/hr rate limit → tests fail after 1-2 runs.
- Fixed in `integration-test/build.gradle.kts`: `workingDir = rootProject.projectDir` added to `desktopTest` task.

### [LOW] `checkRateLimit` false-positive on successful responses — RESOLVED
- Old code threw `RateLimitError` whenever `X-RateLimit-Remaining == 0`, including on 2xx responses (last successful request before limit). Valid data was discarded.
- Fixed: `if (response.status.isSuccess()) return` guard added. Rate limit check now only fires on non-2xx responses.
- File: `core/network/src/commonMain/.../GitHubApiService.kt`

### [LOW] Integration test README drain-loop never terminates on README failure — RESOLVED
- `RepositoryDetailViewModel` sets only `isReadmeLoading = false` on README errors; it does NOT set `state.error`. Tests using `while (readmeContent == null && error == null)` would loop forever if README failed.
- Fixed: loop condition changed to `while (readmeContent == null && isReadmeLoading && error == null)` in 3 tests.
- File: `integration-test/.../RepositoryDetailViewModelIntegrationTest.kt`

### [LOW] `getReadme` silent cache miss when repository row does not yet exist in DB — RESOLVED
- Fixed in README review pass: write is now guarded by `if (cached != null)`.

### [LOW] `jvmToolchain(21)` in `app/build.gradle.kts` violates project constraint of JVM 17 — RESOLVED
- Fixed in README review pass to `jvmToolchain(17)`; note machine only has JDK 21 so this may need to be reverted in practice (see memory).

---

## Resource Registry

*(Empty — populate as agents add resources)*

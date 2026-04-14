# GitHub Viewer — App Features & Requirements

## Overview

A GitHub Viewer Android app built with Kotlin Multiplatform (KMP). The production target is Android with a full Compose UI. The Desktop JVM target exists solely as a test runner for the non-UI layer — no desktop UI is built.

The app uses only the public GitHub REST API (unauthenticated). No login, no OAuth, no private data.

---

## Platforms

| Platform | Purpose |
|---|---|
| Android | Production app — full Compose UI |
| Desktop JVM | Test runner only — no UI, no desktop app |

---

## Tech Stack

| Concern | Library |
|---|---|
| HTTP client | Ktor Client (`OkHttp` engine on Android, `CIO` on Desktop) |
| Serialization | `kotlinx.serialization` |
| Structured cache | Room 2.7+ (KMP, `BundledSQLiteDriver` on Desktop) |
| Async | `kotlinx.coroutines` |
| ViewModel | `androidx.lifecycle:lifecycle-viewmodel` (KMP) |
| DI | Koin (KMP) |
| UI | Jetpack Compose (Android only) |
| Navigation | Jetpack Navigation Compose — type-safe routes |
| Image loading | Coil 3 (KMP-ready) |
| Flow testing | Turbine |

---

## Module Structure

```
Github-Viewer/
├── app/                    Android application — Compose UI, Navigation, DI wiring
│
├── core/
│   ├── network/            KMP — Ktor client, GitHub API service, DTOs, rate limit handling
│   ├── database/           KMP — Room entities, DAOs, database builder
│   └── common/             KMP — Result<T>, AppError, Pagination, coroutine utils
│
├── data/                   KMP — Repository implementations, domain models, mappers
│
├── presentation/           KMP — ViewModels, UiState (no Compose, testable on JVM)
│
└── integration-test/       Desktop JVM only — full-stack tests with real components
```

### Layer Rules

- `commonMain` — domain models, repository interfaces, repository implementations, ViewModels
- `androidMain` — OkHttp engine, Android Room driver, Compose UI
- `desktopMain` — CIO engine, BundledSQLiteDriver (test infra only)
- `app/` — Compose screens, NavHost, Koin app module

### No Use Case Layer

ViewModels call repositories directly. No interactor/use case wrapper classes.

---

## GitHub API

Base URL: `https://api.github.com`

| Feature | Endpoint |
|---|---|
| Search users | `GET /search/users?q={query}&page={n}&per_page=30` |
| Search repositories | `GET /search/repositories?q={query}&page={n}&per_page=30` |
| User profile | `GET /users/{login}` |
| User's public repos | `GET /users/{login}/repos?page={n}&per_page=30` |
| Repository detail | `GET /repos/{owner}/{repo}` |

### Rate Limiting

Unauthenticated requests are limited to **60 requests/hour per IP**.

- Parse `X-RateLimit-Remaining` and `X-RateLimit-Reset` headers on every response
- When remaining hits 0, surface a `RateLimitError(resetAt: Instant)` — show "Rate limit reached, resets at HH:mm" in UI instead of a generic error
- Cache-first strategy reduces unnecessary API calls

---

## Screens & Features

### 1. Search Screen

Entry point of the app.

**Search behaviour:**
- Single search field with a search button
- Two trigger paths for the same search action:
  - **Auto-search**: 500ms debounce after the user stops typing
  - **Manual search**: tap the search button — fires immediately, bypasses debounce wait
- Both users and repositories are searched **simultaneously** from a single query
  - `GET /search/users?q={query}` and `GET /search/repositories?q={query}` fire in parallel
- Results are split into two separate sections on the same screen

**Results layout:**
```
[Search field] [Search button]

── Users ──────────────────────
  [User card]
  [User card]
  [Load more]

── Repositories ───────────────
  [Repo card]
  [Repo card]
  [Load more]
```

**Per-section state (independent):**
- Each section has its own loading indicator, error state, and pagination
- "Load more" for Users does not affect or reset the Repositories section, and vice versa

**User card shows:** avatar, login, display name
**Repo card shows:** full name (`owner/repo`), description, star count, primary language

**Navigation:**
- Tap user card → User Profile Screen
- Tap repo card → Repository Detail Screen

---

### 2. User Profile Screen

Displays public information for a GitHub user.

**Data shown:**
- Avatar
- Display name + login
- Bio
- Location, website/blog (if available)
- Public stats: repositories count, followers, following

**Repository list:**
- Paginated list of the user's public repositories
- Each item shows: repo name, description, star count, language
- Tap → Repository Detail Screen

---

### 3. Repository Detail Screen

Displays public information for a single GitHub repository.

**Data shown:**
- Repository name
- Owner login (displayed as text only — no navigation)
- Description
- Stats: stars, forks, open issues count, watchers
- Primary language
- Topics (tags)
- License name (if available)
- Last pushed date

---

## Data & Caching Strategy

### Cache-First

All screens follow cache-first behaviour:

1. On load — emit cached Room data immediately (if available)
2. Fetch fresh data from API in the background
3. Update Room, emit updated data
4. UI shows "Last updated: X min ago" based on the cached timestamp

### Refresh

- Each screen has a **refresh button** (or pull-to-refresh on Android)
- Refresh triggers a forced network fetch regardless of cache age
- "Last updated" text updates after each successful fetch

### Cache Invalidation

No automatic background sync. Data is only refreshed:
- On first load (if cache is empty)
- When the user explicitly triggers a refresh

---

## Offline / No Network Behavior

- If the device has no network and the cache is empty → show a `NetworkError` state with a retry button
- If the device has no network but the cache has data → display cached data and show a snackbar: **"No internet connection"**
- If a manual refresh fails due to no network → show a snackbar: **"No internet connection"**, keep existing cached data visible

---

## Pagination

Shared pagination implemented in `commonMain` — no dependency on Android Paging 3.

GitHub REST uses page-number pagination: `?page=N&per_page=30`.

```kotlin
data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val hasNextPage: Boolean
)

data class PagingState<T>(
    val items: List<T> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val isLoadingMore: Boolean = false
)
```

- "Load more" appends to the existing list
- Each paginated list (user repos, search users, search repos) manages its own `PagingState` independently

---

## Error Handling

Errors are represented as a sealed class in `core/common`:

```kotlin
sealed class AppError {
    data class NetworkError(val cause: Throwable) : AppError()
    data class HttpError(val code: Int, val message: String) : AppError()
    data class RateLimitError(val resetAt: Instant) : AppError()
    data object NotFoundError : AppError()
    data object UnknownError : AppError()
}
```

- Each UI section (search users, search repos, etc.) surfaces its own error independently
- Errors show an inline error message with a retry action

---

## ViewModel State Shape

```kotlin
// Search screen
data class SearchUiState(
    val query: String = "",
    val users: SectionState<User> = SectionState(),
    val repositories: SectionState<Repository> = SectionState()
)

data class SectionState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val hasNextPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: AppError? = null
)

// User profile screen
data class UserProfileUiState(
    val user: User? = null,
    val repositories: PagingState<Repository> = PagingState(),
    val isLoading: Boolean = false,
    val lastUpdatedAt: Instant? = null,
    val error: AppError? = null
)

// Repository detail screen
data class RepositoryDetailUiState(
    val repository: Repository? = null,
    val isLoading: Boolean = false,
    val lastUpdatedAt: Instant? = null,
    val error: AppError? = null
)
```

---

## Testing Strategy

### Unit Tests (mock — all platforms, no network)

Location: `src/commonTest/` in each module

- Ktor `MockEngine` with fixture JSON files for API responses
- In-memory Room database (`BundledSQLiteDriver`)
- `kotlinx-coroutines-test` for ViewModel and Flow testing
- `Turbine` for asserting `StateFlow` emissions
- Tests cover: repository implementations, ViewModel state transitions, pagination logic, error mapping, cache-first behaviour, offline snackbar trigger conditions

### Integration Tests (real components — Desktop JVM, real network)

Location: `integration-test/src/desktopTest/`

All real components wired together:
- Real Ktor client (CIO engine) → real GitHub API
- Real Room database (in-memory, `BundledSQLiteDriver`)
- Real repository implementations
- Real ViewModels

Secrets loaded from `.env` file (git-ignored):

```
# integration-test/.env
GITHUB_TEST_USER=akexorcist
```

No GitHub token required — public API only.

Run mock tests: `./gradlew test`
Run integration tests: `./gradlew :integration-test:desktopTest`

---

## What Is Explicitly Out of Scope

- User authentication / OAuth / login
- Private repositories
- Creating or modifying GitHub data (issues, PRs, comments, stars)
- Push notifications (FCM)
- Background sync (WorkManager)
- Issues and Pull Requests browsing
- GitHub GraphQL API
- Desktop UI
- iOS target
- README rendering
- Dynamic Color (Material You)
- Navigation from Repository Detail to owner's profile

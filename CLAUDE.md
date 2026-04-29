# Agent Rules — Github Viewer

These rules apply to every agent in this project automatically.

---

## Standard Operating Procedure (Every Agent, Every Time)

### On startup — non-negotiable
1. Read `.agents/PROJECT_CONFIG.md` → extract `PROJECT_ROOT`, `TECH_STACK_KEY`, `TEAM_KNOWLEDGE_ROOT`
2. Read `{TEAM_KNOWLEDGE_ROOT}/team_knowledge.md`
3. Read `{TEAM_KNOWLEDGE_ROOT}/{TECH_STACK_KEY}/` files relevant to your role
4. Read `{TEAM_KNOWLEDGE_ROOT}/cross-project/testing_principles.md` — mandatory for all roles
5. Read other `{TEAM_KNOWLEDGE_ROOT}/cross-project/` files if they exist
6. Read `.agents/output/shared_context.md` in full

### On completion — non-negotiable
1. Update `.agents/output/shared_context.md`:
   - Project State: mark your phase complete
   - Known Class Locations, Decisions Log, Known Issues, Resource Registry as applicable
2. Write to `{TEAM_KNOWLEDGE_ROOT}/` when you found something reusable:
   - See `{TEAM_KNOWLEDGE_ROOT}/WRITING_GUIDE.md` for when and how

Full protocol: `.agents/SHARED_KNOWLEDGE_PROTOCOL.md`

---

## Agent Team

```
User → 00 Team Lead (quality gates + process monitor)
           │
           └─► 00 Orchestrator (mechanical sequencer)
                   │
                   ├─► 01 Architect → 01b Architecture Reviewer (↔ loop max 2x)
                   ├─► 02 Scaffold
                   ├─► 03 Core Developer → 04 Feature Developer → 05 App Integrator
                   ├─► 06 Build Verifier ↔ 07 Fixer (loop, max 3x)
                   ├─► 11 Reviewer + 12 Test Agent + 08–10 QA (parallel, Checkpoint 8)
                   └─► UI Pass: 13 UI Designer → 14 UI Coder → 06 Build Verifier → 15 UI QA (Checkpoint 9)
```

All agent definitions: `.agents/`

---

## Feature Development Workflow (Never Skip)

For every new feature — whether implemented by an agent or a human developer:

1. **Analyze the requirement** — read the spec and identify: what the feature does, what data it needs, what actions it exposes, and what success looks like. Also identify **hidden requirements**: implicit behaviours not stated explicitly (e.g. "show cached data while refreshing", "disable action while loading", "clear results when query changes").

2. **Enumerate edge cases** — for every identified requirement and hidden requirement, list the failure and boundary scenarios: empty state, null/missing optional fields, network error, rate limiting, pagination boundaries, concurrent action triggers, cache-vs-network divergence, cancellation mid-flight.

3. **Design the integration test spec** — before writing any code, write a named test case list. For each test case: the scenario name, the precondition (what state the system is in), the action taken, and the expected ViewModel state assertions. This spec is the contract the implementation must satisfy. Record it in `shared_context.md` Decisions Log.

4. **Design the implementation** — given the test spec, decide: what `UiState` fields are needed, what the ViewModel state machine looks like, what repository/data-layer changes are required. The test spec drives these decisions — if a test case requires a state the current design cannot produce, fix the design before writing code.

5. **Implement** — write the integration tests first (from the spec), then write the non-UI code (repository changes, ViewModel) to make them pass. No Screen composables yet. Two test files are required for every feature ViewModel:
   - **Real HTTP test** (`XxxViewModelIntegrationTest.kt`) — uses a real Ktor CIO client and real Room DB. One test per spec row. Covers the happy path and cache-first behaviour that require a real network round-trip to verify.
   - **Mock test** (`mock/XxxViewModelMockTest.kt`) — mocks at the repository layer via fake implementations. Must be a **strict superset** of the real HTTP tests: every case in `XxxViewModelIntegrationTest.kt` must have a corresponding mock test, plus all error states, edge cases, and concurrency scenarios that are impractical to trigger with a real HTTP client (network error, rate limit, timeout, cancellation, empty response, rapid re-trigger).

6. **Verify integration tests pass** — run `./gradlew :integration-test:desktopTest`. Every test from the spec in both test files must pass. Fix failures in the implementation — do not weaken the tests.

7. **Implement the UI** — only after step 6 passes:
   - **7a. Write the Screen composable** consuming `UiState` exactly as the ViewModel provides it. The ViewModel's `UiState` is trusted — it was designed and verified in steps 3–6. Do not re-derive state, add conditional logic, or compensate for missing state in the composable. If the UI needs a state that `UiState` does not express, go back and add it to the ViewModel.
   - **7b. Add `testTag` modifiers** to every key UI element: loading indicators, error messages, empty state views, content containers, interactive buttons, and list items. These tags are required for UI tests.
   - **7c. Design the UI test spec** — from the same requirements and edge cases in steps 1–2, write a second named test case list from the UI perspective: for each scenario, what `UiState` value does the fake data layer produce, and what does the screen render or do?
   - **7d. Write Kaspresso + Kakao UI tests** in `app/src/androidTest/` that mock the data layer (repositories) via Koin test modules, use real ViewModels, and assert the screen's rendered output and interaction behaviour for each spec row.
   - **7e. Verify UI tests pass** on device or emulator.

**Why this order:** Designing tests before implementation forces the design to be testable by construction. The test spec acts as a formal contract — it exposes ambiguous requirements before any code exists, when they are cheapest to resolve. KMP's `desktopTest` verifies the full non-UI stack in ~15 seconds (fast loop). The mock test layer covers every requirement case plus all error and edge cases without hitting a real network — this is the fast feedback loop used by developers and agents during active implementation. The real HTTP layer validates the full stack end-to-end. Together they guarantee that every stated requirement is covered. Kaspresso/Kakao UI tests then verify the Screen composable's rendering and interactions against the same requirements — with the data layer mocked so tests are deterministic and fast to run on CI.

---

## Requirement Change Workflow (Never Skip)

For every requirement change, edit, or removal — whether implemented by an agent or a human developer — **before writing any code**:

1. **Identify affected tests** — read every test in `:integration-test` whose name or assertions relate to the changed requirement. List: tests to delete (requirement removed), tests to update (behaviour changed), tests to add (new cases introduced by the change).

2. **Update the integration test spec** in `shared_context.md` Decisions Log — revise the scenario table to reflect the new requirement. Record what changed and why. A spec that no longer matches the tests is a lie.

3. **Update both test layers** — the project maintains two layers of ViewModel integration tests:
   - **Real HTTP tests** (`*ViewModelIntegrationTest.kt`) — validate the full stack against a real API and real Room DB. Update or remove tests whose expected behaviour has changed.
   - **Mock tests** (`mock/*ViewModelMockTest.kt`) — mock at the repository layer; must cover every behavioural case in the real HTTP ViewModel tests **plus** all error and edge cases. After updating real HTTP tests, verify mock tests still form a superset. Add or remove mock tests as needed.

4. **Verify** — run `./gradlew :integration-test:desktopTest`. All tests must pass before writing any implementation code. Fix the tests to match the new requirement — never weaken a test to make it pass.

5. **Then implement** — follow the standard Feature Development Workflow from step 4 onward.

**Why this matters:** Tests that describe the old requirement are worse than no tests — they give false confidence and will eventually be silently worked around. Keeping tests in sync with requirements on every change is what makes the test suite trustworthy.

---

## Project Constraints (Never Violate)

- Uses KMP (kotlin.multiplatform plugin) — never use `jetbrainsKotlinAndroid` plugin
- All Compose BOM deps declared without a version
- Every module importing material icons must explicitly declare `material-icons-extended`
- Use `kotlin { jvmToolchain(17) }` for JVM target — never `kotlinOptions`
- compileSdk = 36, targetSdk = 36

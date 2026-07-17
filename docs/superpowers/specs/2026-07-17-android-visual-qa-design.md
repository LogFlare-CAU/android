# Android Visual QA Automation Design

## Goal

Add local visual regression coverage for the LogFlare Android app. The suite must cover every major screen on one representative phone profile in light and dark themes, while also exercising real navigation on an emulator.

CI and pull-request integration are out of scope for this iteration.

## Chosen approach

Use two complementary layers:

1. **Roborazzi Compose snapshot tests** render deterministic screen states on the JVM and compare them with committed reference images.
2. **Maestro emulator flows** launch the real debug app, navigate through user journeys, and capture checkpoints.

The snapshot layer is the primary pixel-regression gate. Maestro verifies integration, navigation, and device rendering. This split keeps routine checks fast without treating JVM rendering as a substitute for the installed app.

Before adopting dependencies, perform a small compatibility spike against the repository's AGP 9.0.1, Kotlin 2.2.10, and Compose configuration. If the selected Roborazzi release cannot run reliably with this toolchain, use the official Compose screenshot-testing plugin where compatible; Paparazzi is the second fallback. The E2E architecture remains unchanged.

## Coverage

Snapshot coverage includes these major screens and meaningful states:

- Login: empty, validation error, loading
- Home: populated, empty, error
- Logs: populated, empty, filtered/error state, detail
- Projects: list, empty, create, settings
- Project detail: populated and empty/error states exposed by the UI
- My Page: overview, add member, edit member, logout confirmation
- Shared full-screen loading and error presentations where applicable

Each state is rendered in:

- A Pixel 7-compatible `412 x 915 dp` viewport at `420 dpi`
- Light theme
- Dark theme

Transient animations, clocks, random identifiers, network timing, and system UI are disabled or replaced with fixed values in snapshot fixtures.

Maestro covers a smaller set of end-to-end journeys because duplicating every state at the device layer would add runtime and instability without equivalent value:

- Login and reach Home
- Open Logs and view a log detail
- Open Projects, create/edit flow, and project settings
- Open My Page and member-management screens
- Logout

Each journey captures named checkpoints in both themes. Mock-mode checkpoints are compared with committed device reference images. Development-server captures are diagnostic artifacts only and do not fail on pixel differences.

## Architecture

### Renderable screen content

Screen composables are separated, where necessary, into:

- A route/container that obtains navigation arguments and ViewModels
- A stateless content composable driven by an immutable UI state and callbacks

Snapshot tests call only the content composables. Production routes retain ownership of side effects and navigation. Existing screens that already expose a suitable stateless boundary do not need restructuring.

### Snapshot fixtures

A test fixture package provides fixed UI states and reusable fake callbacks. Fixtures use stable text, dates, IDs, and list ordering. One test case maps to one descriptive image name:

`<screen>_<state>_<theme>.png`

Reference images are versioned with the repository. Failed comparisons emit actual and diff images under the module's build reports rather than modifying references.

### E2E data modes

The debug app supports two explicitly selected E2E modes:

- **Mock mode:** points the existing dynamic base-URL mechanism at a local deterministic HTTP fixture server. The server returns scripted responses for authentication, logs, projects, and profile/member APIs.
- **Development-server mode:** uses a caller-provided base URL and credentials. Secrets and credentials remain outside the repository.

Mode selection is debug-only and unavailable in release builds. The default debug behavior remains unchanged when no E2E configuration is supplied.

The documented device profile is a Pixel 7-compatible AVD running API 35 at `1080 x 2400` pixels and `420 dpi`. The emulator reaches a host-side mock/development server through `10.0.2.2`. Tests wait for visible semantic states rather than fixed sleeps.

### Device image comparison

After each mock-mode Maestro flow, a JVM comparison utility reads the captured checkpoints and their committed references. It excludes status/navigation bar regions, rejects dimension mismatches, and produces an amplified diff image when changed pixels exceed the documented threshold. The initial threshold is intentionally strict and is relaxed only for a named unstable region that cannot be made deterministic.

Device references use:

`<journey>_<checkpoint>_<theme>.png`

Verification never records or replaces these images. A separate explicit command records new device references for review.

### Local entry points

Repository scripts and Gradle wrapper tasks expose four operations:

- Record/update snapshot references intentionally
- Verify snapshots without changing references
- Run Maestro in deterministic mock mode
- Run Maestro against a caller-supplied development server

The scripts validate prerequisites and print the report or screenshot location on failure. Android builds and tests use the in-repository Gradle Wrapper.

## Baseline update policy

Reference images must never be updated as a side effect of verification. A developer records new images with the dedicated update command, reviews the visual changes, and commits only intended baselines.

Generated actual/diff images and Maestro diagnostic captures are ignored by Git. Stable Roborazzi references and Maestro flow definitions are committed.

## Failure handling

- Snapshot mismatch: fail the verification task and preserve expected, actual, and diff artifacts.
- Missing baseline: fail verification and instruct the developer to run the explicit record command.
- Mock server unavailable: stop before running Maestro and report the expected host/port.
- Emulator unavailable or incompatible: fail prerequisite validation with the required AVD/API configuration.
- Development server unavailable or data differs: fail functional assertions, but do not evaluate pixel equality.
- Roborazzi/toolchain incompatibility during the spike: stop before broad test authoring and adopt the documented fallback.

## Validation

Implementation is accepted when:

1. The Gradle Wrapper builds the debug app and runs existing unit tests.
2. Snapshot verification passes for all listed screens in light and dark themes.
3. Deliberately changing a visible style causes a snapshot mismatch with a usable diff image.
4. All mock-mode Maestro journeys pass on the documented local emulator profile.
5. Development-server mode accepts external URL/credential configuration and completes at least the login-to-Home smoke journey.
6. Release builds contain no E2E-only configuration surface.
7. A clean verification run does not modify tracked files.

## Non-goals

- GitHub Actions or other CI integration
- Multiple phone sizes, tablets, orientations, font scales, or locales
- Pixel comparison against mutable development-server data
- Firebase Test Lab or physical-device farms
- Automatic approval or replacement of changed reference images

# UI Token Lift + Visual Refresh — Design

**Date:** 2026-08-06  
**Status:** Done (implemented on `main` through `a77aa1f`; CI green)  
**Plan:** `docs/superpowers/plans/2026-08-06-ui-token-lift-visual-refresh.md`  
**Supersedes / extends:** `2026-07-21-theme-tokens-design.md` (semantic color migration). This spec adds a **Role** layer, chrome polish, full-screen visual alignment, and bounded presentation refactoring.

## Goal

LogFlare Android UI should read as one Calm Ops product: status bar, logo/wordmark, tab titles, GNB, cards, and lists share the same spacing, type, and surface language in light and dark. Feature screens stop bypassing the design system with magic `dp`, raw hex, or ad-hoc `MaterialTheme` usage. Presentation-layer duplication that blocks that consistency is cleaned up in the same pass.

**Success looks like:**

1. Three-layer tokens (Primitive → Semantic → Role) are the only path for UI styling in feature code.
2. Chrome (status bar / TopAppBar / GNB) and primary screens (Login, Home, Logs, Projects, MyPage + detail/create/settings) look intentional and consistent; Title Case labels; readable contrast in both themes.
3. Loading / empty / error share one presentation pattern.
4. Roborazzi baselines updated for major screens × light/dark so chrome and content are visually verifiable.
5. Navigation, API, auth, and repository behavior unchanged except minimal bugfixes discovered while touching UI.

## Decisions (agreed)

| Topic | Choice |
|-------|--------|
| Scope of visual work | Full visual refresh (chrome + cards/lists/login), not chrome-only |
| Tone | Calm Ops — polish current identity, not Signal Desk density or Soft Product consumer look |
| Brand color | Flexible; green primary may get small contrast tweaks, no hard lock |
| Approach | **B — Token Lift** + layering + refactor (not polish-only A) |
| Logic refactor | **Presentation only** — UI state shaping, shared state views, removing UI dual paths. Domain/data/API/nav graph out of scope |

## As-Is

- `core/designsystem` already has primitives, `AppColors` semantic maps, spacing/radius/dimens, Pretendard, `LogFlareTopAppBar`, `LogFlareGnbItem`.
- App theme bridges to Material3; edge-to-edge with transparent system bars.
- Home TopBar uses wordmark; other routes use ALL CAPS titles (`LOGS`, `PROJECTS`, …).
- Feature screens still hardcode `16.dp`, `RoundedCornerShape(12.dp)`, and often `MaterialTheme.colorScheme` / typography instead of `AppTheme`.
- `ListStateViews` exists in app but still uses raw `dp` in places; not fully Role-driven.
- Prior theme-token work and Visual QA (Roborazzi + Maestro) infrastructure exist; baselines will need refresh after intentional visual change.

## Token model (To-Be)

```text
Primitive  →  Semantic  →  Role / Component
(hex, raw dp) (bg, muted…)  (chrome.topBar, surface.card, layout.screenPadding)
```

### Primitive

Unchanged location: `Color.kt`, base spacing units in `Dimens.kt`. UI code must not import primitives or invent `Color(0x…)`.

### Semantic

Keep and refine existing `AppColors` (see prior theme-tokens spec). Allowed micro-adjustments for Calm Ops contrast (e.g. muted/outline/divider, dark surface steps) without a full rebrand.

### Role (new)

Named layout/surface roles consumed by chrome and screens. Exact API can be a small `AppRoles` / extension on existing `AppSpacing`+`AppDimens`, but the **contract** is:

| Role | Intent |
|------|--------|
| `chrome.topBar` | Height, horizontal padding, background = `surface`, status-bar padding ownership |
| `chrome.gnb` | Bar background, selected = `primary`, unselected = `muted` (or refined secondary) |
| `layout.screenPadding` / `layout.sectionGap` | Screen horizontal margin and section rhythm |
| `surface.card` | Card fill (`surface` / `surfaceVariant`), radius (`radius.large` or Role-fixed) |
| `surface.field` | Input fill via `input` / `inputDisabled` |
| `type.appBarTitle` / `type.sectionTitle` | TopBar and section headers (Title Case) |

**Rules:**

- Feature / app UI uses **Semantic + Role only**.
- Design-system components map Primitive → Semantic; Role composes Semantic + spacing/radius.
- `LogflareandroidTheme` Material3 mapping remains a compatibility adapter; new and migrated code prefers `AppTheme`.

## Chrome

- **Status bar:** Keep edge-to-edge. Top bar owns `statusBarsPadding` and `surface` so system icons sit on a stable field; light/dark appearance flags stay correct.
- **TopAppBar:** Home → wordmark tinted `onSurface`. Other routes → Title Case (`Logs`, `Projects`, `My Page`, `Create Project`, …). Drop ALL CAPS.
- **GNB:** Icon + label tint from tokens (no unspecified multi-ink surprises for nav icons). Labels Title Case. Selected/unselected contrast clear in both themes.

## Typography

- Keep Pretendard.
- Add or clarify roles: app bar title, section title; keep body/caption scale.
- Prefer `AppTheme.typography` over `MaterialTheme.typography` in migrated screens.

## Component boundaries

| Layer | Owns |
|-------|------|
| `core/designsystem` | Tokens (all three layers), TopBar, GNB, buttons, inputs, project/log building blocks, shared Loading/Empty/Error (or Role-backed equivalents) |
| `app/.../ui/common` | App-only composition; no magic `dp`/hex; promote duplicates into designsystem when reused by 2+ features |
| `feature/*` | Data binding, navigation callbacks, UI state → content composables |

Stateless `*ScreenContent` pattern (already used for Visual QA) stays the snapshot entry point.

## In-scope presentation logic

Allowed when it unblocks UI consistency or removes dual paths:

- Unify loading / empty / error presentation across lists and home sections.
- Shape ViewModel / UI state only as needed for content composables (e.g. clearer empty vs error).
- Remove duplicate composables or dead UI helpers discovered during migration.
- Minimal bugfixes that surface while editing UI (e.g. wrong padding on a route).

**Out of scope:** Auth/API/Repository redesign, navigation graph restructure, new product features, data model changes, icon pack replacement (tint/alignment only).

## Screen migration order

1. Designsystem: Role tokens + chrome (TopBar/GNB) + shared state/section primitives.
2. Login (logo, fields, CTA, theme toggle).
3. Main chrome wiring in `MainScaffold` (titles, GNB labels).
4. Home → Logs → Projects → MyPage.
5. Project detail / create / settings + log detail (same patterns).
6. Sweep remaining magic `dp` / `MaterialTheme` color-typography in touched files.
7. Expand `ThemeContractTest` if Roles need contracts; refresh Roborazzi baselines (light/dark) for chrome-visible screens.

## Visual verification

- Roborazzi: Login, Home, Logs, Projects, MyPage (and key detail states already in suite) × light/dark, viewport per existing Visual QA design.
- Snapshots should show top chrome (logo or title) and bottom tabs where applicable so status-bar-adjacent and GNB changes are reviewable.
- Maestro flows optional for smoke after large chrome changes; not a blocker for token work if snapshots pass.

## Non-goals

- Material You dynamic color.
- Cross-platform token sync.
- Full brand redesign / new illustration system.
- Domain-layer architecture rewrite.

## Risks

| Risk | Mitigation |
|------|------------|
| Scope creep into data layer | Hard boundary in this doc; defer to separate specs |
| Snapshot churn | Intentional baseline refresh in one PR/commit series; review diffs for chrome + content |
| Partial migration leaves two styles | Migrate by vertical slice (chrome → screen); do not leave half-tokenized primary tabs |
| Dark contrast regressions | ThemeContract + visual review both themes |

## Open implementation details (resolved)

- Role API: `AppRoles` (`chrome` / `layout` / `surface`) + `LocalAppRoles` / `AppTheme.roles`.
- State views: `LogFlare*State` in designsystem; app `List*State` wrappers keep Visual QA tags.
- Title Case map: `Logs`, `Projects`, `My Page`, `Create Project`, `Project Detail`, `Project Settings`, `Log Details`.

## Spec self-review

- [x] No placeholder TBD sections blocking agreement (open items deferred to plan).
- [x] Consistent with prior theme-tokens + Visual QA specs.
- [x] Scope boundary for logic is explicit.
- [x] Success criteria are testable (tokens, screens, snapshots, no nav/API change).

## Implementation outcome

Shipped 2026-08-06 on `main` (`8516a23..a77aa1f`). Primary tabs and nested screens use Semantic + Role tokens; chrome is Title Case with role-driven TopBar/GNB; Roborazzi baselines refreshed; CI run [31100751538](https://github.com/LogFlare-CAU/android/actions/runs/31100751538) passed.

**Known follow-ups** (see plan completion section): token-arithmetic cleanup, chrome-inclusive snapshots, dead top-padding helper, unify legacy `EmptyState`.

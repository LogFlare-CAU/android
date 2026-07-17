# Task 7 Report: Make local server selection and navigation deterministic

**Branch:** `feat/android-visual-qa`  
**Base:** `84c01f5`  
**Date:** 2026-07-17

## Summary

Implemented synchronous in-memory base URL updates for custom login, argument-free log detail navigation (`log/detail`), build-type-scoped cleartext network policy, and Hilt singleton identity for `BaseUrlProvider` / `MutableBaseUrlProvider`.

---

## RED / GREEN Evidence

### Step 1–2: RED (before implementation)

**Command:**
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*BaseUrlSelectionTest" --tests "*RoutesTest"
```

**Results:**

| Test | Failure |
|------|---------|
| `BaseUrlSelectionTest` | Compile errors: `Unresolved reference 'MutableBaseUrlProvider'`, `Unresolved reference 'setBaseUrl'` |
| `RoutesTest` | Would assert `log/detail` but production route was `log/{logId}` (blocked by compile failure in same module) |

**Pre-change route value:** `Route.LogDetail.path == "log/{logId}"`  
**Pre-change URL behavior:** `DataStoreBaseUrlProvider` had no `setBaseUrl`; cache updated only via async `serverUrl.collectLatest`, so OkHttp could read `null` before auth.

### Step 6: GREEN (after implementation)

**Focused tests:**
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*BaseUrlSelectionTest" --tests "*RoutesTest"
# BUILD SUCCESSFUL — 4 tests passed
```

**Full verification:**
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:processDebugMainManifest :app:processReleaseMainManifest :app:assembleRelease
# BUILD SUCCESSFUL — 125 tests passed
```

**Interim race (fixed):** First full-suite run failed `selectedUrlIsVisibleBeforeSetReturns` with `expected:<http://10.0.2.2:8000/> but was:<null>`. Root cause: async DataStore collector emitted `null` after synchronous `setBaseUrl`. Fixed by ignoring null emissions in the collector (`if (url != null) cached.set(url)`).

---

## DI Identity Reasoning / Evidence

### Binding structure

`AppNetworkBindings` binds **both** interfaces to the **same** `@Singleton` implementation:

```kotlin
@Binds @Singleton
abstract fun bindBaseUrlProvider(impl: DataStoreBaseUrlProvider): BaseUrlProvider

@Binds @Singleton
abstract fun bindMutableBaseUrlProvider(impl: DataStoreBaseUrlProvider): MutableBaseUrlProvider
```

Hilt resolves both `@Binds` methods through a single `DataStoreBaseUrlProvider` instance because both methods share the same concrete parameter type and `@Singleton` scope. `NetworkModule.provideOkHttp(baseUrlProvider: BaseUrlProvider)` therefore receives the identical object whose cache `AuthViewModel` updates via `MutableBaseUrlProvider.setBaseUrl`.

### Tests

| Test | Assertion |
|------|-----------|
| `baseUrlProviderBindingsResolveToSameImplementation` | Both interfaces bound; sole impl is `DataStoreBaseUrlProvider` |
| `mutableProviderIsSameInstanceAsBaseUrlProvider` | Cast to both interfaces returns same object reference |

---

## Synchronous URL Visibility

**Flow in `DataStoreBaseUrlProvider.setBaseUrl`:**
1. Normalize once via `ServerConfigRepository.normalize(url)` (internal)
2. `cached.set(normalized)` — **synchronous**, visible to `getBaseUrl()` / OkHttp interceptor immediately
3. `serverConfigRepository.setNormalizedServerUrl(normalized)` — persists exact normalized value (no second normalization)

**Auth path:** `AuthViewModel.login(serverUrl, …)` calls `mutableBaseUrlProvider.setBaseUrl(serverUrl)` before `performLoginInternal`, ensuring the login request uses the selected host.

**OkHttp:** `HostSelectionInterceptor` remains non-blocking (reads `AtomicReference` only).

---

## Log Detail Route

| Before | After |
|--------|-------|
| `Route("log/{logId}")` with `createRoute(logId)` | `Route("log/detail")` — no path arguments |

Call sites in `MainScaffold` already navigate to `Route.LogDetail.path`; data handoff unchanged via `PendingLogDetailStore`.

---

## Merged Manifest Evidence

### Debug merged manifest

**Path:** `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`

| Attribute / Permission | Value |
|------------------------|-------|
| `uses-permission` | `android.permission.INTERNET` (line 11) |
| `android:usesCleartextTraffic` | `false` (line 38) |
| `android:networkSecurityConfig` | `@xml/network_security_config` (line 34) |

**Packaged debug NSC:** `app/build/intermediates/packaged_res/debug/packageDebugResources/xml/network_security_config.xml`

```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="false">10.0.2.2</domain>
    <domain includeSubdomains="false">localhost</domain>
</domain-config>
```

Cleartext permitted only for emulator host and localhost; no global cleartext, no user-CA debug overrides.

### Release merged manifest

**Path:** `app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`

| Attribute / Permission | Value |
|------------------------|-------|
| `uses-permission` | `android.permission.INTERNET` (line 11) |
| `android:usesCleartextTraffic` | `false` (line 37) |
| `android:networkSecurityConfig` | `@xml/network_security_config` (line 33) |

**Packaged release NSC:** `app/build/intermediates/packaged_res/release/packageReleaseResources/xml/network_security_config.xml`

```xml
<base-config cleartextTrafficPermitted="false" />
```

---

## Changed Files

| File | Change |
|------|--------|
| `core/network/.../MutableBaseUrlProvider.kt` | **Created** — `setBaseUrl` interface |
| `app/.../ServerConfigRepository.kt` | `normalize` internal; `setNormalizedServerUrl` added |
| `app/.../DataStoreBaseUrlProvider.kt` | Implements `MutableBaseUrlProvider`; sync cache + async persist |
| `app/.../AppNetworkBindings.kt` | Dual `@Binds` for singleton identity |
| `app/.../AuthViewModel.kt` | Uses `MutableBaseUrlProvider` before login |
| `app/.../Routes.kt` | `LogDetail` → `log/detail` |
| `app/src/main/AndroidManifest.xml` | `INTERNET`; `usesCleartextTraffic="false"` |
| `app/src/main/res/xml/network_security_config.xml` | Base cleartext false; removed debug-overrides/user-CA |
| `app/src/debug/AndroidManifest.xml` | Debug NSC overlay only |
| `app/src/debug/res/xml/network_security_config.xml` | Domain-scoped cleartext for 10.0.2.2/localhost |
| `app/src/test/.../BaseUrlSelectionTest.kt` | **Created** |
| `app/src/test/.../RoutesTest.kt` | **Created** |

---

## Self-Review

- [x] TDD RED preserved (compile failures + documented pre-change route)
- [x] Synchronous URL visible before `setBaseUrl` returns
- [x] Single normalization path; persist exact normalized value
- [x] OkHttp interceptor non-blocking
- [x] Hilt singleton identity for both provider interfaces
- [x] Log detail route argument-free; `PendingLogDetailStore` unchanged
- [x] Main/release cleartext false; debug local cleartext only
- [x] Removed broad user-CA trust / debug-overrides
- [x] Merged manifest inspection (not source-only)
- [x] 125 unit/snapshot tests pass; release assemble succeeds
- [x] No CI changes

---

## Concerns

1. **Async collector null-guard:** Ignoring null DataStore emissions prevents cache wipe after `setBaseUrl`, but there is no supported “clear server URL” path today. If added later, collector logic would need a deliberate clear signal.

2. **Stale non-null race (theoretical):** If a late async emission carried an older non-null URL after a newer `setBaseUrl`, it could overwrite the cache. DataStore `collectLatest` normally emits the persisted value after edit; risk is low but not formally sequenced with a generation counter.

3. **Debug cleartext scope:** Only `10.0.2.2` and `localhost` are permitted. Physical-device testing against a LAN IP requires extending the debug NSC domain list.

4. **Unused resources:** `app/src/debug/res/values/bools.xml` (`allow_cleartext`) and `app/src/main/res/values/bools.xml` remain but are no longer referenced by NSC (harmless dead resources).

---

## Test / Build Summary (initial Task 7)

| Command | Result |
|---------|--------|
| `:app:testDebugUnitTest` | 125 passed |
| `:app:processDebugMainManifest` | OK |
| `:app:processReleaseMainManifest` | OK |
| `:app:assembleRelease` | OK |

---

## Review Fix: Critical race + DI alias (post-review)

### Cache coordinator semantics

Introduced `BaseUrlCacheCoordinator` (pure, synchronized):

1. `beginLocalWrite(normalized)` — marks pending + publishes immediately; returns previous for rollback
2. `onPersistedEmission(url)` — while pending, ignores null/stale/non-matching; matching value clears pending without changing desired; after ack, normal emissions update cache
3. `rollback(previous)` — restores previous and clears pending
4. `DataStoreBaseUrlProvider.setBaseUrl` serializes with `Mutex`, rolls back on persistence exception

**No clear API:** Documented on `MutableBaseUrlProvider` — intentional; no null-guard that leaves cache/DataStore inconsistent.

### Race test evidence

**Command:**
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*BaseUrlSelectionTest" --tests "*RoutesTest" --tests "*BaseUrlCacheCoordinatorTest"
# BUILD SUCCESSFUL
```

| Test | Proof |
|------|-------|
| `staleNonNullAndNullCannotOverwriteWhilePending` | After `beginLocalWrite`, injects old non-null then null; cache stays at desired URL |
| `matchingValueAcknowledgesWithoutChangingDesired` | Matching emission clears pending; value unchanged |
| `laterNormalValueCanUpdateAfterAcknowledge` | Post-ack emission may update cache |
| `rollbackRestoresPreviousAndClearsPending` | Failed write path restores previous; later emissions work |
| `beginLocalWritePublishesImmediately` | Sync visibility before persist |
| `selectedUrlIsVisibleBeforeSetReturns` | End-to-end provider still green |
| `baseUrlProviderIsAliasOfMutableBinding` | DI structure + alias identity |

### DI alias structure

```kotlin
// AppNetworkBindings — single @Binds
@Binds @Singleton
abstract fun bindMutableBaseUrlProvider(impl: DataStoreBaseUrlProvider): MutableBaseUrlProvider

// companion @Provides alias — OkHttp receives this BaseUrlProvider
@Provides @Singleton
fun provideBaseUrlProvider(mutable: MutableBaseUrlProvider): BaseUrlProvider = mutable
```

Structural test asserts exactly one `@Binds` (Mutable → DataStore), `@Provides` returns `BaseUrlProvider` from `MutableBaseUrlProvider`, and `assertSame(fake, provideBaseUrlProvider(fake))`.

### Other review fixes

- Removed nested `viewModelScope.launch` in login: `performLoginInternal` is `suspend`; each public `login` creates one job; `setBaseUrl` still runs before auth
- Deleted dead `allow_cleartext` bool resources (`app/src/main|debug/res/values/bools.xml`)

### Full re-verification

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:processDebugMainManifest :app:processReleaseMainManifest :app:assembleRelease
# BUILD SUCCESSFUL — 129 tests, 0 failures
```

### Concerns (updated)

1. ~~Stale non-null race~~ — **fixed** via pending coordinator + deterministic unit tests
2. ~~Weak dual @Binds DI proof~~ — **fixed** via single bind + alias provide + structure/alias test
3. ~~Null-guard inconsistency~~ — **removed**; no clear API by design
4. ~~Dead allow_cleartext bools~~ — **deleted**
5. **Debug cleartext scope** remains limited to `10.0.2.2` / `localhost` (LAN device testing needs NSC extension)
6. **No Hilt component integration test** for alias at runtime; structural + `assertSame` on provide method covers the alias contract without spinning up a component

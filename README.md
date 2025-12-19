# LogFlare Android

Minimal Android client for LogFlare — view project errors and receive notifications.

## Stack
- Kotlin + Jetpack Compose (minimal UI)
- Hilt (DI), Retrofit + kotlinx.serialization (network)
- DataStore (JWT token)

## Build
1) Set backend base URL in `gradle.properties` (project-level):
   
   BASE_URL=http://10.0.2.2:8000/

2) Assemble:

```bash
./gradlew :app:assembleDebug
```

## Modules
- app: Compose UI, ViewModels, repositories
- core:model: DTOs generated from Swagger (`docs/swagger.json.txt`)
- core:network: Retrofit interface + Hilt network module

## API
- Swagger JSON: `docs/swagger.json.txt`
- Key endpoints used now:
  - POST `/user/auth`
  - GET `/project/`
  - GET `/log/error`

## Run flow (current)
- Login → token saved (DataStore)
- Projects tab → lists projects
- Select project → Logs tab shows recent error logs

## Backend
- A backend repo is available at `../Backend` (outside this project). Use it for server startup and FCM integration references.

## Notes
- Keep dependencies small. UI intentionally minimal; focus is on working network + navigation.
- FCM and advanced filtering will be added in the next step.

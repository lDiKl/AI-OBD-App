# Avyrox Service — Android B2B App

Professional diagnostic tool for mechanics. Connects via Bluetooth ELM327 adapter,
reads full OBD data (all PIDs, freeze frame), creates cases synced to web dashboard.

## Open in Android Studio

1. Open Android Studio → **Open** → select `avyrox-service/` folder
2. Wait for Gradle sync
3. Add `google-services.json` (from Firebase Console) to `app/`
4. Run on emulator or physical device (API 26+)

## Package Structure

```
com.avyrox.service/
├── ui/               Jetpack Compose screens
│   ├── scan/         Professional OBD Scanner (full PIDs, live data)
│   ├── case/         Create case from scan, AI analysis
│   ├── clients/      Client & vehicle search
│   ├── leads/        Incoming B2C leads (Phase 3+)
│   └── settings/     Shop profile, sync settings
├── viewmodel/        ViewModels (MVVM)
├── repository/       Data repositories (online + offline sync)
├── data/
│   ├── local/        Room DB (offline mode — scan without internet)
│   ├── remote/       Retrofit API client
│   └── obd/          ELM327 Bluetooth (professional mode — all PIDs)
└── di/               Hilt dependency injection
```

## Key Differences from B2C App

| Feature | B2C (Avyrox Drive) | B2B (Avyrox Service) |
|---------|----------------|---------------------|
| OBD data shown | Basic codes + freeze frame | All PIDs + live data graphs |
| Multi-vehicle | No | Yes (switch between cars) |
| Camera/Voice | No | Yes (diagnostic photos + notes) |
| Offline mode | Basic | Full (sync when connected) |
| AI result | Plain language | Technical + checklist |

## Permissions

Extra permissions vs B2C:
- `CAMERA` — diagnostic photos
- `RECORD_AUDIO` — voice notes for mechanics

API docs: `../docs/api_contract.md`

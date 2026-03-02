# DriverAI — Android B2C App

OBD diagnostic assistant for car owners. Connects via Bluetooth ELM327 adapter,
reads DTC codes, explains them in plain language using AI.

## Open in Android Studio

1. Open Android Studio → **Open** → select `mobile-b2c/` folder
2. Wait for Gradle sync
3. Add `google-services.json` (from Firebase Console) to `app/`
4. Run on emulator or physical device (API 26+)

## Package Structure

```
com.driverai.b2c/
├── ui/               Jetpack Compose screens
│   ├── scan/         OBD Scanner screen
│   ├── diagnostic/   Error code list + detail
│   ├── history/      Scan history
│   └── settings/     Account, subscription
├── viewmodel/        ViewModels (MVVM)
├── repository/       Data repositories
├── data/
│   ├── local/        Room DB (offline cache)
│   ├── remote/       Retrofit API client
│   └── obd/          ELM327 Bluetooth connection
└── di/               Hilt dependency injection
```

## Key Dependencies

| Library | Purpose |
|---------|---------|
| Jetpack Compose | UI |
| Hilt | Dependency Injection |
| Retrofit + OkHttp | REST API calls |
| Room | Local SQLite DB |
| Firebase Auth | Authentication |

## OBD Connection

Uses ELM327 Bluetooth adapter (Classic BT or BLE).
`data/obd/` contains the ELM327 protocol implementation.

API docs: `../docs/api_contract.md`

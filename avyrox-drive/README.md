# Avyrox Drive — Android B2C App

OBD diagnostic assistant for car owners. Connects via Bluetooth ELM327 adapter,
reads DTC codes, explains them in plain language using AI.

## Open in Android Studio

1. Open Android Studio → **Open** → select `avyrox-drive/` folder
2. Wait for Gradle sync
3. Add `google-services.json` (from Firebase Console) to `app/`
4. Run on emulator or physical device (API 26+)

## Package Structure

```
com.avyrox.drive/
├── ui/               Jetpack Compose screens
│   ├── scan/         OBD Scanner screen
│   ├── history/      Scan history + detail
│   ├── leads/        Nearby shops + my leads
│   ├── vehicle/      Vehicle management
│   ├── upgrade/      Subscription / paywall
│   └── auth/         Login screen
├── viewmodel/        ViewModels (MVVM)
├── data/
│   ├── auth/         Firebase Auth repository
│   ├── db/           Room AppDatabase
│   ├── leads/        Leads repository
│   ├── network/      Retrofit API services
│   ├── obd/          ELM327 Bluetooth connection
│   ├── scan/         Scan session entities + DAO
│   ├── subscription/ Subscription repository
│   └── vehicle/      Vehicle entity + DAO + repository
└── di/               Hilt dependency injection modules
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

## Deep link

Stripe payment return: `avyroxdrive://payment/success`

API docs: `../docs/api_contract.md`

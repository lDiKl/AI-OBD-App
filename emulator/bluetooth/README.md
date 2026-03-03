# ELM327 Bluetooth Emulator (Windows 11)

Создаёт Bluetooth RFCOMM сервер. Android подключается как к реальному ELM327.

**Зависимости: ноль.** Использует только стандартную библиотеку Python.

> **Почему не pybluez?**
> pybluez и pybluez2 сломаны на Windows 11 (ошибка `bluetooth\windows does not exist`).
> Эмулятор переписан на встроенном `socket.AF_BLUETOOTH` — не нужен pip install.

---

## Запуск

```bash
python elm327_bt_emulator.py
python elm327_bt_emulator.py --scenario misfire
python elm327_bt_emulator.py --scenario sensor_failure
python elm327_bt_emulator.py --list-scenarios
```

---

## Windows 11 — подготовка

1. **Включи Bluetooth**: Settings → Bluetooth & devices → Bluetooth: **On**
2. **Сделай ПК видимым**: Bluetooth & devices → Devices → "Allow Bluetooth devices to find this PC"
3. **Запусти эмулятор** (от имени Администратора если нужно)

---

## Android — подключение

### Проблема

Стандартный ELM327 адаптер использует SDP UUID `00001101-0000-1000-8000-00805F9B34FB`.
Без `pybluez` мы не можем зарегистрировать SDP запись — Android не найдёт сервис по UUID.

### Решение для debug сборок

Добавь в Android приложение поддержку прямого подключения по каналу 1 (без SDP).

**В `BluetoothRepository.kt` или `OBDConnectionManager.kt`:**

```kotlin
// Стандартное подключение (production) — через SDP UUID
private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

fun connectToDevice(device: BluetoothDevice): BluetoothSocket {
    return if (BuildConfig.DEBUG && BuildConfig.USE_EMULATOR) {
        // Прямое подключение по каналу 1 — для эмулятора
        connectByChannel(device, channel = 1)
    } else {
        // Стандартное через UUID (работает с реальным ELM327)
        device.createRfcommSocketToServiceRecord(SPP_UUID)
    }
}

// createRfcommSocket — hidden API, доступен через reflection
private fun connectByChannel(device: BluetoothDevice, channel: Int): BluetoothSocket {
    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
    return method.invoke(device, channel) as BluetoothSocket
}
```

**В `build.gradle.kts` (debug config):**

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("Boolean", "USE_EMULATOR", "true")
        }
        release {
            buildConfigField("Boolean", "USE_EMULATOR", "false")
        }
    }
}
```

### Шаги подключения

1. На телефоне: **Settings → Bluetooth → scan** → найди свой ПК → pair
2. Запусти эмулятор на ПК
3. В приложении (debug build): выбери ПК из списка Bluetooth устройств
4. Приложение подключится по RFCOMM channel 1

---

## Поддерживаемые OBD команды

| Команда | Ответ |
|---------|-------|
| ATZ | ELM327 v1.5 (reset) |
| ATE0/1, ATL0/1, ATS0/1, ATH0/1 | OK |
| ATRV | Battery voltage из сценария |
| 0100 | Supported PIDs |
| 010C | RPM |
| 010D | Speed |
| 0105 | Coolant temp |
| 0111 | Throttle position |
| 012F | Fuel level |
| 03 | Stored DTCs |
| 04 | Clear DTCs |
| 0902 | VIN |

---

## Сценарии

| Файл | DTCs | Описание |
|------|------|----------|
| `normal.json` | — | Нормальная езда |
| `no_errors.json` | — | Холостой ход |
| `misfire.json` | P0301, P0300 | Пропуск воспламенения |
| `sensor_failure.json` | P0133, P0420, P0171 | Ошибки датчиков |

Свой сценарий — создай `scenarios/my.json` и запусти с `--scenario my`.

---

## Troubleshooting

**`OSError: [WinError 10047]`** — socket.AF_BLUETOOTH не поддерживается:
- Убедись что Bluetooth адаптер присутствует и включён
- Запусти от Администратора

**`OSError: [WinError 10049]`** — не удался bind:
- Запусти от Администратора
- Проверь что другой процесс не занял RFCOMM channel 1

**Android не соединяется:**
- Убедись что телефон спарен с ПК (Windows Settings → Bluetooth)
- Эмулятор запущен ДО попытки подключения из приложения
- Используй debug build с `USE_EMULATOR = true`

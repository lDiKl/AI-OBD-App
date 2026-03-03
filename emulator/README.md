# ELM327 Emulator

Тестовый эмулятор ELM327 OBD адаптера. Две версии:

| Версия | Папка | Когда использовать |
|--------|-------|-------------------|
| **Bluetooth (Native)** | `bluetooth/` | Тест Android app с реальным BT подключением |
| **TCP (Docker)** | `tcp/` | Автоматизированные тесты, CI, без Android |

---

## Быстрый старт

### Bluetooth (реальное подключение с Android)
```bash
cd bluetooth
pip install -r requirements.txt
python elm327_bt_emulator.py
# или с конкретным сценарием:
python elm327_bt_emulator.py --scenario misfire
```

### Docker TCP (автотесты / без телефона)
```bash
cd tcp
docker compose up
# или с другим сценарием:
SCENARIO=misfire docker compose up
```

---

## Сценарии

| Сценарий | Описание | DTC коды |
|----------|----------|----------|
| `normal` | Нормальная езда, нет ошибок | — |
| `no_errors` | Холостой ход, нет ошибок | — |
| `misfire` | Пропуск воспламенения цил. 1 | P0301, P0300 |
| `sensor_failure` | Неисправность O2 датчика + катализатор | P0133, P0420, P0171 |

Сценарии — обычные JSON файлы. Можно создавать свои.

---

## Удаление

Папка `emulator/` полностью независима. Просто удали её когда не нужна.

```bash
rm -rf emulator/
```

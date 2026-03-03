# ELM327 TCP Emulator (Docker)

TCP сервер с тем же ELM327 протоколом. Запускается в Docker.
Не создаёт Bluetooth — используй для автоматизированных тестов и CI.

## Запуск

```bash
# Запуск (сценарий normal по умолчанию)
docker compose up

# Другой сценарий
SCENARIO=misfire docker compose up

# В фоне
docker compose up -d

# Остановка
docker compose down
```

## Прямой запуск (без Docker)

```bash
python elm327_tcp_emulator.py
python elm327_tcp_emulator.py --scenario sensor_failure --port 35000
python elm327_tcp_emulator.py --list-scenarios
```

## Тест из терминала

```bash
# Linux/Mac
nc localhost 35000

# Windows (PowerShell)
Test-NetConnection -ComputerName localhost -Port 35000

# Затем вводи команды:
ATZ
ATE0
03
010C
```

## Переменные окружения

| Переменная | Значение по умолчанию | Описание |
|-----------|----------------------|----------|
| `SCENARIO` | `normal` | Имя тестового сценария |
| `PORT` | `35000` | TCP порт |
| `LOG_LEVEL` | `INFO` | Уровень логирования (DEBUG / INFO) |

## Смена сценария без перезапуска

```bash
# Остановить, сменить, запустить
docker compose down
SCENARIO=misfire docker compose up -d
```

## Использование в Android (требует доработки app)

Android не подключается к TCP напрямую через Bluetooth API.
Чтобы использовать TCP в тестах, нужно добавить в приложение режим отладки:

```kotlin
// В ScannerViewModel или BluetoothRepository — только для debug builds
if (BuildConfig.DEBUG && useEmulator) {
    socket = Socket("192.168.1.100", 35000)  // IP вашего ПК
    inputStream = socket.getInputStream()
    outputStream = socket.getOutputStream()
}
```

IP ПК узнать: `ipconfig` → IPv4 адрес.

## Использование в тестах (JUnit / Integration)

```kotlin
@Test
fun testScannerConnectsAndReadsDTCs() {
    val socket = Socket("localhost", 35000)
    val writer = PrintWriter(socket.getOutputStream(), true)
    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

    writer.println("ATZ\r")
    val response = reader.readLine()
    assertTrue(response.contains("ELM327"))

    socket.close()
}
```

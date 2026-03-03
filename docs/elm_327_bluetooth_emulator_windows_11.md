# ELM327 Bluetooth Emulator (Python, Windows 11)

## 🎯 Цель
Созить простой, управляемый эмулятор ELM327 для разработки и тестирования Android/OBD приложения без автомобиля.

Эмулятор должен:
- Работать как Bluetooth SPP устройство (Serial Port Profile)
- Принимать AT-команды
- Принимать OBD-запросы (Mode 01, 03, 04 и т.д.)
- Возвращать заранее заданные ответы
- Позволять легко менять сценарии ответов

---

# 🧱 Архитектура

```
Android App
      ↓
Bluetooth SPP
      ↓
Windows Bluetooth COM Port
      ↓
Python ELM327 Emulator
```

Windows создаёт виртуальный COM-порт при Bluetooth SPP подключении.
Python-скрипт слушает этот COM-порт.

---

# ⚙️ Подготовка Windows 11

## 1️⃣ Установка Python зависимостей

```bash
pip install pyserial
```

---

## 2️⃣ Создание Bluetooth SPP сервера

⚠ В Windows нет встроенного Python API для поднятия Bluetooth SPP сервера напрямую.

Есть 2 варианта:

### Вариант A (Проще — рекомендую)
Использовать виртуальный COM-порт:

- Установить com0com
- Создать пару портов (например COM5 ↔ COM6)
- Приложение подключается к COM5
- Эмулятор слушает COM6

Это самый стабильный способ для разработки.

---

# 📡 Минимальный набор команд ELM327

## AT-команды

| Команда | Назначение |
|----------|------------|
| ATZ | Reset |
| ATE0 | Echo Off |
| ATL0 | Linefeeds Off |
| ATS0 | Spaces Off |
| ATH0 | Headers Off |
| ATSP0 | Auto protocol |


## OBD команды

| Команда | Назначение |
|----------|------------|
| 0100 | Supported PIDs |
| 010C | RPM |
| 010D | Speed |
| 03 | Read DTC |
| 04 | Clear DTC |

---

# 🧠 Важные детали протокола

- Ответ должен заканчиваться `>`
- По умолчанию ELM возвращает echo (если ATE0 не вызван)
- Ответ должен быть в hex
- Формат Mode 01 ответа:
  - Запрос: 01 0C
  - Ответ: 41 0C XX XX

---

# 🧩 Архитектура эмулятора

Мы разделим на:

- Command Parser
- Response Manager
- Scenario Manager

Это позволит менять ответы без переписывания кода.

---

# 🐍 Пример Python ELM327 Emulator

```python
import serial
import threading

PORT = "COM6"   # изменить под ваш порт
BAUDRATE = 38400

class ELM327Emulator:
    def __init__(self):
        self.echo = True
        self.linefeeds = True
        self.spaces = True
        self.headers = False
        self.dtcs = ["0133"]
        self.rpm = 1726  # динамически можно менять
        self.speed = 40

    def format_response(self, data):
        if not self.spaces:
            data = data.replace(" ", "")
        if self.linefeeds:
            data += "\r\n"
        return data + ">"

    def handle_command(self, cmd):
        cmd = cmd.strip().upper()

        if cmd == "ATZ":
            return self.format_response("ELM327 v1.5")

        elif cmd == "ATE0":
            self.echo = False
            return self.format_response("OK")

        elif cmd == "ATL0":
            self.linefeeds = False
            return self.format_response("OK")

        elif cmd == "ATS0":
            self.spaces = False
            return self.format_response("OK")

        elif cmd == "ATH0":
            self.headers = False
            return self.format_response("OK")

        elif cmd == "ATSP0":
            return self.format_response("OK")

        elif cmd == "0100":
            return self.format_response("41 00 BE 3F A8 13")

        elif cmd == "010C":
            value = int(self.rpm * 4)
            A = (value >> 8) & 0xFF
            B = value & 0xFF
            return self.format_response(f"41 0C {A:02X} {B:02X}")

        elif cmd == "010D":
            return self.format_response(f"41 0D {self.speed:02X}")

        elif cmd == "03":
            if not self.dtcs:
                return self.format_response("43 00 00 00 00")
            response = "43"
            for dtc in self.dtcs:
                response += f" {dtc}"
            return self.format_response(response)

        elif cmd == "04":
            self.dtcs.clear()
            return self.format_response("44")

        return self.format_response("?")


def run():
    emulator = ELM327Emulator()
    ser = serial.Serial(PORT, BAUDRATE, timeout=1)
    print("ELM327 Emulator started on", PORT)

    while True:
        if ser.in_waiting:
            data = ser.readline().decode(errors="ignore")
            if not data:
                continue

            if emulator.echo:
                ser.write(data.encode())

            response = emulator.handle_command(data)
            ser.write(response.encode())


if __name__ == "__main__":
    run()
```

---

# 🔄 Как менять сценарии

Можно добавить:

```python
self.profile = "normal"
```

И создать профили:

- normal
- misfire
- sensor_failure
- no_errors

И менять значения RPM, Speed, DTC в зависимости от профиля.

---

# 🚀 Расширения (рекомендую добавить позже)

- Симуляция задержек ответа
- Симуляция обрыва соединения
- Поддержка ISO-TP
- Freeze Frame (Mode 02)
- Поддержка UDS (Mode 22)
- Генерация случайных ошибок

---

# 🧪 Что ты можешь тестировать

- Reconnect logic
- Parsing edge cases
- Очистку ошибок
- UI состояния
- AI-анализ DTC
- Нестабильное соединение

---

# 📌 Следующий шаг

Если хочешь — можно сделать:

1. GUI-версию (PyQt)
2. BLE-эмулятор
3. Полноценный тестовый стенд с логированием
4. Версию для CI тестирования

---

Этот эмулятор полностью достаточен для MVP разработки AI OBD Diagnostic Assistant.


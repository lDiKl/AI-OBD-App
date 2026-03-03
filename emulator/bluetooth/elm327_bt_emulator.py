"""
ELM327 Bluetooth SPP Emulator (Windows 11)
==========================================
Не требует pip install — использует только стандартную библиотеку Python.

ВАЖНО: SDP регистрация (автоматическое обнаружение по UUID) недоступна
без pybluez. Подключение происходит напрямую по RFCOMM каналу 1.

Запуск:
    python elm327_bt_emulator.py
    python elm327_bt_emulator.py --scenario misfire
    python elm327_bt_emulator.py --list-scenarios

На Android:
    1. Windows Settings → Bluetooth → Add device → найди и соедини телефон
    2. Запусти эмулятор
    3. В приложении: подключись к ПК через RFCOMM channel 1
       (см. раздел "Android подключение" в README.md)
"""

import argparse
import json
import logging
import os
import socket
import subprocess
import sys
import threading

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)

SCENARIOS_DIR = os.path.join(os.path.dirname(__file__), "scenarios")
DEFAULT_SCENARIO = os.environ.get("SCENARIO", "normal")
RFCOMM_CHANNEL = 1

# Bluetooth constants (stdlib на Windows не всегда экспортирует их)
AF_BLUETOOTH = getattr(socket, "AF_BLUETOOTH", 32)       # AF_BTH = 32 on Windows
BTPROTO_RFCOMM = getattr(socket, "BTPROTO_RFCOMM", 3)    # BTHPROTO_RFCOMM = 3


# ─────────────────────────────────────────────────────────────
# Scenario Manager
# ─────────────────────────────────────────────────────────────

class ScenarioManager:
    def __init__(self, name: str = DEFAULT_SCENARIO):
        self._data: dict = {}
        self._dtcs: list[str] = []
        self.load(name)

    def load(self, name: str):
        path = os.path.join(SCENARIOS_DIR, f"{name}.json")
        if not os.path.exists(path):
            log.warning(f"Scenario '{name}' not found, using 'normal'")
            path = os.path.join(SCENARIOS_DIR, "normal.json")
        with open(path, encoding="utf-8") as f:
            self._data = json.load(f)
        self._dtcs = list(self._data.get("dtcs", []))
        log.info(
            f"Scenario: [{self._data.get('name', name)}] "
            f"{self._data.get('description', '')} | "
            f"DTCs: {self._dtcs or 'none'}"
        )

    @property
    def rpm(self) -> int:          return self._data.get("rpm", 800)
    @property
    def speed(self) -> int:        return self._data.get("speed", 0)
    @property
    def coolant_temp(self) -> int: return self._data.get("coolant_temp", 90)
    @property
    def intake_temp(self) -> int:  return self._data.get("intake_temp", 25)
    @property
    def throttle(self) -> int:     return self._data.get("throttle", 10)
    @property
    def fuel_level(self) -> int:   return self._data.get("fuel_level", 70)
    @property
    def battery_voltage(self) -> str: return self._data.get("battery_voltage", "12.4V")
    @property
    def dtcs(self) -> list[str]:   return self._dtcs

    def clear_dtcs(self):
        self._dtcs.clear()
        log.info("DTCs cleared")


# ─────────────────────────────────────────────────────────────
# ELM327 Command Handler
# ─────────────────────────────────────────────────────────────

class ELM327Handler:
    def __init__(self, scenario: ScenarioManager):
        self.sm = scenario
        self.echo = True
        self.linefeeds = True
        self.spaces = True
        self.headers = False

    def _fmt(self, data: str) -> str:
        if not self.spaces:
            data = data.replace(" ", "")
        suffix = "\r\n" if self.linefeeds else "\r"
        return data + suffix + ">"

    def process(self, raw: str) -> str:
        cmd = raw.strip().upper()
        if not cmd:
            return ">"
        response = self._dispatch(cmd)
        if self.echo:
            return raw.strip() + "\r\n" + response
        return response

    def _dispatch(self, cmd: str) -> str:  # noqa: PLR0911, PLR0912
        sm = self.sm

        if cmd == "ATZ":
            self.echo = True; self.linefeeds = True
            self.spaces = True; self.headers = False
            return self._fmt("ELM327 v1.5")

        if cmd in ("ATE0", "ATE 0"):   self.echo = False;      return self._fmt("OK")
        if cmd in ("ATE1", "ATE 1"):   self.echo = True;       return self._fmt("OK")
        if cmd in ("ATL0", "ATL 0"):   self.linefeeds = False; return self._fmt("OK")
        if cmd in ("ATL1", "ATL 1"):   self.linefeeds = True;  return self._fmt("OK")
        if cmd in ("ATS0", "ATS 0"):   self.spaces = False;    return self._fmt("OK")
        if cmd in ("ATS1", "ATS 1"):   self.spaces = True;     return self._fmt("OK")
        if cmd in ("ATH0", "ATH 0"):   self.headers = False;   return self._fmt("OK")
        if cmd in ("ATH1", "ATH 1"):   self.headers = True;    return self._fmt("OK")

        if cmd.startswith(("ATSP", "ATAT", "ATST", "ATSH", "ATCAF", "ATSM")):
            return self._fmt("OK")
        if cmd == "ATDP":   return self._fmt("AUTO, ISO 15765-4 (CAN 11/500)")
        if cmd == "ATDPN":  return self._fmt("A6")
        if cmd == "ATAL":   return self._fmt("OK")
        if cmd in ("ATI", "AT@1"): return self._fmt("ELM327 v1.5")
        if cmd == "ATRV":   return self._fmt(sm.battery_voltage)

        if cmd == "0100": return self._fmt("41 00 BE 3F A8 13")
        if cmd == "0120": return self._fmt("41 20 80 01 A0 01")
        if cmd == "0140": return self._fmt("41 40 44 00 00 00")

        if cmd == "0101":
            n = min(len(sm.dtcs), 127)
            return self._fmt(f"41 01 {n:02X} 00 00 00")

        if cmd == "010C":
            v = int(sm.rpm * 4)
            return self._fmt(f"41 0C {(v >> 8) & 0xFF:02X} {v & 0xFF:02X}")
        if cmd == "010D":  return self._fmt(f"41 0D {sm.speed:02X}")
        if cmd == "0105":  return self._fmt(f"41 05 {sm.coolant_temp + 40:02X}")
        if cmd == "010F":  return self._fmt(f"41 0F {sm.intake_temp + 40:02X}")
        if cmd == "0111":  return self._fmt(f"41 11 {int(sm.throttle * 2.55):02X}")
        if cmd == "012F":  return self._fmt(f"41 2F {int(sm.fuel_level * 2.55):02X}")
        if cmd == "010B":  return self._fmt("41 0B 65")
        if cmd == "010A":  return self._fmt("41 0A 5D")
        if cmd == "0104":
            load = min(int(sm.rpm / 60), 100)
            return self._fmt(f"41 04 {int(load * 2.55):02X}")
        if cmd == "0106":  return self._fmt("41 06 80")
        if cmd == "0107":  return self._fmt("41 07 80")
        if cmd == "010E":  return self._fmt("41 0E 86")

        if cmd == "03":
            dtcs = sm.dtcs
            if not dtcs:
                return self._fmt("43 00 00 00 00 00 00")
            parts = ["43"]
            for d in dtcs:
                parts.append(f"{d[:2]} {d[2:]}")
            while len(parts) < 7:
                parts.append("00 00")
            return self._fmt(" ".join(parts))

        if cmd == "04":
            sm.clear_dtcs()
            return self._fmt("44")

        if cmd == "07":
            return self._fmt("47 00 00 00 00 00 00")

        if cmd == "0902":
            vin = "1HGBH41JXMN109186"
            hex_vin = " ".join(f"{ord(c):02X}" for c in vin)
            return self._fmt(f"49 02 01 {hex_vin}")

        log.debug(f"Unknown: {cmd!r}")
        return self._fmt("?")


# ─────────────────────────────────────────────────────────────
# Client handler
# ─────────────────────────────────────────────────────────────

def handle_client(conn: socket.socket, handler: ELM327Handler, addr):
    log.info(f"[+] Connected: {addr}")
    buf = ""
    try:
        while True:
            data = conn.recv(256)
            if not data:
                break
            buf += data.decode("ascii", errors="ignore")
            while "\r" in buf:
                line, buf = buf.split("\r", 1)
                line = line.strip()
                if line:
                    log.info(f"RX: {line!r}")
                    resp = handler.process(line)
                    log.info(f"TX: {resp!r}")
                    conn.sendall(resp.encode("ascii"))
    except OSError as e:
        log.warning(f"Connection error: {e}")
    finally:
        conn.close()
        log.info(f"[-] Disconnected: {addr}")


# ─────────────────────────────────────────────────────────────
# Local Bluetooth MAC helper
# ─────────────────────────────────────────────────────────────

def get_local_bt_mac() -> str:
    """Get local Bluetooth adapter MAC address via PowerShell."""
    try:
        result = subprocess.run(
            ["powershell", "-NoProfile", "-Command",
             "Get-NetAdapter | Where-Object { $_.InterfaceDescription -like '*Bluetooth*' }"
             " | Select-Object -First 1 -ExpandProperty MacAddress"],
            capture_output=True, text=True, timeout=5,
        )
        mac = result.stdout.strip().replace("-", ":")
        if len(mac) == 17:
            return mac
    except Exception:
        pass
    return ""


# ─────────────────────────────────────────────────────────────
# Bluetooth RFCOMM Server
# ─────────────────────────────────────────────────────────────

def run_bluetooth(scenario_name: str):
    scenario = ScenarioManager(scenario_name)

    if not hasattr(socket, "AF_BLUETOOTH") and AF_BLUETOOTH not in vars(socket).values():
        log.error("socket.AF_BLUETOOTH недоступен.")
        log.error("Bluetooth не поддерживается этой версией Python или Windows.")
        sys.exit(1)

    try:
        server = socket.socket(AF_BLUETOOTH, socket.SOCK_STREAM, BTPROTO_RFCOMM)
    except OSError as e:
        log.error(f"Не удалось создать Bluetooth сокет: {e}")
        log.error("Убедись что Bluetooth включён в Windows Settings")
        sys.exit(1)

    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    local_mac = get_local_bt_mac()
    bind_addr = local_mac if local_mac else ""

    try:
        server.bind((bind_addr, RFCOMM_CHANNEL))
    except OSError as e:
        log.error(f"Bind на RFCOMM channel {RFCOMM_CHANNEL} не удался: {e}")
        if not local_mac:
            log.error("Попробуй запустить от имени Администратора")
        sys.exit(1)

    server.listen(3)

    log.info("=" * 60)
    log.info(f"  ELM327 Bluetooth Emulator — RFCOMM channel {RFCOMM_CHANNEL}")
    if local_mac:
        log.info(f"  Bluetooth MAC: {local_mac}")
    log.info(f"  Scenario: {scenario_name}")
    log.info("")
    log.info("  Шаги для подключения Android:")
    log.info("  1. Windows Settings → Bluetooth → Add device")
    log.info("     (если ещё не соединён — подключи телефон к ПК)")
    log.info("  2. В Android приложении: выбери этот ПК")
    log.info("     Используй RFCOMM channel 1 (не UUID)")
    log.info("     (см. README.md → Android подключение)")
    log.info("")
    log.info("  Press Ctrl+C to stop")
    log.info("=" * 60)

    try:
        while True:
            conn, addr = server.accept()
            handler = ELM327Handler(scenario)
            t = threading.Thread(
                target=handle_client,
                args=(conn, handler, addr),
                daemon=True,
            )
            t.start()
    except KeyboardInterrupt:
        log.info("Shutting down...")
    finally:
        server.close()


# ─────────────────────────────────────────────────────────────
# Entrypoint
# ─────────────────────────────────────────────────────────────

def list_scenarios():
    files = [f[:-5] for f in os.listdir(SCENARIOS_DIR) if f.endswith(".json")]
    print("Available scenarios:")
    for name in sorted(files):
        path = os.path.join(SCENARIOS_DIR, f"{name}.json")
        with open(path) as f:
            data = json.load(f)
        print(f"  {name:<20} — {data.get('description', '')}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="ELM327 Bluetooth Emulator (stdlib only)")
    parser.add_argument("--scenario", default=DEFAULT_SCENARIO)
    parser.add_argument("--list-scenarios", action="store_true")
    args = parser.parse_args()

    if args.list_scenarios:
        list_scenarios()
        sys.exit(0)

    run_bluetooth(args.scenario)

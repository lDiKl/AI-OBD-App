"""
ELM327 TCP Emulator (Docker)
============================
Runs inside Docker. Accepts TCP connections instead of Bluetooth.
Use for automated tests, CI, or when Bluetooth is not available.

Usage:
    docker compose up
    docker compose up -e SCENARIO=misfire

Or directly:
    python elm327_tcp_emulator.py
    python elm327_tcp_emulator.py --scenario sensor_failure --port 35000

Test from terminal:
    nc localhost 35000
    > ATZ
    > 03
    > 010C

Android connection (requires TCP mode support in app):
    Host: <your-PC-IP>
    Port: 35000
"""

import argparse
import json
import logging
import os
import random
import socket
import sys
import threading

logging.basicConfig(
    level=os.environ.get("LOG_LEVEL", "INFO"),
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger(__name__)

HOST = "0.0.0.0"
DEFAULT_PORT = int(os.environ.get("PORT", 35000))
SCENARIOS_DIR = os.path.join(os.path.dirname(__file__), "scenarios")
DEFAULT_SCENARIO = os.environ.get("SCENARIO", "normal")

# Pool of realistic DTC codes for random injection
DTC_POOL = [
    "0301",  # P0301 — Cylinder 1 misfire
    "0300",  # P0300 — Random misfire
    "0420",  # P0420 — Catalyst efficiency low (Bank 1)
    "0171",  # P0171 — System too lean (Bank 1)
    "0133",  # P0133 — O2 sensor slow response
    "0401",  # P0401 — EGR flow insufficient
    "0442",  # P0442 — Evap system small leak
    "0507",  # P0507 — Idle control RPM high
    "0128",  # P0128 — Coolant temp below thermostat
    "0455",  # P0455 — Evap system large leak
]

_connection_count = 0  # tracks connections for alternating clean/error pattern


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
            log.warning(f"Scenario '{name}' not found, falling back to 'normal'")
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
    def rpm(self) -> int:
        return self._data.get("rpm", 800)

    @property
    def speed(self) -> int:
        return self._data.get("speed", 0)

    @property
    def coolant_temp(self) -> int:
        return self._data.get("coolant_temp", 90)

    @property
    def intake_temp(self) -> int:
        return self._data.get("intake_temp", 25)

    @property
    def throttle(self) -> int:
        return self._data.get("throttle", 10)

    @property
    def fuel_level(self) -> int:
        return self._data.get("fuel_level", 70)

    @property
    def battery_voltage(self) -> str:
        return self._data.get("battery_voltage", "12.4V")

    @property
    def dtcs(self) -> list[str]:
        return self._dtcs

    def clear_dtcs(self):
        self._dtcs.clear()
        log.info("DTCs cleared")


# ─────────────────────────────────────────────────────────────
# ELM327 Command Handler
# ─────────────────────────────────────────────────────────────

class ELM327Handler:
    """Handles AT commands and OBD Mode 01/03/04/07/09."""

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
        # Normalize: "AT Z" → "ATZ", "AT E0" → "ATE0", "AT SP 0" → "ATSP0"
        if cmd.startswith("AT "):
            cmd = "AT" + cmd[3:].replace(" ", "")
        else:
            # Strip spaces from OBD commands: "02 0C 01" → "020C01", "01 0C" → "010C"
            cmd = cmd.replace(" ", "")
        response = self._dispatch(cmd)
        if self.echo:
            return raw.strip() + "\r\n" + response
        return response

    def _dispatch(self, cmd: str) -> str:  # noqa: PLR0911, PLR0912
        sm = self.sm

        # ── AT — reset ──────────────────────────────────────────
        if cmd == "ATZ":
            self.echo = True
            self.linefeeds = True
            self.spaces = True
            self.headers = False
            return self._fmt("ELM327 v1.5")

        # ── AT — options ─────────────────────────────────────────
        if cmd in ("ATE0", "ATE 0"):
            self.echo = False
            return self._fmt("OK")
        if cmd in ("ATE1", "ATE 1"):
            self.echo = True
            return self._fmt("OK")
        if cmd in ("ATL0", "ATL 0"):
            self.linefeeds = False
            return self._fmt("OK")
        if cmd in ("ATL1", "ATL 1"):
            self.linefeeds = True
            return self._fmt("OK")
        if cmd in ("ATS0", "ATS 0"):
            self.spaces = False
            return self._fmt("OK")
        if cmd in ("ATS1", "ATS 1"):
            self.spaces = True
            return self._fmt("OK")
        if cmd in ("ATH0", "ATH 0"):
            self.headers = False
            return self._fmt("OK")
        if cmd in ("ATH1", "ATH 1"):
            self.headers = True
            return self._fmt("OK")

        # ── AT — protocol / timing (just ACK) ────────────────────
        if cmd.startswith(("ATSP", "ATAT", "ATST", "ATSH", "ATCAF", "ATCSM", "ATSM")):
            return self._fmt("OK")
        if cmd == "ATDP":
            return self._fmt("AUTO, ISO 15765-4 (CAN 11/500)")
        if cmd == "ATDPN":
            return self._fmt("A6")
        if cmd == "ATAL":
            return self._fmt("OK")

        # ── AT — info ────────────────────────────────────────────
        if cmd in ("ATI", "AT@1"):
            return self._fmt("ELM327 v1.5")
        if cmd == "ATRV":
            return self._fmt(sm.battery_voltage)

        # ── Mode 01 — Supported PIDs ─────────────────────────────
        if cmd == "0100":
            return self._fmt("41 00 BE 3F A8 13")
        if cmd == "0120":
            return self._fmt("41 20 80 01 A0 01")
        if cmd == "0140":
            return self._fmt("41 40 44 00 00 00")

        # ── Mode 01 — Current Data ───────────────────────────────
        if cmd == "0101":
            n = min(len(sm.dtcs), 127)
            return self._fmt(f"41 01 {n:02X} 00 00 00")

        if cmd == "010C":          # RPM
            v = int(sm.rpm * 4)
            return self._fmt(f"41 0C {(v >> 8) & 0xFF:02X} {v & 0xFF:02X}")

        if cmd == "010D":          # Speed km/h
            return self._fmt(f"41 0D {sm.speed:02X}")

        if cmd == "0105":          # Coolant temp (offset -40)
            return self._fmt(f"41 05 {sm.coolant_temp + 40:02X}")

        if cmd == "010F":          # Intake air temp (offset -40)
            return self._fmt(f"41 0F {sm.intake_temp + 40:02X}")

        if cmd == "0111":          # Throttle position 0-100%
            return self._fmt(f"41 11 {int(sm.throttle * 2.55):02X}")

        if cmd == "012F":          # Fuel level 0-100%
            return self._fmt(f"41 2F {int(sm.fuel_level * 2.55):02X}")

        if cmd == "010B":          # MAP sensor (kPa)
            return self._fmt("41 0B 65")

        if cmd == "010A":          # Fuel pressure
            return self._fmt("41 0A 5D")

        if cmd == "0104":          # Engine load 0-100%
            load = min(int(sm.rpm / 60), 100)
            return self._fmt(f"41 04 {int(load * 2.55):02X}")

        if cmd == "0106":          # Short-term fuel trim bank 1
            return self._fmt("41 06 80")

        if cmd == "0107":          # Long-term fuel trim bank 1
            return self._fmt("41 07 80")

        if cmd == "010E":          # Ignition advance
            return self._fmt("41 0E 86")

        # ── Mode 03 — Stored DTCs ────────────────────────────────
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

        # ── Mode 02 — Freeze Frame Data ──────────────────────────
        # Response: 42 <PID> <data bytes>  (no frame number in response)
        if cmd == "020C01":   # Engine RPM → (A*256+B)/4
            v = int(sm.rpm * 4)
            return self._fmt(f"42 0C {(v >> 8) & 0xFF:02X} {v & 0xFF:02X}")

        if cmd == "020D01":   # Vehicle speed → A km/h
            return self._fmt(f"42 0D {sm.speed:02X}")

        if cmd == "020501":   # Coolant temp → A-40 °C
            return self._fmt(f"42 05 {sm.coolant_temp + 40:02X}")

        if cmd == "020401":   # Engine load → A*100/255 %
            load = min(int(sm.rpm / 60), 100)
            return self._fmt(f"42 04 {int(load * 2.55):02X}")

        # ── Mode 04 — Clear DTCs ─────────────────────────────────
        if cmd == "04":
            sm.clear_dtcs()
            return self._fmt("44")

        # ── Mode 07 — Pending DTCs ───────────────────────────────
        if cmd == "07":
            return self._fmt("47 00 00 00 00 00 00")

        # ── Mode 09 — Vehicle Info ───────────────────────────────
        if cmd == "0902":
            vin = "1HGBH41JXMN109186"
            hex_vin = " ".join(f"{ord(c):02X}" for c in vin)
            return self._fmt(f"49 02 01 {hex_vin}")

        if cmd == "090A":
            return self._fmt("49 0A 01 45 4C 4D 33 32 37")

        log.debug(f"Unknown command: {cmd!r}")
        return self._fmt("?")


# ─────────────────────────────────────────────────────────────
# Client connection handler
# ─────────────────────────────────────────────────────────────

def handle_client(conn: socket.socket, handler: ELM327Handler, addr, conn_num: int):
    if conn_num % 2 == 0:
        # Even connection → inject 1–5 random DTC codes
        num = random.randint(1, 5)
        handler.sm._dtcs = random.sample(DTC_POOL, num)
        log.info(f"[+] Client connected: {addr}  [#{conn_num} — injecting {num} DTC(s): {[('P' + d) for d in handler.sm._dtcs]}]")
    else:
        handler.sm._dtcs = []
        log.info(f"[+] Client connected: {addr}  [#{conn_num} — clean scan, no DTCs]")
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
                    response = handler.process(line)
                    log.info(f"TX: {response!r}")
                    conn.sendall(response.encode("ascii"))

    except OSError as e:
        log.warning(f"Connection error: {e}")
    finally:
        conn.close()
        log.info(f"[-] Client disconnected: {addr}")


# ─────────────────────────────────────────────────────────────
# TCP Server
# ─────────────────────────────────────────────────────────────

def run_tcp(scenario_name: str, port: int):
    scenario = ScenarioManager(scenario_name)

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, port))
    server.listen(5)

    log.info("=" * 55)
    log.info(f"  ELM327 TCP Emulator listening on port {port}")
    log.info(f"  Scenario: {scenario_name}")
    log.info(f"  Test: nc localhost {port}")
    log.info("  Press Ctrl+C to stop")
    log.info("=" * 55)

    try:
        while True:
            conn, addr = server.accept()
            global _connection_count
            _connection_count += 1
            handler = ELM327Handler(ScenarioManager(scenario_name))
            t = threading.Thread(
                target=handle_client,
                args=(conn, handler, addr, _connection_count),
                daemon=True,
            )
            t.start()
    except KeyboardInterrupt:
        log.info("Shutting down...")
    finally:
        server.close()


def list_scenarios():
    files = [f[:-5] for f in os.listdir(SCENARIOS_DIR) if f.endswith(".json")]
    print("Available scenarios:")
    for name in sorted(files):
        path = os.path.join(SCENARIOS_DIR, f"{name}.json")
        with open(path) as f:
            data = json.load(f)
        print(f"  {name:<20} — {data.get('description', '')}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="ELM327 TCP Emulator (Docker / local)"
    )
    parser.add_argument("--scenario", default=DEFAULT_SCENARIO)
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--list-scenarios", action="store_true")
    args = parser.parse_args()

    if args.list_scenarios:
        list_scenarios()
        sys.exit(0)

    run_tcp(args.scenario, args.port)

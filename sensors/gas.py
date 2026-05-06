# sensors/gas.py
import time, threading, pigpio
from firebase_admin import db
from sensors.buzzer import alert as buz_alert, off as buz_off   # make sure you run from project root
from fcm import init_firebase, send_gas_alert

# === Config ===
BIN_ID          = "smartbinyarn"
STABLE_SECONDS  = 5.0     # must be stable this long before reporting
POLL_SEC        = 0.2     # sensor poll loop
COOLDOWN_SEC    = 10.0    # rest time AFTER an alert (per compartment)
REMIND_SEC      = 60.0    # if leak persists past cooldown, remind again every N sec (set 0 to disable)

# MQ135 digital output pins (BCM)
GAS_PINS = {"toxic": 6, "non_toxic": 5, "food": 13}

# === pigpio setup ===
_pi = pigpio.pi()
if not _pi.connected:
    raise RuntimeError("pigpio not running. sudo systemctl start pigpiod")

for p in GAS_PINS.values():
    _pi.set_mode(p, pigpio.INPUT)
    _pi.set_pull_up_down(p, pigpio.PUD_OFF)  # boards usually drive the line

def log_event(compartment, status):
    evt = {
        "binId": BIN_ID,
        "compartment": compartment,
        "status": status,           # "detected" | "normal"
        "timestamp": int(time.time())
    }
    db.reference("gas_events").push(evt)
    print("📤 RTDB:", evt)

def _buzz_once_async():
    # short, non-blocking beep pattern
    t = threading.Thread(target=buz_alert, daemon=True)
    t.start()

def monitor_gas():
    
    print("🧪 Monitoring MQ135 digital outputs (0 = DETECTED)")
    init_firebase()               # ensure Firebase/FCM app initialized
    buz_off()                     # keep quiet at start

    pending_state = {n: None for n in GAS_PINS}  # current raw state being confirmed
    since_change  = {n: None for n in GAS_PINS}  # when pending_state last changed
    last_reported = {n: None for n in GAS_PINS}  # last *reported* boolean (True leak / False normal)
    last_alert_ts = {n: 0.0  for n in GAS_PINS}  # last time we sent a 'detected' alert
    last_remind_ts= {n: 0.0  for n in GAS_PINS}  # last periodic reminder time

    try:
        while True:
            now = time.time()
            for name, pin in GAS_PINS.items():
                raw  = _pi.read(pin)
                leak = (raw == 0)   # flip to (raw == 1) if your board is active-high

                # initialize the pending tracker on first pass
                if pending_state[name] is None:
                    pending_state[name], since_change[name] = leak, now

                if leak == pending_state[name]:
                    # state hasn't changed; check stability window
                    stable_for = now - since_change[name]
                    if (last_reported[name] != leak) and (stable_for >= STABLE_SECONDS):
                        # a new stable state reached → report once
                        status = "detected" if leak else "normal"
                        print(("⚠️" if leak else "✅"), f"{name}: {status} (stable {STABLE_SECONDS}s)")
                        log_event(name, status)
                        send_gas_alert(BIN_ID, name, status)

                        if leak:
                            _buzz_once_async()
                            last_alert_ts[name] = now
                            last_remind_ts[name] = now  # reset reminder anchor
                        else:
                            buz_off()

                        last_reported[name] = leak

                    # optional periodic reminder if leak persists
                    if leak and last_reported[name] is True and REMIND_SEC > 0:
                        if (now - last_alert_ts[name]) >= COOLDOWN_SEC and (now - last_remind_ts[name]) >= REMIND_SEC:
                            print(f"⏰ Reminder: {name} still leaking (>{int(now - since_change[name])}s).")
                            log_event(name, "detected")           # log the continued condition
                            send_gas_alert(BIN_ID, name, "detected")
                            _buzz_once_async()
                            last_remind_ts[name] = now
                else:
                    # state changed → begin stability timing
                    pending_state[name], since_change[name] = leak, now

            time.sleep(POLL_SEC)

    except KeyboardInterrupt:
        print("\n🧹 Exiting gas monitor.")
    finally:
        try:
            buz_off()
        finally:
            _pi.stop()

def ping():
    print("[ping] Gas sensor module ready ✅")

if __name__ == "__main__":
    # Run from project root so package imports work:
    #   python -m sensors.gas
    monitor_gas()

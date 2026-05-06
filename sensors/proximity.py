# sensors/proximity.py
import time
import threading
import pigpio
import firebase_admin
from firebase_admin import credentials, db, messaging



# =========================
# CONFIG
# =========================
FIREBASE_KEY = "/home/smartbinyarn/project/firebase_key.json"
RTDB_URL     = "https://smartbinyan-default-rtdb.firebaseio.com/"
BIN_ID       = "smartbinyarn"

# Topics your Android app should subscribe to:
TOPIC_ALL       = f"binfull_{BIN_ID}_all"
TOPIC_TOXIC     = f"binfull_{BIN_ID}_toxic"
TOPIC_NON_TOXIC = f"binfull_{BIN_ID}_non_toxic"
TOPIC_FOOD      = f"binfull_{BIN_ID}_food"

# BCM pins for each compartment’s proximity sensor (E18-D80NK OUT)
# Adjust to your actual wiring:
PROX_PINS = {
    "toxic": 27,       # BCM 17  (Phys 11)
    "non_toxic": 22,   # BCM 27  (Phys 13)
    "food": 17         # BCM 22  (Phys 15)
}

ACTIVE_LOW     = True   # E18 modules typically drive LOW when object present
STABLE_SECONDS = 7.0    # require continuous 7s before we report a change
POLL_SEC       = 0.1

# =========================
# FIREBASE INIT
# =========================
if not firebase_admin._apps:
    cred = credentials.Certificate(FIREBASE_KEY)
    firebase_admin.initialize_app(cred, {"databaseURL": RTDB_URL})

# =========================
# GPIO INIT (pigpio)
# =========================
_pi = pigpio.pi()
if not _pi.connected:
    raise RuntimeError("pigpio not running. Start with: sudo systemctl start pigpiod")

for p in PROX_PINS.values():
    _pi.set_mode(p, pigpio.INPUT)
    # Most E18 boards already have proper conditioning; leave pulls OFF.
    _pi.set_pull_up_down(p, pigpio.PUD_OFF)

# =========================
# HELPERS
# =========================
def _is_detected_level(level: int) -> bool:
    """Interpret raw level as detected/blocked."""
    return (level == 0) if ACTIVE_LOW else (level == 1)

def _log_event(compartment: str, status: str):
    
    evt = {
        "binId": BIN_ID,
        "compartment": compartment,
        "status": status,
        "timestamp": int(time.time())
    }
    db.reference("proximity_events").push(evt)
    print(f"📤 RTDB:", evt)

def _send_fcm(compartment: str, status: str):
    """
    Push to topics:
      - binfull_<BIN_ID>_all
      - binfull_<BIN_ID>_<compartment>
    """
    topics = [TOPIC_ALL]
    if compartment == "toxic":
        topics.append(TOPIC_TOXIC)
    elif compartment == "non_toxic":
        topics.append(TOPIC_NON_TOXIC)
    elif compartment == "food":
        topics.append(TOPIC_FOOD)

    if status == "full":
        title = "Bin Full"
        body  = f"{compartment.replace('_',' ').title()} compartment: FULL"
    else:
        title = "Bin Cleared"
        body  = f"{compartment.replace('_',' ').title()} compartment: not full"

    notif = messaging.Notification(title=title, body=body)
    data  = {
        "binId": BIN_ID,
        "compartment": compartment,
        "status": status,                 # "full" | "not full"
        "timestamp": str(int(time.time()))
    }

    for topic in topics:
        try:
            msg_id = messaging.send(messaging.Message(notification=notif, data=data, topic=topic))
            print(f"📨 FCM → {topic}: {msg_id}")
        except Exception as e:
            print("⚠️ FCM send error:", e)

# =========================
# MAIN LOOP
# =========================
def monitor_bin():
   
    print(f"📡 Monitoring proximity sensors (active-{'LOW' if ACTIVE_LOW else 'HIGH'}). "
          f"Stability requirement: {STABLE_SECONDS}s")

    pending_state = {name: None for name in PROX_PINS}  # current raw state being timed
    since_change  = {name: None for name in PROX_PINS}  # when the current raw state started
    last_reported = {name: None for name in PROX_PINS}  # last stable (bool) we reported

    try:
        while True:
            now = time.time()

            for name, pin in PROX_PINS.items():
                raw = _pi.read(pin)                         # 0/1
                detected = _is_detected_level(raw)          # True if object is present

                # Initialize on first loop
                if pending_state[name] is None:
                    pending_state[name] = detected
                    since_change[name]  = now

                if detected == pending_state[name]:
                    # state is stable so far; check if it has been stable long enough and differs from last report
                    if last_reported[name] != detected and (now - since_change[name]) >= STABLE_SECONDS:
                        status = "full" if detected else "not full"
                        print(("🟢" if detected else "⚪"),
                              f"{name}: {status} (stable {STABLE_SECONDS}s)")

                        _log_event(name, status)
                        _send_fcm(name, status)
                        last_reported[name] = detected
                else:
                    # state flipped; start timing the new state
                    pending_state[name] = detected
                    since_change[name]  = now

            time.sleep(POLL_SEC)

    except KeyboardInterrupt:
        print("\n🧹 Exiting proximity monitor.")
    finally:
        _pi.stop()

def ping():
    print("[ping] Proximity (3 sensors) + FCM module ready ✅")

if __name__ == "__main__":
    monitor_bin()

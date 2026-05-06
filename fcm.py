# fcm.py
import firebase_admin
from firebase_admin import credentials, db, messaging

# Edit these for your project
FIREBASE_KEY = "/home/smartbinyarn/project/firebase_key.json"
RTDB_URL     = "https://smartbinyan-default-rtdb.firebaseio.com/"

def init_firebase():
    if not firebase_admin._apps:
        cred = credentials.Certificate(FIREBASE_KEY)
        firebase_admin.initialize_app(cred, {"databaseURL": RTDB_URL})
        print("✅ Firebase initialized")

def send_gas_alert(bin_id: str, compartment: str, status: str):
    """
    Sends:
      - RTDB already written by gas.py (log_event)
      - FCM push to topic: gas_<binId>_<compartment>   e.g., gas_smartbinyarn_toxic
    """
    init_firebase()
    topic = f"gas_{bin_id}_{compartment}"

    title = "Gas Leak Detected!" if status == "detected" else "Gas Normal"
    body  = f"{compartment.replace('_',' ').title()}: {status}"

    message = messaging.Message(
        notification=messaging.Notification(title=title, body=body),
        data={"binId": bin_id, "compartment": compartment, "status": status},
        topic=topic
    )
    try:
        resp = messaging.send(message)
        print("📨 FCM sent:", resp, "->", topic)
    except Exception as e:
        print("⚠️ FCM error:", e)

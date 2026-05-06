#!/usr/bin/env python3
import time
import cv2
import RPi.GPIO as GPIO
from ultralytics import YOLO

# === YOLO MODEL PATH ===
MODEL_PATH = "/home/smartbinyarn/project/thefinal/runs/detect/train/weights/best.pt"

# === SERVO CONFIGURATION ===
DISC_SERVO_PIN = 24   # 180° standard servo
WIPER_SERVO_PIN = 23  # 360° continuous servo

DISC_SERVO_FREQ = 50
WIPER_SERVO_FREQ = 50

# === 180° DISC SERVO SETTINGS ===
def angle_to_duty(angle):
    return 2.5 + (angle / 18.0)  # 0° → 2.5%, 180° → 12.5%

CENTER_ANGLE = 90
LEFT_ANGLE = 0
RIGHT_ANGLE = 180

# === 360° WIPER SERVO SETTINGS ===
WIPER_DUTY_CW = 5.0
WIPER_DUTY_STOP = 0.82
WIPER_ROTATION_TIME = 1.50
PAUSE_BETWEEN_ROTATIONS = 0.5

# === DISC POSITIONS ===
HOME_POSITION = 90
POSITIONS = {
    "food": 90,
    "non-toxic": 180,
    "toxic": 0
}

# === TIMING ===
DROP_HOLD_TIME = 5.0
COOLDOWN_TIME = 3.0

_current_disc_deg = HOME_POSITION

# ================= GPIO SETUP =================
def setup_gpio():
    GPIO.setmode(GPIO.BCM)
    GPIO.setwarnings(False)
    GPIO.setup(DISC_SERVO_PIN, GPIO.OUT)
    GPIO.setup(WIPER_SERVO_PIN, GPIO.OUT)

    disc_pwm = GPIO.PWM(DISC_SERVO_PIN, DISC_SERVO_FREQ)
    wiper_pwm = GPIO.PWM(WIPER_SERVO_PIN, WIPER_SERVO_FREQ)
    disc_pwm.start(0)
    wiper_pwm.start(0)

    print("[GPIO] Servos initialized and ready.")
    return disc_pwm, wiper_pwm


# ================= DISC SERVO =================
def move_disc(pwm, angle, delay=0.6):
    global _current_disc_deg
    pwm.ChangeDutyCycle(angle_to_duty(angle))
    time.sleep(delay)
    pwm.ChangeDutyCycle(0)
    _current_disc_deg = angle
    print(f"[Disc] Moved to {angle}°")

def move_disc_to_position(pwm, target):
    print(f"[Disc] Moving from {_current_disc_deg:.1f}° → {target:.1f}°")
    move_disc(pwm, target)
    print("[Disc] Reached target position.")

def return_disc_home(pwm):
    print("[Disc] Returning to HOME (90°)")
    move_disc(pwm, HOME_POSITION)
    print("[Disc] Centered at 90°.")


# ================= WIPER SERVO =================
def wiper_spin_cw(pwm):
    pwm.ChangeDutyCycle(WIPER_DUTY_CW)

def wiper_stop(pwm):
    pwm.ChangeDutyCycle(WIPER_DUTY_STOP)
    time.sleep(0.2)
    pwm.ChangeDutyCycle(0)

def wiper_full_cycle(pwm):
    """Perform 3 full clockwise rotations then stop."""
    print("🧹 [Wiper] Starting 3 full clockwise rotations...")

    for i in range(1, 4):
        print(f"   ▶️  Rotation {i}/3 in progress...")
        wiper_spin_cw(pwm)
        time.sleep(WIPER_ROTATION_TIME)
        wiper_stop(pwm)

        if i < 3:
            print("   ⏸️  Short pause before next rotation...")
            time.sleep(PAUSE_BETWEEN_ROTATIONS)

    print("🔁 Returning wiper to start position...")
    wiper_stop(pwm)
    print("✅ [Wiper] 3 rotations complete.\n")


# ================= PROCESS FLOW =================
def process_waste(disc_pwm, wiper_pwm, waste_type):
    print(f"\n=== PROCESSING {waste_type.upper()} WASTE ===")

    if waste_type == "food":
        print("[Step 1] Food detected — disc stays at 90°, only wiper moves.")
        wiper_full_cycle(wiper_pwm)
        print(f"[Step 2] Waiting {DROP_HOLD_TIME}s for trash to drop...")
        time.sleep(DROP_HOLD_TIME)
        print("[Step 3] Done. Ready for next detection.\n")
        return

    target = POSITIONS.get(waste_type)
    if target is None:
        print(f"⚠️ Unknown waste type: {waste_type}")
        return

    print(f"[Step 1] Moving disc to {waste_type} position ({target}°)")
    move_disc_to_position(disc_pwm, target)
    time.sleep(0.5)

    print("[Step 2] Wiper sweeping...")
    wiper_full_cycle(wiper_pwm)

    print(f"[Step 3] Waiting {DROP_HOLD_TIME}s for trash to drop...")
    time.sleep(DROP_HOLD_TIME)

    print("[Step 4] Returning disc to HOME (90°)...")
    return_disc_home(disc_pwm)

    print("✅ Process complete.\n")


# ================= INTEGRATION HOOKS =================
def setup_for_integration():
    return setup_gpio()


def monitor_integration(disc_pwm, wiper_pwm):
    try:
        print("🤖 Loading YOLO model...")
        model = YOLO(MODEL_PATH)
        print("✅ Model loaded successfully!")

        cap = cv2.VideoCapture(0, cv2.CAP_V4L2)
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

        if not cap.isOpened():
            print("❌ Error: Camera not found!")
            return

        print("🚀 Smart Bin (integrated) is active! Press Ctrl+C to stop.")
        last_process_time = {"food": 0, "non-toxic": 0, "toxic": 0}

        # 🧠 Track object state
        last_detected_type = None
        object_present = False

        while True:
            ok, frame = cap.read()
            if not ok:
                continue

            results = model(frame, imgsz=416, conf=0.5, verbose=False)
            boxes = getattr(results[0], "boxes", None)

            if boxes is not None and len(boxes) > 0:
                current_time = time.time()

                cls = int(boxes.cls[0])
                conf = float(boxes.conf[0])
                name = model.names[cls].lower().strip()

                if "food" in name:
                    waste_type = "food"
                elif "non" in name and "toxic" in name:
                    waste_type = "non-toxic"
                elif "toxic" in name and "non" not in name:
                    waste_type = "toxic"
                else:
                    continue

                # === Process only if new or changed object ===
                if not object_present or waste_type != last_detected_type:
                    print(f"\n🔍 New detection: {waste_type} ({conf:.2f})")
                    process_waste(disc_pwm, wiper_pwm, waste_type)
                    last_detected_type = waste_type
                    object_present = True
                    last_process_time[waste_type] = current_time
                else:
                    print(f"⏳ {waste_type} still detected, waiting for change...")

            else:
                # No object detected → reset detection state
                if object_present:
                    print("🟢 Object removed — ready for next detection.")
                object_present = False
                last_detected_type = None

            time.sleep(0.05)

    except KeyboardInterrupt:
        print("\n👋 Servo thread stopped by user.")
    finally:
        disc_pwm.stop()
        wiper_pwm.stop()
        GPIO.cleanup()
        print("✅ Servo system cleaned up.")


# ================= STANDALONE RUN =================
if __name__ == "__main__":
    disc_pwm, wiper_pwm = setup_for_integration()
    monitor_integration(disc_pwm, wiper_pwm)

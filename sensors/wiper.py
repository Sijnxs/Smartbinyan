#!/usr/bin/env python3
import time
import RPi.GPIO as GPIO

# ================= 360° WIPER SERVO CONFIGURATION =================
WIPER_SERVO_PIN = 23
WIPER_SERVO_FREQ = 50

# Speed/Direction Settings
WIPER_DUTY_CW = 5.0        # Clockwise rotation (adjust between 2.5–6.5 if needed)
WIPER_DUTY_STOP = 0.82     # Stop signal (neutral, prevents jitter)

# Timing Settings
WIPER_ROTATION_TIME = 1.50   # Time for one full 360° rotation
PAUSE_BETWEEN_ROTATIONS = 0.2  # Pause between each spin


# ================= WIPER CONTROL FUNCTIONS =================

def wiper_spin_cw(pwm):
    """Rotate wiper clockwise."""
    pwm.ChangeDutyCycle(WIPER_DUTY_CW)


def wiper_stop(pwm):
    """Stop the wiper servo."""
    pwm.ChangeDutyCycle(WIPER_DUTY_STOP)
    time.sleep(0.2)
    pwm.ChangeDutyCycle(0)


def wiper_full_cycle(pwm):
    """
    Perform 3 continuous clockwise rotations (no reverse).
    Ends approximately at the same starting position.
    """
    print("🧹 [Wiper] Starting 3 full clockwise rotations...")

    for i in range(1, 4):
        print(f"   ▶️  Rotation {i}/3 in progress...")
        wiper_spin_cw(pwm)
        time.sleep(WIPER_ROTATION_TIME)
        wiper_stop(pwm)

        if i < 3:
            print("   ⏸️  Short pause before next rotation...")
            time.sleep(PAUSE_BETWEEN_ROTATIONS)

    print("✅ [Wiper] 3 clockwise rotations complete. Back to start position.\n")


# ================= SETUP (Standalone Use) =================

def setup_wiper_gpio():
    """Initialize GPIO for wiper servo only."""
    GPIO.setmode(GPIO.BCM)
    GPIO.setwarnings(False)
    GPIO.setup(WIPER_SERVO_PIN, GPIO.OUT)

    wiper_pwm = GPIO.PWM(WIPER_SERVO_PIN, WIPER_SERVO_FREQ)
    wiper_pwm.start(0)

    print(f"[Wiper] GPIO Pin {WIPER_SERVO_PIN} initialized.")
    return wiper_pwm


# ================= STANDALONE TEST =================

if __name__ == "__main__":
    wiper_pwm = setup_wiper_gpio()

    try:
        print("\n🚀 Running 3-rotation wiper test...")
        wiper_full_cycle(wiper_pwm)
        print("✅ Test complete!")

    except KeyboardInterrupt:
        print("\n🛑 Test interrupted by user.")

    finally:
        wiper_stop(wiper_pwm)
        wiper_pwm.stop()
        GPIO.cleanup()
        print("GPIO cleaned up.\n")

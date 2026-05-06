import RPi.GPIO as GPIO
import time

PIN = 24
FREQ = 50

# Angles
CENTER = 90   # Default
LEFT = 0      # Counterclockwise
RIGHT = 180   # Clockwise

def angle_to_duty(angle):
    return 2.5 + (angle / 18.0)  # 0°→2.5%, 180°→12.5%

GPIO.setmode(GPIO.BCM)
GPIO.setup(PIN, GPIO.OUT)
pwm = GPIO.PWM(PIN, FREQ)
pwm.start(0)

def move_servo(angle, delay=0.5):
    pwm.ChangeDutyCycle(angle_to_duty(angle))
    time.sleep(delay)
    pwm.ChangeDutyCycle(0)

try:
    print("\n=== 180° Servo Control ===")
    print("[L] → Turn Left (0°)")
    print("[R] → Turn Right (180°)")
    print("[Q] → Quit\n")

    move_servo(CENTER, 0.6)
    print("Servo set to Center (90°).")

    while True:
        cmd = input("Enter command (L/R/Q): ").strip().lower()

        if cmd == 'l':
            print("↺ Turning Left (0°)...")
            move_servo(LEFT, 0.6)
            print("⏸ Holding for 8 seconds...")
            time.sleep(2)
            print("↩ Returning to Center (90°)...")
            move_servo(CENTER, 0.6)

        elif cmd == 'r':
            print("↻ Turning Right (180°)...")
            move_servo(RIGHT, 0.6)
            print("⏸ Holding for 8 seconds...")
            time.sleep(2)
            print("↩ Returning to Center (90°)...")
            move_servo(CENTER, 0.6)

        elif cmd == 'q':
            print("👋 Exiting...")
            break

        else:
            print("⚠️ Invalid input. Try L, R, or Q.")

finally:
    pwm.stop()
    GPIO.cleanup()
    print("✅ GPIO cleanup complete.")

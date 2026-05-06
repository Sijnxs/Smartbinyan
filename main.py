#!/usr/bin/env python3
import threading
import time
import signal

from sensors.gas import monitor_gas
from sensors.proximity import monitor_bin
from sensors.servo import setup_for_integration, monitor_integration
import RPi.GPIO as GPIO

shutdown_flag = threading.Event()

def signal_handler(sig, frame):
    print("\n\n⚠️  Shutdown signal received...")
    shutdown_flag.set()

def run_servo():
    disc_pwm, wiper_pwm = setup_for_integration()
    try:
        monitor_integration(disc_pwm, wiper_pwm)
    finally:
        disc_pwm.stop()
        wiper_pwm.stop()

def main():
    print("🚀 SMARTBINYAN SYSTEM STARTING...")
    
    signal.signal(signal.SIGINT, signal_handler)
    
    threads = [
        threading.Thread(target=monitor_gas, name="gas", daemon=True),
        threading.Thread(target=monitor_bin, name="proximity", daemon=True),
        threading.Thread(target=run_servo, name="servo", daemon=True)
    ]

    for t in threads:
        t.start()
        time.sleep(0.5)

    print("✅ ALL SYSTEMS OPERATIONAL\n")

    try:
        while not shutdown_flag.is_set():
            time.sleep(1)
    except KeyboardInterrupt:
        shutdown_flag.set()
    finally:
        GPIO.cleanup()
        print("✅ SYSTEM STOPPED")

if __name__ == "__main__":
    main()
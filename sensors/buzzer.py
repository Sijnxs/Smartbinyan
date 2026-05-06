import time
import pigpio

BUZZER_PIN = 25  # BCM pin
_pi = pigpio.pi()
if not _pi.connected:
    raise RuntimeError("pigpio not running. Start with: sudo systemctl start pigpiod")

# Active-low logic (0 = ON, 1 = OFF)
def _on():
    _pi.write(BUZZER_PIN, 0)  # ON for active-low
def _off():
    _pi.write(BUZZER_PIN, 1)  # OFF for active-low

def alert(duration=10.0):
    
    start = time.time()

    while time.time() - start < duration:
        
        _on()
        time.sleep(1.5)
        _off()
        time.sleep(0.3)

        _on()
        time.sleep(1.5)
        _off()
        time.sleep(0.3)

        _on()
        time.sleep(1.5)
        _off()
        time.sleep(0.3)

        _on()
        time.sleep(1.5)
        _off()
        time.sleep(0.3)

        

    _off()

def off():
    """Ensure buzzer is off."""
    _off()

# Make sure buzzer is off when file loads
off()

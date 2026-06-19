# Loud Vibrate

Automatically change your phone's ringer mode based on nearby Wi-Fi SSIDs.

Configure rules that trigger when a Wi-Fi network comes in range or goes out of range — no root, no tasker.

## How it works

The app runs a foreground service that scans for Wi-Fi networks every 30 seconds. When a network you've configured appears or disappears, your ringer mode changes automatically.

## Rules

Each rule has:
- **SSID** — the Wi-Fi network name to watch
- **Trigger** — "comes in range" or "goes out of range"
- **Ringer mode** — Ring, Vibrate, or Silent
- **Enabled/disabled** — toggle individual rules on and off

Rules are processed in order. Drag the handle on the left to reorder them.

### Behavior

- When an SSID with an in-range rule appears, your phone switches to that rule's ringer mode
- Out-of-range rules only fire when no in-range rule is currently active (no configured SSID with an in-range rule is visible)
- If multiple rules match on the same scan, the first one wins

## Use cases

- **Home**: phone on vibrate when connected to your home Wi-Fi
- **Office**: phone on silent when at work
- **Bedroom**: phone on silent when your bedroom network appears
- **Leaving home**: phone rings again when your home network disappears

## Requirements

- Android 10 or later
- Location permission (for Wi-Fi scanning on older devices)
- Nearby Wi-Fi devices permission (Android 13+)
- Notification permission (for the foreground service)
- Notification policy access (to change ringer mode programmatically)

## Building

```bash
./gradlew assembleDebug
```

Requires Android SDK with platform 36 and build-tools 36.0.0.

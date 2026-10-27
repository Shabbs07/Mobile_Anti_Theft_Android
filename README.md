# Mobile Anti-Theft Plus (Android / Java)

Starter project for learning Android security app development. This is source code; it is not a prebuilt APK.

## Features in this version
- Remote-location workflow: user taps a button to send the last-known location to a configured trusted number by SMS.
- SIM state check and best-effort SIM-state-change receiver. Android/device behavior varies; this is not reliable SIM-swap detection.
- Local siren tone (limited to 5 seconds).
- Wrong PIN alert for this app's demo PIN dialog, with emergency-contact SMS attempt.
- Battery level check and best-effort system low-battery broadcast alert.
- Emergency contact setting for SMS alerts.

## Requirements
- Android Studio
- Android SDK Platform 35
- JDK 17 (usually bundled with Android Studio)
- A physical Android device is recommended for location/SMS/SIM testing.
- SMS needs a supported SIM, mobile service, and permission. Carrier charges may apply.

## Run
1. Extract the ZIP.
2. Open this folder in Android Studio.
3. Let Gradle sync and install any requested SDK components.
4. Connect your own Android phone with USB debugging enabled, or launch an emulator.
5. Press Run and grant permissions only for features you choose to test.
6. Save a trusted number, emergency number, and demo PIN before trying alerts.

## Important Android limitations
- The app only sends location when the user presses the location button. Continuous background tracking requires a carefully designed, visible foreground service and additional platform-specific handling; it is intentionally not implemented here.
- A normal third-party app cannot monitor attempts at the Android system lock screen. Wrong-PIN alerts cover only the app's own demo PIN screen.
- SIM state broadcasts and telephony permissions vary by Android version, manufacturer, carrier, and multi-SIM configuration. This receiver is best-effort and may not run in all cases.
- Low-battery broadcasts are system-controlled and may be delayed or restricted by modern Android background limits.
- SMS sending can fail due to permission, SIM, service, carrier, or device policy.
- Use only with devices and phone numbers you own or have permission to test. This app does not hide itself, bypass security controls, or remotely lock/wipe devices.

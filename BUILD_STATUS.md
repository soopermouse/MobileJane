# Build status

Source-level integration completed for Jane Mobile 1.6.0.

Validated in this package:

- authenticated Jane Agent client using bearer authorization
- production HTTPS enforcement and debug-emulator-only cleartext exception
- Android Keystore encryption for bearer token, Jane snapshot and experience journal
- encrypted continuous-voice recordings with legacy WAV migration and 7-day retention
- backup disabled
- explicit continuous-microphone consent and active indicators
- release R8/resource shrinking, non-debuggable release build and environment-driven signing configuration
- Jane Agent route/request compatibility retained
- voice-command precedence fixes and safer calendar-time extraction
- project mutation requests explicitly defer to Agent authorization/approval policy
- ZIP integrity

An APK was not produced in this packaging environment because it does not contain Android SDK 37 or a Gradle wrapper/cache. Open the project in Android Studio and run the `app` configuration for Android compiler, instrumentation and device tests.

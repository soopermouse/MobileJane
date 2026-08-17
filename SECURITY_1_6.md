# Jane Mobile 1.6 security hardening

Jane Mobile 1.6 closes the security gaps identified in the 1.5 static review.

- Jane Agent calls require a user bearer token and send `Authorization: Bearer <token>`.
- Production endpoints must be HTTPS. Main/release network policy denies cleartext traffic.
- Debug builds alone permit `http://10.0.2.2` for the Android emulator.
- The bearer token, Jane snapshot and experience journal are encrypted with an AES-GCM key stored in Android Keystore.
- Android application backup is disabled.
- Continuous voice capture requires explicit in-app consent; the service also refuses to start without that consent.
- Continuous voice recordings are encrypted after capture and expire after 7 days.
- The foreground notification explicitly states when the microphone is active.
- Release builds enable R8/resource shrinking, disable debugging, and use environment-driven signing credentials.
- Voice-command precedence is parenthesized and calendar parsing only treats explicit time expressions as times.
- Project mutation voice requests are handed to Jane Agent as requested actions subject to Agent-side authorization and approval policy, not unconditional execution instructions.

Release signing environment variables:
`JANE_KEYSTORE_PATH`, `JANE_KEYSTORE_PASSWORD`, `JANE_KEY_ALIAS`, `JANE_KEY_PASSWORD`.

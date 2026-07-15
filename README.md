# Jane Mobile for Android — v1 development package

Native Android GUI for the existing Jane Agent and JaneOS.

## Included GUI

- Jane conversation and synchronization
- Push-to-talk voice interaction and spoken replies
- Multi-page document scanner and OCR
- Translation/summary/deadline/action analysis contract
- PDF export
- Voice-activated local recorder
- Offline experience queue
- Jane bridge for the canonical J-machine runtime

## Build an Android installer

This package is complete source code, not a precompiled APK. Android SDK and Gradle dependencies are not installed in the generation environment, so the APK must be compiled once on a machine with Android Studio.

1. Install Android Studio.
2. Open this folder.
3. Allow Gradle Sync and SDK installation to complete.
4. Connect an Android phone with USB debugging enabled.
5. Run `BUILD_AND_INSTALL.cmd`.

Alternatively, select **Build → Build APK(s)** in Android Studio. The APK appears under:

`app/build/outputs/apk/debug/app-debug.apk`

## Connect to main Jane

Start the bridge on the J machine:

```powershell
python -m pip install -r .\bridge\requirements.txt
python .\bridge\jane_mobile_bridge.py
```

Set Jane Mobile's endpoint to the J machine LAN address, for example:

`http://192.168.1.50:8787`

## Current integration boundary

The Android GUI and API routes are implemented. Translation, deep document interpretation, image understanding, audio transcription and final conversational replies still need their marked bridge adapters connected to the canonical Jane Agent provider. They are not silently faked.

Copyright © 2026 Simona Diana Thrussell. All rights reserved. Proprietary software.

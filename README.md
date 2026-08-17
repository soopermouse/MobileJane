# Jane Mobile 1.3

Android companion and daily-life interface for **Jane Agent 4.2.0** and **JaneOS v4**.

Jane Mobile is Jane's portable interface and sensor layer. It does not duplicate the JaneOS cognitive runtime. Conversation, provider use, document analysis, vision, audio and shared projects pass through Jane Agent. Jane Agent remains the application layer over JaneOS.

```text
User / phone / camera / microphone
                |
          Jane Mobile
                |
     Jane Agent API 4.2.0
                |
             JaneOS v4
```

## What is implemented

- Today dashboard with live connection state
- Business-brain summary of projects, goals and investigations
- Quick idea, decision and observation capture
- Persistent mobile chat timeline
- Voice input and spoken Jane responses
- Shared project listing and creation through Jane Agent
- Camera and gallery document capture
- On-device OCR using ML Kit
- Document translation, summary, explanation, deadlines, amounts, risks and required actions
- Portable/offline chat capture when Jane Agent is unavailable
- Local goals and investigations
- Configurable Jane Agent endpoint
- Current Jane Agent 4.2.0 JSON request bodies

## Current API compatibility

Jane Mobile calls only these published Jane Agent endpoints:

```text
GET  /health
POST /chat
GET  /projects
POST /projects
POST /skills/documents
POST /skills/vision
POST /skills/audio
```

The request bodies match Jane Agent 4.2.0:

```json
{"message":"Hello Jane","importance":0.72}
```

```json
{"text":"Document text","translate_to":"English","media_type":null,"byte_size":null}
```

```json
{"name":"Jane Mobile","description":"Mobile companion","status":"active"}
```

Goals and investigations are intentionally stored on the device for now because Jane Agent 4.2.0 does not yet publish structured endpoints for them. The app labels this boundary rather than pretending those records are synchronized.

## Requirements

- Android Studio with Android SDK 37
- JDK 21
- Android 8.0 or newer (`minSdk 26`)
- Jane Agent 4.2.0 installed with API extras
- JaneOS v4, installed as part of the Jane Agent environment

## Start Jane Agent

From the fixed Jane Agent 4.2.0 repository:

```bash
pip install -e ".[api]"
python -m apps.jane.api
```

For a physical Android phone, Jane Agent must listen on the computer's LAN interface:

### Linux or macOS

```bash
JANE_HOST=0.0.0.0 JANE_PORT=8000 python -m apps.jane.api
```

### Windows PowerShell

```powershell
$env:JANE_HOST="0.0.0.0"
$env:JANE_PORT="8000"
python -m apps.jane.api
```

Only expose the development server on a trusted private network. Authentication and TLS are not implemented in Jane Agent 4.2.0.

## Build Jane Mobile

Open this directory in Android Studio and allow Gradle to synchronize. Select the `app` configuration and run it on an emulator or Android device.

With an Android SDK and Gradle installation available:

```bash
gradle assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Connect the app

Open **More → Connection**.

For the Android emulator, use:

```text
http://10.0.2.2:8000
```

For a physical phone, use the computer's private LAN address:

```text
http://192.168.x.x:8000
```

Press **Save and test**. The top bar changes to **Connected** after `GET /health` and `GET /projects` succeed.

## Main areas

### Today

Shows connection state, active projects, open goals, investigations, recent captures and quick actions.

### Jane

Text and voice conversation through `POST /chat`. When the server cannot be reached, the message remains in the local timeline and is recorded for later processing.

### Projects

Lists and creates Jane Agent projects through `GET /projects` and `POST /projects`.

### Documents

Captures one or more pages, performs OCR on the phone, then submits extracted text to `POST /skills/documents` for translation and analysis.

### More

Contains local goals, local investigations and Jane Agent connection settings.

## Deliberate boundaries

Jane Mobile does not embed JaneOS and does not create a second authoritative memory system. Its local snapshot is a device cache and portable capture journal.

The following features require new authenticated Jane Agent endpoints before they can be honestly synchronized:

- structured goals
- structured investigations
- JaneOS memory timeline
- daily briefing feed
- project-linked documents
- background synchronization
- wearable commands

## Next release

1. Add structured Agent endpoints for goals, investigations and briefing data.
2. Add an authenticated mobile session and encrypted transport.
3. Persist analyzed documents and attach them to projects.
4. Add scheduled morning brief notifications.
5. Add a GS-08 Bluetooth compatibility probe before implementing device control.

## Copyright and intellectual property

Copyright © 2026 Simona Diana Thrussell. All rights reserved.

JaneOS, Jane Agent, Jane CLI and Jane Mobile form the Jane Cognitive Platform. The source code, architecture, documentation, terminology, product design and original cognitive-system concepts are protected under applicable international copyright law, including the Berne Convention, and applicable Dutch and Romanian law.

No permission is granted to copy, redistribute, commercialize, translate, adapt, reverse engineer, reimplement or create derivative products from this work except through an explicit written licence from the copyright holder.

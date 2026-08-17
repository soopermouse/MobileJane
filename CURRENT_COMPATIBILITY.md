# Current compatibility

Jane Mobile 1.4 targets:

- Jane Agent 4.2.0 fixed
- JaneOS v4 verified
- Android API 26–37

## Supported server routes

| Method | Route | Mobile feature |
|---|---|---|
| GET | `/health` | Connection test |
| POST | `/chat` | Text and voice conversation |
| GET | `/projects` | Shared project list and mobile dashboard |
| GET | `/alerts` | Jane Alert feed (optional capability; graceful fallback on older Agent builds) |
| POST | `/projects` | Shared project creation/update |
| POST | `/skills/documents` | OCR text analysis and translation |
| POST | `/skills/vision` | Visual-text interpretation |
| POST | `/skills/audio` | Transcript interpretation |

## Not yet exposed by Jane Agent 4.2.0

- goals
- investigations
- memories/timeline
- briefing
- reminders
- authenticated sync

Jane Mobile keeps goals and investigations locally and clearly identifies them as local records until public Agent endpoints exist.


## Jane Mobile 1.4 dashboard

The Today screen is the mobile project-control dashboard. It summarizes all projects, priority metadata, next actions, blockers, recent activity and Jane Alert items. Project metadata keys recognized by the dashboard are `priority`, `next_action`, `blocker`, and `last_activity`.

Jane Alert integration is capability-aware: if `/alerts` is unavailable, the rest of Jane Mobile continues to synchronize normally.

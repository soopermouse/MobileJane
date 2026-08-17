# Jane Mobile 1.4 — Project Dashboard + Jane Alert

Jane Mobile's former Today screen is now the mobile project-control dashboard.

## Dashboard

- all Jane Agent projects in one mobile view
- total, active, blocked, and alert counts
- project priority
- project status
- blocker
- next action
- last activity
- per-project Jane Alert count
- quick access to Jane, project management, document scan, and mobile capture

The dashboard recognizes these optional project metadata keys:

- `priority`
- `next_action`
- `blocker`
- `last_activity`

Projects without those keys remain fully usable.

## Jane Alert

Jane Mobile now attempts `GET /alerts` during refresh. The expected response is a JSON list with these fields:

- `id`
- `project_id` (optional)
- `title`
- `message`
- `severity`
- `status`
- `created_at`

The route is capability-aware. If an older Jane Agent does not expose `/alerts`, health and project synchronization continue normally and the last local alert snapshot is retained.

## Compatibility

- Android API 26–37
- existing Jane Agent project, chat, document, vision, and audio APIs remain unchanged
- Jane Alert is optional until the Agent exposes the public route

# Jane Mobile 1.5 — Secretary in your pocket

Jane Mobile now routes spoken language through a local command layer before falling back to Jane Agent.

Implemented command families:
- project and Jane Alert navigation
- project/attention questions routed to Jane with explicit project context intent
- document scan/translate requests routed to the Document Workbench
- optional target-language extraction for document translation
- "add/save/put this in Project X" requests routed to Jane Agent for project association
- calendar meeting commands open Android's native event editor with extracted date/time/title for user review
- compound scan + translate + project requests are preserved as an Agent instruction while the document workbench opens

The router is deliberately small and deterministic. Open-ended requests still go to Jane Agent. External writes use an explicit review surface where available; calendar commands therefore prefill Android Calendar rather than silently creating an event.

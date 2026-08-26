# J Mobile Android 1.7 — J Agent seam integration

- Uses J Agent v4.2.3 one-time pairing exchange; users no longer type bearer tokens.
- Handles HTTP 401 as an explicit reconnect/pair state and clears the invalid local credential.
- Removes the nonexistent J Agent `/alerts` call; J Alert remains a separate product.
- Drains the encrypted offline ExperienceJournal through `/intake`, committing sync progress per accepted record.
- Document, vision and audio imports use the single typed `/intake` boundary.
- `media_uri` carries the local media reference while OCR/extraction remains on-device.
- Successful online chat journal records are marked synchronized; failed chat is not double-journaled.
- AndroidKeystore key object is cached per JaneCrypto instance rather than reopening the KeyStore on every operation.
- Existing 1.6 transport, backup/export and microphone-consent hardening remains in place.

README intentionally unchanged.

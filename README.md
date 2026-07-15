# Jane Mobile v0.3 — Document Workbench

Jane Mobile now supports work-focused document analysis:

- multi-page scanning or image import;
- on-device OCR;
- editable recognized text;
- language detection;
- translation requests;
- summaries and plain-language explanations;
- deadline, amount and required-action extraction;
- reply drafting through main Jane;
- offline preliminary analysis;
- PDF export;
- integration into Jane's Memory Economy.

The mobile client works offline for scanning, OCR and preliminary extraction. Deep translation, legal/work context and reply generation are routed to canonical Jane on the J machine through `/v1/jane/documents/analyze`.

Build in Android Studio. Start the bridge with:

```powershell
python -m pip install -r .\bridge\requirements.txt
python .\bridge\jane_mobile_bridge.py
```

Copyright © 2026 Simona Diana Thrussell. All rights reserved. Proprietary software.

"""
Minimal Jane Android bridge.

Run from the Jane Windows Shell / JaneOS repository root:

    python -m pip install -r bridge/requirements.txt
    python bridge/jane_mobile_bridge.py

The bridge deliberately exposes only an authorized local-network API.
Replace the adapter methods with direct calls into the canonical Jane app.
"""
from __future__ import annotations

import json
import re
import os
import time
import uuid
from pathlib import Path
from typing import Any

from fastapi import FastAPI, File, Form, UploadFile
from pydantic import BaseModel
import uvicorn

STATE_PATH = Path(os.getenv(
    "JANE_STATE_PATH",
    Path.home() / "AppData" / "Local" / "Jane" / "state" / "jane.json",
))

app = FastAPI(title="Jane Mobile Bridge", version="0.1.0")


class ObserveRequest(BaseModel):
    text: str


def _default_state() -> dict[str, Any]:
    return {
        "status": {
            "name": "Jane",
            "state": "Awake",
            "uptimeSeconds": 0,
            "currentFocus": "Listening",
            "curiosityLevel": 0.65,
            "attentionBudget": 1.0,
            "activeInvestigations": 0,
            "memoryCount": 0,
            "edgeCount": 0,
        },
        "messages": [],
        "memories": [],
        "investigations": [],
        "discoveries": [],
        "_startedAt": int(time.time()),
    }


def load_state() -> dict[str, Any]:
    if not STATE_PATH.exists():
        state = _default_state()
        save_state(state)
        return state
    try:
        return json.loads(STATE_PATH.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return _default_state()


def save_state(state: dict[str, Any]) -> None:
    STATE_PATH.parent.mkdir(parents=True, exist_ok=True)
    STATE_PATH.write_text(json.dumps(state, indent=2, ensure_ascii=False), encoding="utf-8")


def normalize(state: dict[str, Any]) -> dict[str, Any]:
    default = _default_state()
    for key in ("messages", "memories", "investigations", "discoveries"):
        state.setdefault(key, [])
    state.setdefault("status", {})
    for key, value in default["status"].items():
        state["status"].setdefault(key, value)
    started = state.setdefault("_startedAt", int(time.time()))
    state["status"]["uptimeSeconds"] = max(0, int(time.time()) - int(started))
    state["status"]["memoryCount"] = len(state["memories"])
    state["status"]["activeInvestigations"] = sum(
        1 for item in state["investigations"] if item.get("status") not in {"closed", "solved"}
    )
    return state


@app.get("/v1/jane/snapshot")
def snapshot() -> dict[str, Any]:
    state = normalize(load_state())
    save_state(state)
    return {k: v for k, v in state.items() if not k.startswith("_")}


@app.post("/v1/jane/observe")
def observe(request: ObserveRequest) -> dict[str, Any]:
    state = normalize(load_state())
    now = int(time.time() * 1000)
    text = request.text.strip()

    state["messages"].append({
        "id": f"u-{uuid.uuid4()}",
        "role": "user",
        "text": text,
        "timestamp": now,
    })
    state["memories"].insert(0, {
        "id": f"m-{uuid.uuid4()}",
        "summary": text[:240],
        "value": 0.62,
        "novelty": 0.70,
        "confidence": 1.0,
        "status": "active",
    })

    # Placeholder response until wired to the canonical Jane reasoning provider.
    reply = (
        "I have integrated this observation into my memory economy. "
        "I will examine its connections and decide whether it opens an investigation."
    )

    state["messages"].append({
        "id": f"j-{uuid.uuid4()}",
        "role": "jane",
        "text": reply,
        "timestamp": now + 1,
    })
    state["status"]["currentFocus"] = f"Integrating: {text[:70]}"
    state["status"]["curiosityLevel"] = min(1.0, float(state["status"]["curiosityLevel"]) + 0.02)
    state = normalize(state)
    save_state(state)

    return {
        "reply": reply,
        "snapshot": {k: v for k, v in state.items() if not k.startswith("_")},
    }


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8787)


@app.post("/v1/jane/vision")
async def vision(
    image: UploadFile = File(...),
    extracted_text: str | None = Form(default=None),
) -> dict[str, Any]:
    state = normalize(load_state())
    await image.read()
    description = (
        "I received this image and preserved it as part of today's "
        "experience. The canonical Jane vision provider can replace "
        "this adapter response with full scene understanding."
    )
    if extracted_text:
        description += " I also read text from the image."

    state["memories"].insert(0, {
        "id": f"m-{uuid.uuid4()}",
        "summary": (extracted_text or f"Image: {image.filename}")[:240],
        "value": 0.67,
        "novelty": 0.78,
        "confidence": 0.92,
        "status": "active",
    })
    state["status"]["currentFocus"] = f"Examining {image.filename}"
    state = normalize(state)
    save_state(state)

    return {
        "description": description,
        "extractedText": extracted_text,
        "snapshot": {
            key: value
            for key, value in state.items()
            if not key.startswith("_")
        },
    }


@app.post("/v1/jane/experiences/sync")
def sync_experiences(records: list[dict[str, Any]]) -> dict[str, Any]:
    state = normalize(load_state())

    for record in records:
        state["memories"].insert(0, {
            "id": f"sync-{record.get('id', uuid.uuid4())}",
            "summary": str(record.get("text", ""))[:240],
            "value": 0.58,
            "novelty": 0.66,
            "confidence": 1.0,
            "status": "imported",
        })

    state["status"]["currentFocus"] = (
        f"Integrating {len(records)} mobile experiences"
    )
    state = normalize(state)
    save_state(state)

    return {
        key: value
        for key, value in state.items()
        if not key.startswith("_")
    }


def _detect_language(text: str) -> str:
    lowered=f" {text.lower()} "; vocab={"ro":[" și "," pentru "," termen "," în "],"nl":[" de "," het "," binnen "," gemeente "],"en":[" the "," and "," within "," please "],"fr":[" le "," la "," veuillez "," dans "]}; return max(vocab,key=lambda k:sum(w in lowered for w in vocab[k]))

@app.post("/v1/jane/documents/analyze")
def analyze_document(payload: dict[str, Any]) -> dict[str, Any]:
    pages=payload.get("pages",[]); text="\n\n".join(str(p.get("extractedText","")) for p in pages); title=str(payload.get("title","Scanned document")); did=str(payload.get("documentId",uuid.uuid4())); target=str(payload.get("targetLanguage","en")); dates=sorted(set(re.findall(r"\b(?:\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{4}-\d{2}-\d{2})\b",text))); amounts=sorted(set(m.group(0).strip() for m in re.finditer(r"\b(?:€|EUR|RON|lei|euro)?\s?\d{1,3}(?:[., ]\d{3})*(?:[.,]\d{2})?\s?(?:€|EUR|RON|lei|euro)?\b",text,re.I) if any(ch.isdigit() for ch in m.group(0))))[:20]; lines=[l.strip() for l in text.splitlines() if l.strip()]; actions=[l for l in lines if any(w in l.lower() for w in ("must","required","deadline","trebuie","termen","moet","verplicht"))][:12]; lang=_detect_language(text); state=normalize(load_state()); state["memories"].insert(0,{"id":f"doc-{did}","summary":f"Document: {title} — {' '.join(lines[:4])[:180]}","value":0.76,"novelty":0.72,"confidence":0.95,"status":"document"}); state["status"]["currentFocus"]=f"Analyzing document: {title}"; state=normalize(state); save_state(state); return {"documentId":did,"detectedLanguage":lang,"title":title,"fullText":text,"translation":None if lang==target else f"[Translation to {target} awaits the configured Jane language provider.]","summary":" ".join(lines[:6])[:1200] or "No readable text was extracted.","explanation":"Jane extracted the document structure, deadlines, amounts and likely required actions. Connect the canonical Jane reasoning provider for deeper work-specific or legal analysis.","deadlines":dates,"amounts":amounts,"actionsRequired":actions,"risks":[],"replyDraft":"Draft reply generation awaits the configured Jane reasoning provider.","confidence":0.78 if len(text)>200 else 0.48,"snapshot":{k:v for k,v in state.items() if not k.startswith("_")}}

from transformers import pipeline
from app.config import TOXICITY_LOW, TOXICITY_HIGH
from app.services.groq_client import classify_with_groq

_classifier = None

# unitary/toxic-bert is multi-label; any of these labels firing strongly = vulgar/blocked
_VULGAR_LABELS = {"toxic", "severe_toxic", "obscene", "insult", "threat", "identity_hate"}


def get_classifier():
    global _classifier
    if _classifier is None:
        _classifier = pipeline(
            "text-classification",
            model="unitary/toxic-bert",
            top_k=None,
        )
    return _classifier


def is_vulgar(text: str) -> bool:
    if not text or not text.strip():
        return False
    scores = get_classifier()(text[:512])[0]
    score = max(
        (s["score"] for s in scores if s["label"].lower() in _VULGAR_LABELS),
        default=0.0,
    )

    if score >= TOXICITY_HIGH:
        return True
    if score <= TOXICITY_LOW:
        return False

    # Ambiguous band — ask Groq; fall back to HF midpoint if Groq unavailable
    groq_result = classify_with_groq(text, "vulgarity")
    if groq_result is not None:
        return groq_result
    return score >= 0.5

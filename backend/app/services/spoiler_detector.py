import re
from transformers import pipeline
from app.config import SPOILER_LOW, SPOILER_HIGH
from app.services.groq_client import classify_with_groq

_classifier = None
_SPOILER_LABEL = "LABEL_1"

_PLOT_POINT_PATTERNS = [
    r"\b(dies|dead|death|killed|killer|murderer)\b",
    r"\b(ending|finale|final scene|at the end|in the end)\b",
    r"\b(reveal|reveals|revealed|turns out)\b",
    r"\b(was dead the whole time|is the killer|was the killer)\b",
]

def _has_plot_point_signal(text: str) -> bool:
    lowered = text.lower()
    return any(re.search(pattern, lowered) for pattern in _PLOT_POINT_PATTERNS)


def get_classifier():
    global _classifier
    if _classifier is None:
        _classifier = pipeline(
            "text-classification",
            model="bhavyagiri/roberta-base-finetuned-imdb-spoilers",
            top_k=None,
        )
    return _classifier


def predict_spoiler(text: str) -> bool:
    if not text or not text.strip():
        return False

    normalized_text = text.strip()
    scores = get_classifier()(normalized_text[:512])[0]
    score = next((s["score"] for s in scores if s["label"] == _SPOILER_LABEL), 0.0)

    has_plot_signal = _has_plot_point_signal(normalized_text)

    if score >= SPOILER_HIGH:
        return True

    if has_plot_signal:
        groq_result = classify_with_groq(normalized_text, "spoiler")
        if groq_result is not None:
            return groq_result
        return score >= SPOILER_LOW

    if score <= SPOILER_LOW:
        return False

    groq_result = classify_with_groq(normalized_text, "spoiler")
    if groq_result is not None:
        return groq_result

    return score >= 0.5

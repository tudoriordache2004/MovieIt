import json
import requests
from typing import Literal, Optional
from app.config import GROQ_API_KEY, GROQ_MODEL

_GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"

_SYSTEM_PROMPTS = {
    "spoiler": (
        "You are a spoiler detection system for movie reviews. "
        "Determine whether the text reveals specific plot points, character fates, twists, or endings. "
        "Respond ONLY with valid JSON in the form {\"label\": \"yes\"} or {\"label\": \"no\"}. "
        "Answer \"yes\" only if the text clearly spoils the story; answer \"no\" for vague impressions or opinions."
    ),
    "vulgarity": (
        "You are a content moderation system. "
        "Determine whether the text contains profanity, explicit sexual language, hate speech, severe insults, or obscene content. "
        "Respond ONLY with valid JSON in the form {\"label\": \"yes\"} or {\"label\": \"no\"}. "
        "Mild criticism or negative opinions are NOT vulgarity."
    ),
}


def classify_with_groq(
    text: str, task: Literal["spoiler", "vulgarity"]
) -> Optional[bool]:
    """
    Calls the Groq LLM to classify text. Returns True/False, or None on any failure
    so the caller can fall back to the HuggingFace score.
    """
    if not GROQ_API_KEY:
        return None
    try:
        payload = {
            "model": GROQ_MODEL,
            "temperature": 0,
            "max_tokens": 10,
            "response_format": {"type": "json_object"},
            "messages": [
                {"role": "system", "content": _SYSTEM_PROMPTS[task]},
                {"role": "user", "content": text[:1024]},
            ],
        }
        resp = requests.post(
            _GROQ_URL,
            headers={"Authorization": f"Bearer {GROQ_API_KEY}", "Content-Type": "application/json"},
            json=payload,
            timeout=5,
        )
        resp.raise_for_status()
        content = resp.json()["choices"][0]["message"]["content"]
        label = json.loads(content).get("label", "").lower()
        if label in ("yes", "no"):
            return label == "yes"
    except Exception:
        pass
    return None

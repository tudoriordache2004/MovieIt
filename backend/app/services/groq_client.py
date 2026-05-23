import json
import requests
from typing import Literal, Optional
from app.config import GROQ_API_KEY, GROQ_MODEL

_GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"

_SYSTEM_PROMPTS = {
    "spoiler": (
        "You are a spoiler detection system for movie reviews. "
        "Determine whether the text reveals specific plot points, character fates, twists, endings, "
        "relationship outcomes, identity reveals, or final resolutions for the movie being reviewed. "
        "Respond ONLY with valid JSON in the form {\"label\": \"yes\"} or {\"label\": \"no\"}. "
        "Answer \"yes\" if the text states or strongly implies what happens in the story. "
        "Answer \"no\" for vague impressions, hopes, predictions, or opinions that do not reveal an actual outcome."
    ),
    "vulgarity": (
        "You are a content moderation system. "
        "Determine whether the text contains profanity, explicit sexual language, hate speech, severe insults, or obscene content. "
        "Respond ONLY with valid JSON in the form {\"label\": \"yes\"} or {\"label\": \"no\"}. "
        "Mild criticism or negative opinions are NOT vulgarity."
    ),
}


def classify_with_groq(
    text: str, task: Literal["spoiler", "vulgarity"], context: Optional[str] = None
) -> Optional[bool]:
    """
    Calls the Groq LLM to classify text. Returns True/False, or None on any failure
    so the caller can fall back to the HuggingFace score.
    """
    if not GROQ_API_KEY:
        return None
    try:
        user_content = text[:1024]
        if task == "spoiler" and context:
            user_content = f"Movie context: {context[:200]}\nReview text: {text[:1024]}"

        payload = {
            "model": GROQ_MODEL,
            "temperature": 0,
            "max_tokens": 10,
            "response_format": {"type": "json_object"},
            "messages": [
                {"role": "system", "content": _SYSTEM_PROMPTS[task]},
                {"role": "user", "content": user_content},
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

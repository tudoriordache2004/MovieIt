"""
Run from the backend directory with venv active:
    python test_spoiler_detection.py
"""
from app.services.spoiler_detector import get_classifier, predict_spoiler, _SPOILER_LABEL
from app.config import SPOILER_LOW, SPOILER_HIGH

TEST_CASES = [
    # (text, expected_is_spoiler)
    ("This movie was absolutely amazing, great cinematography and acting!", False),
    ("One of the best films I've seen this year. The pacing is perfect.", False),
    ("The villain's backstory was poorly developed and felt rushed.", False),
    ("The twist at the end completely blew my mind — did not see it coming.", True),
    ("Bruce Willis was dead the whole time. The ending ruined it for me.", True),
    ("Marty supreme wins the match in the end", True),
    ("They finally reveal the killer is the detective who was investigating the case.", True),
    ("Great performances all around, especially from the supporting cast.", False),
    ("Marty supreme dies in the end", True),
    ("The twist at the end completely blew my mind", False),
    # extra borderline cases
    ("The protagonist sacrifices himself to save the city in the final act.", True),
    ("Visually stunning but the plot drags in the middle.", False),
    ("I cried when the dog died at the end.", True),
    ("Soundtrack is incredible. Definitely worth a rewatch.", False),
    ("The ending is a bit predictable but still satisfying.", False),
]

def inspect_labels():
    """Print the raw model output for the first two cases so we can see the actual label names."""
    clf = get_classifier()
    print("=== RAW MODEL OUTPUT (first 2 samples) ===")
    for text, _ in TEST_CASES[:2]:
        raw = clf(text[:512])
        print(f"  {text[:60]!r}")
        print(f"  -> {raw}\n")
    print("===========================================\n")

def run() -> float:
    print(f"{'TEXT':<65} {'EXPECTED':<10} {'PREDICTED':<10} {'SCORE':<8} {'MATCH'}")
    print("-" * 110)

    correct = 0
    clf = get_classifier()
    for text, expected in TEST_CASES:
        scores = clf(text[:512])[0]
        spoiler_score = next((s["score"] for s in scores if s["label"] == _SPOILER_LABEL), 0.0)
        predicted = predict_spoiler(text)
        match = predicted == expected
        correct += match
        label_exp = "SPOILER" if expected else "CLEAN"
        label_pred = "SPOILER" if predicted else "CLEAN"
        status = "✓" if match else "✗"
        print(f"{text[:64]:<65} {label_exp:<10} {label_pred:<10} p={spoiler_score:.3f}  {status}")

    accuracy = correct / len(TEST_CASES)
    print("-" * 110)
    print(f"Thresholds: low={SPOILER_LOW} high={SPOILER_HIGH} | Accuracy: {correct}/{len(TEST_CASES)} ({100 * accuracy:.1f}%)")
    return accuracy

if __name__ == "__main__":
    print("Loading model...\n")
    inspect_labels()
    run()

"""
Run from the backend directory with venv active:
    python test_vulgarity_detection.py
"""
from app.services.vulgarity_filter import get_classifier, is_vulgar, _VULGAR_LABELS
from app.config import TOXICITY_LOW, TOXICITY_HIGH

TEST_CASES = [
    # (text, expected_is_vulgar)
    ("This movie was absolutely amazing, great cinematography and acting!", False),
    ("I really did not enjoy this film, it felt slow and boring.", False),
    ("The plot was terrible and the dialogue was lazy.", False),
    ("This is a fucking awful piece of shit movie.", True),
    ("The director is a moron and everyone in this is a clown.", True),
    ("What a stupid movie, complete garbage.", False),
    ("I hate the protagonist so much, he is the worst character ever written.", False),
    ("Go kill yourself if you liked this trash.", True),
    ("Damn, that ending hit hard.", False),
    ("Asshole behaviour from the lead, but well acted.", True),
]


def run() -> float:
    print(f"{'TEXT':<65} {'EXPECTED':<10} {'PREDICTED':<10} {'SCORE':<8} {'MATCH'}")
    print("-" * 110)

    correct = 0
    clf = get_classifier()
    for text, expected in TEST_CASES:
        scores = clf(text[:512])[0]
        score = max(
            (s["score"] for s in scores if s["label"].lower() in _VULGAR_LABELS),
            default=0.0,
        )
        predicted = is_vulgar(text)
        match = predicted == expected
        correct += match
        label_exp = "VULGAR" if expected else "CLEAN"
        label_pred = "VULGAR" if predicted else "CLEAN"
        ok = "✓" if match else "✗"
        print(f"{text[:64]:<65} {label_exp:<10} {label_pred:<10} p={score:.3f}  {ok}")

    accuracy = correct / len(TEST_CASES)
    print("-" * 110)
    print(f"Thresholds: low={TOXICITY_LOW} high={TOXICITY_HIGH} | Accuracy: {correct}/{len(TEST_CASES)} ({100 * accuracy:.1f}%)")
    return accuracy


if __name__ == "__main__":
    print("Loading model...\n")
    run()

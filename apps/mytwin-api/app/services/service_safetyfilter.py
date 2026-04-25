def apply_safety_filter(text: str) -> str:
    safe_text = text

    safe_text = safe_text.replace("will", "may")
    safe_text = safe_text.replace("guarantee", "may help")

    safe_text += "\n\n⚠️ This is general wellness information, not medical advice."

    return safe_text
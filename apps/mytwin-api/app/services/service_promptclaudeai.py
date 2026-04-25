def build_prompt(message, health_data, insights):
    system_prompt = """
You are a health assistant.

Rules:
- Do NOT diagnose or prescribe
- Use cautious, evidence-based language
- Speak in probabilities, not certainties
- Provide general wellness advice only
"""

    user_context = f"""
User data:
- Avg sleep: {health_data['avg_sleep']}h
- Sleep goal: {health_data['sleep_goal']}h
- Steps: {health_data['avg_steps']}
- Resting HR: {health_data['resting_hr']}
- Sleep consistency: {health_data['sleep_consistency']}

Derived:
- Sleep deficit: {insights['sleep_deficit']}
- Activity level: {insights['activity_level']}

Question:
{message}
"""

    return {
        "system": system_prompt,
        "user": user_context
    }
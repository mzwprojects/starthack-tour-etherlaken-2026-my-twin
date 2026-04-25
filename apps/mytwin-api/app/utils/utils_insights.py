def derive_insights(data):
    sleep_deficit = data["sleep_goal"] - data["avg_sleep"]

    if data["avg_steps"] > 8000:
        activity = "high"
    elif data["avg_steps"] > 5000:
        activity = "moderate"
    else:
        activity = "low"

    return {
        "sleep_deficit": round(sleep_deficit, 2),
        "activity_level": activity,
        "risk_flag": sleep_deficit > 2
    }
from fastapi import APIRouter
from pydantic import BaseModel

from ..services.service_samsunghealth import get_user_health_data
from ..services.service_promptclaudeai import build_prompt
from ..services.service_apiclientclaudeai import call_claude
from ..services.service_safetyfilter import apply_safety_filter
from ..utils.utils_insights import derive_insights

router_claudeai = APIRouter()

@router_claudeai.get("/claudeai")
async def chat(req: ChatRequest):
    health_data = await get_user_health_data(req.user_id)
    insights = derive_insights(health_data)
    prompt = build_prompt(req.message, health_data, insights)
    raw_response = await call_claude(prompt)
    safe_response = apply_safety_filter(raw_response)

    return {
        "message": safe_response,
        "meta": {
            "insights": insights
        }
    }

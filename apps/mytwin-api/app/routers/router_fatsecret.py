from fastapi import APIRouter

router_fatsecret = APIRouter()

@router_fatsecret.get("/fatsecret")
def root():
    return {"you are connected to fatsecret"}


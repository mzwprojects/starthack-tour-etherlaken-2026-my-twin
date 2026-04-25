from fastapi import FastAPI
from .routers.router_fatsecret import router_fatsecret
from .routers.router_claudeai import router_claudeai
app = FastAPI()

app.include_router(router_fatsecret)
app.include_router(router_claudeai)

@app.get("/")
def root():
    return {"status": "running"}


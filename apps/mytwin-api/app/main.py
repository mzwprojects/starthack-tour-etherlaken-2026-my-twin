from fastapi import FastAPI
from .routers.router_fatsecret import router_fatsecret
app = FastAPI()

app.include_router(router_fatsecret)

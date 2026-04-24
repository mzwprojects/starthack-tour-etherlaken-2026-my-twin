import httpx
from requests_oauthlib import OAuth1
from core.config import settings

BASE_URL = "https://platform.fatsecret.com/rest/server.api"

auth = OAuth1(
    settings.fatsecret_key,
    settings.fatsecret_secret
)
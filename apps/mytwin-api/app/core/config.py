from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    fatsecret_key: str
    fatsecret_secret: str

    class Config:
        env_file = ".env"

settings = Settings()
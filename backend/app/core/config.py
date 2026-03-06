from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # App
    APP_ENV: str = "development"
    DEBUG: bool = True
    SECRET_KEY: str = "change-me"

    # Database
    DATABASE_URL: str = "postgresql+asyncpg://postgres:password@localhost:5432/ai_obd"
    DATABASE_POOL_SIZE: int = 10

    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"

    # Firebase
    FIREBASE_CREDENTIALS_PATH: str = "./serviceAccountKey.json"
    FIREBASE_CREDENTIALS_JSON: str = ""

    # AI
    ANTHROPIC_API_KEY: str = ""
    OPENAI_API_KEY: str = ""
    LOCAL_LLM_URL: str = "http://localhost:11434"

    # Stripe
    STRIPE_SECRET_KEY: str = ""
    STRIPE_WEBHOOK_SECRET: str = ""
    STRIPE_PRICE_B2C_PREMIUM: str = ""
    STRIPE_PRICE_B2B_BASIC: str = ""
    STRIPE_PRICE_B2B_PRO: str = ""

    # CORS
    ALLOWED_ORIGINS: list[str] = ["http://localhost:5173"]

    # App URLs
    B2C_SUCCESS_URL: str = "https://drive.avyrox.io/payment/success"
    B2C_CANCEL_URL: str = "https://drive.avyrox.io/payment/cancel"
    B2B_SUCCESS_URL: str = "https://cloud.avyrox.io/billing/success"
    B2B_CANCEL_URL: str = "https://cloud.avyrox.io/billing/cancel"


settings = Settings()

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "OmniAgent Studio AI Service"
    internal_token: str = "devmind-internal-token"
    default_model: str = "deepseek-v4-flash"
    deepseek_api_key: str | None = None
    deepseek_base_url: str = "https://api.deepseek.com"
    qwen_api_key: str | None = None
    qwen_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    openai_api_key: str | None = None
    openai_base_url: str = "https://api.openai.com/v1"
    tavily_api_key: str | None = None
    deepseek_timeout_seconds: float = 60.0
    langfuse_enabled: bool = False
    langfuse_public_key: str | None = None
    langfuse_secret_key: str | None = None
    langfuse_host: str = "https://cloud.langfuse.com"

    class Config:
        env_file = ".env"


settings = Settings()

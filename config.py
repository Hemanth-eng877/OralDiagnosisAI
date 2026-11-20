"""OralDiagnosisAI Central Configuration Module

Provides environment-based configuration for Development, Testing, and Production.
"""

import os

BASE_DIR = os.path.abspath(os.path.dirname(__file__))


class Config:
    """Base application configuration."""
    BASE_DIR = BASE_DIR
    SECRET_KEY = os.environ.get("SECRET_KEY", "change-this-secret-key-in-production")
    UPLOAD_FOLDER = os.path.join(BASE_DIR, "uploads")
    MAX_CONTENT_LENGTH = int(os.environ.get("MAX_CONTENT_LENGTH", 8 * 1024 * 1024))
    MODEL_PATH = os.path.join(BASE_DIR, "oral_model.tflite")
    ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg", "webp"}
    CLASS_LABELS = {0: "Normal", 1: "OSCC"}
    
    # Cookie security
    SESSION_COOKIE_HTTPONLY = True
    SESSION_COOKIE_SAMESITE = "Lax"
    SESSION_COOKIE_SECURE = os.environ.get("SESSION_COOKIE_SECURE", "false").lower() == "true"
    PERMANENT_SESSION_LIFETIME = int(os.environ.get("SESSION_LIFETIME_SECONDS", 86400))
    
    # Firebase settings
    FIREBASE_KEY_PATH = os.environ.get("FIREBASE_KEY_PATH", os.path.join(BASE_DIR, "firebase-key.json"))
    FIREBASE_API_KEY = os.environ.get("FIREBASE_API_KEY", "AIzaSyCqGkY4Vv-q9eBRLgMdwKYlIVfQYk4DPnE")
    USE_FALLBACK_DB = os.environ.get("USE_FALLBACK_DB", "0") == "1"


class DevelopmentConfig(Config):
    """Development configuration with debugging enabled."""
    DEBUG = True
    TESTING = False


class TestingConfig(Config):
    """Testing configuration with mock database and fast timeouts."""
    DEBUG = False
    TESTING = True
    WTF_CSRF_ENABLED = False
    USE_FALLBACK_DB = True
    SERVER_NAME = "localhost"


class ProductionConfig(Config):
    """Production configuration with strict security headers."""
    DEBUG = False
    TESTING = False
    SESSION_COOKIE_SECURE = True


CONFIG_MAP = {
    "development": DevelopmentConfig,
    "testing": TestingConfig,
    "production": ProductionConfig,
    "default": DevelopmentConfig,
}


def get_config(env_name=None):
    """Retrieve configuration class by environment name."""
    if env_name is None:
        env_name = os.environ.get("FLASK_ENV", "development").lower()
    return CONFIG_MAP.get(env_name, DevelopmentConfig)

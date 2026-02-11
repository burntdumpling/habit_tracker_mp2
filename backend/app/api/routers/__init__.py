from app.api.routers.auth import router as auth_router
from app.api.routers.habits import router as habits_router

__all__ = ["auth_router", "habits_router"]

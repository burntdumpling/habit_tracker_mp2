from fastapi import Depends, FastAPI
from fastapi.responses import JSONResponse
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.routers import auth_router, habits_router
from app.db.session import get_db

app = FastAPI(title="Habit Tracker API")
app.include_router(auth_router)
app.include_router(habits_router)


@app.get("/")
def read_root() -> dict[str, str]:
    return {"message": "Habit Tracker API"}


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/health/db", response_model=None)
async def health_db(db: AsyncSession = Depends(get_db)) -> dict[str, object]:
    try:
        await db.execute(text("SELECT 1"))
        return {"status": "ok", "db": True}
    except Exception:
        return JSONResponse(
            status_code=503,
            content={"status": "error", "db": False, "detail": "Database unavailable"},
        )

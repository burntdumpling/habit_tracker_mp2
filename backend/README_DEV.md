# FastAPI Local Dev (Step 4)

## 1) Enter backend folder
```bash
cd backend
```

## 2) Create virtual environment
```bash
python -m venv .venv
```

## 3) Activate virtual environment (macOS zsh)
```bash
source .venv/bin/activate
```

## 4) Install requirements
```bash
pip install -r requirements.txt
```

## 5) Create local env file and set secret
```bash
cp .env.example .env
```
Set `SECRET_KEY` in `.env` to a random string.

## 6) Start PostgreSQL in Docker
```bash
docker compose up -d
```

## 7) Run database migrations
```bash
alembic upgrade head
```

## 8) Run API on host machine
```bash
uvicorn app.main:app --reload --port 8000
```

## 9) Test auth flow in Swagger
1. Open `http://localhost:8000/docs`
2. `POST /auth/register` with JSON body:
```json
{
  "email": "user@example.com",
  "password": "strongpassword123"
}
```
3. `POST /auth/token` using `application/x-www-form-urlencoded` with fields:
   - `username` = your email
   - `password` = your password
4. Copy `access_token` from the response.
5. Click **Authorize** in Swagger and paste the token.
6. Call `GET /auth/me`.

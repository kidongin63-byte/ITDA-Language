FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# 환경변수 기본값 (SQLite, Redis 없이 동작)
ENV DEBUG=true
ENV DATABASE_URL=sqlite+aiosqlite:///./itda_dev.db
ENV JWT_SECRET_KEY=production-secret-change-me
ENV TTS_ENGINE=edge
ENV REDIS_URL=

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

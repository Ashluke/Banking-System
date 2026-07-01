from fastapi import FastAPI
from routers import (
    transaction_router,
    fraud_router,
    portfolio_router,
    prediction_router,
    credit_score_router
)

app = FastAPI(
    title="Banking Analytics Service",
    description="Python microservice for banking analytics — transaction insights, credit scoring, fraud detection, portfolio analysis, and savings predictions.",
    version="1.0.0"
)

app.include_router(
    transaction_router.router,
    prefix="/analytics/transactions",
    tags=["Transaction Analytics"]
)

app.include_router(
    fraud_router.router,
    prefix="/analytics/fraud",
    tags=["Fraud Detection"]
)

app.include_router(
    portfolio_router.router,
    prefix="/analytics/portfolio",
    tags=["Portfolio Analytics"]
)

app.include_router(
    prediction_router.router,
    prefix="/analytics/predictions",
    tags=["Predictions"]
)

app.include_router(
    credit_score_router.router,
    prefix="/analytics",
    tags=["Credit Score"]
)


@app.get("/health")
def health_check():
    return {"status": "ok", "service": "banking-analytics"}
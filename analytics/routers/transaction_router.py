from fastapi import APIRouter
from models.transaction import TransactionListRequest
from services.analytics_service import get_transaction_insights, get_spending_trends

router = APIRouter()


@router.post("/insights")
def transaction_insights(request: TransactionListRequest):
    return get_transaction_insights(request)


@router.post("/trends")
def spending_trends(request: TransactionListRequest):
    return get_spending_trends(request)
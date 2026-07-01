from fastapi import APIRouter
from models.transaction import TransactionListRequest
from services.fraud_service import detect_fraud

router = APIRouter()


@router.post("/detect")
def fraud_detection(request: TransactionListRequest):
    return detect_fraud(request)
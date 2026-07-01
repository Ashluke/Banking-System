from fastapi import APIRouter
from models.transaction import TransactionListRequest
from services.prediction_service import predict_savings

router = APIRouter()


@router.post("/savings")
def savings_prediction(request: TransactionListRequest):
    return predict_savings(request)
from fastapi import APIRouter
from pydantic import BaseModel
from models.transaction import TransactionListRequest
from models.account import AccountData
from services.credit_score_service import calculate_credit_score

router = APIRouter()


class CreditScoreRequest(BaseModel):
    transactionData: TransactionListRequest
    accountData: AccountData


@router.post("/credit-score")
def get_credit_score(request: CreditScoreRequest):
    return calculate_credit_score(request.transactionData, request.accountData)
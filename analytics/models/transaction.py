from pydantic import BaseModel
from datetime import datetime
from enum import Enum
from typing import Optional


class TransactionType(str, Enum):
    DEPOSIT = "DEPOSIT"
    WITHDRAW = "WITHDRAW"
    TRANSFER_IN = "TRANSFER_IN"
    TRANSFER_OUT = "TRANSFER_OUT"


class TransactionData(BaseModel):
    id: int
    bankAccountId: int
    relatedTransactionId: Optional[int] = None
    amount: float
    type: TransactionType
    timestamp: datetime


class TransactionListRequest(BaseModel):
    userId: int
    currentBalance: float
    transactions: list[TransactionData]
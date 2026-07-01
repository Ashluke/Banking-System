from pydantic import BaseModel
from datetime import datetime
from enum import Enum


class AccountStatus(str, Enum):
    ACTIVE = "ACTIVE"
    FROZEN = "FROZEN"
    CLOSED = "CLOSED"


class AccountData(BaseModel):
    id: int
    userId: int
    balance: float
    status: AccountStatus
    createdAt: datetime


class AccountListRequest(BaseModel):
    userId: int
    accounts: list[AccountData]
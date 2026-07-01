from pydantic import BaseModel
from datetime import datetime


class StockHolding(BaseModel):
    id: int
    userId: int
    symbol: str
    quantity: float
    purchasePrice: float
    currentPrice: float
    purchasedAt: datetime


class PortfolioRequest(BaseModel):
    userId: int
    holdings: list[StockHolding]
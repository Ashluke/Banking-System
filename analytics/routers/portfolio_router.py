from fastapi import APIRouter
from models.stock import PortfolioRequest
from services.portfolio_service import analyze_portfolio

router = APIRouter()


@router.post("/performance")
def portfolio_performance(request: PortfolioRequest):
    return analyze_portfolio(request)
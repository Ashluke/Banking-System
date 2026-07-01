import pytest
from datetime import datetime, timezone
from models.stock import PortfolioRequest, StockHolding
from services.portfolio_service import analyze_portfolio


def make_holding(id, symbol, quantity, purchase_price, current_price=None):
    return StockHolding(
        id=id,
        userId=1,
        symbol=symbol,
        quantity=quantity,
        purchasePrice=purchase_price,
        currentPrice=current_price if current_price is not None else purchase_price,
        purchasedAt=datetime(2026, 1, 1, tzinfo=timezone.utc)
    )


def make_request(holdings):
    return PortfolioRequest(userId=1, holdings=holdings)


# ===================== ANALYZE PORTFOLIO =====================

def test_empty_holdings_returns_empty_portfolio():
    request = make_request([])
    result = analyze_portfolio(request)

    assert result["summary"]["numberOfHoldings"] == 0
    assert result["summary"]["totalCost"] == 0
    assert result["summary"]["totalCurrentValue"] == 0
    assert result["holdings"] == []


def test_single_holding_no_price_change():
    holdings = [make_holding(1, "AAPL", 10.0, 150.0)]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    assert result["summary"]["numberOfHoldings"] == 1
    assert result["summary"]["totalCost"] == 1500.0
    assert result["summary"]["totalCurrentValue"] == 1500.0
    assert result["summary"]["totalGainLoss"] == 0.0


def test_gain_when_current_price_higher_than_purchase():
    holdings = [make_holding(1, "AAPL", 10.0, 100.0, current_price=150.0)]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    assert result["summary"]["totalGainLoss"] == 500.0
    assert result["summary"]["totalGainLossPercent"] == 50.0


def test_loss_when_current_price_lower_than_purchase():
    holdings = [make_holding(1, "AAPL", 10.0, 150.0, current_price=100.0)]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    assert result["summary"]["totalGainLoss"] == -500.0
    assert result["summary"]["totalGainLossPercent"] < 0


def test_portfolio_weights_sum_to_100():
    holdings = [
        make_holding(1, "AAPL", 10.0, 100.0),
        make_holding(2, "GOOGL", 5.0, 200.0),
    ]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    total_weight = sum(h["portfolioWeight"] for h in result["holdings"])
    assert abs(total_weight - 100.0) < 0.1


def test_single_holding_diversification_score_is_low():
    holdings = [make_holding(1, "AAPL", 10.0, 150.0)]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    assert result["diversificationScore"] <= 20


def test_equal_holdings_diversification_score_is_high():
    holdings = [
        make_holding(1, "AAPL", 10.0, 100.0),
        make_holding(2, "GOOGL", 10.0, 100.0),
        make_holding(3, "MSFT", 10.0, 100.0),
        make_holding(4, "AMZN", 10.0, 100.0),
    ]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    assert result["diversificationScore"] >= 90


def test_best_and_worst_performer_identified():
    holdings = [
        make_holding(1, "AAPL", 10.0, 100.0, current_price=150.0),
        make_holding(2, "GOOGL", 10.0, 100.0, current_price=80.0),
    ]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    assert result["bestPerformer"]["symbol"] == "AAPL"
    assert result["worstPerformer"]["symbol"] == "GOOGL"


def test_risk_level_is_valid_value():
    holdings = [make_holding(1, "AAPL", 10.0, 150.0)]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    assert result["riskLevel"] in ["LOW", "MEDIUM", "HIGH"]


def test_holding_gain_loss_calculated_correctly():
    holdings = [make_holding(1, "AAPL", 5.0, 200.0, current_price=250.0)]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    holding = result["holdings"][0]
    assert holding["costBasis"] == 1000.0
    assert holding["currentValue"] == 1250.0
    assert holding["gainLoss"] == 250.0
    assert holding["gainLossPercent"] == 25.0


def test_multiple_holdings_total_values_are_correct():
    holdings = [
        make_holding(1, "AAPL", 10.0, 100.0, current_price=110.0),
        make_holding(2, "GOOGL", 5.0, 200.0, current_price=180.0),
    ]
    request = make_request(holdings)
    result = analyze_portfolio(request)

    assert result["summary"]["totalCost"] == 2000.0
    assert result["summary"]["totalCurrentValue"] == 2000.0
    assert result["summary"]["totalGainLoss"] == 0.0


def test_result_has_required_fields():
    request = make_request([])
    result = analyze_portfolio(request)

    assert "summary" in result
    assert "holdings" in result
    assert "diversificationScore" in result
    assert "averageDailyVolatility" in result
    assert "riskLevel" in result
    assert "bestPerformer" in result
    assert "worstPerformer" in result
    assert "insight" in result
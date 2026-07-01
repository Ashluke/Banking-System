import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def make_holding(id, symbol, quantity, purchase_price, current_price=None):
    return {
        "id": id,
        "userId": 1,
        "symbol": symbol,
        "quantity": quantity,
        "purchasePrice": purchase_price,
        "currentPrice": current_price if current_price is not None else purchase_price,
        "purchasedAt": "2026-01-01T00:00:00Z"
    }


def make_payload(holdings=None):
    return {
        "userId": 1,
        "holdings": holdings or []
    }


# ===================== POST /analytics/portfolio/performance =====================

def test_portfolio_returns_200_with_empty_holdings():
    response = client.post("/analytics/portfolio/performance", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert data["summary"]["numberOfHoldings"] == 0


def test_portfolio_returns_200_with_holdings():
    holdings = [
        make_holding(1, "AAPL", 10.0, 100.0, 150.0),
        make_holding(2, "GOOGL", 5.0, 200.0, 180.0),
    ]
    response = client.post("/analytics/portfolio/performance", json=make_payload(holdings))
    assert response.status_code == 200
    data = response.json()
    assert data["summary"]["numberOfHoldings"] == 2


def test_portfolio_calculates_gain_correctly():
    holdings = [make_holding(1, "AAPL", 10.0, 100.0, 150.0)]
    response = client.post("/analytics/portfolio/performance", json=make_payload(holdings))
    assert response.status_code == 200
    data = response.json()
    assert data["summary"]["totalGainLoss"] == 500.0
    assert data["summary"]["totalGainLossPercent"] == 50.0


def test_portfolio_response_has_required_fields():
    response = client.post("/analytics/portfolio/performance", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert "summary" in data
    assert "holdings" in data
    assert "diversificationScore" in data
    assert "averageDailyVolatility" in data
    assert "riskLevel" in data
    assert "bestPerformer" in data
    assert "worstPerformer" in data
    assert "insight" in data


def test_portfolio_risk_level_is_valid():
    holdings = [make_holding(1, "AAPL", 10.0, 150.0)]
    response = client.post("/analytics/portfolio/performance", json=make_payload(holdings))
    assert response.status_code == 200
    data = response.json()
    assert data["riskLevel"] in ["LOW", "MEDIUM", "HIGH"]


def test_portfolio_returns_422_when_body_missing():
    response = client.post("/analytics/portfolio/performance", json={})
    assert response.status_code == 422


def test_portfolio_returns_422_when_invalid_holding():
    holdings = [{"id": 1, "symbol": "AAPL"}]  # missing required fields
    response = client.post("/analytics/portfolio/performance", json=make_payload(holdings))
    assert response.status_code == 422
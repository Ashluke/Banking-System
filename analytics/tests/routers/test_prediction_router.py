import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def make_payload(transactions=None, balance=1000.0):
    return {
        "userId": 1,
        "currentBalance": balance,
        "transactions": transactions or []
    }


def make_transaction(id, type, amount, timestamp):
    return {
        "id": id,
        "bankAccountId": 1,
        "relatedTransactionId": None,
        "amount": amount,
        "type": type,
        "timestamp": timestamp
    }


# ===================== POST /analytics/predictions/savings =====================

def test_predictions_returns_200_with_empty_transactions():
    response = client.post("/analytics/predictions/savings", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert data["savingsTrend"] == "INSUFFICIENT_DATA"
    assert data["projections"] == []


def test_predictions_returns_200_with_sufficient_transactions():
    transactions = [
        make_transaction(1, "DEPOSIT", 1000.0, "2026-01-05T10:00:00Z"),
        make_transaction(2, "WITHDRAW", 200.0, "2026-01-15T10:00:00Z"),
        make_transaction(3, "DEPOSIT", 1000.0, "2026-02-05T10:00:00Z"),
        make_transaction(4, "WITHDRAW", 200.0, "2026-02-15T10:00:00Z"),
        make_transaction(5, "DEPOSIT", 1000.0, "2026-03-05T10:00:00Z"),
        make_transaction(6, "WITHDRAW", 200.0, "2026-03-15T10:00:00Z"),
    ]
    response = client.post("/analytics/predictions/savings", json=make_payload(transactions, 2400.0))
    assert response.status_code == 200
    data = response.json()
    assert data["savingsTrend"] != "INSUFFICIENT_DATA"
    assert len(data["projections"]) == 3


def test_predictions_response_has_required_fields():
    response = client.post("/analytics/predictions/savings", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert "currentBalance" in data
    assert "averageMonthlyNetFlow" in data
    assert "savingsTrend" in data
    assert "projections" in data
    assert "monthlyForecast" in data
    assert "insight" in data


def test_predictions_projections_have_required_fields():
    transactions = [
        make_transaction(i, "DEPOSIT", 1000.0, f"2026-0{i}-05T10:00:00Z")
        for i in range(1, 4)
    ]
    response = client.post("/analytics/predictions/savings", json=make_payload(transactions))
    assert response.status_code == 200
    data = response.json()

    for projection in data["projections"]:
        assert "monthsAhead" in projection
        assert "projectedDate" in projection
        assert "projectedBalance" in projection
        assert "estimatedSavings" in projection
        assert "confidencePercent" in projection


def test_predictions_returns_422_when_body_missing():
    response = client.post("/analytics/predictions/savings", json={})
    assert response.status_code == 422
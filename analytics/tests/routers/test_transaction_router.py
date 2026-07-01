import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch
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


# ===================== POST /analytics/transactions/insights =====================

def test_insights_returns_200_with_empty_transactions():
    response = client.post("/analytics/transactions/insights", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert data["summary"]["totalTransactions"] == 0


def test_insights_returns_200_with_transactions():
    transactions = [
        make_transaction(1, "DEPOSIT", 1000.0, "2026-01-05T10:00:00Z"),
        make_transaction(2, "WITHDRAW", 200.0, "2026-01-10T10:00:00Z"),
    ]
    response = client.post("/analytics/transactions/insights", json=make_payload(transactions, 800.0))
    assert response.status_code == 200
    data = response.json()
    assert data["summary"]["totalTransactions"] == 2
    assert data["summary"]["totalDeposited"] == 1000.0
    assert data["summary"]["totalWithdrawn"] == 200.0


def test_insights_response_has_required_fields():
    response = client.post("/analytics/transactions/insights", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert "summary" in data
    assert "monthlyCashFlow" in data
    assert "typeBreakdown" in data
    assert "topDeposits" in data
    assert "topWithdrawals" in data


def test_insights_returns_422_when_body_missing():
    response = client.post("/analytics/transactions/insights", json={})
    assert response.status_code == 422


def test_insights_returns_422_when_invalid_transaction_type():
    transactions = [
        make_transaction(1, "INVALID_TYPE", 100.0, "2026-01-05T10:00:00Z")
    ]
    response = client.post("/analytics/transactions/insights", json=make_payload(transactions))
    assert response.status_code == 422


# ===================== POST /analytics/transactions/trends =====================

def test_trends_returns_200_with_empty_transactions():
    response = client.post("/analytics/transactions/trends", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert data["spendingTrend"] == "INSUFFICIENT_DATA"


def test_trends_returns_200_with_transactions():
    transactions = [
        make_transaction(1, "DEPOSIT", 1000.0, "2026-01-05T10:00:00Z"),
        make_transaction(2, "WITHDRAW", 200.0, "2026-01-15T10:00:00Z"),
    ]
    response = client.post("/analytics/transactions/trends", json=make_payload(transactions))
    assert response.status_code == 200
    data = response.json()
    assert "overallSavingsRate" in data
    assert "spendingTrend" in data
    assert "monthlySavings" in data


def test_trends_returns_422_when_body_missing():
    response = client.post("/analytics/transactions/trends", json={})
    assert response.status_code == 422
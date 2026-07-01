import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def make_payload(transactions=None, balance=5000.0):
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


# ===================== POST /analytics/fraud/detect =====================

def test_fraud_detect_returns_200_with_empty_transactions():
    response = client.post("/analytics/fraud/detect", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert data["riskLevel"] == "LOW"
    assert data["totalFlagsDetected"] == 0


def test_fraud_detect_returns_200_with_normal_transactions():
    transactions = [
        make_transaction(1, "DEPOSIT", 500.0, "2026-01-05T10:00:00Z"),
        make_transaction(2, "WITHDRAW", 100.0, "2026-01-10T14:00:00Z"),
    ]
    response = client.post("/analytics/fraud/detect", json=make_payload(transactions))
    assert response.status_code == 200
    data = response.json()
    assert data["riskLevel"] == "LOW"


def test_fraud_detect_flags_unusual_hours():
    transactions = [
        make_transaction(1, "WITHDRAW", 200.0, "2026-01-05T03:00:00Z"),
    ]
    response = client.post("/analytics/fraud/detect", json=make_payload(transactions))
    assert response.status_code == 200
    data = response.json()
    flag_rules = [f["rule"] for flag in data["flags"] for f in flag["flags"]]
    assert "UNUSUAL_HOURS" in flag_rules


def test_fraud_detect_response_has_required_fields():
    response = client.post("/analytics/fraud/detect", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert "riskLevel" in data
    assert "totalFlagsDetected" in data
    assert "suspiciousTransactionCount" in data
    assert "flags" in data
    assert "summary" in data


def test_fraud_detect_returns_422_when_body_missing():
    response = client.post("/analytics/fraud/detect", json={})
    assert response.status_code == 422


def test_fraud_detect_returns_422_when_invalid_type():
    transactions = [
        make_transaction(1, "INVALID_TYPE", 100.0, "2026-01-05T10:00:00Z")
    ]
    response = client.post("/analytics/fraud/detect", json=make_payload(transactions))
    assert response.status_code == 422
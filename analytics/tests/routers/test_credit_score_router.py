import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def make_transaction(id, type, amount, timestamp):
    return {
        "id": id,
        "bankAccountId": 1,
        "relatedTransactionId": None,
        "amount": amount,
        "type": type,
        "timestamp": timestamp
    }


def make_payload(transactions=None, balance=1000.0, created_at="2024-01-01T00:00:00Z"):
    return {
        "transactionData": {
            "userId": 1,
            "currentBalance": balance,
            "transactions": transactions or []
        },
        "accountData": {
            "id": 1,
            "userId": 1,
            "balance": balance,
            "status": "ACTIVE",
            "createdAt": created_at
        }
    }


# ===================== POST /analytics/credit-score =====================

def test_credit_score_returns_200_with_empty_transactions():
    response = client.post("/analytics/credit-score", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert data["creditScore"] == 400
    assert data["rating"] == "POOR"


def test_credit_score_returns_200_with_transactions():
    transactions = [
        make_transaction(1, "DEPOSIT", 1000.0, "2026-01-05T10:00:00Z"),
        make_transaction(2, "WITHDRAW", 200.0, "2026-01-15T10:00:00Z"),
        make_transaction(3, "DEPOSIT", 1000.0, "2026-02-05T10:00:00Z"),
    ]
    response = client.post("/analytics/credit-score", json=make_payload(transactions, 1800.0))
    assert response.status_code == 200
    data = response.json()
    assert 400 <= data["creditScore"] <= 850


def test_credit_score_response_has_required_fields():
    response = client.post("/analytics/credit-score", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert "creditScore" in data
    assert "rating" in data
    assert "loanEligibility" in data
    assert "breakdown" in data
    assert "insights" in data


def test_credit_score_loan_eligibility_has_required_fields():
    response = client.post("/analytics/credit-score", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    eligibility = data["loanEligibility"]
    assert "eligible" in eligibility
    assert "recommendation" in eligibility
    assert "suggestedInterestRate" in eligibility
    assert "notes" in eligibility


def test_credit_score_breakdown_has_all_factors():
    response = client.post("/analytics/credit-score", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    breakdown = data["breakdown"]
    assert "paymentConsistency" in breakdown
    assert "balanceHistory" in breakdown
    assert "transactionFrequency" in breakdown
    assert "debtToIncomeRatio" in breakdown
    assert "accountAge" in breakdown


def test_credit_score_rating_is_valid_value():
    response = client.post("/analytics/credit-score", json=make_payload())
    assert response.status_code == 200
    data = response.json()
    assert data["rating"] in ["EXCELLENT", "GOOD", "FAIR", "POOR", "VERY_POOR"]


def test_credit_score_returns_422_when_body_missing():
    response = client.post("/analytics/credit-score", json={})
    assert response.status_code == 422


def test_credit_score_returns_422_when_invalid_account_status():
    payload = make_payload()
    payload["accountData"]["status"] = "INVALID_STATUS"
    response = client.post("/analytics/credit-score", json=payload)
    assert response.status_code == 422


def test_credit_score_older_account_scores_higher():
    transactions = [
        make_transaction(1, "DEPOSIT", 1000.0, "2026-01-05T10:00:00Z"),
        make_transaction(2, "WITHDRAW", 200.0, "2026-01-15T10:00:00Z"),
        make_transaction(3, "DEPOSIT", 1000.0, "2026-02-05T10:00:00Z"),
    ]

    old_payload = make_payload(transactions, 1800.0, created_at="2020-01-01T00:00:00Z")
    new_payload = make_payload(transactions, 1800.0, created_at="2026-01-01T00:00:00Z")

    old_response = client.post("/analytics/credit-score", json=old_payload)
    new_response = client.post("/analytics/credit-score", json=new_payload)

    assert old_response.status_code == 200
    assert new_response.status_code == 200
    assert old_response.json()["creditScore"] >= new_response.json()["creditScore"]
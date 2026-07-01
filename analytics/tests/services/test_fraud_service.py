import pytest
from datetime import datetime, timezone
from models.transaction import TransactionListRequest, TransactionData, TransactionType
from services.fraud_service import detect_fraud


def make_transaction(id, type, amount, timestamp):
    return TransactionData(
        id=id,
        bankAccountId=1,
        relatedTransactionId=None,
        amount=amount,
        type=type,
        timestamp=timestamp
    )


def make_request(transactions, balance=5000.0):
    return TransactionListRequest(
        userId=1,
        currentBalance=balance,
        transactions=transactions
    )


# ===================== DETECT FRAUD =====================

def test_empty_transactions_returns_low_risk():
    request = make_request([])
    result = detect_fraud(request)

    assert result["riskLevel"] == "LOW"
    assert result["totalFlagsDetected"] == 0
    assert result["suspiciousTransactionCount"] == 0
    assert result["flags"] == []


def test_normal_transactions_returns_low_risk():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 500.0, datetime(2026, 1, 5, 10, 0, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 100.0, datetime(2026, 1, 10, 14, 0, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 500.0, datetime(2026, 2, 5, 9, 0, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = detect_fraud(request)

    assert result["riskLevel"] == "LOW"
    assert result["totalFlagsDetected"] == 0


def test_large_amount_flag_detected():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 100.0, datetime(2026, 1, 5, 10, 0, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.DEPOSIT, 100.0, datetime(2026, 1, 10, 10, 0, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 100.0, datetime(2026, 1, 15, 10, 0, tzinfo=timezone.utc)),
        make_transaction(4, TransactionType.DEPOSIT, 5000.0, datetime(2026, 1, 20, 10, 0, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = detect_fraud(request)

    flag_rules = [f["rule"] for flag in result["flags"] for f in flag["flags"]]
    assert "LARGE_AMOUNT" in flag_rules


def test_unusual_hours_flag_detected():
    transactions = [
        make_transaction(1, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 5, 2, 30, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = detect_fraud(request)

    flag_rules = [f["rule"] for flag in result["flags"] for f in flag["flags"]]
    assert "UNUSUAL_HOURS" in flag_rules


def test_rapid_transactions_flag_detected():
    base = datetime(2026, 1, 5, 10, 0, tzinfo=timezone.utc)
    from datetime import timedelta
    transactions = [
        make_transaction(i, TransactionType.WITHDRAW, 50.0, base + timedelta(minutes=i))
        for i in range(7)
    ]
    request = make_request(transactions)
    result = detect_fraud(request)

    flag_rules = [f["rule"] for flag in result["flags"] for f in flag["flags"]]
    assert "RAPID_TRANSACTIONS" in flag_rules


def test_rapid_account_drain_flag_detected():
    transactions = [
        make_transaction(1, TransactionType.WITHDRAW, 4500.0, datetime(2026, 1, 5, 10, 0, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions, balance=500.0)
    result = detect_fraud(request)

    all_rules = [f["rule"] for flag in result["flags"] for f in flag["flags"]]
    assert "RAPID_ACCOUNT_DRAIN" in all_rules


def test_risk_level_medium_with_few_flags():
    transactions = [
        make_transaction(1, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 5, 3, 0, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 6, 4, 0, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = detect_fraud(request)

    assert result["riskLevel"] in ["LOW", "MEDIUM"]


def test_result_has_required_fields():
    request = make_request([])
    result = detect_fraud(request)

    assert "riskLevel" in result
    assert "totalFlagsDetected" in result
    assert "suspiciousTransactionCount" in result
    assert "flags" in result
    assert "summary" in result
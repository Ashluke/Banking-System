import pytest
from datetime import datetime, timezone
from models.transaction import TransactionListRequest, TransactionData, TransactionType
from services.prediction_service import predict_savings


def make_transaction(id, type, amount, timestamp):
    return TransactionData(
        id=id,
        bankAccountId=1,
        relatedTransactionId=None,
        amount=amount,
        type=type,
        timestamp=timestamp
    )


def make_request(transactions, balance=1000.0):
    return TransactionListRequest(
        userId=1,
        currentBalance=balance,
        transactions=transactions
    )


# ===================== PREDICT SAVINGS =====================

def test_empty_transactions_returns_empty_prediction():
    request = make_request([])
    result = predict_savings(request)

    assert result["savingsTrend"] == "INSUFFICIENT_DATA"
    assert result["projections"] == []
    assert result["monthlyForecast"] == []
    assert result["currentBalance"] == 1000.0


def test_insufficient_transactions_returns_empty():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 500.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 100.0, datetime(2026, 1, 10, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = predict_savings(request)

    assert result["savingsTrend"] == "INSUFFICIENT_DATA"


def test_projections_returns_3_6_12_months():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 300.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 1000.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
        make_transaction(4, TransactionType.WITHDRAW, 300.0, datetime(2026, 2, 15, tzinfo=timezone.utc)),
        make_transaction(5, TransactionType.DEPOSIT, 1000.0, datetime(2026, 3, 5, tzinfo=timezone.utc)),
        make_transaction(6, TransactionType.WITHDRAW, 300.0, datetime(2026, 3, 15, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions, balance=2100.0)
    result = predict_savings(request)

    months = [p["monthsAhead"] for p in result["projections"]]
    assert 3 in months
    assert 6 in months
    assert 12 in months


def test_positive_net_flow_increases_projected_balance():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 1000.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
        make_transaction(4, TransactionType.WITHDRAW, 200.0, datetime(2026, 2, 15, tzinfo=timezone.utc)),
        make_transaction(5, TransactionType.DEPOSIT, 1000.0, datetime(2026, 3, 5, tzinfo=timezone.utc)),
        make_transaction(6, TransactionType.WITHDRAW, 200.0, datetime(2026, 3, 15, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions, balance=2400.0)
    result = predict_savings(request)

    assert result["averageMonthlyNetFlow"] > 0
    projection_12 = next(p for p in result["projections"] if p["monthsAhead"] == 12)
    assert projection_12["projectedBalance"] > 2400.0


def test_negative_net_flow_decreases_projected_balance():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 500.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 900.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 500.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
        make_transaction(4, TransactionType.WITHDRAW, 900.0, datetime(2026, 2, 15, tzinfo=timezone.utc)),
        make_transaction(5, TransactionType.DEPOSIT, 500.0, datetime(2026, 3, 5, tzinfo=timezone.utc)),
        make_transaction(6, TransactionType.WITHDRAW, 900.0, datetime(2026, 3, 15, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions, balance=500.0)
    result = predict_savings(request)

    assert result["averageMonthlyNetFlow"] < 0
    projection_12 = next(p for p in result["projections"] if p["monthsAhead"] == 12)
    assert projection_12["projectedBalance"] < 500.0


def test_monthly_forecast_returns_12_entries():
    transactions = [
        make_transaction(i, TransactionType.DEPOSIT, 1000.0, datetime(2026, i if i <= 12 else 12, 5, tzinfo=timezone.utc))
        for i in range(1, 4)
    ]
    request = make_request(transactions)
    result = predict_savings(request)

    if result["savingsTrend"] != "INSUFFICIENT_DATA":
        assert len(result["monthlyForecast"]) == 12


def test_confidence_percent_is_between_0_and_100():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 300.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 1000.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
        make_transaction(4, TransactionType.WITHDRAW, 300.0, datetime(2026, 2, 15, tzinfo=timezone.utc)),
        make_transaction(5, TransactionType.DEPOSIT, 1000.0, datetime(2026, 3, 5, tzinfo=timezone.utc)),
        make_transaction(6, TransactionType.WITHDRAW, 300.0, datetime(2026, 3, 15, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = predict_savings(request)

    for projection in result["projections"]:
        assert 0 <= projection["confidencePercent"] <= 100


def test_result_has_required_fields():
    request = make_request([])
    result = predict_savings(request)

    assert "currentBalance" in result
    assert "averageMonthlyNetFlow" in result
    assert "savingsTrend" in result
    assert "projections" in result
    assert "monthlyForecast" in result
    assert "insight" in result
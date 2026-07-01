import pytest
from datetime import datetime, timezone
from models.transaction import TransactionListRequest, TransactionData, TransactionType
from services.analytics_service import get_transaction_insights, get_spending_trends


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


# ===================== GET TRANSACTION INSIGHTS =====================

def test_insights_empty_transactions_returns_empty():
    request = make_request([])
    result = get_transaction_insights(request)

    assert result["summary"]["totalTransactions"] == 0
    assert result["summary"]["totalDeposited"] == 0
    assert result["monthlyCashFlow"] == []
    assert result["typeBreakdown"] == []


def test_insights_returns_correct_totals():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 10, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 500.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions, balance=1300.0)
    result = get_transaction_insights(request)

    assert result["summary"]["totalTransactions"] == 3
    assert result["summary"]["totalDeposited"] == 1500.0
    assert result["summary"]["totalWithdrawn"] == 200.0
    assert result["summary"]["netFlow"] == 1300.0


def test_insights_returns_monthly_cash_flow():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.DEPOSIT, 800.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.WITHDRAW, 300.0, datetime(2026, 2, 10, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = get_transaction_insights(request)

    assert len(result["monthlyCashFlow"]) == 2


def test_insights_average_transaction_amount():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 100.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.DEPOSIT, 300.0, datetime(2026, 1, 10, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = get_transaction_insights(request)

    assert result["summary"]["averageTransactionAmount"] == 200.0


def test_insights_transfer_in_and_out_totals():
    transactions = [
        make_transaction(1, TransactionType.TRANSFER_IN, 500.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.TRANSFER_OUT, 200.0, datetime(2026, 1, 6, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = get_transaction_insights(request)

    assert result["summary"]["totalTransferredIn"] == 500.0
    assert result["summary"]["totalTransferredOut"] == 200.0


def test_insights_top_deposits_returns_max_3():
    transactions = [
        make_transaction(i, TransactionType.DEPOSIT, float(i * 100), datetime(2026, 1, i, tzinfo=timezone.utc))
        for i in range(1, 6)
    ]
    request = make_request(transactions)
    result = get_transaction_insights(request)

    assert len(result["topDeposits"]) <= 3


# ===================== GET SPENDING TRENDS =====================

def test_trends_empty_transactions_returns_empty():
    request = make_request([])
    result = get_spending_trends(request)

    assert result["overallSavingsRate"] == 0
    assert result["spendingTrend"] == "INSUFFICIENT_DATA"
    assert result["monthlySavings"] == []


def test_trends_positive_savings_rate():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 10, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = get_spending_trends(request)

    assert result["overallSavingsRate"] == 80.0


def test_trends_zero_savings_rate_when_all_withdrawn():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 1000.0, datetime(2026, 1, 10, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions, balance=0.0)
    result = get_spending_trends(request)

    assert result["overallSavingsRate"] == 0.0


def test_trends_detects_increasing_spending():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 100.0, datetime(2026, 1, 10, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 1000.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
        make_transaction(4, TransactionType.WITHDRAW, 100.0, datetime(2026, 2, 10, tzinfo=timezone.utc)),
        make_transaction(5, TransactionType.DEPOSIT, 1000.0, datetime(2026, 3, 5, tzinfo=timezone.utc)),
        make_transaction(6, TransactionType.WITHDRAW, 900.0, datetime(2026, 3, 10, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = get_spending_trends(request)

    assert result["spendingTrend"] == "INCREASING"


def test_trends_monthly_savings_structure():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 300.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    result = get_spending_trends(request)

    assert len(result["monthlySavings"]) == 1
    month = result["monthlySavings"][0]
    assert "month" in month
    assert "income" in month
    assert "expenses" in month
    assert "savings" in month
    assert "savingsRate" in month
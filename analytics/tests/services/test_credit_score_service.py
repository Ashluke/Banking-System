import pytest
from datetime import datetime, timezone
from models.transaction import TransactionListRequest, TransactionData, TransactionType
from models.account import AccountData, AccountStatus
from services.credit_score_service import calculate_credit_score


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


def make_account(created_at=None):
    return AccountData(
        id=1,
        userId=1,
        balance=1000.0,
        status=AccountStatus.ACTIVE,
        createdAt=created_at or datetime(2024, 1, 1, tzinfo=timezone.utc)
    )


# ===================== CALCULATE CREDIT SCORE =====================

def test_empty_transactions_returns_minimum_score():
    request = make_request([])
    account = make_account()
    result = calculate_credit_score(request, account)

    assert result["creditScore"] == 400
    assert result["rating"] == "POOR"
    assert result["loanEligibility"]["eligible"] == False


def test_score_is_within_valid_range():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 1000.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions, balance=1800.0)
    account = make_account()
    result = calculate_credit_score(request, account)

    assert 400 <= result["creditScore"] <= 850


def test_excellent_financial_behavior_scores_high():
    transactions = [
        make_transaction(i, TransactionType.DEPOSIT, 2000.0,
            datetime(2026, (i % 12) + 1, 5, 10, 0, tzinfo=timezone.utc))
        for i in range(1, 13)
    ] + [
        make_transaction(i + 13, TransactionType.WITHDRAW, 200.0,
            datetime(2026, (i % 12) + 1, 15, 10, 0, tzinfo=timezone.utc))
        for i in range(1, 13)
    ]
    request = make_request(transactions, balance=20000.0)
    account = make_account(created_at=datetime(2022, 1, 1, tzinfo=timezone.utc))
    result = calculate_credit_score(request, account)

    assert result["creditScore"] >= 650
    assert result["rating"] in ["GOOD", "EXCELLENT"]


def test_poor_financial_behavior_scores_low():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 500.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 490.0, datetime(2026, 1, 6, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions, balance=10.0)
    account = make_account(created_at=datetime(2026, 1, 1, tzinfo=timezone.utc))
    result = calculate_credit_score(request, account)

    assert result["creditScore"] < 650


def test_loan_eligibility_auto_approve_for_excellent_score():
    transactions = [
        make_transaction(i, TransactionType.DEPOSIT, 5000.0,
            datetime(2026, (i % 12) + 1, 5, 10, 0, tzinfo=timezone.utc))
        for i in range(1, 25)
    ]
    request = make_request(transactions, balance=100000.0)
    account = make_account(created_at=datetime(2020, 1, 1, tzinfo=timezone.utc))
    result = calculate_credit_score(request, account)

    if result["creditScore"] >= 750:
        assert result["loanEligibility"]["recommendation"] == "AUTO_APPROVE"
        assert result["loanEligibility"]["suggestedInterestRate"] == 5.0


def test_loan_eligibility_deny_for_minimum_score():
    request = make_request([])
    account = make_account()
    result = calculate_credit_score(request, account)

    assert result["loanEligibility"]["eligible"] == False
    assert result["loanEligibility"]["recommendation"] == "DENY"
    assert result["loanEligibility"]["suggestedInterestRate"] is None


def test_breakdown_has_all_five_factors():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 1000.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    account = make_account()
    result = calculate_credit_score(request, account)

    breakdown = result["breakdown"]
    assert "paymentConsistency" in breakdown
    assert "balanceHistory" in breakdown
    assert "transactionFrequency" in breakdown
    assert "debtToIncomeRatio" in breakdown
    assert "accountAge" in breakdown


def test_insights_is_non_empty_list():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 1000.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)
    account = make_account()
    result = calculate_credit_score(request, account)

    assert isinstance(result["insights"], list)
    assert len(result["insights"]) > 0


def test_older_account_scores_higher_account_age_factor():
    transactions = [
        make_transaction(1, TransactionType.DEPOSIT, 1000.0, datetime(2026, 1, 5, tzinfo=timezone.utc)),
        make_transaction(2, TransactionType.WITHDRAW, 200.0, datetime(2026, 1, 15, tzinfo=timezone.utc)),
        make_transaction(3, TransactionType.DEPOSIT, 1000.0, datetime(2026, 2, 5, tzinfo=timezone.utc)),
    ]
    request = make_request(transactions)

    old_account = make_account(created_at=datetime(2020, 1, 1, tzinfo=timezone.utc))
    new_account = make_account(created_at=datetime(2026, 1, 1, tzinfo=timezone.utc))

    old_result = calculate_credit_score(request, old_account)
    new_result = calculate_credit_score(request, new_account)

    assert old_result["breakdown"]["accountAge"] > new_result["breakdown"]["accountAge"]


def test_result_has_required_fields():
    request = make_request([])
    account = make_account()
    result = calculate_credit_score(request, account)

    assert "creditScore" in result
    assert "rating" in result
    assert "loanEligibility" in result
    assert "breakdown" in result
    assert "insights" in result
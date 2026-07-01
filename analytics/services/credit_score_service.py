import pandas as pd
from datetime import datetime, timezone
from models.transaction import TransactionListRequest, TransactionType
from models.account import AccountData


def calculate_credit_score(request: TransactionListRequest, account: AccountData) -> dict:

    transactions = request.transactions
    current_balance = request.currentBalance

    if not transactions:
        return _empty_score()

    df = pd.DataFrame([{
        "id": t.id,
        "type": t.type.value,
        "amount": abs(t.amount),
        "raw_amount": t.amount,
        "timestamp": t.timestamp
    } for t in transactions])

    df["timestamp"] = pd.to_datetime(df["timestamp"], utc=True)
    df = df.sort_values("timestamp")

    now = datetime.now(timezone.utc)

    # 1. Payment consistency (35%)
    # Regular deposits = stable income = higher score
    deposits = df[df["type"].isin([TransactionType.DEPOSIT.value])]
    withdrawals = df[df["type"].isin([TransactionType.WITHDRAW.value, TransactionType.TRANSFER_OUT.value])]

    deposit_count = len(deposits)
    withdrawal_count = len(withdrawals)
    total_transactions = len(df)

    # Penalize if withdrawals dominate
    if total_transactions == 0:
        consistency_ratio = 0.5
    else:
        consistency_ratio = deposit_count / total_transactions

    # Check for sudden large drains (penalize)
    avg_withdrawal = withdrawals["amount"].mean() if not withdrawals.empty else 0
    max_withdrawal = withdrawals["amount"].max() if not withdrawals.empty else 0
    sudden_drain_penalty = 1.0
    if avg_withdrawal > 0 and max_withdrawal > avg_withdrawal * 3:
        sudden_drain_penalty = 0.7

    consistency_score = min(100, consistency_ratio * 100 * sudden_drain_penalty)
    payment_consistency = consistency_score * 0.35

    # 2. Account balance history (30%)
    # Higher average balance relative to total activity = better score
    total_deposited = deposits["amount"].sum() if not deposits.empty else 0
    total_withdrawn = withdrawals["amount"].sum() if not withdrawals.empty else 0
    net_flow = total_deposited - total_withdrawn

    # Normalize balance score — current balance vs total deposited
    if total_deposited == 0:
        balance_ratio = 0
    else:
        balance_ratio = min(1.0, current_balance / total_deposited)

    # Penalize if balance is near zero
    near_zero_penalty = 1.0
    if current_balance < 500:
        near_zero_penalty = 0.6
    elif current_balance < 1000:
        near_zero_penalty = 0.8

    balance_score = min(100, balance_ratio * 100 * near_zero_penalty)
    balance_history = balance_score * 0.30

    # 3. Transaction frequency (15%)
    # Active account usage = engaged user = lower risk
    account_age_days = max(1, (now - df["timestamp"].min()).days)
    transactions_per_month = (total_transactions / account_age_days) * 30

    # Ideal: 5-20 transactions per month
    if transactions_per_month <= 0:
        frequency_score = 0
    elif transactions_per_month < 2:
        frequency_score = 30
    elif transactions_per_month <= 20:
        frequency_score = min(100, transactions_per_month * 5)
    else:
        # Too many transactions can indicate instability
        frequency_score = max(50, 100 - (transactions_per_month - 20) * 2)

    transaction_frequency = frequency_score * 0.15

    # 4. Debt-to-income ratio (10%)
    # Lower withdrawals relative to deposits = better ratio
    if total_deposited == 0:
        dti_score = 0
    else:
        dti_ratio = total_withdrawn / total_deposited
        if dti_ratio <= 0.3:
            dti_score = 100
        elif dti_ratio <= 0.5:
            dti_score = 80
        elif dti_ratio <= 0.7:
            dti_score = 60
        elif dti_ratio <= 0.9:
            dti_score = 40
        else:
            dti_score = 20

    debt_to_income = dti_score * 0.10

    # 5. Account age (10%)
    # Older account = more history = more trustworthy
    account_created = account.createdAt
    if account_created.tzinfo is None:
        account_created = account_created.replace(tzinfo=timezone.utc)

    account_age_days_total = max(1, (now - account_created).days)

    if account_age_days_total >= 730:   # 2+ years
        age_score = 100
    elif account_age_days_total >= 365: # 1-2 years
        age_score = 75
    elif account_age_days_total >= 180: # 6-12 months
        age_score = 50
    elif account_age_days_total >= 90:  # 3-6 months
        age_score = 30
    else:
        age_score = 15

    account_age = age_score * 0.10

    # Final Score
    raw_score = payment_consistency + balance_history + transaction_frequency + debt_to_income + account_age

    # Scale to 400-850 range (standard credit score range)
    final_score = int(400 + (raw_score / 100) * 450)
    final_score = max(400, min(850, final_score))

    return {
        "creditScore": final_score,
        "rating": _get_rating(final_score),
        "loanEligibility": _get_loan_eligibility(final_score),
        "breakdown": {
            "paymentConsistency": round(payment_consistency / 0.35, 2),
            "balanceHistory": round(balance_history / 0.30, 2),
            "transactionFrequency": round(transaction_frequency / 0.15, 2),
            "debtToIncomeRatio": round(debt_to_income / 0.10, 2),
            "accountAge": round(account_age / 0.10, 2)
        },
        "insights": _generate_insights(
            consistency_score, balance_score, frequency_score, dti_score, age_score,
            current_balance, total_deposited, total_withdrawn, account_age_days_total
        )
    }


def _get_rating(score: int) -> str:
    if score >= 750:
        return "EXCELLENT"
    elif score >= 650:
        return "GOOD"
    elif score >= 550:
        return "FAIR"
    elif score >= 400:
        return "POOR"
    else:
        return "VERY_POOR"


def _get_loan_eligibility(score: int) -> dict:
    if score >= 750:
        return {
            "eligible": True,
            "recommendation": "AUTO_APPROVE",
            "suggestedInterestRate": 5.0,
            "notes": "Excellent credit history. Eligible for best rates."
        }
    elif score >= 650:
        return {
            "eligible": True,
            "recommendation": "APPROVE",
            "suggestedInterestRate": 8.5,
            "notes": "Good credit history. Standard rates apply."
        }
    elif score >= 550:
        return {
            "eligible": True,
            "recommendation": "APPROVE_WITH_CONDITIONS",
            "suggestedInterestRate": 12.0,
            "notes": "Fair credit history. Higher rate or collateral may be required."
        }
    elif score >= 400:
        return {
            "eligible": False,
            "recommendation": "MANUAL_REVIEW",
            "suggestedInterestRate": None,
            "notes": "Poor credit history. Manual review required before approval."
        }
    else:
        return {
            "eligible": False,
            "recommendation": "DENY",
            "suggestedInterestRate": None,
            "notes": "Insufficient credit history to approve a loan."
        }


def _generate_insights(consistency, balance, frequency, dti, age,
                       current_balance, total_deposited, total_withdrawn, account_age_days) -> list[str]:
    insights = []

    if consistency < 50:
        insights.append("Irregular deposit patterns detected. Consistent income deposits improve your score.")
    if balance < 50:
        insights.append("Low account balance relative to total deposits. Maintaining a higher balance improves your score.")
    if current_balance < 500:
        insights.append("Current balance is very low. Building a savings buffer strengthens your credit profile.")
    if frequency < 30:
        insights.append("Low account activity. Regular use of your account demonstrates financial engagement.")
    if dti < 50:
        insights.append("High withdrawal-to-deposit ratio detected. Reducing unnecessary withdrawals improves your score.")
    if age < 30:
        insights.append("Account is relatively new. Credit scores improve naturally over time with good financial habits.")
    if total_deposited > 0 and total_withdrawn / total_deposited > 0.9:
        insights.append("Almost all deposited funds are being withdrawn. Try to maintain a savings buffer.")

    if not insights:
        insights.append("Your financial profile looks healthy. Keep maintaining good habits to sustain your score.")

    return insights


def _empty_score() -> dict:
    return {
        "creditScore": 400,
        "rating": "POOR",
        "loanEligibility": {
            "eligible": False,
            "recommendation": "DENY",
            "suggestedInterestRate": None,
            "notes": "No transaction history available to calculate a credit score."
        },
        "breakdown": {
            "paymentConsistency": 0,
            "balanceHistory": 0,
            "transactionFrequency": 0,
            "debtToIncomeRatio": 0,
            "accountAge": 0
        },
        "insights": ["No transaction history found. Start using your account to build a credit profile."]
    }
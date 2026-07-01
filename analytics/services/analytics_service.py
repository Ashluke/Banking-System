import pandas as pd
from datetime import datetime, timezone
from models.transaction import TransactionListRequest, TransactionType


def get_transaction_insights(request: TransactionListRequest) -> dict:

    transactions = request.transactions

    if not transactions:
        return _empty_insights()

    df = pd.DataFrame([{
        "id": t.id,
        "type": t.type.value,
        "amount": abs(t.amount),
        "timestamp": t.timestamp
    } for t in transactions])

    df["timestamp"] = pd.to_datetime(df["timestamp"], utc=True)
    df["month"] = df["timestamp"].dt.to_period("M").astype(str)
    df["date"] = df["timestamp"].dt.date

    deposits = df[df["type"].isin([TransactionType.DEPOSIT.value])]
    withdrawals = df[df["type"].isin([TransactionType.WITHDRAW.value, TransactionType.TRANSFER_OUT.value])]
    transfers_in = df[df["type"] == TransactionType.TRANSFER_IN.value]
    transfers_out = df[df["type"] == TransactionType.TRANSFER_OUT.value]

    total_deposited = deposits["amount"].sum() if not deposits.empty else 0
    total_withdrawn = withdrawals["amount"].sum() if not withdrawals.empty else 0
    total_transferred_in = transfers_in["amount"].sum() if not transfers_in.empty else 0
    total_transferred_out = transfers_out["amount"].sum() if not transfers_out.empty else 0
    net_flow = total_deposited - total_withdrawn

    # Monthly cash flow
    monthly_cash_flow = []
    for month, group in df.groupby("month"):
        month_deposits = group[group["type"].isin([TransactionType.DEPOSIT.value])]["amount"].sum()
        month_withdrawals = group[group["type"].isin([
            TransactionType.WITHDRAW.value, TransactionType.TRANSFER_OUT.value
        ])]["amount"].sum()
        monthly_cash_flow.append({
            "month": month,
            "totalIn": round(float(month_deposits), 2),
            "totalOut": round(float(month_withdrawals), 2),
            "netFlow": round(float(month_deposits - month_withdrawals), 2)
        })

    # Transaction breakdown by type
    type_breakdown = df.groupby("type").agg(
        count=("amount", "count"),
        total=("amount", "sum")
    ).reset_index().to_dict(orient="records")

    type_breakdown = [{
        "type": row["type"],
        "count": int(row["count"]),
        "total": round(float(row["total"]), 2)
    } for row in type_breakdown]

    # Average transaction amount
    avg_transaction = round(float(df["amount"].mean()), 2) if not df.empty else 0

    # Largest transactions
    top_deposits = deposits.nlargest(3, "amount")[["amount", "timestamp"]].to_dict(orient="records")
    top_withdrawals = withdrawals.nlargest(3, "amount")[["amount", "timestamp"]].to_dict(orient="records")

    return {
        "summary": {
            "totalTransactions": len(df),
            "totalDeposited": round(float(total_deposited), 2),
            "totalWithdrawn": round(float(total_withdrawn), 2),
            "totalTransferredIn": round(float(total_transferred_in), 2),
            "totalTransferredOut": round(float(total_transferred_out), 2),
            "netFlow": round(float(net_flow), 2),
            "averageTransactionAmount": avg_transaction,
            "currentBalance": request.currentBalance
        },
        "monthlyCashFlow": monthly_cash_flow,
        "typeBreakdown": type_breakdown,
        "topDeposits": [{"amount": round(float(r["amount"]), 2), "timestamp": str(r["timestamp"])} for r in top_deposits],
        "topWithdrawals": [{"amount": round(float(r["amount"]), 2), "timestamp": str(r["timestamp"])} for r in top_withdrawals]
    }


def get_spending_trends(request: TransactionListRequest) -> dict:

    transactions = request.transactions

    if not transactions:
        return _empty_trends()

    df = pd.DataFrame([{
        "type": t.type.value,
        "amount": abs(t.amount),
        "timestamp": t.timestamp
    } for t in transactions])

    df["timestamp"] = pd.to_datetime(df["timestamp"], utc=True)
    df["month"] = df["timestamp"].dt.to_period("M").astype(str)

    deposits = df[df["type"] == TransactionType.DEPOSIT.value]
    withdrawals = df[df["type"].isin([TransactionType.WITHDRAW.value, TransactionType.TRANSFER_OUT.value])]

    # Monthly savings trend
    monthly_savings = []
    for month in sorted(df["month"].unique()):
        month_data = df[df["month"] == month]
        month_in = month_data[month_data["type"].isin([
            TransactionType.DEPOSIT.value, TransactionType.TRANSFER_IN.value
        ])]["amount"].sum()
        month_out = month_data[month_data["type"].isin([
            TransactionType.WITHDRAW.value, TransactionType.TRANSFER_OUT.value
        ])]["amount"].sum()
        savings = month_in - month_out
        savings_rate = round(float(savings / month_in * 100), 2) if month_in > 0 else 0
        monthly_savings.append({
            "month": month,
            "income": round(float(month_in), 2),
            "expenses": round(float(month_out), 2),
            "savings": round(float(savings), 2),
            "savingsRate": savings_rate
        })

    # Overall savings rate
    total_in = deposits["amount"].sum() if not deposits.empty else 0
    total_out = withdrawals["amount"].sum() if not withdrawals.empty else 0
    overall_savings_rate = round(float((total_in - total_out) / total_in * 100), 2) if total_in > 0 else 0

    # Spending trend direction
    if len(monthly_savings) >= 2:
        recent = monthly_savings[-1]["expenses"]
        previous = monthly_savings[-2]["expenses"]
        if recent > previous * 1.1:
            trend = "INCREASING"
        elif recent < previous * 0.9:
            trend = "DECREASING"
        else:
            trend = "STABLE"
    else:
        trend = "INSUFFICIENT_DATA"

    return {
        "overallSavingsRate": overall_savings_rate,
        "spendingTrend": trend,
        "monthlySavings": monthly_savings
    }


def _empty_insights() -> dict:
    return {
        "summary": {
            "totalTransactions": 0,
            "totalDeposited": 0,
            "totalWithdrawn": 0,
            "totalTransferredIn": 0,
            "totalTransferredOut": 0,
            "netFlow": 0,
            "averageTransactionAmount": 0,
            "currentBalance": 0
        },
        "monthlyCashFlow": [],
        "typeBreakdown": [],
        "topDeposits": [],
        "topWithdrawals": []
    }


def _empty_trends() -> dict:
    return {
        "overallSavingsRate": 0,
        "spendingTrend": "INSUFFICIENT_DATA",
        "monthlySavings": []
    }
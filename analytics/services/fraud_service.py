import pandas as pd
from datetime import datetime, timezone, timedelta
from models.transaction import TransactionListRequest, TransactionType


def detect_fraud(request: TransactionListRequest) -> dict:

    transactions = request.transactions

    if not transactions:
        return _empty_fraud_result()

    df = pd.DataFrame([{
        "id": t.id,
        "type": t.type.value,
        "amount": abs(t.amount),
        "timestamp": t.timestamp
    } for t in transactions])

    df["timestamp"] = pd.to_datetime(df["timestamp"], utc=True)
    df = df.sort_values("timestamp").reset_index(drop=True)

    flags = []
    suspicious_transaction_ids = set()

    avg_amount = df["amount"].mean()
    std_amount = df["amount"].std() if len(df) > 1 else 0

    for _, row in df.iterrows():

        transaction_flags = []

        # Flag 1: Amount > 3x average (unusually large transaction)
        if avg_amount > 0 and row["amount"] > avg_amount * 3:
            transaction_flags.append({
                "rule": "LARGE_AMOUNT",
                "description": f"Transaction amount ({row['amount']:.2f}) is more than 3x the average ({avg_amount:.2f})"
            })

        # Flag 2: Unusual hours (10PM - 5AM)
        hour = row["timestamp"].hour
        if hour >= 22 or hour < 5:
            transaction_flags.append({
                "rule": "UNUSUAL_HOURS",
                "description": f"Transaction occurred at unusual hours ({hour:02d}:00)"
            })

        # Flag 3: Rapid transactions — more than 5 within 10 minutes
        window_start = row["timestamp"] - timedelta(minutes=10)
        window_end = row["timestamp"] + timedelta(minutes=10)
        nearby = df[
            (df["timestamp"] >= window_start) &
            (df["timestamp"] <= window_end)
        ]
        if len(nearby) > 5:
            transaction_flags.append({
                "rule": "RAPID_TRANSACTIONS",
                "description": f"{len(nearby)} transactions detected within a 10-minute window"
            })

        if transaction_flags:
            suspicious_transaction_ids.add(int(row["id"]))
            flags.append({
                "transactionId": int(row["id"]),
                "amount": round(float(row["amount"]), 2),
                "type": row["type"],
                "timestamp": str(row["timestamp"]),
                "flags": transaction_flags
            })

    # Flag 4: Rapid account drain — more than 80% of balance withdrawn in one day
    withdrawals = df[df["type"].isin([TransactionType.WITHDRAW.value, TransactionType.TRANSFER_OUT.value])]
    if not withdrawals.empty and request.currentBalance > 0:
        df["date"] = df["timestamp"].dt.date
        for date, group in withdrawals.groupby(withdrawals["timestamp"].dt.date):
            daily_withdrawn = group["amount"].sum()
            total_available = request.currentBalance + daily_withdrawn
            if total_available > 0 and daily_withdrawn / total_available > 0.8:
                drain_ids = group["id"].tolist()
                for tid in drain_ids:
                    suspicious_transaction_ids.add(int(tid))
                flags.append({
                    "transactionId": None,
                    "amount": round(float(daily_withdrawn), 2),
                    "type": "MULTIPLE",
                    "timestamp": str(date),
                    "flags": [{
                        "rule": "RAPID_ACCOUNT_DRAIN",
                        "description": f"More than 80% of account balance ({daily_withdrawn:.2f}) withdrawn on {date}"
                    }]
                })

    # Risk level
    flag_count = len(flags)
    if flag_count == 0:
        risk_level = "LOW"
    elif flag_count <= 2:
        risk_level = "MEDIUM"
    elif flag_count <= 5:
        risk_level = "HIGH"
    else:
        risk_level = "CRITICAL"

    return {
        "riskLevel": risk_level,
        "totalFlagsDetected": flag_count,
        "suspiciousTransactionCount": len(suspicious_transaction_ids),
        "flags": flags,
        "summary": _generate_fraud_summary(risk_level, flag_count)
    }


def _generate_fraud_summary(risk_level: str, flag_count: int) -> str:
    if risk_level == "LOW":
        return "No suspicious activity detected. Account transactions appear normal."
    elif risk_level == "MEDIUM":
        return f"{flag_count} suspicious pattern(s) detected. Manual review recommended."
    elif risk_level == "HIGH":
        return f"{flag_count} suspicious pattern(s) detected. Immediate review required."
    else:
        return f"{flag_count} critical suspicious pattern(s) detected. Account should be flagged for investigation."


def _empty_fraud_result() -> dict:
    return {
        "riskLevel": "LOW",
        "totalFlagsDetected": 0,
        "suspiciousTransactionCount": 0,
        "flags": [],
        "summary": "No transaction history available to analyze."
    }
import pandas as pd
import numpy as np
from datetime import datetime, timezone, timedelta
from models.transaction import TransactionListRequest, TransactionType


def predict_savings(request: TransactionListRequest) -> dict:

    transactions = request.transactions

    if not transactions or len(transactions) < 3:
        return _empty_prediction(request.currentBalance)

    df = pd.DataFrame([{
        "type": t.type.value,
        "amount": abs(t.amount),
        "timestamp": t.timestamp
    } for t in transactions])

    df["timestamp"] = pd.to_datetime(df["timestamp"], utc=True)
    df = df.sort_values("timestamp")
    df["month"] = df["timestamp"].dt.to_period("M").astype(str)

    # Monthly net flow per month
    monthly_net = []
    for month, group in df.groupby("month"):
        month_in = group[group["type"].isin([
            TransactionType.DEPOSIT.value, TransactionType.TRANSFER_IN.value
        ])]["amount"].sum()
        month_out = group[group["type"].isin([
            TransactionType.WITHDRAW.value, TransactionType.TRANSFER_OUT.value
        ])]["amount"].sum()
        monthly_net.append(float(month_in - month_out))

    if len(monthly_net) < 2:
        return _empty_prediction(request.currentBalance)

    # Average monthly net flow
    avg_monthly_net = float(np.mean(monthly_net))
    std_monthly_net = float(np.std(monthly_net))

    current_balance = request.currentBalance
    now = datetime.now(timezone.utc)

    # Project 3, 6, 12 months
    projections = []
    for months_ahead in [3, 6, 12]:
        projected_balance = current_balance + (avg_monthly_net * months_ahead)
        projected_date = (now + timedelta(days=30 * months_ahead)).strftime("%Y-%m")

        # Confidence — lower std relative to avg = higher confidence
        if avg_monthly_net != 0:
            confidence = max(0, min(100, 100 - (std_monthly_net / abs(avg_monthly_net)) * 50))
        else:
            confidence = 50

        projections.append({
            "monthsAhead": months_ahead,
            "projectedDate": projected_date,
            "projectedBalance": round(float(projected_balance), 2),
            "estimatedSavings": round(float(avg_monthly_net * months_ahead), 2),
            "confidencePercent": round(confidence, 2)
        })

    # Monthly breakdown for next 12 months
    monthly_forecast = []
    running_balance = current_balance
    for i in range(1, 13):
        running_balance += avg_monthly_net
        forecast_date = (now + timedelta(days=30 * i)).strftime("%Y-%m")
        monthly_forecast.append({
            "month": forecast_date,
            "projectedBalance": round(float(running_balance), 2),
            "estimatedNetFlow": round(float(avg_monthly_net), 2)
        })

    # Trend direction
    if len(monthly_net) >= 3:
        recent_avg = float(np.mean(monthly_net[-3:]))
        older_avg = float(np.mean(monthly_net[:-3])) if len(monthly_net) > 3 else recent_avg
        if recent_avg > older_avg * 1.1:
            trend = "IMPROVING"
        elif recent_avg < older_avg * 0.9:
            trend = "DECLINING"
        else:
            trend = "STABLE"
    else:
        trend = "INSUFFICIENT_DATA"

    return {
        "currentBalance": round(current_balance, 2),
        "averageMonthlyNetFlow": round(avg_monthly_net, 2),
        "savingsTrend": trend,
        "projections": projections,
        "monthlyForecast": monthly_forecast,
        "insight": _generate_prediction_insight(avg_monthly_net, trend, projections)
    }


def _generate_prediction_insight(avg_monthly_net: float, trend: str, projections: list) -> str:
    if avg_monthly_net <= 0:
        return "Your expenses currently exceed your income. Without changes, your balance will decline over time."

    projection_12 = next((p for p in projections if p["monthsAhead"] == 12), None)
    if projection_12:
        if trend == "IMPROVING":
            return f"Your savings trend is improving. At this rate you could have {projection_12['projectedBalance']:.2f} in 12 months."
        elif trend == "DECLINING":
            return f"Your savings trend is declining. Consider reducing expenses to maintain your balance."
        else:
            return f"Your finances are stable. At your current rate you could have {projection_12['projectedBalance']:.2f} in 12 months."

    return "Keep maintaining your current financial habits to achieve steady savings growth."


def _empty_prediction(current_balance: float) -> dict:
    return {
        "currentBalance": round(current_balance, 2),
        "averageMonthlyNetFlow": 0,
        "savingsTrend": "INSUFFICIENT_DATA",
        "projections": [],
        "monthlyForecast": [],
        "insight": "Not enough transaction history to generate predictions. At least 3 transactions are required."
    }
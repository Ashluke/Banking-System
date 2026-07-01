import pandas as pd
import numpy as np
from models.stock import PortfolioRequest


def analyze_portfolio(request: PortfolioRequest) -> dict:

    holdings = request.holdings

    if not holdings:
        return _empty_portfolio()

    portfolio_items = []
    total_cost = 0.0
    total_current_value = 0.0

    for holding in holdings:
        symbol = holding.symbol
        quantity = float(holding.quantity)
        purchase_price = float(holding.purchasePrice)
        current_price = float(holding.currentPrice)

        cost_basis = quantity * purchase_price
        current_value = quantity * current_price
        gain_loss = current_value - cost_basis
        gain_loss_percent = (gain_loss / cost_basis * 100) if cost_basis > 0 else 0

        total_cost += cost_basis
        total_current_value += current_value

        portfolio_items.append({
            "symbol": symbol,
            "quantity": quantity,
            "purchasePrice": round(purchase_price, 6),
            "currentPrice": round(current_price, 6),
            "costBasis": round(cost_basis, 2),
            "currentValue": round(current_value, 2),
            "gainLoss": round(gain_loss, 2),
            "gainLossPercent": round(gain_loss_percent, 2),
            "portfolioWeight": 0.0
        })

    # Portfolio weight per holding
    for item in portfolio_items:
        item["portfolioWeight"] = round(
            (item["currentValue"] / total_current_value * 100) if total_current_value > 0 else 0, 2
        )

    total_gain_loss = total_current_value - total_cost
    total_gain_loss_percent = (total_gain_loss / total_cost * 100) if total_cost > 0 else 0

    # Diversification score
    weights = [item["portfolioWeight"] / 100 for item in portfolio_items]
    diversification_score = _calculate_diversification_score(weights)

    # Volatility — daily price change percent per holding
    volatility_data = []
    for holding in holdings:
        if holding.purchasePrice > 0:
            daily_change_percent = abs((holding.currentPrice - holding.purchasePrice) / holding.purchasePrice * 100)
            volatility_data.append(daily_change_percent)

    avg_volatility = round(float(np.mean(volatility_data)), 2) if volatility_data else 0.0
    risk_level = _get_risk_level(avg_volatility, diversification_score)

    # Best and worst performers
    sorted_items = sorted(portfolio_items, key=lambda x: x["gainLossPercent"], reverse=True)
    best_performer = sorted_items[0] if sorted_items else None
    worst_performer = sorted_items[-1] if sorted_items else None

    return {
        "summary": {
            "totalCost": round(total_cost, 2),
            "totalCurrentValue": round(total_current_value, 2),
            "totalGainLoss": round(total_gain_loss, 2),
            "totalGainLossPercent": round(total_gain_loss_percent, 2),
            "numberOfHoldings": len(portfolio_items)
        },
        "holdings": portfolio_items,
        "diversificationScore": diversification_score,
        "averageDailyVolatility": avg_volatility,
        "riskLevel": risk_level,
        "bestPerformer": {
            "symbol": best_performer["symbol"],
            "gainLossPercent": best_performer["gainLossPercent"]
        } if best_performer else None,
        "worstPerformer": {
            "symbol": worst_performer["symbol"],
            "gainLossPercent": worst_performer["gainLossPercent"]
        } if worst_performer else None,
        "insight": _generate_portfolio_insight(
            total_gain_loss_percent, diversification_score, risk_level, len(portfolio_items)
        )
    }


def _calculate_diversification_score(weights: list[float]) -> int:
    if not weights or len(weights) == 1:
        return 20

    hhi = sum(w ** 2 for w in weights)
    min_hhi = 1.0 / len(weights)
    max_hhi = 1.0

    if max_hhi == min_hhi:
        score = 100
    else:
        score = int(100 - ((hhi - min_hhi) / (max_hhi - min_hhi)) * 100)

    return max(0, min(100, score))


def _get_risk_level(avg_volatility: float, diversification_score: int) -> str:
    if avg_volatility > 3.0 and diversification_score < 40:
        return "HIGH"
    elif avg_volatility > 2.0 or diversification_score < 50:
        return "MEDIUM"
    else:
        return "LOW"


def _generate_portfolio_insight(
        total_gain_loss_percent: float,
        diversification_score: int,
        risk_level: str,
        holding_count: int) -> str:

    insights = []

    if total_gain_loss_percent > 0:
        insights.append(f"Your portfolio is up {total_gain_loss_percent:.2f}% overall.")
    else:
        insights.append(f"Your portfolio is down {abs(total_gain_loss_percent):.2f}% overall.")

    if diversification_score < 40:
        insights.append("Your portfolio is heavily concentrated. Consider spreading across more assets.")
    elif diversification_score < 70:
        insights.append("Your portfolio has moderate diversification. Adding more variety can reduce risk.")
    else:
        insights.append("Your portfolio is well diversified.")

    if risk_level == "HIGH":
        insights.append("Current risk level is high due to volatility and concentration.")
    elif risk_level == "MEDIUM":
        insights.append("Current risk level is moderate.")
    else:
        insights.append("Current risk level is low.")

    return " ".join(insights)


def _empty_portfolio() -> dict:
    return {
        "summary": {
            "totalCost": 0,
            "totalCurrentValue": 0,
            "totalGainLoss": 0,
            "totalGainLossPercent": 0,
            "numberOfHoldings": 0
        },
        "holdings": [],
        "diversificationScore": 0,
        "averageDailyVolatility": 0,
        "riskLevel": "LOW",
        "bestPerformer": None,
        "worstPerformer": None,
        "insight": "No holdings found. Add stock holdings to see portfolio analytics."
    }
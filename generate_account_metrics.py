#!/usr/bin/env python3
"""
Generate account_metrics.json from accounts.csv and transactions.csv

This script reads the seed data CSV files and calculates account metrics
including balances, cash flow, category breakdowns, and multi-currency support.
"""

import csv
import json
from collections import defaultdict
from typing import Dict, List, Any
from datetime import datetime

# Exchange rates
EXCHANGE_RATES = {
    "INR_to_USD": 1/93.1663,
    "EUR_to_USD": 1/0.8490
}


def parse_csv_line(line: str) -> List[str]:
    """Parse a CSV line handling quoted fields."""
    result = []
    current = []
    in_quotes = False
    
    for char in line:
        if char == '"':
            in_quotes = not in_quotes
        elif char == ',' and not in_quotes:
            result.append(''.join(current))
            current = []
        else:
            current.append(char)
    
    result.append(''.join(current))
    return result


def load_accounts(csv_path: str) -> Dict[str, Dict[str, Any]]:
    """Load accounts from CSV file."""
    accounts = {}
    
    with open(csv_path, 'r') as f:
        reader = csv.reader(f)
        header = next(reader)
        
        for row in reader:
            if not row:
                continue
                
            account = {
                'id': row[0],
                'name': row[1],
                'initialBalance': 0.0,  # Accounts start with zero balance
                'currencyCode': row[2],
                'structureType': row[3],
                'colorHex': row[4],
                'icon': row[5],
                'allowedCurrencies': row[6].strip('"'),
                'profileId': int(row[7])
            }
            accounts[account['id']] = account
    
    return accounts


def load_transactions_csv(csv_path: str) -> List[Dict[str, Any]]:
    """Load transactions from CSV file."""
    transactions_list = []
    
    with open(csv_path, 'r') as f:
        reader = csv.reader(f)
        header = next(reader)
        
        for row in reader:
            if not row:
                continue
                
            transaction = {
                'title': row[0],
                'amount': float(row[1]),
                'category': row[2],
                'categoryId': int(row[3]) if row[3] else None,
                'timestamp': int(row[4]) if row[4] else None,
                'currency': row[5],
                'isIncome': row[6].lower() == 'true',
                'accountId': row[7],
                'paymentMethod': row[8],
                'payee': row[9],
                'note': row[10],
                'tags': row[11],
                'recurring': row[12] if row[12] else None,
                'profileId': int(row[13])
            }
            transactions_list.append(transaction)
    
    return transactions_list


def calculate_account_metrics(account: Dict[str, Any], 
                              account_transactions: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Calculate metrics for a single account."""
    
    # Initialize currency-specific cash flow
    currency_cash_flow = defaultdict(lambda: {'income': 0.0, 'expenses': 0.0})
    
    # Category breakdown
    category_breakdown = defaultdict(lambda: {'amount': 0.0, 'count': 0})
    
    # Process transactions
    for tx in account_transactions:
        currency = tx['currency']
        amount = tx['amount']
        
        if tx['isIncome']:
            currency_cash_flow[currency]['income'] += amount
        else:
            currency_cash_flow[currency]['expenses'] += amount
            category_breakdown[tx['category']]['amount'] += amount
        
        category_breakdown[tx['category']]['count'] += 1
    
    # Calculate net cash flow per currency
    for currency in currency_cash_flow:
        currency_cash_flow[currency]['net'] = (
            currency_cash_flow[currency]['income'] - 
            currency_cash_flow[currency]['expenses']
        )
    
    # Convert non-USD currencies to USD
    total_usd_income = currency_cash_flow.get('USD', {}).get('income', 0.0)
    total_usd_expenses = currency_cash_flow.get('USD', {}).get('expenses', 0.0)
    
    for currency in currency_cash_flow:
        if currency == 'USD':
            continue
        
        if currency == 'INR':
            rate = EXCHANGE_RATES['INR_to_USD']
        elif currency == 'EUR':
            rate = EXCHANGE_RATES['EUR_to_USD']
        else:
            rate = 1.0  # Default to 1:1 for unknown currencies
        
        income_usd = currency_cash_flow[currency]['income'] * rate
        expenses_usd = currency_cash_flow[currency]['expenses'] * rate
        net_usd = currency_cash_flow[currency]['net'] * rate
        
        total_usd_income += income_usd
        total_usd_expenses += expenses_usd
        
        currency_cash_flow[currency]['convertedToUSD'] = net_usd
    
    # Calculate net cash flow and final balance
    net_cash_flow = total_usd_income - total_usd_expenses
    final_balance = account['initialBalance'] + net_cash_flow
    net_change = net_cash_flow
    
    # Build multi-currency cash flow object
    multi_currency_cash_flow = {}
    for currency, data in sorted(currency_cash_flow.items()):
        multi_currency_cash_flow[currency] = {
            'totalIncome': data['income'],
            'totalExpenses': data['expenses'],
            'netCashFlow': data['net']
        }
        if currency != 'USD':
            multi_currency_cash_flow[currency]['convertedToUSD'] = data['convertedToUSD']
    
    # Build category breakdown (exclude Initial Balance)
    category_breakdown_list = []
    for category, data in sorted(category_breakdown.items()):
        if category == 'Initial Balance':
            continue  # Skip initial balance transactions
        category_breakdown_list.append({
            'category': category,
            'amount': data['amount'],
            'transactionCount': data['count']
        })
    
    # Build account metrics
    metrics = {
        'accountId': account['id'],
        'accountName': account['name'],
        'currencyCode': account['currencyCode'],
        'structureType': account['structureType'],
        'currentBalance': round(final_balance, 2),
        'initialBalance': account['initialBalance'],
        'netChange': round(net_change, 2),
        'transactionCount': len(account_transactions),
        'cashFlow': {
            'totalIncome': round(total_usd_income, 2),
            'totalExpenses': round(total_usd_expenses, 2),
            'netCashFlow': round(net_cash_flow, 2)
        },
        'multiCurrencyCashFlow': multi_currency_cash_flow,
        'categoryBreakdown': category_breakdown_list,
        'monthlyTrend': [
            {
                'month': '2024-01',
                'income': round(total_usd_income, 2),
                'expenses': round(total_usd_expenses, 2),
                'balance': round(final_balance, 2)
            }
        ]
    }
    
    return metrics


def calculate_global_breakdown(all_transactions: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Calculate global currency breakdown across all accounts."""
    currency_data = defaultdict(lambda: {'income': 0.0, 'expenses': 0.0, 'accounts': set()})
    
    for tx in all_transactions:
        currency = tx['currency']
        if tx['isIncome']:
            currency_data[currency]['income'] += tx['amount']
        else:
            currency_data[currency]['expenses'] += tx['amount']
        currency_data[currency]['accounts'].add(tx['accountId'])
    
    # Convert to USD for totals
    global_breakdown = {}
    total_usd_income = 0.0
    total_usd_expenses = 0.0
    
    for currency, data in sorted(currency_data.items()):
        if currency == 'USD':
            converted_income = data['income']
            converted_expenses = data['expenses']
        elif currency == 'INR':
            converted_income = data['income'] * EXCHANGE_RATES['INR_to_USD']
            converted_expenses = data['expenses'] * EXCHANGE_RATES['INR_to_USD']
        elif currency == 'EUR':
            converted_income = data['income'] * EXCHANGE_RATES['EUR_to_USD']
            converted_expenses = data['expenses'] * EXCHANGE_RATES['EUR_to_USD']
        else:
            converted_income = data['income']
            converted_expenses = data['expenses']
        
        total_usd_income += converted_income
        total_usd_expenses += converted_expenses
    
    # Build global breakdown with raw values
    for currency, data in sorted(currency_data.items()):
        global_breakdown[currency] = {
            'totalIncome': round(data['income'], 2),
            'totalExpenses': round(data['expenses'], 2),
            'netCashFlow': round(data['income'] - data['expenses'], 2),
            'accountCount': len(data['accounts'])
        }
    
    # Update USD breakdown to include converted amounts
    if 'USD' in global_breakdown:
        global_breakdown['USD']['totalIncome'] = round(total_usd_income, 2)
        global_breakdown['USD']['totalExpenses'] = round(total_usd_expenses, 2)
        global_breakdown['USD']['netCashFlow'] = round(total_usd_income - total_usd_expenses, 2)
    
    return global_breakdown


def generate_metrics(accounts_csv: str, transactions_csv: str):
    """Generate account metrics from CSV files and print JSON output."""
    
    # Load data
    accounts = load_accounts(accounts_csv)
    all_transactions = load_transactions_csv(transactions_csv)
    
    # Group transactions by account
    transactions_by_account = defaultdict(list)
    for tx in all_transactions:
        transactions_by_account[tx['accountId']].append(tx)
    
    # Calculate metrics for each account
    account_metrics = []
    total_balance = 0.0
    total_income = 0.0
    total_expenses = 0.0
    currencies_used = set()
    multi_currency_accounts = 0
    accounts_with_transactions = set()
    
    for account_id, account in sorted(accounts.items()):
        account_txs = transactions_by_account.get(account_id, [])
        
        if account_txs:
            accounts_with_transactions.add(account_id)
        
        # Check if multi-currency
        allowed_currencies = account['allowedCurrencies'].split(',')
        if len(allowed_currencies) > 1:
            multi_currency_accounts += 1
        
        # Calculate metrics
        metrics = calculate_account_metrics(account, account_txs)
        account_metrics.append(metrics)
        
        # Accumulate totals
        total_balance += metrics['currentBalance']
        total_income += metrics['cashFlow']['totalIncome']
        total_expenses += metrics['cashFlow']['totalExpenses']
        
        # Track currencies
        for tx in account_txs:
            currencies_used.add(tx['currency'])
    
    # Calculate global breakdown
    global_breakdown = calculate_global_breakdown(all_transactions)
    
    # Build summary
    summary = {
        'totalAccounts': len(accounts),
        'totalBalance': round(total_balance, 2),
        'totalIncome': round(total_income, 2),
        'totalExpenses': round(total_expenses, 2),
        'netCashFlow': round(total_income - total_expenses, 2),
        'activeAccounts': len(accounts_with_transactions),
        'accountsWithTransactions': len(accounts_with_transactions),
        'totalTransactions': len(all_transactions),
        'currenciesUsed': sorted(list(currencies_used)),
        'multiCurrencyAccounts': multi_currency_accounts
    }
    
    # Build final output
    output = {
        'accountMetrics': account_metrics,
        'summary': summary,
        'globalCurrencyBreakdown': global_breakdown,
        'exchangeRatesUsed': EXCHANGE_RATES
    }
    
    # Print JSON to stdout
    print(json.dumps(output, indent=2))


if __name__ == '__main__':
    # File paths
    accounts_csv = 'app/src/main/assets/accounts.csv'
    transactions_csv = 'app/src/main/assets/transactions.csv'
    
    # Generate metrics and print to stdout
    generate_metrics(accounts_csv, transactions_csv)

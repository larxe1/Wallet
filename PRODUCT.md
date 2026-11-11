# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Individual users managing personal multi-wallet finances, recurring obligations, payment cards, and installment purchases in Philippine Pesos (`PHP` / `₱`), authenticated per account via Supabase.

## Product Purpose

Money Tracker replaces fragmented spreadsheets with a unified personal finance workspace for tracking cumulative wallet balances, monthly and annual spending trends, category distributions, recurring auto-posted transactions, and daily cashflow schedules.

## Positioning

A private-by-default personal ledger engineered around multi-wallet accounting, automated client-side recurring posting, multi-month installment splits, and calendar cashflow projections backed directly by the user's Supabase database.

## Operating Context

- Authenticated web application built with React, Vite, React Router, Recharts, `date-fns`, Motion, Lucide React, and `@supabase/supabase-js`.
- Currency formatting uses `en-PH` (`PHP` / `₱`) across all ledgers, charts, KPIs, and cards.
- Dashboard initializes with privacy mode enabled (`••••••`) so balances stay masked in public settings until explicitly revealed.
- Month selection starts from August 2026 (`minMonth={7}`, `minYear={2026}`) forward across Dashboard, Expenses, Income, and Calendar views.

## Capabilities and Constraints

- **Brand & Release**: `Money Tracker` (`v1.1.0`), preserving release history from `v1.0.0` (formerly WalletWatch).
- **Dashboard & Wallet Aggregation**: Computes cumulative all-time income/expense balances per wallet, 6-month/12-month spending trends, category breakdowns, and top-5 category averages while excluding the `'alex'` wallet (case-insensitive) in `useDashboard`.
- **Expenses & Installments**: Month-scoped expense ledger supporting wallet, multi-category, and multi-payment-method filtering, inline arithmetic calculator input, and multi-month installment batching (` (Installment i/N)`).
- **Income & Calendar**: Month-scoped income ledger plus a 7-column monthly calendar showing posted transactions and projected `'monthly'` / `'interval'` recurring schedules.
- **Recurring Auto-Posting**: Runs once per session mount via `useAutoPost` to deduplicate current-month income/expense records and automatically post due active `recurring_transactions`.
- **Settings**: Wallet CRUD (protecting `is_default`), Payment Method creation and drag-and-drop `sort_order` persistence, and Category CRUD with default category seeding.
- **Data Constraints**: All existing Supabase tables (`wallets`, `categories`, `payment_methods`, `expenses`, `income`, `recurring_transactions`) and hook payload contracts must remain intact; budgeting limits are not part of the schema.

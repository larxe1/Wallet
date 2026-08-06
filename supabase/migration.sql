-- WalletWatch Database Migration Script
-- INSTRUCTIONS: Run this entire script in the Supabase SQL Editor.
-- It will set up all tables, indexes, RLS policies, RPC functions, and automated data seeding for new users.

-- 1. TABLES

-- Wallets
create table public.wallets (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users(id) on delete cascade not null,
  name text not null,
  color text default '#6366f1',
  sort_order int default 0,
  created_at timestamptz default now() not null
);

-- Categories
create table public.categories (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users(id) on delete cascade not null,
  name text not null,
  icon text default '📦',
  sort_order int default 0,
  created_at timestamptz default now() not null
);

-- Payment Methods
create table public.payment_methods (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users(id) on delete cascade not null,
  type text not null check (type in ('credit', 'debit', 'cash')),
  identifier text,
  label text not null,
  sort_order int default 0,
  created_at timestamptz default now() not null
);

-- Recurring Expenses (Created BEFORE expenses to satisfy foreign key requirement)
create table public.recurring_expenses (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users(id) on delete cascade not null,
  wallet_id uuid references public.wallets(id) on delete cascade not null,
  category_id uuid references public.categories(id) on delete set null,
  payment_method_id uuid references public.payment_methods(id) on delete set null,
  amount numeric(12, 2) not null check (amount > 0),
  description text,
  day_of_month int not null check (day_of_month between 1 and 31),
  is_active boolean default true,
  last_posted date,
  created_at timestamptz default now() not null
);

-- Expenses
create table public.expenses (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users(id) on delete cascade not null,
  wallet_id uuid references public.wallets(id) on delete cascade not null,
  category_id uuid references public.categories(id) on delete set null,
  payment_method_id uuid references public.payment_methods(id) on delete set null,
  amount numeric(12, 2) not null check (amount > 0),
  description text,
  expense_date date not null default current_date,
  is_recurring boolean default false,
  recurring_id uuid references public.recurring_expenses(id) on delete set null,
  created_at timestamptz default now() not null
);

-- Income
create table public.income (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references auth.users(id) on delete cascade not null,
  wallet_id uuid references public.wallets(id) on delete cascade not null,
  amount numeric(12, 2) not null check (amount > 0),
  description text,
  income_date date not null default current_date,
  created_at timestamptz default now() not null
);


-- 2. INDEXES
create index idx_expenses_user_date on public.expenses(user_id, expense_date);
create index idx_expenses_wallet on public.expenses(wallet_id);
create index idx_expenses_category on public.expenses(category_id);
create index idx_income_user_date on public.income(user_id, income_date);
create index idx_income_wallet on public.income(wallet_id);
create index idx_recurring_user_active on public.recurring_expenses(user_id, is_active);


-- 3. RLS POLICIES
alter table public.wallets enable row level security;
alter table public.categories enable row level security;
alter table public.payment_methods enable row level security;
alter table public.recurring_expenses enable row level security;
alter table public.expenses enable row level security;
alter table public.income enable row level security;

-- Policies for wallets
create policy "Users can view their own wallets" on public.wallets for select using (auth.uid() = user_id);
create policy "Users can insert their own wallets" on public.wallets for insert with check (auth.uid() = user_id);
create policy "Users can update their own wallets" on public.wallets for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "Users can delete their own wallets" on public.wallets for delete using (auth.uid() = user_id);

-- Policies for categories
create policy "Users can view their own categories" on public.categories for select using (auth.uid() = user_id);
create policy "Users can insert their own categories" on public.categories for insert with check (auth.uid() = user_id);
create policy "Users can update their own categories" on public.categories for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "Users can delete their own categories" on public.categories for delete using (auth.uid() = user_id);

-- Policies for payment_methods
create policy "Users can view their own payment_methods" on public.payment_methods for select using (auth.uid() = user_id);
create policy "Users can insert their own payment_methods" on public.payment_methods for insert with check (auth.uid() = user_id);
create policy "Users can update their own payment_methods" on public.payment_methods for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "Users can delete their own payment_methods" on public.payment_methods for delete using (auth.uid() = user_id);

-- Policies for recurring_expenses
create policy "Users can view their own recurring_expenses" on public.recurring_expenses for select using (auth.uid() = user_id);
create policy "Users can insert their own recurring_expenses" on public.recurring_expenses for insert with check (auth.uid() = user_id);
create policy "Users can update their own recurring_expenses" on public.recurring_expenses for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "Users can delete their own recurring_expenses" on public.recurring_expenses for delete using (auth.uid() = user_id);

-- Policies for expenses
create policy "Users can view their own expenses" on public.expenses for select using (auth.uid() = user_id);
create policy "Users can insert their own expenses" on public.expenses for insert with check (auth.uid() = user_id);
create policy "Users can update their own expenses" on public.expenses for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "Users can delete their own expenses" on public.expenses for delete using (auth.uid() = user_id);

-- Policies for income
create policy "Users can view their own income" on public.income for select using (auth.uid() = user_id);
create policy "Users can insert their own income" on public.income for insert with check (auth.uid() = user_id);
create policy "Users can update their own income" on public.income for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "Users can delete their own income" on public.income for delete using (auth.uid() = user_id);


-- 4. RPC FUNCTIONS

-- a) get_monthly_spending
create or replace function get_monthly_spending(p_user_id uuid, p_year int, p_month int, p_wallet_id uuid default null)
returns table(category_name text, category_icon text, wallet_name text, wallet_id uuid, total_amount numeric)
language plpgsql
security definer
as $$
begin
  return query
  select 
    c.name as category_name,
    c.icon as category_icon,
    w.name as wallet_name,
    w.id as wallet_id,
    sum(e.amount) as total_amount
  from public.expenses e
  left join public.categories c on e.category_id = c.id
  left join public.wallets w on e.wallet_id = w.id
  where e.user_id = p_user_id
    and extract(year from e.expense_date) = p_year
    and extract(month from e.expense_date) = p_month
    and (p_wallet_id is null or e.wallet_id = p_wallet_id)
  group by c.name, c.icon, w.name, w.id
  order by total_amount desc;
end;
$$;

-- b) get_3month_avg
create or replace function get_3month_avg(p_user_id uuid, p_year int, p_month int)
returns table(category_name text, wallet_name text, avg_amount numeric)
language plpgsql
security definer
as $$
declare
  target_date date := make_date(p_year, p_month, 1);
  start_date date := target_date - interval '2 months';
  end_date date := (target_date + interval '1 month') - interval '1 day';
begin
  return query
  select 
    c.name as category_name,
    w.name as wallet_name,
    sum(e.amount) / 3.0 as avg_amount
  from public.expenses e
  left join public.categories c on e.category_id = c.id
  left join public.wallets w on e.wallet_id = w.id
  where e.user_id = p_user_id
    and e.expense_date >= start_date
    and e.expense_date <= end_date
  group by c.name, w.name;
end;
$$;

-- c) get_wallet_balances
create or replace function get_wallet_balances(p_user_id uuid, p_year int, p_month int)
returns table(wallet_id uuid, wallet_name text, wallet_color text, total_income numeric, total_expenses numeric, balance numeric)
language plpgsql
security definer
as $$
begin
  return query
  with inc as (
    select i.wallet_id, sum(i.amount) as amt
    from public.income i
    where i.user_id = p_user_id
      and extract(year from i.income_date) = p_year
      and extract(month from i.income_date) = p_month
    group by i.wallet_id
  ),
  exp as (
    select e.wallet_id, sum(e.amount) as amt
    from public.expenses e
    where e.user_id = p_user_id
      and extract(year from e.expense_date) = p_year
      and extract(month from e.expense_date) = p_month
    group by e.wallet_id
  )
  select 
    w.id as wallet_id,
    w.name as wallet_name,
    w.color as wallet_color,
    coalesce(inc.amt, 0) as total_income,
    coalesce(exp.amt, 0) as total_expenses,
    (coalesce(inc.amt, 0) - coalesce(exp.amt, 0)) as balance
  from public.wallets w
  left join inc on w.id = inc.wallet_id
  left join exp on w.id = exp.wallet_id
  where w.user_id = p_user_id;
end;
$$;

-- d) get_annual_summary
create or replace function get_annual_summary(p_user_id uuid, p_year int)
returns table(month_num int, total_expenses numeric, total_income numeric)
language plpgsql
security definer
as $$
begin
  return query
  with months as (
    select generate_series(1, 12) as m
  ),
  inc as (
    select extract(month from i.income_date)::int as m, sum(i.amount) as amt
    from public.income i
    where i.user_id = p_user_id and extract(year from i.income_date) = p_year
    group by extract(month from i.income_date)::int
  ),
  exp as (
    select extract(month from e.expense_date)::int as m, sum(e.amount) as amt
    from public.expenses e
    where e.user_id = p_user_id and extract(year from e.expense_date) = p_year
    group by extract(month from e.expense_date)::int
  )
  select 
    m.m as month_num,
    coalesce(exp.amt, 0) as total_expenses,
    coalesce(inc.amt, 0) as total_income
  from months m
  left join inc on m.m = inc.m
  left join exp on m.m = exp.m
  order by m.m;
end;
$$;


-- 5. SEED DATA FUNCTION & TRIGGER

create or replace function seed_default_data(p_user_id uuid)
returns void
language plpgsql
security definer
as $$
begin
  -- Insert default wallets
  insert into public.wallets (user_id, name, color, sort_order) values
    (p_user_id, 'Elijah', '#6366f1', 1),
    (p_user_id, 'Combined Wallet', '#f59e0b', 2),
    (p_user_id, 'Alex', '#14b8a6', 3);

  -- Insert default categories
  insert into public.categories (user_id, name, icon, sort_order) values
    (p_user_id, 'Bills', '💳', 1),
    (p_user_id, 'Grocery', '🛒', 2),
    (p_user_id, 'Healthcare', '❤️', 3),
    (p_user_id, 'Games', '🎮', 4);

  -- Insert default payment methods
  insert into public.payment_methods (user_id, type, label, sort_order) values
    (p_user_id, 'cash', 'Cash', 1),
    (p_user_id, 'debit', 'Maribank', 2),
    (p_user_id, 'debit', 'GoTyme', 3),
    (p_user_id, 'debit', 'GCash', 4);
end;
$$;

create or replace function handle_new_user()
returns trigger as $$
begin
  perform seed_default_data(new.id);
  return new;
end;
$$ language plpgsql security definer;

-- Recreate trigger (drop first in case of reruns)
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function handle_new_user();

import { useEffect, useRef } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';
import { format, isSameMonth, parseISO, addDays, isBefore, isSameDay } from 'date-fns';

export const useAutoPost = () => {
  const { user } = useAuth();
  const hasRun = useRef(false);

  useEffect(() => {
    if (!user || hasRun.current) return;
    hasRun.current = true;

    const checkAndPost = async () => {
      try {
        const today = new Date();
        const currentDay = today.getDate();
        const currentMonthStart = format(new Date(today.getFullYear(), today.getMonth(), 1), 'yyyy-MM-dd');

        // ONE-TIME CLEANUP OF DUPLICATES
        const { data: recentIncome } = await supabase.from('income').select('*').gte('income_date', currentMonthStart);
        if (recentIncome && recentIncome.length > 0) {
          const grouped = {};
          recentIncome.forEach(i => {
            const key = i.description + '|' + i.income_date + '|' + i.wallet_id;
            if (!grouped[key]) grouped[key] = [];
            grouped[key].push(i);
          });
          for (const key in grouped) {
            if (grouped[key].length > 1) {
              const toDelete = grouped[key].slice(1).map(i => i.id);
              await supabase.from('income').delete().in('id', toDelete);
            }
          }
        }
        
        const { data: recentExp } = await supabase.from('expenses').select('*').gte('expense_date', currentMonthStart);
        if (recentExp && recentExp.length > 0) {
          const grouped = {};
          recentExp.forEach(e => {
            const key = e.description + '|' + e.expense_date + '|' + e.wallet_id;
            if (!grouped[key]) grouped[key] = [];
            grouped[key].push(e);
          });
          for (const key in grouped) {
            if (grouped[key].length > 1) {
              const toDelete = grouped[key].slice(1).map(e => e.id);
              await supabase.from('expenses').delete().in('id', toDelete);
            }
          }
        }

        // Fetch active recurring transactions
        const { data: recurring, error } = await supabase
          .from('recurring_transactions')
          .select('*')
          .eq('user_id', user.id)
          .eq('is_active', true);

        if (error || !recurring) return;

        for (const item of recurring) {
          const type = item.frequency_type || 'monthly';
          const postTransaction = async (postDateStr) => {
            if (item.type === 'income') {
              await supabase.from('income').insert([{
                user_id: user.id,
                amount: item.amount,
                description: item.description || 'Recurring Income',
                wallet_id: item.wallet_id,
                income_date: postDateStr
              }]);
            } else {
              await supabase.from('expenses').insert([{
                user_id: user.id,
                amount: item.amount,
                description: item.description || 'Recurring Expense',
                wallet_id: item.wallet_id,
                category_id: item.category_id,
                payment_method_id: item.payment_method_id,
                expense_date: postDateStr
              }]);
            }
          };

          if (type === 'monthly') {
            const isDue = item.day_of_month <= currentDay;
            const alreadyPostedThisMonth = item.last_posted_date && 
              isSameMonth(parseISO(item.last_posted_date), today);

            if (isDue && !alreadyPostedThisMonth) {
              const postDate = format(new Date(today.getFullYear(), today.getMonth(), item.day_of_month), 'yyyy-MM-dd');
              await postTransaction(postDate);
              await supabase.from('recurring_transactions').update({ last_posted_date: postDate }).eq('id', item.id);
            }
          } else if (type === 'interval') {
            if (!item.start_date || !item.interval_days) continue;
            
            let baseline = item.last_posted_date ? parseISO(item.last_posted_date) : parseISO(item.start_date);
            let nextDue = item.last_posted_date ? addDays(baseline, item.interval_days) : baseline;
            
            let lastPosted = null;
            while (isBefore(nextDue, today) || isSameDay(nextDue, today)) {
              const postDateStr = format(nextDue, 'yyyy-MM-dd');
              await postTransaction(postDateStr);
              lastPosted = postDateStr;
              nextDue = addDays(nextDue, item.interval_days);
            }

            if (lastPosted) {
              await supabase.from('recurring_transactions').update({ last_posted_date: lastPosted }).eq('id', item.id);
            }
          }
        }
      } catch (err) {
        console.error('Error auto-posting recurring transactions:', err);
      }
    };

    checkAndPost();
  }, [user]);
};

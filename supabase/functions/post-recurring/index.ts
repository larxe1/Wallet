import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

Deno.serve(async (req) => {
  try {
    // 1. Create a Supabase client using the service role key
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const supabaseServiceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')

    if (!supabaseUrl || !supabaseServiceRoleKey) {
      throw new Error('Missing Supabase environment variables')
    }

    const supabase = createClient(supabaseUrl, supabaseServiceRoleKey)

    // 2. Get today's date information
    const today = new Date()
    const todayDayOfMonth = today.getDate()
    
    // Formatting today's date for 'YYYY-MM-DD' comparison and insertion
    const currentYear = today.getFullYear()
    const currentMonth = today.getMonth() // 0-11
    
    // YYYY-MM-DD format based on local UTC execution time
    const todayDateString = today.toISOString().split('T')[0]

    // 3. Query all active recurring expenses where day_of_month = today's day
    const { data: recurringExpenses, error: fetchError } = await supabase
      .from('recurring_expenses')
      .select('*')
      .eq('is_active', true)
      .eq('day_of_month', todayDayOfMonth)

    if (fetchError) {
      throw fetchError
    }

    let postedCount = 0
    let skippedCount = 0
    let errorCount = 0

    // 4. Process each matched recurring expense
    for (const expense of recurringExpenses || []) {
      // Check if last_posted is already in the current month
      let shouldPost = true
      
      if (expense.last_posted) {
        const lastPostedDate = new Date(expense.last_posted)
        if (
          lastPostedDate.getFullYear() === currentYear &&
          lastPostedDate.getMonth() === currentMonth
        ) {
          shouldPost = false
        }
      }

      if (shouldPost) {
        // 5. Insert a new expense with is_recurring = true and recurring_id set
        const { error: insertError } = await supabase
          .from('expenses')
          .insert({
            user_id: expense.user_id,
            wallet_id: expense.wallet_id,
            category_id: expense.category_id,
            payment_method_id: expense.payment_method_id,
            amount: expense.amount,
            description: expense.description || 'Recurring Expense',
            expense_date: todayDateString,
            is_recurring: true,
            recurring_id: expense.id
          })

        if (insertError) {
          console.error(`Error inserting expense for recurring_id ${expense.id}:`, insertError)
          errorCount++
          continue
        }

        // 6. Update last_posted to today
        const { error: updateError } = await supabase
          .from('recurring_expenses')
          .update({ last_posted: todayDateString })
          .eq('id', expense.id)

        if (updateError) {
          console.error(`Error updating last_posted for recurring_id ${expense.id}:`, updateError)
          errorCount++
        } else {
          postedCount++
        }
      } else {
        skippedCount++
      }
    }

    // 7. Return a summary of what was posted
    const summary = {
      message: 'Recurring expenses processing completed',
      date: todayDateString,
      day_of_month_processed: todayDayOfMonth,
      stats: {
        total_found: (recurringExpenses || []).length,
        posted: postedCount,
        skipped_already_posted: skippedCount,
        errors: errorCount
      }
    }

    return new Response(
      JSON.stringify(summary),
      { headers: { 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    console.error('Edge Function Error:', error)
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})

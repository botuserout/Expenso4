package com.example.expenso.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expenso.R;
import com.example.expenso.models.Expense;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses;

    public ExpenseAdapter(List<Expense> expenses) {
        this.expenses = expenses;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        if (expenses == null) return;
        Expense expense = expenses.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenses == null ? 0 : expenses.size();
    }

    public void updateExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        notifyDataSetChanged();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView expenseTitle;
        private final TextView expenseCategory;
        private final TextView expenseAmount;
        private final TextView expenseDate;
        private final View expenseIcon;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            expenseTitle = itemView.findViewById(R.id.expense_title);
            expenseCategory = itemView.findViewById(R.id.expense_category);
            expenseAmount = itemView.findViewById(R.id.expense_amount);
            expenseDate = itemView.findViewById(R.id.expense_date);
            expenseIcon = itemView.findViewById(R.id.expense_icon);
        }

        public void bind(Expense expense) {
            // Set title (use description if available, otherwise category)
            String title = expense.getDescription() != null && !expense.getDescription().isEmpty()
                    ? expense.getDescription()
                    : expense.getCategory() + " Expense";
            expenseTitle.setText(title);

            // Set category and date
            expenseCategory.setText(expense.getCategory());

            // Format amount
            expenseAmount.setText(String.format(Locale.getDefault(), "-\u20b9%.2f", expense.getAmount()));

            // Format date
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                Date date = inputFormat.parse(expense.getDate());
                expenseDate.setText(outputFormat.format(date));
            } catch (Exception e) {
                expenseDate.setText(expense.getDate());
            }

            // Set category color
            setCategoryColor(expense.getCategory());
        }

        private void setCategoryColor(String category) {
            int colorResId;
            switch (category.toLowerCase()) {
                case "food":
                    colorResId = R.color.food_color;
                    break;
                case "transport":
                case "travel":
                    colorResId = R.color.travel_color;
                    break;
                case "shopping":
                    colorResId = R.color.shopping_color;
                    break;
                case "entertainment":
                    colorResId = R.color.pastel_pink;
                    break;
                case "bills":
                    colorResId = R.color.bills_color;
                    break;
                case "health":
                    colorResId = R.color.pastel_green;
                    break;
                case "education":
                    colorResId = R.color.pastel_blue;
                    break;
                default:
                    colorResId = R.color.pastel_purple;
                    break;
            }
            expenseIcon.setBackgroundColor(itemView.getContext().getColor(colorResId));
        }
    }
}
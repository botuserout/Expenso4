package com.example.expenso.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expenso.R;
import com.example.expenso.models.SharedExpense;
import java.util.List;

public class SharedExpenseAdapter extends RecyclerView.Adapter<SharedExpenseAdapter.ViewHolder> {

    private List<SharedExpense> sharedExpenses;
    private OnSettleClickListener listener;

    public interface OnSettleClickListener {
        void onSettleClick(SharedExpense expense);
    }

    public SharedExpenseAdapter(List<SharedExpense> sharedExpenses, OnSettleClickListener listener) {
        this.sharedExpenses = sharedExpenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shared_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SharedExpense expense = sharedExpenses.get(position);
        holder.personName.setText(expense.getPersonName() != null ? expense.getPersonName() : "Friend");
        holder.amount.setText(String.format("₹%.2f", expense.getAmount()));
        holder.status.setText(expense.getStatus());

        if ("PAID".equals(expense.getStatus())) {
            holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
            holder.btnSettle.setVisibility(View.GONE);
        } else {
            holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red));
            holder.btnSettle.setVisibility(View.VISIBLE);
        }

        holder.btnSettle.setOnClickListener(v -> listener.onSettleClick(expense));
    }

    @Override
    public int getItemCount() {
        return sharedExpenses.size();
    }

    public void updateList(List<SharedExpense> newList) {
        this.sharedExpenses = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView personName, amount, status;
        Button btnSettle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            personName = itemView.findViewById(R.id.tv_shared_person);
            amount = itemView.findViewById(R.id.tv_shared_amount);
            status = itemView.findViewById(R.id.tv_shared_status);
            btnSettle = itemView.findViewById(R.id.btn_settle_up);
        }
    }
}

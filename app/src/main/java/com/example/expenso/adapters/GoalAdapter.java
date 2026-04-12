package com.example.expenso.adapters;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expenso.R;
import com.example.expenso.database.GoalDao;
import com.example.expenso.models.Goal;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private List<Goal> goalList;
    private GoalDao goalDao;
    private Context context;

    public GoalAdapter(Context context, List<Goal> goalList) {
        this.context = context;
        this.goalList = goalList;
        this.goalDao = new GoalDao(context);
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal goal = goalList.get(position);
        holder.tvName.setText(goal.getName());
        holder.tvIcon.setText(goal.getIcon());
        holder.tvAmountInfo.setText(String.format("₹%.0f / ₹%.0f", goal.getSavedAmount(), goal.getTargetAmount()));
        holder.tvPercent.setText(goal.getProgress() + "%");
        holder.progressBar.setProgress(goal.getProgress());

        holder.btnAddSavings.setOnClickListener(v -> showAddSavingsDialog(goal, position));
        holder.btnDelete.setOnClickListener(v -> showDeleteConfirmation(goal, position));
    }

    private void showAddSavingsDialog(Goal goal, int position) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Amount to add");

        new AlertDialog.Builder(context)
                .setTitle("Add Savings to " + goal.getName())
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String amountStr = input.getText().toString();
                    if (!amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        goalDao.updateSavedAmount(goal.getGoalId(), amount);
                        goal.setSavedAmount(goal.getSavedAmount() + amount);
                        notifyItemChanged(position);
                        Toast.makeText(context, "Savings added!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmation(Goal goal, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete '" + goal.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (goalDao.deleteGoal(goal.getGoalId())) {
                        goalList.remove(position);
                        notifyItemRemoved(position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return goalList.size();
    }

    public void updateGoals(List<Goal> newGoals) {
        this.goalList = newGoals;
        notifyDataSetChanged();
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIcon, tvAmountInfo, tvPercent;
        ProgressBar progressBar;
        MaterialButton btnAddSavings, btnDelete;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_goal_name);
            tvIcon = itemView.findViewById(R.id.tv_goal_icon);
            tvAmountInfo = itemView.findViewById(R.id.tv_goal_amount_info);
            tvPercent = itemView.findViewById(R.id.tv_goal_percent);
            progressBar = itemView.findViewById(R.id.goal_progress_bar);
            btnAddSavings = itemView.findViewById(R.id.btn_add_savings);
            btnDelete = itemView.findViewById(R.id.btn_delete_goal);
        }
    }
}

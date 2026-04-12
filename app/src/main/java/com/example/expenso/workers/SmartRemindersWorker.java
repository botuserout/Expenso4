package com.example.expenso.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.expenso.database.ExpenseDao;
import com.example.expenso.utils.NotificationHelper;
import com.example.expenso.utils.PinManager;

public class SmartRemindersWorker extends Worker {

    public SmartRemindersWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PinManager pinManager = new PinManager(context);
        
        if (!pinManager.isNotificationsEnabled()) return Result.success();
        
        int userId = pinManager.getCurrentUserId();
        if (userId == -1) return Result.success();

        ExpenseDao expenseDao = new ExpenseDao(context);
        double totalSpent = expenseDao.getTotalExpenses(userId);
        double totalBudget = expenseDao.getTotalBudget(userId);

        // check budget usage periodic
        if (totalBudget > 0) {
            double usage = (totalSpent / totalBudget) * 100;
            if (usage >= 90 && usage < 100) {
                NotificationHelper.showNotification(context, "🚨 Budget Reminder", "You have used over 90% of your budget. Spend wisely!");
            }
        }

        // Daily limit advice
        double remaining = Math.max(0, totalBudget - totalSpent);
        if (remaining > 0) {
            // Very simple logic: suggest spending 1/10th of remaining
            double dailyAdvice = remaining / 10;
            NotificationHelper.showNotification(context, "💡 Daily Advice", "You can spend ₹" + String.format("%.0f", dailyAdvice) + " safely today.");
        }

        return Result.success();
    }
}

package com.example.expenso.utils;

import android.content.Context;
import com.example.expenso.database.ExpenseDao;

public class SmartNotificationManager {

    public static void checkAndTriggerSmartAlert(Context context, double currentExpenseAmount, String category) {
        ExpenseDao expenseDao = new ExpenseDao(context);
        PinManager pinManager = new PinManager(context);
        int userId = pinManager.getCurrentUserId();

        double totalSpent = expenseDao.getTotalExpenses(userId);
        double totalBudget = expenseDao.getTotalBudget(userId);

        // Feature 1: Context-Aware Expense Alert
        if (totalBudget > 0) {
            double remaining = Math.max(0, totalBudget - totalSpent);
            NotificationHelper.showNotification(context, 
                "Expense Tracked: " + category,
                "You spent ₹" + String.format("%.0f", currentExpenseAmount) + ". ₹" + String.format("%.0f", remaining) + " left in your budget.");
        }

        // Feature 2: Budget Warning System
        if (totalBudget > 0) {
            double usage = (totalSpent / totalBudget) * 100;
            if (usage >= 100) {
                NotificationHelper.showNotification(context, "❌ Budget Exceeded!", "Control your spending.");
            } else if (usage >= 90) {
                NotificationHelper.showNotification(context, "🚨 Critical Usage", "You are close to exceeding your budget (90%+).");
            } else if (usage >= 70) {
                NotificationHelper.showNotification(context, "⚠️ Budget Warning", "You have used 70% of your budget.");
            }
        }

        // Feature 3: Unusual Spending Detection
        double avgExpense = expenseDao.getAverageExpense(userId);
        if (avgExpense > 0 && currentExpenseAmount > (avgExpense * 2)) {
            NotificationHelper.showNotification(context, "⚠️ Unusual Spending", "This expense is significantly higher than your usual spending.");
        }
    }
}

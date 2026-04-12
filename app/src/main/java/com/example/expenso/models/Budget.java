package com.example.expenso.models;

public class Budget {
    private int budgetId;
    private int userId;
    private String category;
    private double limitAmount;
    private String period; // "monthly", "weekly", "daily", etc.

    public Budget() {
    }

    public Budget(int userId, String category, double limitAmount, String period) {
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
    }

    public Budget(int budgetId, int userId, String category, double limitAmount, String period) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
    }

    // Getters and setters
    public int getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(int budgetId) {
        this.budgetId = budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(double limitAmount) {
        this.limitAmount = limitAmount;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
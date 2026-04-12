package com.example.expenso.models;

public class Expense {
    private int expenseId;
    private int userId;
    private double amount;
    private String category;
    private String date;
    private String description;

    // Default constructor
    public Expense() {
    }

    // Constructor with all fields except ID (for new expenses)
    public Expense(int userId, double amount, String category, String date, String description) {
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    // Constructor with all fields (for existing expenses)
    public Expense(int expenseId, int userId, double amount, String category, String date, String description) {
        this.expenseId = expenseId;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    // Getters and Setters
    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "expenseId=" + expenseId +
                ", userId=" + userId +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", date='" + date + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

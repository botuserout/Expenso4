package com.example.expenso.models;

public class Goal {
    private int goalId;
    private int userId;
    private String name;
    private double targetAmount;
    private double savedAmount;
    private String icon;

    public Goal() {}

    public Goal(int userId, String name, double targetAmount, double savedAmount, String icon) {
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
        this.icon = icon;
    }

    public int getGoalId() { return goalId; }
    public void setGoalId(int goalId) { this.goalId = goalId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getProgress() {
        if (targetAmount <= 0) return 0;
        int progress = (int) ((savedAmount / targetAmount) * 100);
        return Math.min(progress, 100);
    }
}

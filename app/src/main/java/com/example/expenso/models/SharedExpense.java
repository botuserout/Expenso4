package com.example.expenso.models;

public class SharedExpense {
    private int sharedId;
    private int expenseId;
    private int payerId;
    private int owesToId;
    private double amount;
    private String status; // "PENDING" or "PAID"

    // For display convenience
    private String personName; 

    public SharedExpense() {}

    public int getSharedId() { return sharedId; }
    public void setSharedId(int sharedId) { this.sharedId = sharedId; }

    public int getExpenseId() { return expenseId; }
    public void setExpenseId(int expenseId) { this.expenseId = expenseId; }

    public int getPayerId() { return payerId; }
    public void setPayerId(int payerId) { this.payerId = payerId; }

    public int getOwesToId() { return owesToId; }
    public void setOwesToId(int owesToId) { this.owesToId = owesToId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
}

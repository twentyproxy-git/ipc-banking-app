package com.example.ipcbanking.models;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.Date;

public class AccountItem implements Serializable {

    private String id; // ID document

    @PropertyName("account_number")
    private String accountNumber;

    @PropertyName("account_type")
    private String accountType;

    @PropertyName("balance")
    private double balance;

    @PropertyName("owner_id")
    private String ownerId;

    // [SỬA LỖI Ở ĐÂY]
    // Đổi từ Timestamp sang Date để không bị lỗi Serializable khi chuyển màn hình
    @PropertyName("created_at")
    private Date createdAt;

    // Các trường khác (nếu có) như profit_rate, monthly_payment...
    @PropertyName("profit_rate")
    private double profitRate;

    @PropertyName("monthly_payment")
    private double monthlyPayment;

    public AccountItem() { }

    // --- GETTER & SETTER ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("account_number")
    public String getAccountNumber() { return accountNumber; }
    @PropertyName("account_number")
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    @PropertyName("account_type")
    public String getAccountType() { return accountType; }
    @PropertyName("account_type")
    public void setAccountType(String accountType) { this.accountType = accountType; }

    @PropertyName("balance")
    public double getBalance() { return balance; }
    @PropertyName("balance")
    public void setBalance(double balance) { this.balance = balance; }

    @PropertyName("owner_id")
    public String getOwnerId() { return ownerId; }
    @PropertyName("owner_id")
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    // [CẬP NHẬT GETTER/SETTER]
    @PropertyName("created_at")
    public Date getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @PropertyName("profit_rate")
    public double getProfitRate() { return profitRate; }
    @PropertyName("profit_rate")
    public void setProfitRate(double profitRate) { this.profitRate = profitRate; }

    @PropertyName("monthly_payment")
    public double getMonthlyPayment() { return monthlyPayment; }
    @PropertyName("monthly_payment")
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }
}
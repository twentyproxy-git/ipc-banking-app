package com.example.ipcbanking.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.Date;

public class TransactionItem implements Serializable {
    private String id;

    @PropertyName("sender_id")
    private String senderId;

    @PropertyName("sender_account")
    private String senderAccount;

    @PropertyName("receiver_account")
    private String receiverAccount;

    @PropertyName("receiver_name")
    private String receiverName;

    @PropertyName("amount")
    private double amount;

    @PropertyName("message")
    private String message;

    @PropertyName("type")
    private String type;

    @PropertyName("status")
    private String status;

    @PropertyName("created_at")
    private Date createdAt;

    public TransactionItem() { }

    public TransactionItem(String senderId, String senderAccount, String receiverAccount, String receiverName, double amount, String message, String type, String status, Date createdAt) {
        this.senderId = senderId;
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.receiverName = receiverName;
        this.amount = amount;
        this.message = message;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    // --- GETTERS & SETTERS ---

    @Exclude
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    @PropertyName("sender_id")
    public String getSenderId() { return senderId; }
    @PropertyName("sender_id")
    public void setSenderId(String senderId) { this.senderId = senderId; }

    @PropertyName("sender_account")
    public String getSenderAccount() { return senderAccount; }
    @PropertyName("sender_account")
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }

    @PropertyName("receiver_account")
    public String getReceiverAccount() { return receiverAccount; }
    @PropertyName("receiver_account")
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }

    @PropertyName("receiver_name")
    public String getReceiverName() { return receiverName; }
    @PropertyName("receiver_name")
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    @PropertyName("amount")
    public double getAmount() { return amount; }
    @PropertyName("amount")
    public void setAmount(double amount) { this.amount = amount; }

    @PropertyName("message")
    public String getMessage() { return message; }
    @PropertyName("message")
    public void setMessage(String message) { this.message = message; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("created_at")
    public Date getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
package com.example.ipcbanking.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.Date;

public class TransactionItem implements Serializable {
    private String id;

    @PropertyName("type")
    private String type;

    @PropertyName("sender_account")
    private String senderAccount;

    @PropertyName("sender_name")
    private String senderName;

    @PropertyName("receiver_account")
    private String receiverAccount;

    @PropertyName("receiver_name")
    private String receiverName;

    @PropertyName("counterparty_bank")
    private String counterpartyBank;

    @PropertyName("amount")
    private double amount;

    @PropertyName("message")
    private String message;

    @PropertyName("status")
    private String status;

    @PropertyName("created_at")
    private Date createdAt;

    public TransactionItem() {}

    public TransactionItem(String type,
                           String senderAccount, String senderName,
                           String receiverAccount, String receiverName,
                           String counterpartyBank,
                           double amount,
                           String message,
                           String status,
                           Date createdAt) {
        this.type = type;
        this.senderAccount = senderAccount;
        this.senderName = senderName;
        this.receiverAccount = receiverAccount;
        this.receiverName = receiverName;
        this.counterpartyBank = counterpartyBank;
        this.amount = amount;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("sender_account")
    public String getSenderAccount() { return senderAccount; }
    @PropertyName("sender_account")
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }

    @PropertyName("sender_name")
    public String getSenderName() { return senderName; }
    @PropertyName("sender_name")
    public void setSenderName(String senderName) { this.senderName = senderName; }

    @PropertyName("receiver_account")
    public String getReceiverAccount() { return receiverAccount; }
    @PropertyName("receiver_account")
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }

    @PropertyName("receiver_name")
    public String getReceiverName() { return receiverName; }
    @PropertyName("receiver_name")
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    @PropertyName("counterparty_bank")
    public String getCounterpartyBank() { return counterpartyBank; }
    @PropertyName("counterparty_bank")
    public void setCounterpartyBank(String counterpartyBank) { this.counterpartyBank = counterpartyBank; }

    @PropertyName("amount")
    public double getAmount() { return amount; }
    @PropertyName("amount")
    public void setAmount(double amount) { this.amount = amount; }

    @PropertyName("message")
    public String getMessage() { return message; }
    @PropertyName("message")
    public void setMessage(String message) { this.message = message; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("created_at")
    public Date getCreatedAt() { return createdAt; }
    @PropertyName("created_at")
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}

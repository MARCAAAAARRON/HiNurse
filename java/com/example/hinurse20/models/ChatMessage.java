package com.example.hinurse20.models;

import java.util.Date;

public class ChatMessage {
    private String messageId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String message;
    private String messageType; // "text", "image", "file"
    private boolean isFromUser;
    private Date timestamp;
    private boolean isRead;
    private String imageUrl;
    private String status; // "sending", "sent", "read"
    private Date sentAt;
    private Date readAt;

    // Default constructor for Firestore
    public ChatMessage() {}

    public ChatMessage(String messageId, String chatId, String senderId, String senderName, 
                      String message, boolean isFromUser) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.messageType = "text";
        this.isFromUser = isFromUser;
        this.timestamp = new Date();
        this.isRead = false;
        this.status = "sending";
        this.sentAt = null;
        this.readAt = null;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public boolean isFromUser() { return isFromUser; }
    public void setFromUser(boolean fromUser) { isFromUser = fromUser; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }

    public Date getReadAt() { return readAt; }
    public void setReadAt(Date readAt) { this.readAt = readAt; }
}


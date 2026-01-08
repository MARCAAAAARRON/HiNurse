package com.example.hinurse20.models;

import java.util.Date;
import java.util.List;

public class MedicalRecord {
    private String recordId;
    private String userId;
    private String fullName; // Patient's full name
    private String recordType; // "appointment", "vaccination", "medication", "allergy", "condition"
    private String title;
    private String description;
    private String nurseId;
    private String nurseName;
    private Date recordDate;
    private List<String> attachments; // URLs to files/images
    private String status; // "active", "inactive", "resolved"
    private Date createdAt;
    private Date updatedAt;

    // Default constructor for Firestore
    public MedicalRecord() {}

    public MedicalRecord(String recordId, String userId, String recordType, String title, 
                        String description, String nurseId, String nurseName) {
        this.recordId = recordId;
        this.userId = userId;
        this.recordType = recordType;
        this.title = title;
        this.description = description;
        this.nurseId = nurseId;
        this.nurseName = nurseName;
        this.recordDate = new Date();
        this.status = "active";
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNurseId() { return nurseId; }
    public void setNurseId(String nurseId) { this.nurseId = nurseId; }

    public String getNurseName() { return nurseName; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }

    public Date getRecordDate() { return recordDate; }
    public void setRecordDate(Date recordDate) { this.recordDate = recordDate; }

    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}


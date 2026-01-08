package com.example.hinurse20.models;

import java.util.Date;

public class Appointment {
    private String appointmentId;
    private String userId;
    private String nurseId;
    private String nurseName;
    private Date appointmentDate;
    private String timeSlot;
    private String reason;
    private String status; // "scheduled", "completed", "cancelled"
    private String notes;
    private Date createdAt;
    private Date updatedAt;

    // Default constructor for Firestore
    public Appointment() {}

    public Appointment(String appointmentId, String userId, String nurseId, String nurseName, 
                      Date appointmentDate, String timeSlot, String reason) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.nurseId = nurseId;
        this.nurseName = nurseName;
        this.appointmentDate = appointmentDate;
        this.timeSlot = timeSlot;
        this.reason = reason;
        this.status = "scheduled";
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNurseId() { return nurseId; }
    public void setNurseId(String nurseId) { this.nurseId = nurseId; }

    public String getNurseName() { return nurseName; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }

    public Date getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(Date appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}


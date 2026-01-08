package com.example.hinurse20.models;

import java.util.Date;
import java.util.List;

public class SymptomAnalysis {
    private String analysisId;
    private String userId;
    private List<String> symptoms;
    private String severity; // "low", "moderate", "high", "urgent"
    private String urgency; // "immediate", "within_24h", "within_week", "routine"
    private String recommendation; // "self_care", "schedule_appointment", "urgent_care", "emergency"
    private String selfCareTips;
    private String suggestedTimeframe;
    private boolean requiresAppointment;
    private String priorityLevel; // "1" (highest) to "5" (lowest)
    private Date createdAt;
    private Date updatedAt;

    // Default constructor for Firestore
    public SymptomAnalysis() {}

    public SymptomAnalysis(String analysisId, String userId, List<String> symptoms) {
        this.analysisId = analysisId;
        this.userId = userId;
        this.symptoms = symptoms;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getSelfCareTips() { return selfCareTips; }
    public void setSelfCareTips(String selfCareTips) { this.selfCareTips = selfCareTips; }

    public String getSuggestedTimeframe() { return suggestedTimeframe; }
    public void setSuggestedTimeframe(String suggestedTimeframe) { this.suggestedTimeframe = suggestedTimeframe; }

    public boolean isRequiresAppointment() { return requiresAppointment; }
    public void setRequiresAppointment(boolean requiresAppointment) { this.requiresAppointment = requiresAppointment; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public String getSeverityDisplayText() {
        switch (severity) {
            case "low": return "Low";
            case "moderate": return "Moderate";
            case "high": return "High";
            case "urgent": return "Urgent";
            default: return "Unknown";
        }
    }

    public String getUrgencyDisplayText() {
        switch (urgency) {
            case "immediate": return "Immediate attention needed";
            case "within_24h": return "Within 24 hours";
            case "within_week": return "Within a week";
            case "routine": return "Routine care";
            default: return "Unknown";
        }
    }

    public String getRecommendationDisplayText() {
        switch (recommendation) {
            case "self_care": return "Self-care recommended";
            case "schedule_appointment": return "Schedule an appointment";
            case "urgent_care": return "Seek urgent care";
            case "emergency": return "Go to emergency room";
            default: return "Unknown";
        }
    }
}






















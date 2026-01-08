package com.example.hinurse20.models;

import java.util.Date;

public class User {
    private String userId;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String studentId;
    private String phoneNumber;
    private String photoUrl;
    private String photoBase64; // fallback when Storage is unavailable
    private Date dateOfBirth;
    private String emergencyContact;
    private String emergencyPhone;
    private Date createdAt;
    private Date updatedAt;
    private String role; // e.g., "patient", "nurse"

    // Default constructor for Firestore
    public User() {}

    public User(String userId, String email, String firstName, String lastName, String studentId) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.studentId = studentId;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(middleName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        return fullName.toString().trim();
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}



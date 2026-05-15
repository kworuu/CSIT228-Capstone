package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents a person sheltered at an evacuation center.
 * Maps to the {@code evacuees} table.
 */
public class Evacuee {

    private Long id;
    private String fullNameEnc;
    private String contactEnc;
    private String barangay;
    private String photoPath;
    private Long evacuationCenterId;
    private String notes;
    private LocalDateTime createdAt;

    public Evacuee() {}

    public Evacuee(Long id, String fullNameEnc, String contactEnc, String barangay,
                   String photoPath, Long evacuationCenterId,
                   String notes, LocalDateTime createdAt) {
        this.id = id;
        this.fullNameEnc = fullNameEnc;
        this.contactEnc = contactEnc;
        this.barangay = barangay;
        this.photoPath = photoPath;
        this.evacuationCenterId = evacuationCenterId;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullNameEnc() { return fullNameEnc; }
    public void setFullNameEnc(String fullNameEnc) { this.fullNameEnc = fullNameEnc; }
    public String getContactEnc() { return contactEnc; }
    public void setContactEnc(String contactEnc) { this.contactEnc = contactEnc; }
    public String getBarangay() { return barangay; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public Long getEvacuationCenterId() { return evacuationCenterId; }
    public void setEvacuationCenterId(Long evacuationCenterId) { this.evacuationCenterId = evacuationCenterId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
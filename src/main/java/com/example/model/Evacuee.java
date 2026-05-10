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
    private Long familyGroupId;
    private VerificationStatus verificationStatus;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private String notes;
    private LocalDateTime createdAt;

    public Evacuee() {
        this.verificationStatus = VerificationStatus.PENDING;
    }

    public Evacuee(Long id, String fullNameEnc, String contactEnc, String barangay,
                   String photoPath, Long evacuationCenterId, Long familyGroupId,
                   VerificationStatus verificationStatus, Long verifiedBy,
                   LocalDateTime verifiedAt, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.fullNameEnc = fullNameEnc;
        this.contactEnc = contactEnc;
        this.barangay = barangay;
        this.photoPath = photoPath;
        this.evacuationCenterId = evacuationCenterId;
        this.familyGroupId = familyGroupId;
        this.verificationStatus = verificationStatus;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = verifiedAt;
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
    public Long getFamilyGroupId() { return familyGroupId; }
    public void setFamilyGroupId(Long familyGroupId) { this.familyGroupId = familyGroupId; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isPending() { return verificationStatus == VerificationStatus.PENDING; }
    public boolean isVerified() { return verificationStatus == VerificationStatus.VERIFIED; }
}
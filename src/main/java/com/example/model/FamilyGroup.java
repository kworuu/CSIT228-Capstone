package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents a family unit registered together at an evacuation center.
 * Maps to the {@code family_groups} table.
 */
public class FamilyGroup {

    private Long id;
    private String headOfFamily;
    private int memberCount;
    private Long evacuationCenterId;
    private LocalDateTime createdAt;

    public FamilyGroup() {}

    public FamilyGroup(Long id, String headOfFamily, int memberCount,
                       Long evacuationCenterId, LocalDateTime createdAt) {
        this.id = id;
        this.headOfFamily = headOfFamily;
        this.memberCount = memberCount;
        this.evacuationCenterId = evacuationCenterId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHeadOfFamily() { return headOfFamily; }
    public void setHeadOfFamily(String headOfFamily) { this.headOfFamily = headOfFamily; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public Long getEvacuationCenterId() { return evacuationCenterId; }
    public void setEvacuationCenterId(Long evacuationCenterId) { this.evacuationCenterId = evacuationCenterId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return headOfFamily + "'s family (" + memberCount + ")"; }
}
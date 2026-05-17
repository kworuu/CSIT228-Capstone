package com.example.model;
/*
* This is redundant I will temprarily put it here
* */

public class EvacueeRecord {

    private final String id;
    private final String fullName;
    private final String assignedCenter;
    private final String barangay;
    private final String registeredAt;

    public EvacueeRecord(String id,
                         String fullName,
                         String assignedCenter,
                         String barangay,
                         String registeredAt) {
        this.id             = id;
        this.fullName       = fullName;
        this.assignedCenter = assignedCenter;
        this.barangay       = barangay;
        this.registeredAt   = registeredAt;
    }

    public String getId()             { return id; }
    public String getFullName()       { return fullName; }
    public String getAssignedCenter() { return assignedCenter; }
    public String getBarangay()       { return barangay; }
    public String getRegisteredAt()   { return registeredAt; }
    public String getStatus()         { return "Active"; }
}

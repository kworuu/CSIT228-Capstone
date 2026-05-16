package com.example.model;

import java.time.LocalDateTime;

public record Evacuee(
        long id,
        String fullNameEnc,
        String contactEnc,
        String barangay,
        String photoPath,
        long evacuationCenterId,
        Long userId,
        String notes,
        LocalDateTime createdAt
) {}
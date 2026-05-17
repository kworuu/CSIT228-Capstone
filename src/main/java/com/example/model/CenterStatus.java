package com.example.model;

import java.time.LocalDateTime;

public record CenterStatus(
        long id,
        long centerId,
        String structuralStatus,
        String structuralNotes,
        String event,
        LocalDateTime reportedAt
) {}

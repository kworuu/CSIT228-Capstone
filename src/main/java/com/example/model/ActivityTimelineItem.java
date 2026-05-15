package com.example.model;

public record ActivityTimelineItem(
        String timestamp,
        String action,
        String target,
        String center,
        String performedBy
) {}
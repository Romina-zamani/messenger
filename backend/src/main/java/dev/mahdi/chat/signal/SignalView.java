package dev.mahdi.chat.signal;

import java.time.Instant;

public record SignalView(Long id, String name, String category, String unit, double value,
                         SignalStatus status, Instant updatedAt, long version) {}

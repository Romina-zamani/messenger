package dev.mahdi.chat.signal;

import java.time.Instant;

public record SignalUpdate(String type, Long id, double value, SignalStatus status, Instant updatedAt, long version) {
  public SignalUpdate(Long id, double value, SignalStatus status, Instant updatedAt, long version) {
    this("SIGNAL_UPDATED", id, value, status, updatedAt, version);
  }
}

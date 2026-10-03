package dev.mahdi.chat.signal;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "signals")
class SignalEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
  @Column(nullable = false, unique = true, length = 80) String name;
  @Column(nullable = false, length = 24) String category;
  @Column(nullable = false, length = 12) String unit;
  @Column(name = "signal_value", nullable = false) double value;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) SignalStatus status;
  @Column(name = "updated_at", nullable = false) Instant updatedAt;
  @Version @Column(nullable = false) long version;

  protected SignalEntity() {}
  SignalEntity(String name, String category, String unit, double value, SignalStatus status, Instant updatedAt) {
    this.name = name; this.category = category; this.unit = unit; this.value = value; this.status = status; this.updatedAt = updatedAt;
  }
}

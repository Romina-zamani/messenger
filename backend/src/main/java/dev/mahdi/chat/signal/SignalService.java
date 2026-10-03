package dev.mahdi.chat.signal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
public class SignalService {
  private static final String[][] TYPES = {{"CPU", "%"}, {"MEMORY", "%"}, {"LATENCY", "ms"}, {"THROUGHPUT", "req/s"}, {"TEMPERATURE", "°C"}, {"AVAILABILITY", "%"}};
  private final SignalRepository repository;
  private final Clock clock = Clock.systemUTC();
  SignalService(SignalRepository repository) { this.repository = repository; }

  @Transactional public void seedIfEmpty() {
    if (repository.count() != 0) return;
    Instant now = clock.instant();
    List<SignalEntity> seeds = new ArrayList<>(1000);
    for (int i = 1; i <= 1000; i++) {
      String[] type = TYPES[(i - 1) % TYPES.length];
      double value = initialValue(type[0], i);
      seeds.add(new SignalEntity(type[0] + " " + String.format("%04d", i), type[0], type[1], value, status(type[0], value), now));
    }
    repository.saveAll(seeds);
  }

  @Transactional(readOnly = true) public List<SignalView> snapshot() { return repository.findAll().stream().map(this::view).toList(); }

  @Transactional public SignalUpdate updateOne(long id) {
    SignalEntity signal = repository.findById(id).orElseThrow();
    signal.value = nextValue(signal.category, signal.value, id + signal.version);
    signal.status = status(signal.category, signal.value);
    signal.updatedAt = clock.instant();
    repository.flush();
    return new SignalUpdate(signal.id, signal.value, signal.status, signal.updatedAt, signal.version);
  }

  private SignalView view(SignalEntity e) { return new SignalView(e.id, e.name, e.category, e.unit, e.value, e.status, e.updatedAt, e.version); }
  private static double initialValue(String category, int seed) { return switch (category) { case "LATENCY" -> 20 + seed % 180; case "THROUGHPUT" -> 300 + seed % 700; case "TEMPERATURE" -> 35 + seed % 45; case "AVAILABILITY" -> 99 + (seed % 100) / 100.0; default -> 20 + seed % 65; }; }
  private static double nextValue(String category, double current, long seed) { double step = ((seed * 37) % 21 - 10) / 3.0; double max = category.equals("THROUGHPUT") ? 1500 : category.equals("LATENCY") ? 600 : category.equals("AVAILABILITY") ? 100 : 120; double min = category.equals("AVAILABILITY") ? 95 : 0; return Math.round(Math.max(min, Math.min(max, current + step)) * 10.0) / 10.0; }
  private static SignalStatus status(String category, double value) {
    if (category.equals("AVAILABILITY")) return value < 97 ? SignalStatus.CRITICAL : value < 99 ? SignalStatus.WARNING : SignalStatus.HEALTHY;
    if (category.equals("LATENCY")) return value > 400 ? SignalStatus.CRITICAL : value > 250 ? SignalStatus.WARNING : SignalStatus.HEALTHY;
    if (category.equals("THROUGHPUT")) return value < 150 ? SignalStatus.CRITICAL : value < 300 ? SignalStatus.WARNING : SignalStatus.HEALTHY;
    return value > 95 ? SignalStatus.CRITICAL : value > 80 ? SignalStatus.WARNING : SignalStatus.HEALTHY;
  }
}

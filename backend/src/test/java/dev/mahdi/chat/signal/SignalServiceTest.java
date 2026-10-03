package dev.mahdi.chat.signal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SignalServiceTest {
  @Autowired SignalService service;
  @Autowired SignalRepository repository;

  @Test void seedsExactlyOneThousandSignalsIdempotently() {
    service.seedIfEmpty();
    service.seedIfEmpty();
    assertEquals(1000, repository.count());
    assertEquals(1000, service.snapshot().size());
  }

  @Test void updateChangesExactlyOneSignal() {
    service.seedIfEmpty();
    var before = service.snapshot();
    SignalUpdate changed = service.updateOne(42L);
    var after = service.snapshot();

    assertEquals(42L, changed.id());
    assertEquals(1, after.stream().filter(signal -> {
      var previous = before.stream().filter(item -> item.id().equals(signal.id())).findFirst().orElseThrow();
      return signal.version() > previous.version();
    }).count());
  }
}

package dev.mahdi.chat.signal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "signals.simulator.enabled", havingValue = "true", matchIfMissing = true)
class SignalSimulator {
  private final SignalService service;
  private final Optional<SignalRedisBridge> redis;
  SignalSimulator(SignalService service, Optional<SignalRedisBridge> redis) { this.service = service; this.redis = redis; }
  @Scheduled(fixedDelayString = "${signals.simulator.interval-ms:100}", initialDelayString = "${signals.simulator.initial-delay-ms:1000}")
  public void tick() { SignalUpdate update=service.updateOne(ThreadLocalRandom.current().nextLong(1, 1001));redis.ifPresent(bridge->bridge.publish(update)); }
}

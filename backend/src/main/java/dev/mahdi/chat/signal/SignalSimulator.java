package dev.mahdi.chat.signal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "signals.simulator.enabled", havingValue = "true", matchIfMissing = true)
class SignalSimulator {
  private final SignalService service;
  private final SignalRedisBridge redis;
  SignalSimulator(SignalService service, SignalRedisBridge redis) { this.service = service; this.redis = redis; }
  @Scheduled(fixedDelayString = "${signals.simulator.interval-ms:100}", initialDelayString = "${signals.simulator.initial-delay-ms:1000}")
  public void tick() { redis.publish(service.updateOne(ThreadLocalRandom.current().nextLong(1, 1001))); }
}

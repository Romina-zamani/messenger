package dev.mahdi.chat.signal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class SignalSeeder implements ApplicationRunner {
  private final SignalService service;
  SignalSeeder(SignalService service) { this.service = service; }
  @Override public void run(ApplicationArguments args) { service.seedIfEmpty(); }
}

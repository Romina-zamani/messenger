package dev.mahdi.chat.signal;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SignalFeedState {
  private final AtomicReference<String> state = new AtomicReference<>("DEGRADED");
  public String get() { return state.get(); }
  public void live() { state.set("LIVE"); }
  public void degraded() { state.set("DEGRADED"); }
}

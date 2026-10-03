package dev.mahdi.chat.signal;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/signals")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*", "http://127.0.0.1:*", "https://127.0.0.1:*"})
public class SignalController {
  private final SignalService service;
  SignalController(SignalService service) { this.service = service; }
  @GetMapping public List<SignalView> snapshot() { return service.snapshot(); }
}

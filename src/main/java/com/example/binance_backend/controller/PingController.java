package com.example.binance_backend.controller;

import java.util.Map;
import java.util.Collections;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

  @GetMapping("/ping")
  public Map<String,String> ping() {
    // returns { "message": "pong" }
    return Collections.singletonMap("message", "pong");
  }
}

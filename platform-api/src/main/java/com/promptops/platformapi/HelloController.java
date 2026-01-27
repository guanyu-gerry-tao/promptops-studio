package com.promptops.platformapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A health check endpoint. My first Controller.
 */
@RestController
public class HelloController {

  @GetMapping("/hello")
  public String hello() {
    return "Hello World! PromptOps Studio platform-api is running!";
  }
}

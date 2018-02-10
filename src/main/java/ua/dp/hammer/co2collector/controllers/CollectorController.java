package ua.dp.hammer.co2collector.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class CollectorController {
   @GetMapping(path = "/alarm")
   public String getStatistics() {
      return "aaa";
   }
}

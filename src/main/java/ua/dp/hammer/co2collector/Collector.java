package ua.dp.hammer.co2collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Collector {

   public static void main(String[] args) throws Exception {
      SpringApplication.run(Collector.class, args);
   }
}

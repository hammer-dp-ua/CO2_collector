import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
@RequestMapping("/")
public class CollectorMain {

   @GetMapping(path = "/alarm")
   public String getStatistics() {
      return "aaa";
   }

   public static void main(String[] args) throws Exception {
      SpringApplication.run(CollectorMain.class, args);
   }
}

package ua.dp.hammer.co2collector.controllers;

import jssc.SerialPortList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class CollectorController {

   @GetMapping(path = "/alarm")
   public String getStatistics() {
      return "aaa";
   }

   @GetMapping(path = "/ports")
   public @ResponseBody String[] getAvailablePorts() {
      return SerialPortList.getPortNames();
   }
}

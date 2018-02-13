package ua.dp.hammer.co2collector.controllers;

import jssc.SerialPortList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ua.dp.hammer.co2collector.beans.Co2SensorBean;
import ua.dp.hammer.co2collector.models.Co2Data;

import java.util.Set;

@RestController
@RequestMapping("/")
public class CollectorController {

   @Autowired
   private Co2SensorBean co2SensorBean;

   @GetMapping(path = "/getForThePeriod")
   public Set<Co2Data> getDataForThePeriod(@RequestParam("period") Period period) {
      Set<Co2Data> co2Data = null;

      if (period == Period.HOUR) {
         co2Data = co2SensorBean.getForHour();
      }
      return co2Data;
   }

   @GetMapping(path = "/ports")
   public @ResponseBody String[] getAvailablePorts() {
      return SerialPortList.getPortNames();
   }

   private enum Period {
      HOUR, DAY, MONTH, YEAR, ALL
   }
}

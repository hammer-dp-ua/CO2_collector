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

   private Co2SensorBean co2SensorBean;

   @GetMapping(path = "/getForThePeriod")
   public long[][] getDataForThePeriod(@RequestParam("period") Period period) {
      Set<Co2Data> co2Data = null;

      if (period == Period.HOUR) {
         co2Data = co2SensorBean.getForHour();
      } else if (period == Period.DAY) {
         co2Data = co2SensorBean.getForDay();
      } else if (period == Period.WEEK) {
         co2Data = co2SensorBean.getForWeek();
      } else if (period == Period.MONTH) {
         co2Data = co2SensorBean.getForMonth();
      } else if (period == Period.YEAR) {
         co2Data = co2SensorBean.getForYear();
      } else { // Period.ALL
         co2Data = co2SensorBean.getAll();
      }
      return co2SensorBean.convertToArray(co2Data);
   }

   @GetMapping(path = "/ports")
   public @ResponseBody String[] getAvailablePorts() {
      return SerialPortList.getPortNames();
   }

   private enum Period {
      HOUR, DAY, WEEK, MONTH, YEAR, ALL
   }

   @Autowired
   private void setCo2SensorBean(Co2SensorBean co2SensorBean) {
      this.co2SensorBean = co2SensorBean;
   }
}

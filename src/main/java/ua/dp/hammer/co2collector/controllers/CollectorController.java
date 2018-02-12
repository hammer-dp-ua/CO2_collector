package ua.dp.hammer.co2collector.controllers;

import jssc.SerialPortList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ua.dp.hammer.co2collector.beans.Co2SensorBean;
import ua.dp.hammer.co2collector.models.Co2Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedSet;
import java.util.TreeSet;

@RestController
@RequestMapping("/")
public class CollectorController {

   public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

   @Autowired
   private Co2SensorBean co2SensorBean;

   @GetMapping(path = "/getForThePeriod")
   public String getDataForThePeriod(@RequestParam("period") Period period) {
      LocalDateTime currentDateTime = LocalDateTime.now();
      Path lastFilePath = FileSystems.getDefault().getPath(co2SensorBean.getDataDirLocation(),
            currentDateTime.format(Co2SensorBean.DATA_FILE_NAME_FORMATTER));
      LocalDateTime startDateTime = null;

      if (period == Period.HOUR) {
         startDateTime = currentDateTime.minusHours(1);
         String startDateTimeShort = startDateTime.format(TIME_FORMATTER);
         boolean getDataFromPrevFile = false;
         SortedSet<Co2Data> co2Data = new TreeSet<>();

         try (BufferedReader br = Files.newBufferedReader(lastFilePath)) {
            String line = br.readLine();

            if (line == null) {
               // do something
            }

            if (line.startsWith(startDateTimeShort)) {
               getDataFromPrevFile = true;

            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return "aaa";
   }

   @GetMapping(path = "/ports")
   public @ResponseBody String[] getAvailablePorts() {
      return SerialPortList.getPortNames();
   }

   private enum Period {
      HOUR, DAY, MONTH, YEAR, ALL
   }
}

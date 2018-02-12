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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/")
public class CollectorController {

   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
   private static final Pattern SAVED_DATA_PATTERN = Pattern.compile("^(\\d{2}:\\d{2}):\\d{2} (\\d{3,4})$");

   @Autowired
   private Co2SensorBean co2SensorBean;

   @GetMapping(path = "/getForThePeriod")
   public Set<Co2Data> getDataForThePeriod(@RequestParam("period") Period period) {
      LocalDateTime currentDateTime = LocalDateTime.now();
      String lastFileName = currentDateTime.format(Co2SensorBean.DATA_FILE_NAME_FORMATTER);
      Path lastFilePath = FileSystems.getDefault().getPath(co2SensorBean.getDataDirLocation(), lastFileName);
      LocalDateTime startDateTime = null;
      Set<Co2Data> co2Data = new LinkedHashSet<>();

      if (period == Period.HOUR) {
         startDateTime = currentDateTime.minusHours(1);
         String startDateTimeShort = startDateTime.format(TIME_FORMATTER);
         boolean getDataFromPrevFile = false;

         try (BufferedReader br = Files.newBufferedReader(lastFilePath)) {
            String line = br.readLine();

            if (line == null) {
               // do something
            }

            if (line.startsWith(startDateTimeShort)) {
               getDataFromPrevFile = true;
               co2Data.add(createCo2DataElement(lastFileName, line));
            }

            while ((line = br.readLine()) != null) {
               if (line.startsWith(startDateTimeShort)) {
                  co2Data.add(createCo2DataElement(lastFileName, line));
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return co2Data;
   }

   @GetMapping(path = "/ports")
   public @ResponseBody String[] getAvailablePorts() {
      return SerialPortList.getPortNames();
   }

   private Co2Data createCo2DataElement(String date, String line) {
      if (line == null) {
         return null;
      }

      Matcher matcher = SAVED_DATA_PATTERN.matcher(line);
      if (matcher.find()) {
         String time = matcher.group(1);
         String value = matcher.group(2);
         return new Co2Data(date + "T" + time, Integer.valueOf(value));
      }
      return null;
   }

   private enum Period {
      HOUR, DAY, MONTH, YEAR, ALL
   }
}

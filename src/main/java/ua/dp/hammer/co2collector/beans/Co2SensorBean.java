package ua.dp.hammer.co2collector.beans;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import ua.dp.hammer.co2collector.models.Co2Data;
import ua.dp.hammer.co2collector.utils.CRC16Modbus;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Co2SensorBean {

   private static final Logger LOGGER = LogManager.getLogger(Co2SensorBean.class);

   private static final int[] READ_CO2 = new int[]{0xFE, 0x04, 0x00, 0x03, 0x00, 0x01, 0xD5, 0xC5};
   private static final DateTimeFormatter DATA_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
   private static final DateTimeFormatter DATA_TIME_FORMATTER_SHORT = DateTimeFormatter.ofPattern("HH:mm");
   private static final DateTimeFormatter DATA_FILE_NAME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
   private static final Pattern SAVED_DATA_PATTERN = Pattern.compile("^(\\d{2}:\\d{2}):\\d{2} (\\d{3,4})$");
   private static final int READ_CO2_VALUE_PERIOD_MS = 10000;
   private static final int SAVE_CO2_VALUE_PERIOD_MS = 30000;

   static final Comparator FILES_COMPARATOR = new Comparator<Path>() {
      @Override
      public int compare(Path o1, Path o2) {
         return o1.getFileName().toString().compareTo(o2.getFileName().toString());
      }
   };

   private Environment environment;

   private ConcurrentMap<DeferredResult<long[]>, Long> lastDateDeferredResults = new ConcurrentHashMap<>();
   private SerialPort serialPort = null;
   private String dataDirLocation;
   private String comPort;
   private int skipCounter;

   @PostConstruct
   public void init() {
      dataDirLocation = environment.getRequiredProperty("dataDirLocation");
      comPort = environment.getRequiredProperty("comPort");

      if (READ_CO2_VALUE_PERIOD_MS > SAVE_CO2_VALUE_PERIOD_MS) {
         throw new IllegalArgumentException("Saving CO2 value is more frequently than reading");
      }

      openPort();
   }

   @PreDestroy
   public void deInit() {
      if (serialPort != null) {
         try {
            serialPort.closePort();
            serialPort = null;
         } catch (SerialPortException ex) {
            LOGGER.error(ex);
         }
      }
   }

   @Scheduled(fixedRate = READ_CO2_VALUE_PERIOD_MS)
   public void readCo2Value() {
      if (serialPort != null && !serialPort.isOpened()) {
         LOGGER.error("Can't read value. COM port is closed");
      }

      skipCounter++;
      if (serialPort != null && serialPort.isOpened()) {
         try {
            serialPort.writeIntArray(READ_CO2);
         } catch (SerialPortException ex) {
            LOGGER.error(ex);
         }
      }
   }

   public Set<Co2Data> getForHour() {
      LocalDateTime currentDateTime = LocalDateTime.now();
      LocalDateTime startDateTime = currentDateTime.minusHours(1);

      return getCo2Data(currentDateTime, startDateTime);
   }

   public Set<Co2Data> getForDay() {
      LocalDateTime currentDateTime = LocalDateTime.now();
      LocalDateTime startDateTime = currentDateTime.minusDays(1);

      return getCo2Data(currentDateTime, startDateTime);
   }

   public Set<Co2Data> getForWeek() {
      LocalDateTime currentDateTime = LocalDateTime.now();
      LocalDateTime startDateTime = currentDateTime.minusWeeks(1);

      return getCo2Data(currentDateTime, startDateTime);
   }

   public Set<Co2Data> getForMonth() {
      LocalDateTime currentDateTime = LocalDateTime.now();
      LocalDateTime startDateTime = currentDateTime.minusMonths(1);

      return getCo2Data(currentDateTime, startDateTime);
   }

   public Set<Co2Data> getForYear() {
      LocalDateTime currentDateTime = LocalDateTime.now();
      LocalDateTime startDateTime = currentDateTime.minusYears(1);

      return getCo2Data(currentDateTime, startDateTime);
   }

   public Set<Co2Data> getAll() {
      LocalDateTime currentDateTime = LocalDateTime.now();
      LocalDateTime startDateTime = LocalDateTime.of(LocalDate.ofYearDay(2000, 1), LocalTime.MIDNIGHT);

      return getCo2Data(currentDateTime, startDateTime);
   }

   public void setLastValueOnUpdate(DeferredResult<long[]> deferredResult, long lastValueTimestamp) {
      lastDateDeferredResults.put(deferredResult, lastValueTimestamp);
   }

   public long[][] convertToArray(Set<Co2Data> co2Data) {
      if (co2Data == null) {
         return new long[0][];
      }

      long[][] returnValue = new long[co2Data.size()][];
      int i = 0;

      for (Co2Data co2DataElement : co2Data) {
         long[] element = new long[2];

         element[0] = co2DataElement.getDateTimeMillis();
         element[1] = co2DataElement.getValue();
         returnValue[i] = element;
         i++;
      }
      return returnValue;
   }

   private Set<Co2Data> getCo2Data(LocalDateTime currentDateTime, LocalDateTime startDateTime) {
      String startTimeShort = startDateTime.format(DATA_TIME_FORMATTER_SHORT);
      String endTimeShort = currentDateTime.format(DATA_TIME_FORMATTER_SHORT);
      String firstFileName = startDateTime.format(DATA_FILE_NAME_FORMATTER);
      Set<Co2Data> co2Data = new LinkedHashSet<>();
      AtomicBoolean earlierFilesExist = new AtomicBoolean(false);
      SortedSet<Path> files = getFiles(firstFileName, earlierFilesExist);

      int i = 1;
      for (Path filePath : files) {
         boolean isLastFile = (i == files.size());
         fillCo2Data(co2Data, filePath, startTimeShort, endTimeShort, isLastFile, earlierFilesExist.get());
         i++;
      }
      return co2Data;
   }

   private SortedSet<Path> getFiles(String firstFileName, AtomicBoolean earlierFilesExist) {
      SortedSet<Path> files = new TreeSet<Path>(FILES_COMPARATOR);
      Path dataDirLocationPath = FileSystems.getDefault().getPath(dataDirLocation);

      try {
         Files.walkFileTree(dataDirLocationPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
               if (filePath.getFileName().toString().compareTo(firstFileName) >= 0) {
                  files.add(filePath);
               } else {
                  earlierFilesExist.set(true);
               }
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (IOException e) {
         LOGGER.error(e);
      }
      return files;
   }

   private void fillCo2Data(Set<Co2Data> co2Data, Path filePath, String startTimeShort, String endTimeShort,
                            boolean isLastFile, boolean earlierFilesExist) {
      try (BufferedReader br = Files.newBufferedReader(filePath)) {
         boolean isFirstFile = co2Data.isEmpty();
         String line = null;

         while ((line = br.readLine()) != null) {
            if (isFirstFile && earlierFilesExist && line.compareTo(startTimeShort) < 0) {
               continue;
            }
            if (isLastFile && line.compareTo(endTimeShort) > 0) {
               break;
            }
            co2Data.add(createCo2DataElement(filePath.getFileName().toString(), line));
         }
      } catch (IOException e) {
         LOGGER.error(e);
      }
   }

   private void openPort() {
      try {
         serialPort = new SerialPort(comPort);
         serialPort.openPort();
         serialPort.setParams(SerialPort.BAUDRATE_9600,
               SerialPort.DATABITS_8,
               SerialPort.STOPBITS_1,
               SerialPort.PARITY_NONE);
         int mask = SerialPort.MASK_RXCHAR;
         serialPort.setEventsMask(mask);
         serialPort.addEventListener(new SerialPortReader());
      } catch (SerialPortException ex) {
         LOGGER.error(ex);
      }
   }

   private void saveValue(int value) {
      Path dataDir = FileSystems.getDefault().getPath(dataDirLocation);

      if (Files.isDirectory(dataDir)) {
         updateLastDateDeferredResults(value);

         if (skipCounter >= SAVE_CO2_VALUE_PERIOD_MS / READ_CO2_VALUE_PERIOD_MS) {
            Path dataFile = FileSystems.getDefault().getPath(dataDirLocation,
                  LocalDate.now().format(DATA_FILE_NAME_FORMATTER));

            skipCounter = 0;
            try (BufferedWriter bw = Files.newBufferedWriter(dataFile, StandardOpenOption.APPEND,
                  StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {

               bw.write(LocalTime.now().format(DATA_TIME_FORMATTER) + " " + value);
               bw.newLine();
            } catch (IOException e) {
               LOGGER.error(e);
            }
         }
      } else {
         LOGGER.error(dataDir.toString() + " doesn't exist");
      }
   }

   private void updateLastDateDeferredResults(int value) {
      LocalDateTime currentDateTime = LocalDateTime.now();
      HashSet<DeferredResult<long[]>> processedDeferredResults = new HashSet<>();

      for (DeferredResult<long[]> deferredResult : lastDateDeferredResults.keySet()) {
         long deferredResultTimestamp = lastDateDeferredResults.get(deferredResult);
         LocalDateTime deferredResultDateTime;

         if (deferredResultTimestamp > 1000) {
            deferredResultDateTime = LocalDateTime.ofEpochSecond(deferredResultTimestamp / 1000,
                  0, ZoneOffset.UTC);
         } else {
            deferredResultDateTime = LocalDateTime.MIN;
         }

         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current date/time: " + currentDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) +
               "; deferred result date/time: " + deferredResultDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
         }

         if (currentDateTime.minusMinutes(1).isAfter(deferredResultDateTime)) {
            long[] element = new long[2];
            long currentTimestamp = currentDateTime.toEpochSecond(ZoneOffset.UTC) * 1000;

            element[0] = currentTimestamp;
            element[1] = value;
            deferredResult.setResult(element);
            processedDeferredResults.add(deferredResult);

            if (LOGGER.isDebugEnabled()) {
               LOGGER.debug("Deferred result is set with value " + value);
            }
         }
      }

      for (DeferredResult<long[]> processedDeferredResult : processedDeferredResults) {
         lastDateDeferredResults.remove(processedDeferredResult);
      }
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

   private class SerialPortReader implements SerialPortEventListener {

      @Override
      public void serialEvent(SerialPortEvent event) {
         if(serialPort != null && event.isRXCHAR()){
            if(event.getEventValue() == 7) { //Check bytes count in the input buffer
               try {
                  int[] received = serialPort.readIntArray(event.getEventValue());
                  CRC16Modbus calculatedCrc = new CRC16Modbus();
                  calculatedCrc.update(received, 0, 5);
                  int readCrc = received[5];
                  readCrc |= received[6] << 8;

                  if (readCrc == calculatedCrc.getValue()) {
                     int co2Value = received[4];
                     co2Value |= received[3] << 8;

                     LOGGER.debug("CO2 value: " + co2Value);

                     saveValue(co2Value);
                  } else {
                     LOGGER.error("Wrong CRC value");
                  }
               } catch (SerialPortException ex) {
                  LOGGER.error(ex);
               }
            } else {
               LOGGER.warn("Received " + event.getEventValue() + " bytes");
            }
         }
      }
   }

   @Autowired
   private void setEnvironment(Environment environment) {
      this.environment = environment;
   }
}

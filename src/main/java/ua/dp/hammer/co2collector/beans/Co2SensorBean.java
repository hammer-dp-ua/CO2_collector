package ua.dp.hammer.co2collector.beans;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Co2SensorBean {

   private static final int[] READ_CO2 = new int[]{0xFE, 0x04, 0x00, 0x03, 0x00, 0x01, 0xD5, 0xC5};
   private static final DateTimeFormatter DATA_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
   private static final DateTimeFormatter DATA_TIME_FORMATTER_SHORT = DateTimeFormatter.ofPattern("HH:mm");
   private static final DateTimeFormatter DATA_FILE_NAME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
   private static final Pattern SAVED_DATA_PATTERN = Pattern.compile("^(\\d{2}:\\d{2}):\\d{2} (\\d{3,4})$");
   static final Comparator FILES_COMPARATOR = new Comparator<Path>() {
      @Override
      public int compare(Path o1, Path o2) {
         return o1.getFileName().toString().compareTo(o2.getFileName().toString());
      }
   };

   private static SerialPort serialPort = null;

   private String dataDirLocation = "Z:\\IdeaProjects\\CO2_collector\\data";

   @PostConstruct
   public void init() {
      openPort();
   }

   @PreDestroy
   public void deInit() {
      if (serialPort != null) {
         try {
            serialPort.closePort();
            serialPort = null;
         } catch (SerialPortException ex) {
            System.out.println(ex);
         }
      }
   }

   @Scheduled(fixedRate = 30000)
   public void readCo2Value() {
      if (serialPort != null && !serialPort.isOpened()) {
         System.out.println("Port is closed");
      }

      if (serialPort != null && serialPort.isOpened()) {
         try {
            serialPort.writeIntArray(READ_CO2);
         } catch (SerialPortException ex) {
            System.out.println(ex);
         }
      }
   }

   public Set<Co2Data> getForHour() {
      LocalDateTime currentDateTime = LocalDateTime.now();
      String lastFileName = currentDateTime.format(DATA_FILE_NAME_FORMATTER);
      Path lastFilePath = FileSystems.getDefault().getPath(dataDirLocation, lastFileName);
      LocalDateTime startDateTime = currentDateTime.minusHours(1);
      String startDateTimeShort = startDateTime.format(DATA_TIME_FORMATTER_SHORT);
      String firstFileName = startDateTime.format(DATA_FILE_NAME_FORMATTER);
      Set<Co2Data> co2Data = new LinkedHashSet<>();
      Path dataDirLocationPath = FileSystems.getDefault().getPath(dataDirLocation);
      SortedSet<Path> files = new TreeSet<Path>(FILES_COMPARATOR);

      try {
         Files.walkFileTree(dataDirLocationPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
               if (filePath.getFileName().toString().compareTo(firstFileName) >= 0) {
                  files.add(filePath);
               }
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (IOException e) {
         e.printStackTrace();
      }

      for (Path filePath : files) {
         fillCo2Data(co2Data, filePath, startDateTimeShort);
      }
      return co2Data;
   }

   private void fillCo2Data(Set<Co2Data> co2Data, Path filePath, String startDateTimeShort) {
      try (BufferedReader br = Files.newBufferedReader(filePath)) {
         boolean firstFile = co2Data.isEmpty();
         String line = null;

         while ((line = br.readLine()) != null) {
            if (nonono !firstFile || line.startsWith(startDateTimeShort)) {
               co2Data.add(createCo2DataElement(filePath.getFileName().toString(), line));
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void openPort() {
      try {
         serialPort = new SerialPort("COM3");
         serialPort.openPort();
         serialPort.setParams(SerialPort.BAUDRATE_9600,
               SerialPort.DATABITS_8,
               SerialPort.STOPBITS_1,
               SerialPort.PARITY_NONE);
         int mask = SerialPort.MASK_RXCHAR;
         serialPort.setEventsMask(mask);
         serialPort.addEventListener(new SerialPortReader());
      } catch (SerialPortException ex) {
         System.out.println(ex);
      }
   }

   private void saveValue(int value) {
      Path dataDir = FileSystems.getDefault().getPath(dataDirLocation);

      if (Files.isDirectory(dataDir)) {
         Path dataFile = FileSystems.getDefault().getPath(dataDirLocation,
               LocalDate.now().format(DATA_FILE_NAME_FORMATTER));

         try (BufferedWriter bw = Files.newBufferedWriter(dataFile, StandardOpenOption.APPEND, StandardOpenOption.WRITE,
               StandardOpenOption.CREATE)) {

            bw.write(LocalTime.now().format(DATA_TIME_FORMATTER) + " " + value);
            bw.newLine();
         } catch (IOException e) {
            e.printStackTrace();
         }
      } else {
         System.out.println(dataDir.toString() + " doesn't exist");
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
                     System.out.println("CO2 value: " + co2Value);

                     saveValue(co2Value);
                  } else {
                     System.out.println("Wrong CRC value");
                  }
               } catch (SerialPortException ex) {
                  System.out.println(ex);
               }
            } else {
               System.out.println("Received " + event.getEventValue() + " bytes");
            }
         }
      }
   }
}

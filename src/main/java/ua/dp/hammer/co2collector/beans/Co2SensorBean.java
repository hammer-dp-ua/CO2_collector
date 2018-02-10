package ua.dp.hammer.co2collector.beans;

import jssc.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class Co2SensorBean {

   private static final int[] READ_CO2 = new int[]{0xFE, 0x04, 0x0, 0x3, 0x0, 0x1, 0xD5, 0xC5};

   private static SerialPort serialPort = null;

   @PostConstruct
   public void init() {
      String[] portNames = SerialPortList.getPortNames();
      System.out.println("Port names:");
      for(int i = 0; i < portNames.length; i++){
         System.out.println(portNames[i]);
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
            System.out.println(ex);
         }
      }
   }

   @Scheduled(fixedRate = 10000)
   public void readCo2Value() {
      if (serialPort != null && serialPort.isOpened()) {
         try {
            serialPort.writeIntArray(READ_CO2);
         } catch (SerialPortException ex) {
            System.out.println(ex);
         }
      }
   }

   private void openPort() {
      try {
         serialPort = new SerialPort("COM3");
         serialPort.openPort();
         serialPort.setParams(SerialPort.BAUDRATE_9600,
               SerialPort.DATABITS_8,
               SerialPort.STOPBITS_2,
               SerialPort.PARITY_NONE);
         int mask = SerialPort.MASK_RXCHAR;
         serialPort.setEventsMask(mask);
         serialPort.addEventListener(new SerialPortReader());
      } catch (SerialPortException ex) {
         System.out.println(ex);
      }
   }

   private static class SerialPortReader implements SerialPortEventListener {

      public void serialEvent(SerialPortEvent event) {
         if(serialPort != null && event.isRXCHAR()){
            if(event.getEventValue() > 0) { //Check bytes count in the input buffer
               try {
                  byte buffer[] = serialPort.readBytes(14);
                  System.out.println(buffer);
               } catch (SerialPortException ex) {
                  System.out.println(ex);
               }
            }
         }
      }
   }
}

package ua.dp.hammer.co2collector.beans;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.dp.hammer.co2collector.utils.CRC16Modbus;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class Co2SensorBean {

   private static final int[] READ_CO2 = new int[]{0xFE, 0x04, 0x00, 0x03, 0x00, 0x01, 0xD5, 0xC5};

   private static SerialPort serialPort = null;

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

   @Scheduled(fixedRate = 10000)
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

   private static class SerialPortReader implements SerialPortEventListener {

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

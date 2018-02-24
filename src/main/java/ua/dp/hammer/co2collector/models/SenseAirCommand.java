package ua.dp.hammer.co2collector.models;

import ua.dp.hammer.co2collector.utils.CRC16Modbus;

public class SenseAirCommand {
   private int address = 0xFE;
   /*
    * 0x03 - Read Holding Registers;
    * 0x04 - Read Input Registers
    * 0x06 - Write Single Register
    */
   private int functionCode;
   // 16 bits
   private int startingAddress;
   // 16 bits
   private int data;

   public int getAddress() {
      return address;
   }

   public void setAddress(int address) {
      this.address = address;
   }

   public int getFunctionCode() {
      return functionCode;
   }

   public void setFunctionCode(int functionCode) {
      this.functionCode = functionCode;
   }

   public int getStartingAddress() {
      return startingAddress;
   }

   public void setStartingAddress(int startingAddress) {
      this.startingAddress = startingAddress;
   }

   public int getData() {
      return data;
   }

   public void setData(int data) {
      this.data = data;
   }

   public int[] getIntArray() {
      int[] result = new int[8];

      result[0] = address;
      result[1] = functionCode;
      result[3] = startingAddress;
      result[4] = data >>> 8;
      result[5] = data & 0xFF;

      CRC16Modbus calculatedCrc = new CRC16Modbus();
      calculatedCrc.update(result, 0, 6);
      long crcValue = calculatedCrc.getValue();
      result[6] = (int) (crcValue & 0xFF);
      result[7] = (int) (crcValue >>> 8);
      return result;
   }
}

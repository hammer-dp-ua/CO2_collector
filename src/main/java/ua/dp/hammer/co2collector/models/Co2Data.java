package ua.dp.hammer.co2collector.models;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Co2Data {

   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

   private String dateTime;
   private int value;

   public Co2Data(String dateTime, int value) {
      this.dateTime = dateTime;
      this.value = value;
   }

   public long getDateTimeMillis() {
      if (dateTime != null) {
         return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER).toEpochSecond(ZoneOffset.UTC) * 1000;
      }
      return -1;
   }

   public String getDateTime() {
      return dateTime;
   }

   public void setDateTime(String dateTime) {
      this.dateTime = dateTime;
   }

   public int getValue() {
      return value;
   }

   public void setValue(int value) {
      this.value = value;
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Co2Data)) {
         return false;
      }

      Co2Data thatObject = (Co2Data) o;

      if (dateTime == null && thatObject.dateTime == null) {
         return true;
      }
      if (dateTime == null || thatObject.dateTime == null) {
         return false;
      }
      return dateTime.equals(thatObject.dateTime);
   }

   @Override
   public int hashCode() {
      return dateTime != null ? dateTime.hashCode() : -1;
   }
}

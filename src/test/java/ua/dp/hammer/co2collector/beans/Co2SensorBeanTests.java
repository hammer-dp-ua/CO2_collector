package ua.dp.hammer.co2collector.beans;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;

public class Co2SensorBeanTests {

   @org.junit.Test
   public void testFilesSorting() {
      SortedSet<Path> files = new TreeSet<Path>(Co2SensorBean.FILES_COMPARATOR);

      files.add(FileSystems.getDefault().getPath("2018-01-01"));
      files.add(FileSystems.getDefault().getPath("2018-02-01"));
      files.add(FileSystems.getDefault().getPath("2018-01-02"));
      files.add(FileSystems.getDefault().getPath("2017-01-02"));
      files.add(FileSystems.getDefault().getPath("2017-10-01"));
      files.add(FileSystems.getDefault().getPath("2017-10-02"));

      for (Path path : files) {
         System.out.println(path.getFileName().toString());
      }
   }
}

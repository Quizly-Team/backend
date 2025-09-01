package org.quizly.quizly.core.util;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeUtil {

  private static final DateTimeFormatter HH_MM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public static String toString(ZonedDateTime zonedDateTime) {
    if (zonedDateTime == null) {
      return null;
    }
    try {
      return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    } catch (DateTimeException e) {
      return null;
    }
  }

  public static String toString(LocalTime time) {
    if (time == null) {
      return null;
    }
    try {
      return time.format(HH_MM_FORMATTER);
    } catch (DateTimeException e) {
      return null;
    }
  }
}

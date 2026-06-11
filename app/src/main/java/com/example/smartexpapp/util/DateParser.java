package com.example.smartexpapp.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParser {

    private static final String[] DATE_FORMATS = {
            "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd",
            "dd-MM-yyyy", "MM-dd-yyyy", "yyyy-MM-dd",
            "dd.MM.yyyy", "MM.dd.yyyy", "yyyy.MM.dd",
            "dd MM yyyy", "MM dd yyyy", "yyyy MM dd",
            "dd MMM yyyy", "MMM dd yyyy", "MMM dd, yyyy",
            "dd/MM/yy", "MM/dd/yy",
            "dd-MM-yy", "MM-dd-yy",
            "dd.MM.yy", "MM.dd.yy",
            "dd MM yy", "MM dd yy"
    };

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b\\d{1,4}[/\\-.\\s]+\\d{1,2}[/\\-.\\s]+\\d{1,4}\\b|" +
                    "\\b\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4}\\b|" +
                    "\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{2,4}\\b",
            Pattern.CASE_INSENSITIVE);

    public static List<Long> extractDates(String text) {
        Set<Long> uniqueDates = new HashSet<>();
        if (text == null || text.isEmpty())
            return new ArrayList<>();

        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            String match = matcher.group();
            // normalize spaces
            match = match.replaceAll("\\s+", " ").trim();

            for (String format : DATE_FORMATS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                    sdf.setLenient(false);
                    Date date = sdf.parse(match);
                    if (date != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        int year = cal.get(Calendar.YEAR);
                        // Sanity check for valid expiry years
                        if (year >= 2000 && year < 2100) {
                            uniqueDates.add(date.getTime());
                            break;
                        }
                    }
                } catch (ParseException ignored) {
                }
            }
        }

        List<Long> result = new ArrayList<>(uniqueDates);
        Collections.sort(result);
        return result;
    }
}

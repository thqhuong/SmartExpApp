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
    public static final class DateCandidate {
        private final long dateMillis;
        private final String matchedText;
        private final String snippet;
        private final float confidence;

        private DateCandidate(long dateMillis, String matchedText, String snippet, float confidence) {
            this.dateMillis = dateMillis;
            this.matchedText = matchedText;
            this.snippet = snippet;
            this.confidence = confidence;
        }

        public long getDateMillis() {
            return dateMillis;
        }

        public String getMatchedText() {
            return matchedText;
        }

        public String getSnippet() {
            return snippet;
        }

        public float getConfidence() {
            return confidence;
        }
    }

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

    // Compact dates printed without separators, common on food labels (e.g. 290527, 20261227).
    private static final Pattern COMPACT_DATE_PATTERN = Pattern.compile("\\b(\\d{8}|\\d{6})\\b");
    private static final String[] COMPACT_FORMATS_8 = { "ddMMyyyy", "yyyyMMdd", "MMddyyyy" };
    private static final String[] COMPACT_FORMATS_6 = { "ddMMyy", "yyMMdd", "MMddyy" };

    public static List<Long> extractDates(String text) {
        List<DateCandidate> candidates = extractDateCandidates(text);
        Set<Long> uniqueDates = new HashSet<>();
        for (DateCandidate candidate : candidates) {
            uniqueDates.add(candidate.getDateMillis());
        }

        List<Long> result = new ArrayList<>(uniqueDates);
        Collections.sort(result);
        return result;
    }

    public static List<DateCandidate> extractDateCandidates(String text) {
        List<DateCandidate> candidates = new ArrayList<>();
        Set<Long> uniqueDates = new HashSet<>();
        if (text == null || text.isEmpty())
            return candidates;

        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            String match = matcher.group();
            String snippet = snippetAround(text, matcher.start(), matcher.end());
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
                            long dateMillis = date.getTime();
                            if (uniqueDates.add(dateMillis)) {
                                candidates.add(new DateCandidate(dateMillis, match, snippet, confidenceFor(match, snippet)));
                            }
                            break;
                        }
                    }
                } catch (ParseException ignored) {
                }
            }
        }

        // Second pass: compact dates with no separators (e.g. 290527, 20261227). These are
        // ambiguous, so every valid interpretation is added as a separate candidate and the
        // user picks the right one in the OCR review dialog.
        Matcher compactMatcher = COMPACT_DATE_PATTERN.matcher(text);
        while (compactMatcher.find()) {
            String match = compactMatcher.group();
            String snippet = snippetAround(text, compactMatcher.start(), compactMatcher.end());
            String[] formats = match.length() == 8 ? COMPACT_FORMATS_8 : COMPACT_FORMATS_6;
            for (String format : formats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                    sdf.setLenient(false);
                    Date date = sdf.parse(match);
                    if (date != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        int year = cal.get(Calendar.YEAR);
                        if (year >= 2000 && year < 2100) {
                            long dateMillis = date.getTime();
                            if (uniqueDates.add(dateMillis)) {
                                candidates.add(new DateCandidate(dateMillis, match, snippet, confidenceFor(match, snippet)));
                            }
                        }
                    }
                } catch (ParseException ignored) {
                }
            }
        }

        Collections.sort(candidates, (left, right) -> Long.compare(left.getDateMillis(), right.getDateMillis()));
        return candidates;
    }

    private static String snippetAround(String text, int start, int end) {
        int snippetStart = Math.max(0, start - 24);
        int snippetEnd = Math.min(text.length(), end + 24);
        return text.substring(snippetStart, snippetEnd).replaceAll("\\s+", " ").trim();
    }

    private static float confidenceFor(String match, String snippet) {
        float confidence = 0.65f;
        String lower = snippet.toLowerCase(Locale.US);
        if (lower.contains("exp") || lower.contains("best before") || lower.contains("use by")) {
            confidence += 0.25f;
        }
        if (match.matches(".*\\d{4}.*")) {
            confidence += 0.1f;
        }
        return Math.min(confidence, 1.0f);
    }
}

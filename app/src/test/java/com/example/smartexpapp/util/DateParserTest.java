package com.example.smartexpapp.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateParserTest {
    @Test
    public void extractDateCandidatesIncludesSnippetAndConfidence() {
        List<DateParser.DateCandidate> candidates = DateParser.extractDateCandidates(
                "Organic milk best before 12/25/2026 keep refrigerated");

        assertEquals(1, candidates.size());
        DateParser.DateCandidate candidate = candidates.get(0);
        assertTrue(candidate.getSnippet().contains("best before"));
        assertEquals("12/25/2026", candidate.getMatchedText());
        assertTrue(candidate.getConfidence() > 0.8f);
    }

    @Test
    public void extractDateCandidatesDeduplicatesDates() {
        List<DateParser.DateCandidate> candidates = DateParser.extractDateCandidates(
                "EXP 12/25/2026 duplicate 12/25/2026");

        assertEquals(1, candidates.size());
    }

    @Test
    public void extractDatesKeepsLegacyMillisApi() {
        List<Long> dates = DateParser.extractDates("Use by Jan 4, 2027");

        assertFalse(dates.isEmpty());
    }
}

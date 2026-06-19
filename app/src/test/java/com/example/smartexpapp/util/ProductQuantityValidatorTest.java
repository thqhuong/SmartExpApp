package com.example.smartexpapp.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProductQuantityValidatorTest {
    @Test
    public void normalizeRejectsMissingAndNonPositiveValues() {
        assertNull(ProductQuantityValidator.normalize(null));
        assertNull(ProductQuantityValidator.normalize(""));
        assertNull(ProductQuantityValidator.normalize(" "));
        assertNull(ProductQuantityValidator.normalize("0"));
        assertNull(ProductQuantityValidator.normalize("0.0"));
        assertNull(ProductQuantityValidator.normalize("-1"));
        assertNull(ProductQuantityValidator.normalize("abc"));
        assertNull(ProductQuantityValidator.normalize("1 kg"));
    }

    @Test
    public void normalizeAllowsPositiveWholeAndDecimalValues() {
        assertEquals("1", ProductQuantityValidator.normalize("1"));
        assertEquals("1.5", ProductQuantityValidator.normalize("1.50"));
        assertEquals("0.5", ProductQuantityValidator.normalize(".5"));
        assertEquals("2.25", ProductQuantityValidator.normalize(" 2.250 "));
    }
}

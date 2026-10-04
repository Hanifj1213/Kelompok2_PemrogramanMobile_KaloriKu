package com.hajun.kaloriku

import com.hajun.kaloriku.util.parseDecimalInput
import com.hajun.kaloriku.util.parseIntegerInput
import com.hajun.kaloriku.util.toPlainInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumericInputTest {
    @Test fun rejectsEmptyAndWhitespace() {
        listOf("", " ", "\n").forEach { assertNull(parseDecimalInput(it, 1.0..3000.0)) }
    }
    @Test fun acceptsCommaAndDotDecimals() {
        assertEquals(150.5, parseDecimalInput("150,5", 1.0..3000.0)!!, 0.0001)
        assertEquals(150.5, parseDecimalInput("150.5", 1.0..3000.0)!!, 0.0001)
    }
    @Test fun acceptsBoundaries() {
        assertEquals(1.0, parseDecimalInput("1", 1.0..3000.0)!!, 0.0)
        assertEquals(3000.0, parseDecimalInput("3000", 1.0..3000.0)!!, 0.0)
    }
    @Test fun rejectsOutOfRange() {
        listOf("0", "-1", "3000.1", "4000").forEach { assertNull(parseDecimalInput(it, 1.0..3000.0)) }
    }
    @Test fun rejectsNonFiniteAndExponent() {
        listOf("NaN", "Infinity", "-Infinity", "1e3", "1E999").forEach { assertNull(parseDecimalInput(it, 1.0..3000.0)) }
    }
    @Test fun rejectsMixedOrRepeatedSeparators() {
        listOf("1.000,5", "1,000.5", "1..5", "1,,5", ".", ",").forEach { assertNull(parseDecimalInput(it, 1.0..3000.0)) }
    }
    @Test fun preservesZeroIntegerTarget() {
        assertEquals(0, parseIntegerInput("0", 0..300))
    }
    @Test fun integerInputCanBeBlankWhileEditing() {
        listOf("", " ", "1.5", "-1", "301", "9999999999999").forEach { assertNull(parseIntegerInput(it, 0..300)) }
    }
    @Test fun plainInputHasNoThousandsGrouping() {
        assertEquals("1000", 1000.0.toPlainInput())
        assertEquals(1000.0, parseDecimalInput(1000.0.toPlainInput(), 1.0..3000.0)!!, 0.0)
    }
    @Test fun plainInputPreservesFractionalWeights() {
        assertEquals("150.5", 150.5.toPlainInput())
        assertEquals("0.001", 0.001.toPlainInput())
    }
    @Test fun nonFiniteFormatIsBlank() {
        assertEquals("", Double.NaN.toPlainInput())
        assertEquals("", Double.POSITIVE_INFINITY.toPlainInput())
    }
}

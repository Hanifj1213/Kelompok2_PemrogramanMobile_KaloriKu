package com.hajun.kaloriku

import com.hajun.kaloriku.util.BMI_SCALE_MAX
import com.hajun.kaloriku.util.BMI_SCALE_MIN
import com.hajun.kaloriku.util.BmiCategory
import com.hajun.kaloriku.util.bmiCategory
import com.hajun.kaloriku.util.bmiMarkerFraction
import com.hajun.kaloriku.util.calculateBmi
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.parseDecimalInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BmiTest {
    @Test fun calculatesBmiFromKilogramsAndCentimeters() {
        assertEquals(22.857142857142858, calculateBmi(70.0, 175.0)!!, 0.000001)
    }

    @Test fun calculatesWithFractionalWeightAndHeight() {
        assertEquals(20.0, calculateBmi(70.3125, 187.5)!!, 0.000001)
    }

    @Test fun rejectsMissingWeightOrHeight() {
        assertNull(calculateBmi(null, 175.0))
        assertNull(calculateBmi(70.0, null))
        assertNull(calculateBmi(null, null))
    }

    @Test fun rejectsNonFiniteWeight() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            assertNull(calculateBmi(it, 175.0))
        }
    }

    @Test fun rejectsNonFiniteHeight() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            assertNull(calculateBmi(70.0, it))
        }
    }

    @Test fun rejectsNonPositiveWeight() {
        listOf(0.0, -0.0, -1.0).forEach { assertNull(calculateBmi(it, 175.0)) }
    }

    @Test fun rejectsNonPositiveHeight() {
        listOf(0.0, -0.0, -1.0).forEach { assertNull(calculateBmi(70.0, it)) }
    }

    @Test fun rejectsOverflowAndUnderflowResults() {
        assertNull(calculateBmi(Double.MAX_VALUE, 1.0))
        assertNull(calculateBmi(70.0, Double.MIN_VALUE))
        assertNull(calculateBmi(70.0, Double.MAX_VALUE))
        assertNull(calculateBmi(Double.MIN_VALUE, 250.0))
    }

    @Test fun blankInputCannotCreateBmiOrMarker() {
        listOf("", " ", "\n").forEach { text ->
            val weight = parseDecimalInput(text, 20.0..300.0)
            val height = parseDecimalInput(text, 100.0..250.0)
            assertNull(calculateBmi(weight, 175.0))
            assertNull(calculateBmi(70.0, height))
            assertNull(bmiMarkerFraction(calculateBmi(weight, height)))
        }
    }

    @Test fun formatsActualBmiWithIndonesianDecimalSeparator() {
        val bmi = calculateBmi(70.0, 175.0)!!
        assertEquals("22,9", bmi.formatDecimal())
    }

    @Test fun below18Point5IsUnderweight() {
        assertEquals(BmiCategory.KURANG, bmiCategory(18.499999))
    }

    @Test fun at18Point5IsNormal() {
        assertEquals(BmiCategory.NORMAL, bmiCategory(18.5))
        assertEquals(BmiCategory.NORMAL, bmiCategory(18.500001))
    }

    @Test fun below23RemainsNormalWithoutRoundingTheCategory() {
        assertEquals(BmiCategory.NORMAL, bmiCategory(22.999999))
    }

    @Test fun at23IsOverweight() {
        assertEquals(BmiCategory.BERLEBIH, bmiCategory(23.0))
        assertEquals(BmiCategory.BERLEBIH, bmiCategory(23.000001))
    }

    @Test fun below25IsOverweight() {
        assertEquals(BmiCategory.BERLEBIH, bmiCategory(24.999999))
    }

    @Test fun at25AndUpIsObesityIncluding25To30() {
        listOf(25.0, 25.000001, 27.0, 29.999999, 30.0, 40.0).forEach {
            assertEquals(BmiCategory.OBESITAS, bmiCategory(it))
        }
    }

    @Test fun invalidBmiHasNoCategoryAndNoMarker() {
        listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -1.0).forEach {
            assertNull(bmiCategory(it))
            assertNull(bmiMarkerFraction(it))
        }
    }

    @Test fun categoryBoundsAndLabelsMatchTheLegend() {
        assertEquals(4, BmiCategory.entries.size)
        assertEquals(listOf("Kurang", "Normal", "Berlebih", "Obesitas"), BmiCategory.entries.map { it.label })
        assertNull(BmiCategory.KURANG.lowerInclusive)
        assertNull(BmiCategory.OBESITAS.upperExclusive)
        BmiCategory.entries.zipWithNext().forEach { (before, after) ->
            assertEquals(before.upperExclusive, after.lowerInclusive)
            assertEquals(after, bmiCategory(after.lowerInclusive))
        }
    }

    @Test fun markerUses15To35ScaleEndpointsAndMidpoint() {
        assertEquals(15.0, BMI_SCALE_MIN, 0.0)
        assertEquals(35.0, BMI_SCALE_MAX, 0.0)
        assertEquals(0f, bmiMarkerFraction(15.0)!!, 0f)
        assertEquals(0.5f, bmiMarkerFraction(25.0)!!, 0f)
        assertEquals(1f, bmiMarkerFraction(35.0)!!, 0f)
    }

    @Test fun markerAlignsWithExactCategoryBoundaries() {
        assertEquals(0.175f, bmiMarkerFraction(18.5)!!, 0.000001f)
        assertEquals(0.4f, bmiMarkerFraction(23.0)!!, 0.000001f)
        assertEquals(0.5f, bmiMarkerFraction(25.0)!!, 0.000001f)
    }

    @Test fun markerClampsBelowScaleWithoutChangingActualBmi() {
        val bmi = calculateBmi(40.0, 200.0)!!
        assertEquals(10.0, bmi, 0.0)
        assertEquals("10,0", bmi.formatDecimal())
        assertEquals(BmiCategory.KURANG, bmiCategory(bmi))
        assertEquals(0f, bmiMarkerFraction(bmi)!!, 0f)
    }

    @Test fun markerClampsAboveScaleWithoutChangingActualBmi() {
        val bmi = calculateBmi(100.0, 150.0)!!
        assertEquals(44.44444444444444, bmi, 0.000001)
        assertEquals("44,4", bmi.formatDecimal())
        assertEquals(BmiCategory.OBESITAS, bmiCategory(bmi))
        assertEquals(1f, bmiMarkerFraction(bmi)!!, 0f)
    }

    @Test fun markerClampsExtremeFiniteBmiRatherThanOverflowing() {
        assertEquals(0f, bmiMarkerFraction(Double.MIN_VALUE)!!, 0f)
        assertEquals(1f, bmiMarkerFraction(Double.MAX_VALUE)!!, 0f)
    }
}

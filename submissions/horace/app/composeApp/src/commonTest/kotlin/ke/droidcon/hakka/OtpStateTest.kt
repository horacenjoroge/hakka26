package ke.droidcon.hakka

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtpStateTest {
    private val isValid: (String) -> Boolean = { it == "123456" }

    private fun typed(code: String): OtpState =
        code.foldIndexed(OtpState()) { index, state, digit -> state.input(index, digit.toString()) }

    @Test
    fun typingADigitMovesFocusToTheNextBox() {
        val state = OtpState().input(0, "4")
        assertEquals('4', state.digits[0])
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun nonDigitsAreRejected() {
        val state = OtpState().input(0, "a")
        assertEquals(OtpState(), state)
    }

    @Test
    fun typingOverAFilledBoxReplacesTheDigit() {
        val state = typed("1").focus(0).input(0, "17")
        assertEquals('7', state.digits[0])
    }

    @Test
    fun backspaceOnAnEmptyBoxClearsThePreviousDigitAndMovesBack() {
        val state = typed("12").backspace(2)
        assertEquals(listOf('1', null, null, null, null, null), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceOnTheFirstEmptyBoxDoesNothing() {
        assertEquals(OtpState(), OtpState().backspace(0))
    }

    @Test
    fun pastingTextFillsAllBoxesWithItsDigits() {
        val state = OtpState().input(0, "Your code is 482913")
        assertEquals("482913", state.code)
        assertTrue(state.isComplete)
    }

    @Test
    fun pastingIntoAFilledBoxIgnoresTheExistingDigit() {
        val state = typed("4").focus(0).input(0, "4482913")
        assertEquals("482913", state.code)
    }

    @Test
    fun submitDoesNothingUntilAllSixDigitsAreFilled() {
        val state = typed("12345")
        assertFalse(state.isComplete)
        assertEquals(state, state.submit(isValid))
    }

    @Test
    fun correctCodeSucceeds() {
        assertEquals(OtpStatus.Success, typed("123456").submit(isValid).status)
    }

    @Test
    fun wrongCodeShowsErrorClearsBoxesAndFocusesTheFirst() {
        val state = typed("654321").submit(isValid)
        assertEquals(OtpStatus.Error, state.status)
        assertTrue(state.digits.all { it == null })
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun editingAfterAnErrorClearsTheStatus() {
        val state = typed("654321").submit(isValid).input(0, "1")
        assertEquals(OtpStatus.Idle, state.status)
    }

    @Test
    fun resendLabelCountsDownThenOffersResend() {
        assertEquals("Resend in 0:30", resendLabel(30))
        assertEquals("Resend in 0:05", resendLabel(5))
        assertEquals("Resend", resendLabel(0))
    }
}

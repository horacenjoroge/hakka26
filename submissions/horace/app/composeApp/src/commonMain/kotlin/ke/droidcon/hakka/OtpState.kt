package ke.droidcon.hakka

const val OTP_LENGTH = 6
const val RESEND_SECONDS = 30

enum class OtpStatus { Idle, Success, Error }

/**
 * Immutable state for the OTP input. Every user action returns a new state,
 * so the UI holds a single value and the behaviour can be unit tested.
 */
data class OtpState(
    val digits: List<Char?> = List(OTP_LENGTH) { null },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.Idle,
) {
    val isComplete: Boolean get() = digits.all { it != null }

    val code: String get() = digits.joinToString("") { it?.toString().orEmpty() }

    /** Handles new text in the box at [index]: typing, replacing or pasting. */
    fun input(index: Int, raw: String): OtpState {
        if (raw.isEmpty()) return clear(index)
        val typed = raw.filter { it in '0'..'9' }
        if (typed.isEmpty()) return this

        val current = digits[index]
        val incoming = if (current != null && typed.length > 1 && typed.first() == current) {
            typed.drop(1)
        } else {
            typed
        }
        return if (incoming.length >= OTP_LENGTH) {
            fillFrom(0, incoming.take(OTP_LENGTH))
        } else {
            fillFrom(index, incoming)
        }
    }

    /** Backspace with the box at [index] already empty: move back and clear that digit. */
    fun backspace(index: Int): OtpState {
        if (digits[index] != null) return clear(index)
        if (index == 0) return this
        val previous = index - 1
        return copy(
            digits = digits.replaceAt(previous, null),
            focusedIndex = previous,
            status = OtpStatus.Idle,
        )
    }

    fun focus(index: Int): OtpState = if (index == focusedIndex) this else copy(focusedIndex = index)

    fun submit(isValid: (String) -> Boolean): OtpState {
        if (!isComplete) return this
        return if (isValid(code)) {
            copy(status = OtpStatus.Success)
        } else {
            OtpState(status = OtpStatus.Error)
        }
    }

    private fun clear(index: Int): OtpState =
        copy(digits = digits.replaceAt(index, null), focusedIndex = index, status = OtpStatus.Idle)

    private fun fillFrom(start: Int, value: String): OtpState {
        val updated = digits.toMutableList()
        var last = start
        value.forEachIndexed { offset, digit ->
            val target = start + offset
            if (target < OTP_LENGTH) {
                updated[target] = digit
                last = target
            }
        }
        return copy(
            digits = updated,
            focusedIndex = (last + 1).coerceAtMost(OTP_LENGTH - 1),
            status = OtpStatus.Idle,
        )
    }
}

fun resendLabel(secondsLeft: Int): String =
    if (secondsLeft > 0) "Resend in 0:${secondsLeft.toString().padStart(2, '0')}" else "Resend"

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }

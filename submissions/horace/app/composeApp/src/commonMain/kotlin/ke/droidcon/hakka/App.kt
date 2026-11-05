package ke.droidcon.hakka

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hakka.composeapp.generated.resources.Res
import hakka.composeapp.generated.resources.thamani_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

private const val VALID_CODE = "123456"

@Composable
fun App() {
    val colors = if (isSystemInDarkTheme()) OtpColors.Dark else OtpColors.Light
    Box(modifier = Modifier.fillMaxSize().background(colors.sheet)) {
        BackgroundScreen(colors)
        Box(Modifier.fillMaxSize().background(colors.scrim))
        OtpSheet(colors, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** The screen behind the sheet, blurred and dimmed by the scrim. */
@Composable
private fun BackgroundScreen(colors: OtpColors) {
    Column(
        modifier = Modifier.fillMaxSize().blur(8.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Text("Welcome back", color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OtpSheet(colors: OtpColors, modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(OtpState()) }
    var resendRequests by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(RESEND_SECONDS) }

    LaunchedEffect(resendRequests) {
        secondsLeft = RESEND_SECONDS
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .background(colors.sheet, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.thamani_logo),
            contentDescription = "Thamani logo",
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Enter your code",
            color = colors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "A 6-digit code has been sent to your phone.",
            color = colors.textSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        OtpInput(
            state = state,
            colors = colors,
            onStateChange = { state = it },
        )
        Spacer(Modifier.height(24.dp))
        ResendLine(
            secondsLeft = secondsLeft,
            colors = colors,
            onResend = { resendRequests += 1 },
        )
        Spacer(Modifier.height(16.dp))
        StatusMessage(state.status, colors)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { state = state.submit { it == VALID_CODE } },
            enabled = state.isComplete,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.buttonBg,
                contentColor = colors.buttonText,
                disabledContainerColor = colors.buttonDisabledBg,
                disabledContentColor = colors.buttonText,
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("otp_submit"),
        ) {
            Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Six single-digit boxes. All behaviour lives in [OtpState]; this only renders it. */
@Composable
fun OtpInput(
    state: OtpState,
    colors: OtpColors,
    onStateChange: (OtpState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val requesters = remember { List(OTP_LENGTH) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.focusedIndex, state.status) {
        requesters[state.focusedIndex].requestFocus()
        keyboard?.show()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(OTP_LENGTH) { index ->
            DigitBox(
                digit = state.digits[index],
                index = index,
                isFocused = index == state.focusedIndex,
                colors = colors,
                focusRequester = requesters[index],
                onInput = { onStateChange(state.input(index, it)) },
                onBackspaceWhenEmpty = { onStateChange(state.backspace(index)) },
                onFocused = { onStateChange(state.focus(index)) },
            )
        }
    }
}

@Composable
private fun DigitBox(
    digit: Char?,
    index: Int,
    isFocused: Boolean,
    colors: OtpColors,
    focusRequester: FocusRequester,
    onInput: (String) -> Unit,
    onBackspaceWhenEmpty: () -> Unit,
    onFocused: () -> Unit,
) {
    val text = digit?.toString().orEmpty()
    val (background, border, textColor) = when {
        digit != null -> Triple(
            colors.digitFilledBg,
            if (isFocused) BorderStroke(1.5.dp, colors.digitFocusedOutline) else null,
            colors.digitFilledText,
        )
        isFocused -> Triple(colors.digitFocusedBg, BorderStroke(1.5.dp, colors.digitFocusedOutline), colors.textPrimary)
        else -> Triple(colors.digitEmptyBg, BorderStroke(1.dp, colors.digitEmptyOutline), colors.textPrimary)
    }

    BasicTextField(
        value = TextFieldValue(text, selection = TextRange(text.length)),
        onValueChange = { onInput(it.text) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(color = textColor, fontSize = 18.sp, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier
            .size(46.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .onPreviewKeyEvent { event ->
                val isEmptyBackspace = event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace && digit == null
                if (isEmptyBackspace) onBackspaceWhenEmpty()
                isEmptyBackspace
            }
            .testTag("otp_digit_${index + 1}")
            .semantics { contentDescription = "Digit ${index + 1} of $OTP_LENGTH" },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background, CircleShape)
                    .then(if (border != null) Modifier.border(border, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (digit == null) {
                    Text("–", color = colors.digitPlaceholder, fontSize = 18.sp)
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ResendLine(secondsLeft: Int, colors: OtpColors, onResend: () -> Unit) {
    val canResend = secondsLeft == 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Didn't receive the code? ", color = colors.textSecondary, fontSize = 14.sp)
        Text(
            resendLabel(secondsLeft),
            color = if (canResend) colors.link else colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = if (canResend) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier
                .testTag("otp_resend")
                .clickable(enabled = canResend, role = Role.Button, onClick = onResend),
        )
    }
}

@Composable
private fun StatusMessage(status: OtpStatus, colors: OtpColors) {
    val (message, color) = when (status) {
        OtpStatus.Success -> "You're in." to colors.success
        OtpStatus.Error -> "That code didn't work. Try again." to colors.error
        OtpStatus.Idle -> "" to Color.Unspecified
    }
    Text(
        message,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag("otp_status"),
    )
}

package com.example.morsekey

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.widget.Button

class MorseKeyboardService : InputMethodService() {

    enum class Mode { SINGLE, DOT, DASH, SWIPE }

    // --- ADAPTIVE TIMING ENGINE ---
    private var tUnit = 150f
    private var startTime = 0L
    private var nextRepeatTime = 0L
    private var lastSpaceTime = 0L
    private var currentSequence = ""

    // Multi-touch & Swipe Tracking
    private var activeView: View? = null
    private var startTouchX = 0f
    private var currentDiffX = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var colorLoopRunnable: Runnable? = null
    private var letterPauseRunnable: Runnable? = null

    // --- MORSE DICTIONARY ---
    private val morseMap = mapOf(
        ".-" to "a", "-..." to "b", "-.-." to "c", "-.." to "d", "." to "e",
        "..-." to "f", "--." to "g", "...." to "h", ".." to "i", ".---" to "j",
        "-.-" to "k", ".-.." to "l", "--" to "m", "-." to "n", "---" to "o",
        ".--." to "p", "--.-" to "q", ".-." to "r", "..." to "s", "-" to "t",
        "..-" to "u", "...-" to "v", ".--" to "w", "-..-" to "x", "-.--" to "y",
        "--.." to "z", "-----" to "0", ".----" to "1", "..---" to "2",
        "...--" to "3", "....-" to "4", "....." to "5", "-...." to "6",
        "--..." to "7", "---.." to "8", "----." to "9"
    )

    override fun onCreateInputView(): View {
        val sharedPreferences = getSharedPreferences("MorseKeyPrefs", Context.MODE_PRIVATE)
        val selectedLayout = sharedPreferences.getString("selected_layout", "single")

        val keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null)

        val singleLayout = keyboardView.findViewById<View>(R.id.layout_single)
        val splitLayout = keyboardView.findViewById<View>(R.id.layout_split)
        val swipeLayout = keyboardView.findViewById<View>(R.id.layout_swipe)

        singleLayout?.visibility = View.GONE
        splitLayout?.visibility = View.GONE
        swipeLayout?.visibility = View.GONE

        when (selectedLayout) {
            "single" -> singleLayout?.visibility = View.VISIBLE
            "split" -> splitLayout?.visibility = View.VISIBLE
            "swipe" -> swipeLayout?.visibility = View.VISIBLE
        }

        val singleButton = keyboardView.findViewById<Button>(R.id.morse_input_zone)
        val btnDot = keyboardView.findViewById<Button>(R.id.btn_dot)
        val btnDash = keyboardView.findViewById<Button>(R.id.btn_dash)
        val swipeAnchor = keyboardView.findViewById<Button>(R.id.swipe_anchor)

        // Bind the specific modes to each layout button
        singleButton?.let { setupTouchListener(it, Mode.SINGLE) }
        btnDot?.let { setupTouchListener(it, Mode.DOT) }
        btnDash?.let { setupTouchListener(it, Mode.DASH) }
        swipeAnchor?.let { setupTouchListener(it, Mode.SWIPE) }

        return keyboardView
    }

    private fun setupTouchListener(button: View, mode: Mode) {
        button.setOnTouchListener { view, event ->
            // Calculate a scalable swipe threshold (approx 40dp) based on the phone's screen density
            val swipeThreshPx = 40f * resources.displayMetrics.density

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (activeView != null) return@setOnTouchListener true
                    activeView = view

                    // Reset swipe tracking variables
                    startTouchX = event.x
                    currentDiffX = 0f

                    letterPauseRunnable?.let { handler.removeCallbacks(it) }
                    startTime = System.currentTimeMillis()
                    nextRepeatTime = startTime + (tUnit * 7.0f).toLong()
                    vibrate(30)

                    colorLoopRunnable = object : Runnable {
                        override fun run() {
                            val duration = System.currentTimeMillis() - startTime
                            val dashThresh = tUnit * 1.5f
                            val spaceThresh = tUnit * 4.0f
                            val delThresh = tUnit * 7.0f

                            // Dynamically map base colors based on Layout Mode
                            val baseColor = when (mode) {
                                Mode.DASH -> "#0088FF"
                                Mode.DOT -> "#2E7D32"
                                Mode.SINGLE -> "#2E7D32" // Starts green
                                Mode.SWIPE -> {
                                    if (currentDiffX > swipeThreshPx) "#0088FF" // Drag Right -> Blue (Dash)
                                    else if (currentDiffX < -swipeThreshPx) "#2E7D32" // Drag Left -> Green (Dot)
                                    else "#444466" // Holding Center -> Neutral Purple
                                }
                            }

                            when {
                                duration >= delThresh -> {
                                    view.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#C62828")) // Red (Delete)
                                    val now = System.currentTimeMillis()
                                    if (now > nextRepeatTime) {
                                        vibrate(50)
                                        if (currentSequence.isNotEmpty()) {
                                            currentSequence = ""
                                        } else {
                                            currentInputConnection?.deleteSurroundingText(1, 0)
                                        }
                                        nextRepeatTime = now + 150L
                                    }
                                }
                                duration >= spaceThresh -> {
                                    view.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DDDDDD")) // White (Space)
                                }
                                mode == Mode.SINGLE && duration >= dashThresh -> {
                                    view.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F57F17")) // Orange (Dash)
                                }
                                else -> {
                                    view.backgroundTintList = ColorStateList.valueOf(Color.parseColor(baseColor))
                                }
                            }
                            handler.postDelayed(this, 16)
                        }
                    }
                    handler.post(colorLoopRunnable!!)
                }

                MotionEvent.ACTION_MOVE -> {
                    // Update geographic drag distance in real-time while holding
                    if (activeView == view) {
                        currentDiffX = event.x - startTouchX
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (activeView != view) return@setOnTouchListener true
                    activeView = null

                    colorLoopRunnable?.let { handler.removeCallbacks(it) }

                    // Setting tint to null lets the view revert back to the dark gray rounded XML shape
                    view.backgroundTintList = null

                    val pressDuration = System.currentTimeMillis() - startTime
                    val dashThresh = tUnit * 1.5f
                    val spaceThresh = tUnit * 4.0f
                    val delThresh = tUnit * 7.0f

                    when {
                        pressDuration < spaceThresh -> {
                            // Register the action based on which mode is currently active
                            val addedSymbol = when (mode) {
                                Mode.SINGLE -> if (pressDuration >= dashThresh) "-" else "."
                                Mode.DOT -> "."
                                Mode.DASH -> "-"
                                Mode.SWIPE -> if (currentDiffX > swipeThreshPx) "-" else "."
                            }
                            currentSequence += addedSymbol

                            // Only apply adaptive speed mapping to the Single Button layout.
                            // (Because swipe speeds are geographic, mapping them to temporal timing would break the engine).
                            if (mode == Mode.SINGLE) {
                                val impliedT = if (pressDuration >= dashThresh) pressDuration / 3f else pressDuration.toFloat()
                                if (impliedT in 50f..400f) {
                                    tUnit = (tUnit * 0.9f) + (impliedT * 0.1f)
                                }
                            }

                            letterPauseRunnable = Runnable { commitSequenceToApp() }
                            handler.postDelayed(letterPauseRunnable!!, (tUnit * 3.0f).toLong())
                        }
                        pressDuration.toFloat() in spaceThresh..delThresh -> {
                            commitSequenceToApp()

                            val now = System.currentTimeMillis()
                            val ic = currentInputConnection
                            val textBefore = ic?.getTextBeforeCursor(2, 0)?.toString() ?: ""

                            // Double-space for smart punctuation
                            if (textBefore.endsWith(" ") && textBefore.trim().isNotEmpty() && (now - lastSpaceTime < 3000L)) {
                                ic?.deleteSurroundingText(1, 0)
                                ic?.commitText(". ", 1)
                                lastSpaceTime = 0L
                            } else {
                                ic?.commitText(" ", 1)
                                lastSpaceTime = now
                            }
                            vibrate(50)
                        }
                        else -> {
                            currentSequence = ""
                        }
                    }
                }
            }
            true
        }
    }

    private fun commitSequenceToApp() {
        if (currentSequence.isEmpty()) return
        val letter = morseMap[currentSequence] ?: "?"
        val finalLetter = getCapitalizedLetter(letter)
        currentInputConnection?.commitText(finalLetter, 1)
        currentSequence = ""
    }

    private fun getCapitalizedLetter(letter: String): String {
        val ic = currentInputConnection ?: return letter
        val textBefore = ic.getTextBeforeCursor(3, 0) ?: return letter.uppercase()
        val textStr = textBefore.toString()

        if (textStr.isEmpty()) return letter.uppercase()

        val trimmed = textStr.trimEnd()
        if (trimmed.isEmpty()) return letter.uppercase()

        val lastChar = trimmed.last()
        if (lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == '\n') {
            return letter.uppercase()
        }

        return letter
    }

    private fun vibrate(milliseconds: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }
}



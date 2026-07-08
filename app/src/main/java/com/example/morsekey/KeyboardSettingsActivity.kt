package com.example.morsekey

import android.content.Context
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class KeyboardSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard_settings) // A simple XML layout with a RadioGroup

        val radioGroup = findViewById<RadioGroup>(R.id.layoutSelectionGroup)
        val sharedPreferences = getSharedPreferences("MorseKeyPrefs", Context.MODE_PRIVATE)

        // Load the currently saved layout (defaulting to 'single')
        val currentLayout = sharedPreferences.getString("selected_layout", "single")
        when (currentLayout) {
            "single" -> radioGroup.check(R.id.radio_single_zone)
            "split" -> radioGroup.check(R.id.radio_split_zone)
            "swipe" -> radioGroup.check(R.id.radio_swipe_zone)
        }

        // Save the new choice when the user clicks a different option
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val editor = sharedPreferences.edit()
            when (checkedId) {
                R.id.radio_single_zone -> editor.putString("selected_layout", "single")
                R.id.radio_split_zone -> editor.putString("selected_layout", "split")
                R.id.radio_swipe_zone -> editor.putString("selected_layout", "swipe")
            }
            editor.apply()
        }
    }
}



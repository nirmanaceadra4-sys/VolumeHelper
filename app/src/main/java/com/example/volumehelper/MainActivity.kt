package com.example.volumehelper

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val am = getSystemService(AUDIO_SERVICE) as AudioManager

        findViewById<Button>(R.id.btnUp).setOnClickListener {
            am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
        }
        findViewById<Button>(R.id.btnDown).setOnClickListener {
            am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
        }
    }
}

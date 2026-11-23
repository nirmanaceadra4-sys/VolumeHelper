package com.example.volumehelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var am: AudioManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var panel: LinearLayout
    private var root: LinearLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        if (root == null) showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        root?.let {
            try {
                wm.removeView(it)
            } catch (e: Exception) {
            }
        }
        root = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("vol", "Volume button", NotificationManager.IMPORTANCE_LOW)
        )
        return Notification.Builder(this, "vol")
            .setContentTitle("Floating volume button is on")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .build()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun circle(text: String, sizeDp: Int, color: Int): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 22f
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(color)
        tv.background = bg
        val s = dp(sizeDp)
        val lp = LinearLayout.LayoutParams(s, s)
        lp.topMargin = dp(8)
        tv.layoutParams = lp
        return tv
    }

    private fun change(direction: Int) {
        am.adjustSuggestedStreamVolume(
            direction,
            AudioManager.USE_DEFAULT_STREAM_TYPE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun showOverlay() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        am = getSystemService(AUDIO_SERVICE) as AudioManager

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER_HORIZONTAL

        val bubble = circle("\uD83D\uDD0A", 52, 0xCC222222.toInt())
        val up = circle("+", 48, 0xCC1565C0.toInt())
        val down = circle("\u2212", 48, 0xCC1565C0.toInt())
        val close = circle("\u2715", 40, 0xCCB71C1C.toInt())

        panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.gravity = Gravity.CENTER_HORIZONTAL
        panel.visibility = View.GONE
        panel.addView(up)
        panel.addView(down)
        panel.addView(close)

        container.addView(bubble)
        container.addView(panel)

        up.setOnClickListener { change(AudioManager.ADJUST_RAISE) }
        down.setOnClickListener { change(AudioManager.ADJUST_LOWER) }
        close.setOnClickListener { stopSelf() }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = dp(200)

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = e.rawX
                    touchY = e.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    if (moved) {
                        params.x = startX + dx
                        params.y = startY + dy
                        wm.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        panel.visibility =
                            if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        wm.updateViewLayout(container, params)
                    }
                    true
                }
                else -> false
            }
        }

        root = container
        wm.addView(container, params)
    }
}

package com.example.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    // Xin quyền thông báo lúc chạy (Android 13+)
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationHelper.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val subtitle = findViewById<TextView>(R.id.tvTopSubtitle)

        nav.setOnItemSelectedListener { item ->
            val (fragment, title) = when (item.itemId) {
                R.id.nav_calendar -> CalendarFragment() to getString(R.string.nav_calendar)
                R.id.nav_upcoming -> UpcomingFragment() to getString(R.string.nav_upcoming)
                R.id.nav_filter -> SearchFragment() to "Tìm kiếm & Lọc"
                R.id.nav_settings -> SettingsFragment() to getString(R.string.nav_settings)
                else -> return@setOnItemSelectedListener false
            }
            show(fragment)
            subtitle.text = title
            true
        }

        findViewById<FloatingActionButton>(R.id.fabCreate).setOnClickListener {
            EventEditActivity.start(this)
        }
        findViewById<View>(R.id.btnSearch).setOnClickListener { nav.selectedItemId = R.id.nav_filter }
        findViewById<View>(R.id.btnBell).setOnClickListener { nav.selectedItemId = R.id.nav_upcoming }

        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_calendar
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}

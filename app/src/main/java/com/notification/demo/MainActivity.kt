package com.notification.demo

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.notification.demo.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), OnClickListener {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            // User allow the permission.
        } else {
            // User deny the permission.
        }
    }

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS)
        }
        binding.Normal.setOnClickListener(this)
        binding.NormalWithAction.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val cbSound = binding.cbSound.isChecked
        val cbLock = binding.cbLock.isChecked
        val cbHead = binding.cbHead.isChecked
        val cbAutoCancel = binding.cbAutoCancel.isChecked
        val cbOnly = binding.cbOnly.isChecked
        val context = v?.context ?: this
        when (v) {
            binding.Normal -> {
                NotificationUtil.normal(context, cbSound, cbLock, cbHead, cbAutoCancel, cbOnly)
            }
        }
    }

    // NotificationManager.areNotificationsEnabled()+.

    private fun jumpNotificationSetting() {
        val applicationInfo = applicationInfo
        try {
            val intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("app_package", applicationInfo.packageName)
            intent.putExtra("android.provider.extra.APP_PACKAGE", applicationInfo.packageName)
            intent.putExtra("app_uißd", applicationInfo.uid)
            startActivity(intent)
        } catch (t: Throwable) {
            t.printStackTrace()
            val intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
            intent.data = Uri.fromParts("package", applicationInfo.packageName, null)
            startActivity(intent)
        }
    }

}
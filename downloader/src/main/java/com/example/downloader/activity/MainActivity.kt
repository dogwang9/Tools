package com.example.downloader.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.downloader.R
import com.example.downloader.fragment.DownloadFragment
import com.example.downloader.fragment.HistoryFragment
import com.example.downloader.model.eventbus.ClearCompleteNotificationEvent
import com.example.downloader.service.DownloadService
import com.example.lib.utils.AndroidUtils
import com.example.lib.utils.PermissionUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus


class MainActivity : AppCompatActivity() {

    private lateinit var mViewPager2: ViewPager2
    private val mLauncher1: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val mLauncher2: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PermissionUtils.checkNotificationPermission(
                this
            )
        ) {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.suggestion)
                .setMessage(R.string.suggest_grant_notification_permission)
                .setNegativeButton(R.string.cancel) { dialog, which -> }
                .setPositiveButton(R.string.grant) { dialog, which ->
                    PermissionUtils.getNotificationPermission(this, mLauncher1, mLauncher2)
                }
                .show()
        }

        // 点击通知后触发清理已完成的通知
        if (DownloadService.CLEAR_COMPLETE_NOTIFICATION == intent?.getStringExtra(DownloadService.CLEAR_COMPLETE_NOTIFICATION)) {
            EventBus.getDefault().post(ClearCompleteNotificationEvent())
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // 点击通知后触发清理已完成的通知
        if (DownloadService.CLEAR_COMPLETE_NOTIFICATION == intent?.getStringExtra(DownloadService.CLEAR_COMPLETE_NOTIFICATION)) {
            EventBus.getDefault().post(ClearCompleteNotificationEvent())
        }
    }

    fun selectDownloadIndex() {
        if (mViewPager2.currentItem == 0) {
            return
        }
        mViewPager2.currentItem = 0
    }

    fun selectHistoryIndex() {
        if (mViewPager2.currentItem == 1) {
            return
        }
        mViewPager2.currentItem = 1
    }

    private fun initView() {
        mViewPager2 = findViewById(R.id.v_viewpager2)
        val navigationView = findViewById<BottomNavigationView>(R.id.v_bottom_navigation)
        mViewPager2.offscreenPageLimit = 2

        mViewPager2.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) {
                    DownloadFragment()
                } else {
                    HistoryFragment()
                }
            }

            override fun getItemCount(): Int {
                return 2
            }
        }

        navigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_download -> {
                    mViewPager2.currentItem = 0
                    true
                }

                R.id.navigation_history -> {
                    mViewPager2.currentItem = 1
                    true
                }

                else -> false
            }
        }

        mViewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                navigationView.menu[position].isChecked = true

                if (position != 0) {
                    AndroidUtils.hideKeyboard(this@MainActivity, navigationView)
                }
            }
        })
    }

}
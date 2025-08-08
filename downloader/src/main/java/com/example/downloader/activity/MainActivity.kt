package com.example.downloader.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
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
import com.example.lib.utils.AndroidUtils
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {
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
    }

    private fun initView() {
        val viewPager2 = findViewById<ViewPager2>(R.id.v_viewpager2)
        val navigationView = findViewById<BottomNavigationView>(R.id.v_bottom_navigation)
        viewPager2.offscreenPageLimit = 2

        viewPager2.adapter = object : FragmentStateAdapter(this) {
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
                    viewPager2.currentItem = 0
                    true
                }

                R.id.navigation_history -> {
                    viewPager2.currentItem = 1
                    true
                }

                else -> false
            }
        }

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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
package com.example.tool

import android.content.Intent
import android.os.Bundle
import com.example.lib.mvvm.BaseActivity
import com.example.tool.databinding.ActivityMainBinding
import com.example.tool.reactiontest.ReactionTestActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnReactionTest.setOnClickListener {
            startActivity(Intent(this, ReactionTestActivity::class.java))
        }
    }
}
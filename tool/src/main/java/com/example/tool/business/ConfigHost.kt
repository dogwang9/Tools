package com.example.tool.business

import android.content.Context
import androidx.core.content.edit
import com.example.lib.utils.AndroidUtils

object ConfigHost {

    private const val CONFIG_NAME = "main_config"
    private const val KEY_REACTION_TEST_RECORD = "reaction_test_record"

    fun getReactionTestRecord(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getLong(KEY_REACTION_TEST_RECORD, 0L)
    }

    fun setReactionTestRecord(context: Context, record: Long) {
        context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_REACTION_TEST_RECORD, record)
        }
    }

}
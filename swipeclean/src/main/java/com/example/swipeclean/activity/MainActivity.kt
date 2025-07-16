package com.example.swipeclean.activity

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lib.utils.PermissionUtils
import com.example.lib.utils.StringUtils
import com.example.swipeclean.adapter.AlbumAdapter
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.business.ConfigHost
import com.example.swipeclean.dialog.SortDialogFragment
import com.example.swipeclean.model.Album
import com.example.swipeclean.other.Constants.KEY_INTENT_ALBUM_ID
import com.example.swipeclean.other.Constants.MIN_SHOW_LOADING_TIME
import com.example.tools.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mContentView: View
    private lateinit var mAlbumsView: View
    private lateinit var mEmptyView: View
    private lateinit var mCompletedTextView: TextView
    private lateinit var mCleanedTextView: TextView
    private lateinit var mSortButton: MaterialButton
    private lateinit var mAdapter: AlbumAdapter
    private lateinit var mLoadingView: View
    private val launcher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            prepareData()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (PermissionUtils.checkReadImagePermission(this)) {
            lifecycleScope.launch(Dispatchers.IO) {
                AlbumController.syncDatabase()
            }
        }

        initView()
    }

    override fun onStart() {
        super.onStart()
        prepareData()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (mLoadingView.isVisible) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun initView() {
        mRecyclerView = findViewById(R.id.v_recyclerview)
        mContentView = findViewById(R.id.v_content)
        mEmptyView = findViewById(R.id.v_empty)
        mCompletedTextView = findViewById(R.id.tv_completed_content)
        mCleanedTextView = findViewById(R.id.tv_cleaned_content)
        mSortButton = findViewById(R.id.btn_sort_order)
        mAlbumsView = findViewById(R.id.v_albums)
        mLoadingView = findViewById(R.id.v_loading)

        mAdapter = AlbumAdapter { albumId, albumFormatDate, completed ->
            if (completed) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(albumFormatDate)
                    .setMessage("要再次清理此文件夹中的图片吗")
                    .setCancelable(false)
                    .setNegativeButton("取消") { dialog, which -> }
                    .setPositiveButton("确认") { dialog, which ->
                        val album: Album? =
                            AlbumController.getAlbums().find { it.getId() == albumId }

                        if (album?.photos?.isNotEmpty() == true) {
                            mLoadingView.visibility = View.VISIBLE
                            val startTime = SystemClock.elapsedRealtime()

                            lifecycleScope.launch(Dispatchers.IO) {
                                for (photo in album.photos) {
                                    photo.cancelOperated()
                                    AlbumController.cleanCompletedPhoto(photo)
                                }

                                runOnUiThread {
                                    val intent = Intent(
                                        this@MainActivity,
                                        OperationActivity::class.java
                                    )
                                    intent.putExtra(KEY_INTENT_ALBUM_ID, albumId)
                                    val spendTime = SystemClock.elapsedRealtime() - startTime
                                    mRecyclerView.postDelayed(
                                        {
                                            mLoadingView.visibility = View.GONE
                                            startActivity(intent)
                                        }, MIN_SHOW_LOADING_TIME - spendTime
                                    )
                                }
                            }
                        }
                    }
                    .show()

            } else {
                val intent = Intent(
                    this@MainActivity,
                    if (isAlbumOperated(albumId)) RecycleBinActivity::class.java else OperationActivity::class.java
                )
                intent.putExtra(KEY_INTENT_ALBUM_ID, albumId)
                startActivity(intent)
            }
        }
        mAdapter.setHasStableIds(true)

        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.setLayoutManager(LinearLayoutManager(this))
        mRecyclerView.setAdapter(mAdapter)

        findViewById<View>(R.id.btn_sort_order).setOnClickListener {
            SortDialogFragment.newInstance().show(supportFragmentManager, "SortDialogFragment")
        }
    }

    fun loadAlbums() {
        mLoadingView.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val startTime = SystemClock.elapsedRealtime()
            val albums = AlbumController.loadAlbums()

            runOnUiThread {
                val spendTime = SystemClock.elapsedRealtime() - startTime
                mRecyclerView.postDelayed(
                    {
                        mLoadingView.visibility = View.GONE
                        if (albums.isEmpty()) {
                            mEmptyView.visibility = View.VISIBLE
                            mAlbumsView.visibility = View.GONE
                            mSortButton.visibility = View.GONE

                        } else {
                            mSortButton.visibility = View.VISIBLE
                            mEmptyView.visibility = View.GONE
                            mAlbumsView.visibility = View.VISIBLE

                            mCleanedTextView.text =
                                StringUtils.getHumanFriendlyByteCount(
                                    ConfigHost.getCleanedSize(
                                        this@MainActivity
                                    ), 1
                                )

                            mCompletedTextView.text =
                                String.format(
                                    Locale.getDefault(),
                                    "%d/%d",
                                    albums.stream().filter(Album::isCompleted).count(),
                                    albums.size
                                )

                            sortAlbums(albums)
                        }
                    },
                    MIN_SHOW_LOADING_TIME - spendTime
                )
            }
        }
    }

    private fun prepareData() {
        if (!PermissionUtils.checkReadImagePermission(this)
        ) {
            MaterialAlertDialogBuilder(this)
                .setTitle("请授权")
                .setMessage("授权以访问设备上的图片")
                .setCancelable(false)
                .setNegativeButton("关闭") { dialog, which ->
                    finish()
                }
                .setPositiveButton("去授权") { dialog, which ->
                    PermissionUtils.getReadImagePermission(this, launcher)
                }
                .show()

        } else {
            loadAlbums()
        }
    }

    private fun sortAlbums(albums: ArrayList<Album>) {
        when (ConfigHost.getSortType(this)) {
            SortDialogFragment.DATE_DOWN -> {
                albums.sortByDescending(Album::getDateTime)
            }

            SortDialogFragment.DATE_UP -> {
                albums.sortBy(Album::getDateTime)
            }

            SortDialogFragment.SIZE_DOWN -> {
                albums.sortByDescending(Album::getTotalCount)
            }

            SortDialogFragment.SIZE_UP -> {
                albums.sortBy(Album::getTotalCount)
            }

            SortDialogFragment.UNFINISHED_DOWN -> {
                albums.sortBy(Album::isOperated)
            }

            SortDialogFragment.UNFINISHED_UP -> {
                albums.sortByDescending(Album::isOperated)
            }
        }
        mAdapter.setData(albums)
        mRecyclerView.scrollToPosition(0)
    }

    private fun isAlbumOperated(albumId: Long): Boolean {
        val album = AlbumController.getAlbums()
            .find { it.getId() == albumId }

        return album?.photos.isNullOrEmpty() || album.isOperated()
    }
}
package com.example.swipeclean.activity

import android.content.res.Resources
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lib.utils.MediaStoreUtils
import com.example.lib.utils.StringUtils.getHumanFriendlyByteCount
import com.example.swipeclean.adapter.RecyclerBinAdapter
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.business.ConfigHost
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Photo
import com.example.swipeclean.other.Constants.KEY_INTENT_ALBUM_ID
import com.example.swipeclean.other.Constants.MIN_SHOW_LOADING_TIME
import com.example.tools.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.loadingindicator.LoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections
import java.util.Locale

class RecycleBinActivity : AppCompatActivity() {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: RecyclerBinAdapter
    private lateinit var mEmptyTrashButton: View
    private lateinit var mRestoreAllButton: View
    private lateinit var mBackButton: MaterialButton
    private lateinit var mTitleTextView: TextView
    private lateinit var mLoadingView: LoadingIndicator

    private var mAlbum: Album? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recycle_bin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()

        mAlbum = AlbumController.getAlbums().find { item ->
            item.getId() == intent.getLongExtra(KEY_INTENT_ALBUM_ID, 0)
        }

        showDeletedPhotos(mAlbum?.photos?.filter { item -> item.isDelete })
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (mLoadingView.isVisible) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun initView() {
        mRecyclerView = findViewById(R.id.v_recyclerview)
        mEmptyTrashButton = findViewById(R.id.btn_empty_trash)
        mRestoreAllButton = findViewById(R.id.btn_restore_all)
        mBackButton = findViewById(R.id.iv_back)
        mTitleTextView = findViewById(R.id.tv_title)
        mLoadingView = findViewById(R.id.v_loading)

        mBackButton.setOnClickListener { finish() }
    }

    private fun showDeletedPhotos(deletedPhotos: List<Photo>?) {
        if (deletedPhotos.isNullOrEmpty()) {
            finish()
            return
        }
        Collections.reverse(deletedPhotos)
        mAdapter = RecyclerBinAdapter(
            deletedPhotos.toMutableList()
        ) { photo, position ->
            mAdapter.notifyItemRemoved(position)
            mAdapter.removePhoto(photo)
            showTotalSize(mAdapter.getTotalSize())
            lifecycleScope.launch(Dispatchers.IO) {
                AlbumController.converseDeleteToKeepPhoto(photo)
            }
            photo.isKeep = true
            photo.isDelete = false

            if (mAdapter.photos.isEmpty()) {
                finish()
            }
        }

        val spanCount =
            3.coerceAtLeast((Resources.getSystem().displayMetrics.widthPixels / (140 * Resources.getSystem().displayMetrics.density)).toInt())
        val layoutManager = GridLayoutManager(this, spanCount)

        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = layoutManager

        showTotalSize(mAdapter.getTotalSize())

        mEmptyTrashButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("删除图片")
                .setMessage("一旦删除，图像将无法恢复")
                .setPositiveButton(
                    "删除"
                ) { _, _ ->
                    mLoadingView.visibility = View.VISIBLE
                    val startTime = SystemClock.elapsedRealtime()

                    lifecycleScope.launch(Dispatchers.IO) {
                        ConfigHost.setCleanedSize(
                            mAdapter.getTotalSize(),
                            this@RecycleBinActivity
                        )
                        val destPaths: MutableList<String> = ArrayList()
                        var finishCount = 0

                        for (photo in deletedPhotos) {
                            finishCount++
                            destPaths.add(photo.path)
                            mAlbum?.photos?.remove(photo)
                            AlbumController.cleanCompletedPhoto(photo)

                            val file = File(photo.path)
                            if (file.exists()) {
                                file.delete()
                            }

                            if (finishCount % 100 == 0) {
                                MediaStoreUtils.scanSync(this@RecycleBinActivity, destPaths)
                                destPaths.clear()
                            }
                        }

                        MediaStoreUtils.scanSync(this@RecycleBinActivity, destPaths)

                        runOnUiThread {
                            val spendTime = SystemClock.elapsedRealtime() - startTime
                            mEmptyTrashButton.postDelayed(
                                {
                                    mLoadingView.visibility = View.GONE
                                    showDeleteResult()
                                },
                                MIN_SHOW_LOADING_TIME - spendTime
                            )
                        }
                    }
                }
                .setNegativeButton("取消") { _, _ -> }
                .show()
        }

        mRestoreAllButton.setOnClickListener {
            mLoadingView.visibility = View.VISIBLE
            val startTime = SystemClock.elapsedRealtime()
            showTotalSize(0)
            mRecyclerView.visibility = View.GONE
            lifecycleScope.launch(Dispatchers.IO) {
                for (photo in mAdapter.photos) {
                    photo.isDelete = false
                    photo.isKeep = true
                    AlbumController.converseDeleteToKeepPhoto(photo)
                }

                runOnUiThread {
                    val spendTime = SystemClock.elapsedRealtime() - startTime
                    mLoadingView.postDelayed(
                        {
                            mLoadingView.visibility = View.GONE
                            finish()
                        },
                        MIN_SHOW_LOADING_TIME - spendTime
                    )
                }
            }
        }
    }

    private fun showDeleteResult() {
        (findViewById<TextView>(R.id.tv_free_up_size)!!).text =
            getHumanFriendlyByteCount(mAdapter.getTotalSize(), 1)
        (findViewById<TextView>(R.id.tv_deleted_count)!!).text =
            String.format(
                Locale.getDefault(),
                "%d张图片",
                mAdapter.photos.size
            )

        findViewById<View>(R.id.v_trash_bin).visibility = View.GONE
        findViewById<View>(R.id.v_complete).visibility = View.VISIBLE

        mTitleTextView.text = mAlbum?.formatData
        findViewById<View>(R.id.btn_got_it).setOnClickListener { finish() }
    }

    private fun showTotalSize(totalSize: Long) {
        mTitleTextView.text = String.format(
            Locale.getDefault(),
            "%s (%s)",
            "垃圾箱",
            getHumanFriendlyByteCount(totalSize, 1)
        )
    }
}
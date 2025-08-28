package com.example.swipeclean.activity

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.component1
import androidx.activity.result.component2
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
    private val mPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            initData()
        }

    private val mOperationLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { (resultCode, data) ->
            if (resultCode == RESULT_OK) {
                val albumId = data?.getLongExtra(KEY_INTENT_ALBUM_ID, 0L)
                val index = mAdapter.albums.indexOfFirst { it.getId() == albumId }
                if (index != -1) {
                    mAdapter.notifyItemChanged(index)
                }
            }
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (mLoadingView.isVisible) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    fun onChangeSort() {
        sortAlbums(mAdapter.albums)
        mAdapter.notifyDataSetChanged()
        mRecyclerView.scrollToPosition(0)
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

        initData()
    }

    private fun initData() {
        if (!PermissionUtils.checkReadImagePermission(this)
        ) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_reminder)
                .setMessage(R.string.dialog_notification_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel) { dialog, which ->
                    finish()
                }
                .setPositiveButton(R.string.grant_permission) { dialog, which ->
                    PermissionUtils.getReadImagePermission(this, mPermissionLauncher)
                }
                .show()
            return
        }

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

                            mAdapter =
                                AlbumAdapter(sortAlbums(albums)) { albumId, albumFormatDate, completed ->
                                    if (completed) {
                                        MaterialAlertDialogBuilder(this@MainActivity)
                                            .setTitle(albumFormatDate)
                                            .setMessage(R.string.dialog_clean_album_again_message)
                                            .setCancelable(false)
                                            .setNegativeButton(R.string.cancel) { dialog, which -> }
                                            .setPositiveButton(R.string.clean) { dialog, which ->
                                                val album: Album? =
                                                    AlbumController.getAlbums()
                                                        .find { it.getId() == albumId }

                                                if (album?.photos?.isNotEmpty() == true) {
                                                    mLoadingView.visibility = View.VISIBLE
                                                    val startTime = SystemClock.elapsedRealtime()

                                                    lifecycleScope.launch(Dispatchers.IO) {
                                                        album.photos.let { photos ->
                                                            AlbumController.cleanCompletedPhoto(
                                                                photos
                                                            )
                                                            photos.forEach { it.cancelOperated() }
                                                        }

                                                        runOnUiThread {
                                                            val intent = Intent(
                                                                this@MainActivity,
                                                                OperationActivity::class.java
                                                            )
                                                            intent.putExtra(
                                                                KEY_INTENT_ALBUM_ID,
                                                                albumId
                                                            )
                                                            val spendTime =
                                                                SystemClock.elapsedRealtime() - startTime
                                                            mRecyclerView.postDelayed(
                                                                {
                                                                    mLoadingView.visibility =
                                                                        View.GONE
                                                                    mOperationLauncher.launch(intent)
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
                                        mOperationLauncher.launch(intent)
                                    }
                                }
                            mAdapter.setHasStableIds(true)

                            mRecyclerView.setHasFixedSize(true)
                            mRecyclerView.setLayoutManager(LinearLayoutManager(this@MainActivity))
                            mRecyclerView.setAdapter(mAdapter)

                            findViewById<View>(R.id.btn_sort_order).setOnClickListener {
                                SortDialogFragment.newInstance()
                                    .show(supportFragmentManager, "SortDialogFragment")
                            }
                        }
                    },
                    MIN_SHOW_LOADING_TIME - spendTime
                )
            }
        }
    }

    private fun sortAlbums(albums: ArrayList<Album>): ArrayList<Album> {
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
        return albums
    }

    private fun isAlbumOperated(albumId: Long): Boolean {
        val album = AlbumController.getAlbums()
            .find { it.getId() == albumId }

        return album?.photos.isNullOrEmpty() || album.isOperated()
    }
}
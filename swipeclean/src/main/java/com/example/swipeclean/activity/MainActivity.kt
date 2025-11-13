package com.example.swipeclean.activity

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lib.mvvm.BaseActivity
import com.example.lib.utils.AndroidUtils
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
import com.example.tools.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : BaseActivity<ActivityMainBinding>() {
    private lateinit var mAdapter: AlbumAdapter
    private val mPermissionLauncher1: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            initData()
        }

    private val mPermissionLauncher2: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { (resultCode, data) ->
            //如果此处有回调 证明用户是通过intent跳转到了系统的授权界面了 此时应该判断获取的是部分访问还是完全访问还是不允许访问
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

        if (PermissionUtils.checkReadImagePermission(this)) {
            lifecycleScope.launch(Dispatchers.IO) {
                AlbumController.syncDatabase()
            }
        }

        initData()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (binding.vLoading.isVisible) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    fun onChangeSort() {
        sortAlbums(mAdapter.albums)
        mAdapter.notifyDataSetChanged()
        binding.rvAlbums.scrollToPosition(0)
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
                    PermissionUtils.getReadImagePermission(
                        this,
                        mPermissionLauncher1,
                        mPermissionLauncher2
                    )
                }
                .show()
            return
        }

        binding.vLoading.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val startTime = SystemClock.elapsedRealtime()
            val albums = AlbumController.loadAlbums()

            runOnUiThread {
                val spendTime = SystemClock.elapsedRealtime() - startTime
                binding.rvAlbums.postDelayed(
                    {
                        binding.vLoading.visibility = View.GONE
                        if (albums.isEmpty()) {
                            binding.clEmpty.visibility = View.VISIBLE
                            binding.clAlbums.visibility = View.GONE
                            binding.btnSortOrder.visibility = View.GONE

                        } else {
                            binding.btnSortOrder.visibility = View.VISIBLE
                            binding.clEmpty.visibility = View.GONE
                            binding.clAlbums.visibility = View.VISIBLE

                            binding.tvCleanedContent.text =
                                StringUtils.getHumanFriendlyByteCount(
                                    ConfigHost.getCleanedSize(
                                        this@MainActivity
                                    ), 1
                                )

                            binding.tvCompletedContent.text =
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

                                                if (album?.images?.isNotEmpty() == true) {
                                                    binding.vLoading.visibility = View.VISIBLE
                                                    val startTime = SystemClock.elapsedRealtime()

                                                    lifecycleScope.launch(Dispatchers.IO) {
                                                        album.images.let { photos ->
                                                            AlbumController.cleanCompletedImage(
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
                                                            binding.rvAlbums.postDelayed(
                                                                {
                                                                    binding.vLoading.visibility =
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

                            binding.rvAlbums.addItemDecoration(object :
                                RecyclerView.ItemDecoration() {
                                override fun getItemOffsets(
                                    outRect: Rect,
                                    view: View,
                                    parent: RecyclerView,
                                    state: RecyclerView.State
                                ) {
                                    super.getItemOffsets(outRect, view, parent, state)
                                    if (parent.getChildAdapterPosition(view) == mAdapter.itemCount - 1) {
                                        outRect.bottom =
                                            AndroidUtils.getNavigationBarHeight(this@MainActivity)
                                    }
                                }
                            })

                            binding.rvAlbums.setHasFixedSize(true)
                            binding.rvAlbums.setLayoutManager(LinearLayoutManager(this@MainActivity))
                            binding.rvAlbums.setAdapter(mAdapter)

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

        return album?.images.isNullOrEmpty() || album.isOperated()
    }
}
package com.example.swipeclean.activity

import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lib.photoview.PhotoViewFragment
import com.example.lib.utils.PermissionUtils
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections

class RecycleBinActivity : AppCompatActivity(), PhotoViewFragment.Listener {
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: RecyclerBinAdapter
    private lateinit var mEmptyTrashButton: View
    private lateinit var mRestoreAllButton: View
    private lateinit var mBackButton: MaterialButton
    private lateinit var mTitleTextView: TextView
    private lateinit var mLoadingView: LoadingIndicator
    private var mAlbum: Album? = null

    private val newDeleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                mLoadingView.visibility = View.VISIBLE
                lifecycleScope.launch(Dispatchers.IO) {
                    ConfigHost.setCleanedSize(
                        mAdapter.getTotalSize(),
                        this@RecycleBinActivity
                    )
                    mAdapter.photos.let {
                        AlbumController.cleanCompletedPhoto(it)
                        mAlbum?.photos?.removeAll(it)
                    }

                    delay(MIN_SHOW_LOADING_TIME)

                    runOnUiThread {
                        mLoadingView.visibility = View.GONE
                        showDeleteResult()
                    }
                }
            }
        }

    private val oldDeleteLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                useOldDelete()
            }
        }

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

        showDeletedPhotos(mAlbum?.photos?.filter { item -> item.isDelete() })
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (mLoadingView.isVisible) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun showPhoto(imageView: ImageView, index: Int) {
        Glide
            .with(this)
            .load(mAdapter.photos[index].sourceUri)
            .into(imageView)
    }

    override fun getRecyclerView(): RecyclerView {
        return mRecyclerView
    }

    override fun getUri(position: Int): Uri? {
        return mAdapter.photos[position].sourceUri
    }

    override fun getImageView(position: Int): ImageView? {
        val holder = mRecyclerView.findViewHolderForAdapterPosition(position)
        return if (holder is RecyclerBinAdapter.MyViewHolder) {
            holder.photoImageView
        } else {
            null
        }
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
            deletedPhotos.toMutableList(),
            { photo, position ->
                mAdapter.notifyItemRemoved(position)
                mAdapter.removePhoto(photo)
                showTotalSize(mAdapter.getTotalSize())
                photo.doKeep()
                lifecycleScope.launch(Dispatchers.IO) {
                    AlbumController.converseDeleteToKeepPhoto(photo)
                }

                if (mAdapter.photos.isEmpty()) {
                    finish()
                }
            }, { photoImageView, photo, position ->
                photo.sourceUri?.let {
                    PhotoViewFragment.show(
                        this,
                        position,
                        mAdapter.photos.size,
                        it,
                        photoImageView
                    )
                }
            }
        )

        val spanCount =
            3.coerceAtLeast((Resources.getSystem().displayMetrics.widthPixels / (140 * Resources.getSystem().displayMetrics.density)).toInt())
        val layoutManager = GridLayoutManager(this, spanCount)

        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = layoutManager

        showTotalSize(mAdapter.getTotalSize())

        mEmptyTrashButton.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_picture)
                    .setMessage(R.string.dialog_delete_picture_message)
                    .setPositiveButton(
                        R.string.delete
                    ) { _, _ ->
                        if (PermissionUtils.checkWritePermission(this)) {
                            useOldDelete()

                        } else {
                            PermissionUtils.getWritePermission(this, oldDeleteLauncher)
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()

            } else {
                newDeleteLauncher.launch(
                    IntentSenderRequest.Builder(
                        MediaStore.createDeleteRequest(
                            contentResolver,
                            mAdapter.photos.map { it.sourceUri }).intentSender
                    ).build()
                )
            }
        }

        mRestoreAllButton.setOnClickListener {
            mLoadingView.visibility = View.VISIBLE
            val startTime = SystemClock.elapsedRealtime()
            showTotalSize(0)
            mRecyclerView.visibility = View.GONE
            lifecycleScope.launch(Dispatchers.IO) {
                mAdapter.photos.let { photos ->
                    AlbumController.converseDeleteToKeepPhoto(photos)
                    photos.forEach { it.doKeep() }
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
        (findViewById<TextView>(R.id.tv_deleted_count)!!).text = resources.getQuantityString(
            R.plurals.picture_count,
            mAdapter.photos.size,
            mAdapter.photos.size
        )

        findViewById<View>(R.id.v_trash_bin).visibility = View.GONE
        findViewById<View>(R.id.v_complete).visibility = View.VISIBLE

        mTitleTextView.text = mAlbum?.formatData
        findViewById<View>(R.id.btn_got_it).setOnClickListener { finish() }
    }

    private fun showTotalSize(totalSize: Long) {
        mTitleTextView.text =
            getString(R.string.trash_bin_size, getHumanFriendlyByteCount(totalSize, 1))
    }

    private fun useOldDelete() {
        mLoadingView.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            ConfigHost.setCleanedSize(
                mAdapter.getTotalSize(),
                this@RecycleBinActivity
            )

            mAdapter.photos.let { deletePhotos ->
                AlbumController.cleanCompletedPhoto(deletePhotos)
                mAlbum?.photos?.removeAll(deletePhotos)

                deletePhotos
                    .mapNotNull { it.sourceUri }
                    .forEach { contentResolver.delete(it, null, null) }
            }
            delay(MIN_SHOW_LOADING_TIME)

            runOnUiThread {
                mLoadingView.visibility = View.GONE
                showDeleteResult()
            }
        }
    }
}
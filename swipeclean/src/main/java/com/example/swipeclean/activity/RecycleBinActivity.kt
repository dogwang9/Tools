package com.example.swipeclean.activity

import android.content.Intent
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lib.mvvm.BaseActivity
import com.example.lib.photoview.PhotoViewFragment
import com.example.lib.utils.PermissionUtils
import com.example.lib.utils.StringUtils.getHumanFriendlyByteCount
import com.example.swipeclean.adapter.RecyclerBinAdapter
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.business.ConfigHost
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Image
import com.example.swipeclean.other.Constants.KEY_INTENT_ALBUM_ID
import com.example.swipeclean.other.Constants.MIN_SHOW_LOADING_TIME
import com.example.tools.R
import com.example.tools.databinding.ActivityRecycleBinBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections

class RecycleBinActivity : BaseActivity<ActivityRecycleBinBinding>(), PhotoViewFragment.Listener {
    private lateinit var mAdapter: RecyclerBinAdapter
    private var mAlbum: Album? = null

    private val newDeleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                binding.vLoading.visibility = View.VISIBLE
                lifecycleScope.launch(Dispatchers.IO) {
                    ConfigHost.setCleanedSize(
                        mAdapter.getTotalSize(),
                        this@RecycleBinActivity
                    )
                    mAdapter.images.let {
                        AlbumController.cleanCompletedImage(it)
                        mAlbum?.images?.removeAll(it)
                    }

                    delay(MIN_SHOW_LOADING_TIME)

                    runOnUiThread {
                        binding.vLoading.visibility = View.GONE
                        showDeleteResult()
                    }
                }
            }
        }

    private val mOldDeleteLauncher1: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                useOldDelete()
            }
        }

    private val mOldDeleteLauncher2: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { (resultCode, data) ->
            if (PermissionUtils.checkWritePermission(this)) {
                useOldDelete()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()

        mAlbum = AlbumController.getAlbums().find { item ->
            item.getId() == intent.getLongExtra(KEY_INTENT_ALBUM_ID, 0)
        }

        showDeletedPhotos(mAlbum?.images?.filter { item -> item.isDelete() })
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (binding.vLoading.isVisible) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun showPhoto(imageView: ImageView, index: Int) {
        Glide
            .with(this)
            .load(mAdapter.images[index].sourceUri)
            .into(imageView)
    }

    override fun getRecyclerView(): RecyclerView {
        return binding.rvPhotos
    }

    override fun getUri(position: Int): Uri? {
        return mAdapter.images[position].sourceUri
    }

    override fun getImageView(position: Int): ImageView? {
        val holder = binding.rvPhotos.findViewHolderForAdapterPosition(position)
        return if (holder is RecyclerBinAdapter.MyViewHolder) {
            holder.binding.ivCover
        } else {
            null
        }
    }

    private fun initView() {
        binding.ivBack.setOnClickListener { finish() }
    }

    private fun showDeletedPhotos(deletedImages: List<Image>?) {
        if (deletedImages.isNullOrEmpty()) {
            finish()
            return
        }
        Collections.reverse(deletedImages)
        mAdapter = RecyclerBinAdapter(
            deletedImages.toMutableList(),
            { photo, position ->
                mAdapter.notifyItemRemoved(position)
                mAdapter.removeImage(photo)
                showTotalSize(mAdapter.getTotalSize())
                photo.doKeep()
                lifecycleScope.launch(Dispatchers.IO) {
                    AlbumController.converseDeleteToKeepImage(photo)
                }

                if (mAdapter.images.isEmpty()) {
                    finish()
                }
            }, { photoImageView, photo, position ->
                photo.sourceUri?.let {
                    PhotoViewFragment.show(
                        this,
                        position,
                        mAdapter.images.size,
                        it,
                        photoImageView
                    )
                }
            }
        )

        val spanCount =
            3.coerceAtLeast((Resources.getSystem().displayMetrics.widthPixels / (140 * Resources.getSystem().displayMetrics.density)).toInt())
        val layoutManager = GridLayoutManager(this, spanCount)

        binding.rvPhotos.adapter = mAdapter
        binding.rvPhotos.layoutManager = layoutManager

        showTotalSize(mAdapter.getTotalSize())

        binding.btnEmptyTrash.setOnClickListener {
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
                            PermissionUtils.getWritePermission(
                                this,
                                mOldDeleteLauncher1,
                                mOldDeleteLauncher2
                            )
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()

            } else {
                newDeleteLauncher.launch(
                    IntentSenderRequest.Builder(
                        MediaStore.createDeleteRequest(
                            contentResolver,
                            mAdapter.images.map { it.sourceUri }).intentSender
                    ).build()
                )
            }
        }

        binding.btnRestoreAll.setOnClickListener {
            binding.vLoading.visibility = View.VISIBLE
            val startTime = SystemClock.elapsedRealtime()
            showTotalSize(0)
            binding.rvPhotos.visibility = View.GONE
            lifecycleScope.launch(Dispatchers.IO) {
                mAdapter.images.let { photos ->
                    AlbumController.converseDeleteToKeepImage(photos)
                    photos.forEach { it.doKeep() }
                }

                runOnUiThread {
                    val spendTime = SystemClock.elapsedRealtime() - startTime
                    binding.vLoading.postDelayed(
                        {
                            binding.vLoading.visibility = View.GONE
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
            mAdapter.images.size,
            mAdapter.images.size
        )

        findViewById<View>(R.id.cl_trash_bin).visibility = View.GONE
        findViewById<View>(R.id.cl_complete).visibility = View.VISIBLE

        binding.tvTitle.text = mAlbum?.formatData
        findViewById<View>(R.id.btn_got_it).setOnClickListener { finish() }
    }

    private fun showTotalSize(totalSize: Long) {
        binding.tvTitle.text =
            getString(R.string.trash_bin_size, getHumanFriendlyByteCount(totalSize, 1))
    }

    private fun useOldDelete() {
        binding.vLoading.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            ConfigHost.setCleanedSize(
                mAdapter.getTotalSize(),
                this@RecycleBinActivity
            )

            mAdapter.images.let { deletePhotos ->
                AlbumController.cleanCompletedImage(deletePhotos)
                mAlbum?.images?.removeAll(deletePhotos)

                deletePhotos
                    .mapNotNull { it.sourceUri }
                    .forEach { contentResolver.delete(it, null, null) }
            }
            delay(MIN_SHOW_LOADING_TIME)

            runOnUiThread {
                binding.vLoading.visibility = View.GONE
                showDeleteResult()
            }
        }
    }
}
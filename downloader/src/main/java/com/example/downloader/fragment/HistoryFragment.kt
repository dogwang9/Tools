package com.example.downloader.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.downloader.MyApplication
import com.example.downloader.R
import com.example.downloader.adapter.TaskHistoryAdapter
import com.example.downloader.dialog.HistoryDetailDialogFragment
import com.example.downloader.model.TaskHistory
import com.example.downloader.model.eventbus.AddHistoryMessage
import com.example.downloader.model.eventbus.DeleteHistoryMessage
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class HistoryFragment : Fragment() {

    private lateinit var mAdapter: TaskHistoryAdapter
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mSourcesContainer: HorizontalScrollView
    private lateinit var mAllChip: Chip
    private lateinit var mSourcesGroup: ChipGroup
    private val mSources: ArrayList<String> = ArrayList<String>()
    private var mCurrentSource = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        initView(view)
        return view
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(addHistoryMessage: AddHistoryMessage) {
        if (mCurrentSource == "All" || mCurrentSource == addHistoryMessage.taskHistory.source) {
            mAdapter.insertItem(addHistoryMessage.taskHistory)
            mRecyclerView.scrollToPosition(mAdapter.itemCount - 1)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(deleteHistoryMessage: DeleteHistoryMessage) {
        mAdapter.deleteItem(deleteHistoryMessage.id)
    }

    private fun initView(view: View) {
        mRecyclerView = view.findViewById<RecyclerView>(R.id.v_recyclerview)
        mSourcesContainer = view.findViewById<HorizontalScrollView>(R.id.hs_source)
        mSourcesGroup = view.findViewById<ChipGroup>(R.id.cg_source)
        mAllChip = view.findViewById<Chip>(R.id.chip_all)

        mRecyclerView.setHasFixedSize(true)
        mAdapter = TaskHistoryAdapter()
        mAdapter.setHasStableIds(true)
        mAdapter.onMoreClick = { taskHistory, position ->
            val historyDetailDialogFragment =
                HistoryDetailDialogFragment.newInstance(taskHistory)
            historyDetailDialogFragment.show(childFragmentManager, "HistoryDetailDialogFragment")
        }
        context?.let {
            val layoutManager = LinearLayoutManager(it)
            layoutManager.reverseLayout = true
            layoutManager.stackFromEnd = true
            mRecyclerView.layoutManager = layoutManager
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val allHistory = MyApplication.database.historyDao().queryAll()
            allHistory.forEach { history ->
                val file = history.path?.let { File(it) }

                if (file?.exists() == true) {
                    history.available = true
                    history.size = file.length()

                } else {
                    history.available = false
                }

                if (!TextUtils.isEmpty(history.source) && !mSources.contains(history.source)) {
                    mSources.add(history.source!!)
                }
            }

            withContext(Dispatchers.Main) {
                mAdapter.setData(ArrayList<TaskHistory>(allHistory))
                mRecyclerView.adapter = mAdapter

                if (mSources.size > 1) {
                    mSourcesContainer.visibility = View.VISIBLE

                    mAllChip.setOnClickListener {
                        mCurrentSource = "All"
                        loadHistory()
                    }

                    mSources.forEach { source ->
                        val chip = Chip(context)
                        chip.text = source
                        chip.isCheckable = true
                        chip.setOnClickListener {
                            mCurrentSource = source
                            loadHistory()
                        }
                        mSourcesGroup.addView(chip)
                    }

                } else {
                    mSourcesContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val histories = if ("All" == mCurrentSource) {
                MyApplication.database.historyDao().queryAll()
            } else {
                MyApplication.database.historyDao().queryBySource(mCurrentSource)
            }

            histories.forEach { history ->
                val file = history.path?.let { File(it) }

                if (file?.exists() == true) {
                    history.available = true
                    history.size = file.length()

                } else {
                    history.available = false
                }
            }

            withContext(Dispatchers.Main) {
                mAdapter.setData(ArrayList<TaskHistory>(histories))
            }
        }
    }
}
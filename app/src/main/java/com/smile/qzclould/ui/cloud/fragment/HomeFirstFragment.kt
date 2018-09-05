package com.smile.qzclould.ui.cloud.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import androidx.navigation.Navigation
import com.chad.library.adapter.base.BaseQuickAdapter
import com.smile.qielive.common.BaseFragment
import com.smile.qzclould.R
import com.smile.qzclould.common.App
import com.smile.qzclould.common.Constants
import com.smile.qzclould.ui.cloud.activity.FolderDetailActivity
import com.smile.qzclould.ui.cloud.adapter.FileListAdapter
import com.smile.qzclould.ui.cloud.bean.DirecotoryBean
import com.smile.qzclould.ui.cloud.dialog.BuildNewFolderDialog
import com.smile.qzclould.ui.cloud.viewmodel.CloudViewModel
import com.smile.qzclould.utils.DLog
import kotlinx.android.synthetic.main.frag_home_first.*
import kotlinx.android.synthetic.main.view_search_bar.*

class HomeFirstFragment: BaseFragment() {

    private val mModel by lazy { ViewModelProviders.of(this).get(CloudViewModel::class.java) }

    private val mDialog by lazy { BuildNewFolderDialog() }
    private val mLayoutManager by lazy { LinearLayoutManager(mActivity) }
    private val mAdapter by lazy { FileListAdapter() }
    private val mPageSize = 20

    private var mOffset = 0

    private var mOrderType = 0 //排序 0按 文件名 1 按时间

    override fun getLayoutId(): Int {
        return R.layout.frag_home_first
    }

    override fun initData() {
        loadFileList()
    }

    override fun initView(savedInstanceState: Bundle?) {
        mRvFile.layoutManager = mLayoutManager
        mAdapter.bindToRecyclerView(mRvFile)
    }

    override fun initListener() {
        mNewFloder.setOnClickListener {
            mDialog.setDialogClickListener(object : BuildNewFolderDialog.DialogButtonClickListener {
                override fun onConfirmClick(folderName: String) {
                    showLoading()
                    mModel.createDirectory(folderName)
                }
            })
            if(!mDialog.isAdded) {
                mDialog.show(childFragmentManager, "new_folder")
            }
        }

        mAdapter.setOnLoadMoreListener( { loadFileList() }, mRvFile)

        mAdapter.setOnItemClickListener { adapter, view, position ->
            val bundle = Bundle()
            bundle.putString("parent_name", (adapter.getItem(position) as DirecotoryBean).name)
            bundle.putString("parent_uuid", (adapter.getItem(position) as DirecotoryBean).uuid)
            jumpActivity(FolderDetailActivity::class.java, bundle)
        }

        mRefreshLayout.setOnRefreshListener {
            mOffset = 0
            loadFileList()
        }

        mBtnSort.setOnClickListener {
//            mModel.parseUrl("magnet:?xt=urn:btih:3d5184b5cb76c47500950a2bd945f65383fcea71&dn=LuckyStar")
            mOffset = 0
            when {
                mOrderType == 0 -> mOrderType = 1
                mOrderType == 1 -> mOrderType = 0
            }
            mModel.listFile("", "", mOffset, mPageSize, 0, "", mOrderType, 1)
        }
    }

    override fun initViewModel() {

        mModel.listFileResult.observe(this, Observer {

            mRefreshLayout.isRefreshing = false
            if(it!!.isEmpty()) {
                mAdapter.loadMoreEnd(true)
            } else {
                mAdapter.loadMoreComplete()
            }
            if(mOffset == 0) {
                mAdapter.setNewData(it)
            } else {
                mAdapter.addData(it)
            }
            mOffset += it?.size!!
        })

        mModel.createDirectoryResult.observe(this, Observer {
            stopLoading()
            showToast(Constants.TOAST_SUCCESS, App.instance.getString(R.string.create_directory_success))
            mAdapter.addData(0, it!!)
            mOffset += 1
        })

        mModel.parseUrlResult.observe(this, Observer {
            mModel.offlineDownloadStart(it!!.taskHash, "", arrayOf(1,2))
        })

        mModel.errorStatus.observe(this, Observer {
            mRefreshLayout.isRefreshing = false
            stopLoading()
            showToast(Constants.TOAST_ERROR, it?.errorMessage!!)
        })
    }

    private fun loadFileList() {
        mModel.listFile("", "", mOffset, mPageSize, 0, "", mOrderType, 1)
    }

}
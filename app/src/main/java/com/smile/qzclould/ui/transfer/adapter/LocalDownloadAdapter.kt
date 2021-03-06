package com.smile.qzclould.ui.transfer.adapter

import android.arch.lifecycle.Observer
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadList
import com.liulishuo.filedownloader.FileDownloadSampleListener
import com.liulishuo.filedownloader.FileDownloader
import com.liulishuo.filedownloader.util.FileDownloadUtils
import com.smile.qzclould.R
import com.smile.qzclould.common.App
import com.smile.qzclould.common.Constants
import com.smile.qzclould.db.Direcotory
import com.smile.qzclould.event.FileDownloadCompleteEvent
import com.smile.qzclould.ui.transfer.bean.FileDetailBean
import com.smile.qzclould.ui.transfer.viewmodel.TransferViewModel
import com.smile.qzclould.utils.DLog
import com.smile.qzclould.utils.FileUtils
import com.smile.qzclould.utils.RxBus
import com.smile.qzclould.utils.WetagUtil
import io.netopen.hotbitmapgg.library.view.RingProgressBar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File

class LocalDownloadAdapter : BaseQuickAdapter<Direcotory, BaseViewHolder> {
    private val mViewModel by lazy { TransferViewModel() }
    lateinit var observer: Observer<FileDetailBean>
    private val waitDownLoadList = ArrayList<Direcotory>()

    companion object {
        var savePath = FileUtils.createDir() + File.separator
    }

    private val mTaskDownloadListener = object : FileDownloadSampleListener() {
        override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            super.pending(task, soFarBytes, totalBytes)
            val itemData = getFileBeanById(task?.tag as String)
            itemData?.downloadStatus = 5
            itemData?.taskId = task.id
            notifyItemChanged(data.indexOf(itemData))
            updateFileInfo(itemData)
        }

        override fun started(task: BaseDownloadTask?) {
            super.started(task)
            val itemData = getFileBeanById(task?.tag as String)
            itemData?.downloadStatus = 1
            itemData?.taskId = task.id
            notifyItemChanged(data.indexOf(itemData))
            updateFileInfo(itemData)
        }

        override fun connected(task: BaseDownloadTask?, etag: String?, isContinue: Boolean, soFarBytes: Int, totalBytes: Int) {
            super.connected(task, etag, isContinue, soFarBytes, totalBytes)
        }

        override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            super.progress(task, soFarBytes, totalBytes)
            val itemData = getFileBeanById(task?.tag as String)
            itemData?.totalSize = totalBytes
            itemData?.downloadSize = soFarBytes

            val percent = soFarBytes / totalBytes.toFloat()
            var progress = (percent * 100).toInt()
            if(progress < 0) {
                progress = 0
            }
            itemData?.downProgress = progress

            itemData?.downloadStatus = 1
            notifyItemChanged(data.indexOf(itemData))
            updateFileInfo(itemData)
        }

        override fun error(task: BaseDownloadTask?, e: Throwable?) {
            super.error(task, e)


            val itemData = getFileBeanById(task?.tag as String)
//            mViewModel.loadFileDetail(itemData!!.path, task?.tag as Int)
            itemData?.downloadStatus = 3
            notifyItemChanged(data.indexOf(itemData))
            updateFileInfo(itemData)
        }

        override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            super.paused(task, soFarBytes, totalBytes)
            val itemData = getFileBeanById(task?.tag as String)
            val index = data.indexOf(itemData)
            itemData?.downloadStatus = 2
            notifyItemChanged(index)
            updateFileInfo(itemData)
        }

        override fun completed(task: BaseDownloadTask?) {
            super.completed(task)
            val itemData = getFileBeanById(task?.tag as String)
            doAsync {
                val hashStr = WetagUtil.getEtagHash(File(savePath + itemData?.name))
                uiThread {
                    DLog.i("------" + hashStr + "************************" + itemData?.fileDetail!!.storeId)
                    if(hashStr != itemData?.fileDetail!!.storeId) {
                        val index = data.indexOf(itemData)
                        itemData?.downloadStatus = 6
                        itemData?.downProgress = 0
                        notifyItemChanged(index)
                        updateFileInfo(itemData)
                        File(savePath + itemData?.name).deleteRecursively()
                    } else {
                        data.remove(itemData)
                        notifyDataSetChanged()
                        RxBus.post(FileDownloadCompleteEvent())
                        deleteFile(itemData)
                    }
                }
            }
        }
    }

    constructor() : super(R.layout.item_local_download) {
        initViewModel()
    }

    fun startDownload(file: Direcotory) {
        val path = savePath + file.name
        val task = FileDownloader.getImpl().create(file.fileDetail?.downloadAddress)
                .setPath(path)
                .setTag(file.uuid)
                .setCallbackProgressTimes(750)
                .setListener(mTaskDownloadListener)
        file.taskId = task.id
        task?.start()
    }

    fun getFileBeanById(uuid:String):Direcotory?{
        return data.firstOrNull { TextUtils.equals(uuid,it?.uuid) }
    }

    override fun setNewData(data: List<Direcotory>?) {
        super.setNewData(data)

        for (item in data!!) {
            when (item.downloadStatus) {
                1, 5 -> {
                    if (item.fileDetail != null) {
                        startDownload(item!!)
                    } else {
                        getDownloadInfo(item.path, data.indexOf(item))
                    }
                }
                0, 3 -> {
                    getDownloadInfo(item.path, data.indexOf(item))
                }
            }
        }
    }

    private fun deleteFile(fileInfo: Direcotory?) {
        if (fileInfo == null) {
            return
        }
        doAsync {
            val dao = App.getCloudDatabase()?.DirecotoryDao()
            dao?.deleteDirecotory(fileInfo)
        }
    }

    private fun updateFileInfo(fileInfo: Direcotory?) {
        if (fileInfo == null) {
            return
        }
        doAsync {
            val dao = App.getCloudDatabase()?.DirecotoryDao()
            dao?.updateDirecotoryInfo(fileInfo)
        }
    }

    private fun initViewModel() {
        observer = Observer {
            data[it!!.position].fileDetail = it
            startDownload(data[it!!.position])
        }
        mViewModel.fileDetail.observeForever(observer)
    }

    private fun deleteTask(file: FileDetailBean?) {
        var savePath = FileDownloadUtils.getDefaultSaveRootPath() + File.separator + file?.name
        File(savePath).delete()
        File(FileDownloadUtils.getTempPath(savePath)).delete()
    }

    fun getDownloadInfo(path: String, pos: Int) {
        mViewModel.loadFileDetail(path, pos)
    }

    override fun convert(helper: BaseViewHolder?, item: Direcotory) {
        with(helper?.getView<ImageView>(R.id.mIcon)) {
            when {
                item?.mime!!.contains(Constants.MIME_FOLDER) -> this?.setImageResource(R.mipmap.img_directory)
                item.mime.contains(Constants.MIME_IMG) -> this?.setImageResource(R.mipmap.img_image)
                item.mime.contains(Constants.MIME_AUDIO) -> this?.setImageResource(R.mipmap.img_mime_mp3)
                item.mime.contains(Constants.MIME_TEXT) -> this?.setImageResource(R.mipmap.img_mime_txt)
                item.mime.contains(Constants.MIME_VIDEO) -> this?.setImageResource(R.mipmap.img_mime_video)
                item.mime.contains(Constants.MIME_DOC) -> this?.setImageResource(R.mipmap.img_mime_doc)
                item.mime.contains(Constants.MIME_EXCEL) -> this?.setImageResource(R.mipmap.img_mime_excel)
                item.mime.contains(Constants.MIME_PDF) -> this?.setImageResource(R.mipmap.img_mime_pdf)
                item.mime.contains(Constants.MIME_ZIP) -> this?.setImageResource(R.mipmap.img_mime_zip)
                else -> this?.setImageResource(R.mipmap.img_file_unkonw)
            }
        }
        helper?.setText(R.id.mTvFileName, item.name)

        with(helper?.getView<ImageView>(R.id.mIvStatus)) {
            this?.setImageResource(when (item.downloadStatus) {
                0 -> R.drawable.icon_download_24dp
                1 -> R.drawable.icon_pause_24dp
                2 -> R.drawable.icon_download_24dp
                3 -> R.drawable.icon_download_24dp
                5 -> R.drawable.ic_queue_24dp
                6 -> R.drawable.icon_download_24dp
                else -> 0
            })
        }

        with(helper?.getView<RingProgressBar>(R.id.mDlProgress)) {
            this?.progress = item?.downProgress
        }

        with(helper?.getView<FrameLayout>(R.id.mFlDownload)) {
            when (item.downloadStatus) {
                0, 1, 2, 3, 5, 6 -> {
                    this?.visibility = View.VISIBLE
                }
                4 -> {
                    this?.visibility = View.GONE
                }
            }
        }

        with(helper?.getView<TextView>(R.id.mTvDownloadStatus)) {
            this?.text = when (item.downloadStatus) {
                0 -> "等待下载....."
                1 -> "正在下载 ${item?.downProgress}%"
                2 -> "下载暂停"
                3 -> "下载失败"
                4 -> "下载完成"
                5 -> "队列中"
                6 -> "文件损坏"
                else -> ""
            }
        }

        helper?.getView<FrameLayout>(R.id.mFlDownload)?.setOnClickListener {
            when (item.downloadStatus) {
                0, 3, 6 -> {
                    getDownloadInfo(item.path, helper.adapterPosition)
                }
                2 -> {
                    item.fileDetail?.position = helper.adapterPosition
                    startDownload(item)
                }
                1, 5 -> {
                    FileDownloader.getImpl().pause(item.taskId)
                }
            }
        }


    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mViewModel.fileDetail.removeObserver(observer)
    }


}
package com.geniusgithub.mediaplayer.player.picture.model;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.geniusgithub.common.util.AlwaysLog;
import com.geniusgithub.common.util.FileHelper;
import com.geniusgithub.common.util.FileManager;
import com.geniusgithub.mediaplayer.dlna.model.MediaItem;
import com.geniusgithub.mediaplayer.player.SingleSecondTimer;
import com.geniusgithub.mediaplayer.player.common.AbstractTimer;
import com.geniusgithub.mediaplayer.util.CommonLog;
import com.geniusgithub.mediaplayer.util.LogFactory;

import java.util.List;

public class PictureControlCenter implements DownLoadHelper.IDownLoadCallback {
	private static final CommonLog log = LogFactory.createLog();
	private static final String TAG = PictureControlCenter.class.getSimpleName();
	private final static int PLAY_NEXT = 0x0001;
	private final static int AUTO_PLAY_INTERVAL = 3000;
	
	
	private Context mContext;
	private int mCurIndex = 0;
	private List<MediaItem> mPictureList;
	private boolean isAutoPlay = false;
	
	private DownLoadHelper mDownLoadHelper;
	private DownLoadHelper.IDownLoadCallback mCallback;
	
	private AbstractTimer mAutoPlayerTimer;
	private Handler mHandler;
	private int RunningTaskCount = 0;
	
	
	public PictureControlCenter(Context context){
		mContext = context;
		mDownLoadHelper = new DownLoadHelper();
		mAutoPlayerTimer = new SingleSecondTimer(context);
		mAutoPlayerTimer.setTimeInterval(AUTO_PLAY_INTERVAL);
	
	}
	
	public void init(){
		mDownLoadHelper.init();
		
		mHandler = new Handler(){

			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
					case PLAY_NEXT:
					{
						if (!isHaveFile()){
							return ;
						}		
						
						int count = getTaskCount();
						if (count > 0){
							log.e("getTaskCount = " + count + ", so don't play it now");
							return ;
						}
						
						mCurIndex++;
						mCurIndex = reviceIndex(mCurIndex);
						
						downLoad(mCurIndex);
					}
						break;
				}
			}
			
		};
		
		mAutoPlayerTimer.setHandler(mHandler, PLAY_NEXT);
		setTaskCount(0);
	}
	
	public void unInit(){
		mDownLoadHelper.unInit();
		mAutoPlayerTimer.stopTimer();
		setTaskCount(0);
	}
	
	public void updateMediaInfo(int index, List<MediaItem> list){
		mCurIndex = index;
		mPictureList = list;
	}
	
	public void setDownLoadCallback(DownLoadHelper.IDownLoadCallback callback){
		mCallback = callback;
	}
	
	
	
	public void play(int index){
		if (!isHaveFile()){
			return ;
		}

		mCurIndex = reviceIndex(index);		
		downLoad(mCurIndex);
	}
	
	public void prev(){
		if (!isHaveFile()){
			return ;
		}
		
		mCurIndex--;
		mCurIndex = reviceIndex(mCurIndex);
		downLoad(mCurIndex);
	}
	
	public void next(){
		if (!isHaveFile()){
			return ;
		}
		
		
		mCurIndex++;
		mCurIndex = reviceIndex(mCurIndex);	
		downLoad(mCurIndex);
	}
	
	private void downLoad(int index){
		String requestUrl = mPictureList.get(mCurIndex).getRes();
		String saveUri = FileManager.getBrowseCacheFullPath(requestUrl);
		if (isFileCacheExist(saveUri)){
			AlwaysLog.i(TAG, "isFileCacheExist saveUri = " + saveUri + ", do return it now...");
			if (mCallback != null){
				mCallback.downLoadComplete(true, saveUri);
			}
		}else{
			mDownLoadHelper.syncDownLoadFile(requestUrl, saveUri, this);
			startDownLoad(mPictureList.get(mCurIndex).getTitle());
		}

	}

	private boolean isFileCacheExist(String saveUri){
		return FileHelper.fileIsExist(saveUri);
	}
	
	
	public void startAutoPlay(boolean flag){
		if (flag == isAutoPlay || !isHaveFile()){
			return ;
		}
		
		if (flag){
			mAutoPlayerTimer.startTimer();
			isAutoPlay = true;
		}else{
			mAutoPlayerTimer.stopTimer();
			isAutoPlay = false;
		}
	}
	
	
	
	
	private boolean isHaveFile(){
		if (mPictureList != null && mPictureList.size() > 0){
			return true;
		}
		
		return false;
	}
	
	private int reviceIndex(int index)
	{
		if (index < 0)
		{
			index = mPictureList.size() - 1;
		}
		
		if (index >= mPictureList.size())
		{
			index = 0;
		}
		
		return index;
	}

	@Override
	public void startDownLoad(String title) {
		addTaskCount();
		if (mCallback != null){
			mCallback.startDownLoad(title);
		}
	}

	@Override
	public void downLoadComplete(boolean isSuccess, String savePath) {
		subTaskCount();
		if (mCallback != null){
			mCallback.downLoadComplete(isSuccess, savePath);
		}
	}
	
	
	private  synchronized void setTaskCount(int count){
		RunningTaskCount = count;
	}
	
	private  synchronized void addTaskCount(){
		RunningTaskCount++;
	}

	private synchronized void subTaskCount(){
		RunningTaskCount--;
	}
	
	private synchronized int getTaskCount(){
		return RunningTaskCount;
	}
}

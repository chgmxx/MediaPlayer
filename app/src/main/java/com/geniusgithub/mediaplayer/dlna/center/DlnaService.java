package com.geniusgithub.mediaplayer.dlna.center;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.geniusgithub.mediaplayer.AllShareApplication;
import com.geniusgithub.mediaplayer.dlna.ControlPointImpl;
import com.geniusgithub.mediaplayer.dlna.IControlPointStatu;
import com.geniusgithub.mediaplayer.dlna.proxy.AllShareProxy;
import com.geniusgithub.mediaplayer.player.picture.DelCacheFileManager;
import com.geniusgithub.mediaplayer.util.CommonLog;
import com.geniusgithub.mediaplayer.util.CommonUtil;
import com.geniusgithub.mediaplayer.util.LogFactory;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.cybergarage.util.AlwaysLog;

public class DlnaService extends Service implements IBaseEngine,
													DeviceChangeListener,
													ControlCenterWorkThread.ISearchDeviceListener{
	
	private static final CommonLog log = LogFactory.createLog();
	private static final String TAG = DlnaService.class.getSimpleName();

	public static final String SEARCH_DEVICES = "com.geniusgithub.allshare.search_device";
	public static final String RESET_SEARCH_DEVICES = "com.geniusgithub.allshare.reset_search_device";
	
	private static final int NETWORK_CHANGE = 0x0001;
	private boolean firstReceiveNetworkChangeBR = true;
	private  NetworkStatusChangeBR mNetworkStatusChangeBR;
	
	
	private ControlPointImpl mControlPoint;
	private  ControlCenterWorkThread mCenterWorkThread;
	private  AllShareProxy mAllShareProxy;
	private  Handler mHandler;


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		AlwaysLog.i(TAG, "DlnaService onCreate");
		init();
	}
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (intent != null && intent.getAction() != null){
			String action = intent.getAction();
			if (DlnaService.SEARCH_DEVICES.equals(action)) {
				startEngine();
			}else if (DlnaService.RESET_SEARCH_DEVICES.equals(action)){
				restartEngine();
			}
		}else{
			log.e("intent = " + intent);
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		log.e("DlnaService onDestroy");
		unInit();
		super.onDestroy();
	}
	
	
	private void init(){
		mAllShareProxy = AllShareProxy.getInstance(this);
		
		mControlPoint = new ControlPointImpl();
		AllShareApplication.getInstance().setControlPoint(mControlPoint);
		mControlPoint.addDeviceChangeListener(this);
		mControlPoint.addSearchResponseListener(new SearchResponseListener() {		
			public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
			}
		});
	

		mCenterWorkThread = new ControlCenterWorkThread(this, mControlPoint);
		mCenterWorkThread.setSearchListener(this);
		
		mHandler = new Handler(){

			public void handleMessage(Message msg) {
				switch(msg.what){
					case NETWORK_CHANGE:
						mAllShareProxy.resetSearch();
						break;
				}
			}
			
		};
		
		registerNetworkStatusBR();


		DelCacheFileManager mDelthumbnailManager = new DelCacheFileManager();
		mDelthumbnailManager.clearThumbnailCache();


		boolean ret = CommonUtil.openWifiBrocast(this);
		AlwaysLog.i(TAG, "openWifiBrocast = " + ret);
	}
	
	private void unInit(){
		unRegisterNetworkStatusBR();
		stopEngine();
		AllShareApplication.getInstance().setControlPoint(null);

		DelCacheFileManager mDelthumbnailManager = new DelCacheFileManager();
		mDelthumbnailManager.clearThumbnailCache();

	}

	
	@Override
	public boolean startEngine() {
		AlwaysLog.i(TAG, "startEngine");
		if (AllShareApplication.getInstance().getControlStatus() != IControlPointStatu.STATUS_STARTED){
			AllShareApplication.getInstance().updateControlStauts(IControlPointStatu.STATUS_STARTING);
		}
		awakeWorkThread();
		return true;
	}


	@Override
	public boolean stopEngine() {
		AlwaysLog.i(TAG, "stopEngine");
		exitWorkThread();
		return true;
	}


	@Override
	public boolean restartEngine() {
		AlwaysLog.i(TAG, "restartEngine");
		AllShareApplication.getInstance().updateControlStauts(IControlPointStatu.STATUS_STARTING);

		mCenterWorkThread.setCompleteFlag(false);
		awakeWorkThread();

		return true;
	}




	@Override
	public void deviceAdded(Device dev) {
		mAllShareProxy.addDevice(dev);
	}


	@Override
	public void deviceRemoved(Device dev) {
		mAllShareProxy.removeDevice(dev);
	}
	
	
	private void awakeWorkThread(){

		if (mCenterWorkThread.isAlive()){
			mCenterWorkThread.awakeThread();
		}else{
			mCenterWorkThread.start();
		}
	}
	
	private void exitWorkThread(){
		if (mCenterWorkThread != null && mCenterWorkThread.isAlive()){
			mCenterWorkThread.exit();
			long time1 = System.currentTimeMillis();
			while(mCenterWorkThread.isAlive()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			long time2 = System.currentTimeMillis();
			log.e("exitCenterWorkThread cost time:" + (time2 - time1));
			mCenterWorkThread = null;
		}
	}
	
	
	@Override
	public void onSearchComplete(boolean searchSuccess) {

/*		if (!searchSuccess){
			sendSearchDeviceFailBrocast(this);
		}*/
	}

	@Override
	public void onStartComplete(boolean startSuccess) {

		mControlPoint.flushLocalAddress();
	//	sendStartDeviceEventBrocast(this, startSuccess);
		AlwaysLog.i(TAG, "onStartComplete startSuccess = " + startSuccess);
		if (startSuccess){
			AllShareApplication.getInstance().updateControlStauts(IControlPointStatu.STATUS_STARTED);
		}else{

		}

	}

	@Override
	public void onStopComplete() {
		AlwaysLog.i(TAG, "onStopComplete");
		AllShareApplication.getInstance().updateControlStauts(IControlPointStatu.STATUS_SOTP);
	}

/*	public static final String SEARCH_DEVICES_FAIL = "com.geniusgithub.allshare.search_devices_fail";
	public static void sendSearchDeviceFailBrocast(Context context){
		log.e("sendSearchDeviceFailBrocast");
		Intent intent = new Intent(SEARCH_DEVICES_FAIL);
		context.sendBroadcast(intent);
	}*/

/*	public static final String START_DEVICES_EVENT = "com.geniusgithub.allshare.start_devices_event";
	public static void sendStartDeviceEventBrocast(Context context, boolean startSuccess){
		log.e("sendStartDeviceEventBrocast startSuccess = " + startSuccess);
		Intent intent = new Intent(START_DEVICES_EVENT);
		context.sendBroadcast(intent);
	}*/
	
	private class NetworkStatusChangeBR extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent != null){
				String action = intent.getAction();
				if (action != null){
					if (action.equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION)){
						sendNetworkChangeMessage();
					}
				}
			}
			
		}
		
	}
	
	private void registerNetworkStatusBR(){
		if (mNetworkStatusChangeBR == null){
			mNetworkStatusChangeBR = new NetworkStatusChangeBR();
			registerReceiver(mNetworkStatusChangeBR, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		}
	}
	
	private void unRegisterNetworkStatusBR(){
		if (mNetworkStatusChangeBR != null){
			unregisterReceiver(mNetworkStatusChangeBR);
		}
	}
	
	private void sendNetworkChangeMessage(){
		if (firstReceiveNetworkChangeBR){
			log.e("first receive the NetworkChangeMessage, so drop it...");
			firstReceiveNetworkChangeBR = false;
			return ;
		}
		
		mHandler.removeMessages(NETWORK_CHANGE);
		mHandler.sendEmptyMessageDelayed(NETWORK_CHANGE, 500);
	}

}

package com.dji.uxsdkdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;

public class RTMPService extends Service {

    public static final String TAG = "RTMPService";
    private LiveStreamManager.OnLiveChangeListener listener;

    public RTMPService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        new Thread(new Runnable() {
            String liveShowUrl = "rtmp://192.168.1.117:1935/live/home";

            @Override
            public void run() {
                while (!MApplication.isAircraftConnected() || !isLiveStreamManagerOn()) {
                }
                if (MApplication.isAircraftConnected()) {
                    if (isLiveStreamManagerOn()) {
                        if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
                            Log.d(TAG, "already started!");
                        } else {
                            DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(liveShowUrl);
                            int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                            Log.d(TAG, result == 0 ? "start success" : "error code" + result);
                            DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
                            if (isLiveStreamManagerOn()) {
                                DJISDKManager.getInstance().getLiveStreamManager().registerListener(listener);
                            }
                            Log.d(TAG, "startLive:" + result +
                                    "\n isVideoStreamSpeedConfigurable:" + DJISDKManager.getInstance().getLiveStreamManager().isVideoStreamSpeedConfigurable() +
                                    "\n isLiveAudioEnabled:" + DJISDKManager.getInstance().getLiveStreamManager().isLiveAudioEnabled());
                        }
                    } else {
                        //showToast("onStreamError!");
                    }
                } else {
                    //showToast("AircraftconnectedCurrent Error");
                }
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private boolean isLiveStreamManagerOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            ToastUtils.setResultToToast("No live stream manager!");
            return false;
        }
        return true;
    }

}
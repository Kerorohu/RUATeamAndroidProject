package com.dji.uxsdkdemo;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import dji.common.flightcontroller.ObstacleDetectionSector;
import dji.common.flightcontroller.VisionDetectionState;
import dji.common.flightcontroller.VisionSensorPosition;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdksharedlib.DJISDKCache;

public class MainActivity extends AppCompatActivity {

    private Compass compass;
    private static final String TAG = MainActivity.class.getName();
    private LiveStreamManager.OnLiveChangeListener listener;
    private String liveShowUrl;
    Boolean temp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.start);

        listener = new LiveStreamManager.OnLiveChangeListener() {
            @Override
            public void onStatusChanged(int i) {
                showToast("status changed : " + i);
            }
        };


        liveShowUrl = "rtmp://192.168.1.117:1935/live/home";
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*FlyingTask flyingTask = new FlyingTask(MainActivity.this);
                FlyingTask.FlyTask flyTask = flyingTask.new FlyTask();
                flyTask.start();*/

                Intent intent = new Intent("android.intent.action.Input");
                startActivity(intent);

                /*new Thread(new Runnable() {
                    @Override
                    public void run() {

                        //飞行代码
                        if(MApplication.isAircraftConnected()){
                            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                                FlightController flightController =
                                        ((Aircraft) MApplication.getProductInstance()).getFlightController();
                                // 2. 设置启动虚拟摇杆模式
                                temp = false;
                                while (temp != true) {
                                    flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (null != djiError)
                                                showToast(djiError.getDescription());
                                        }
                                    });

                                    flightController.getVirtualStickModeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean aBoolean) {
                                            temp = aBoolean;
                                            if (aBoolean == true)
                                                showToast("VirtualStickMode is" + aBoolean);
                                        }

                                        @Override
                                        public void onFailure(DJIError djiError) {
                                            if (null != djiError)
                                                showToast(djiError.getDescription());
                                        }
                                    });
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }


                                // 3. 起飞
                                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (null != djiError)
                                            showToast(djiError.getDescription());
                                    }
                                });

                                // 4. 设置飞行模式
                                setFilghtMode(flightController);
                                // 5. 停 5 s
                                try {
                                    Thread.sleep(5000);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // 6. 初始化 TimerTask 和 Timer
                                SendControlDataTask task = new SendControlDataTask(flightController);
                                Timer timer = new Timer();
                                timer.schedule(task, 0, 1);
                                // 7. 飞行矩形轨迹
                                try {
                                    showToast("Starting Task");
                                    setControlData(0,1,0,0, task);
                                    Thread.sleep(5000);
                                    setControlData(0,0,0,0, task);
                                    Thread.sleep(1000);

                                    setControlData(1,0,0,0, task);
                                    Thread.sleep(3000);
                                    setControlData(0,0,0,0, task);
                                    Thread.sleep(1000);

                                    setControlData(0,-1,0,0, task);
                                    Thread.sleep(5000);
                                    setControlData(0,0,0,0, task);
                                    Thread.sleep(1000);

                                    setControlData(-1,0,0,0, task);
                                    Thread.sleep(3000);
                                    setControlData(0,0,0,0, task);
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    showToast(e.getMessage());
                                } finally {
                                    // 8. 删除 Timer 线程
                                    timer.cancel();
                                    timer.purge();
                                    timer = null;
                                    task = null;
                                    // 9. 降落
                                    flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if(null != djiError)
                                                showToast(djiError.getDescription());
                                        }
                                    });

                                }
                            }else {
                                showToast("FlightControllerCurrent Error");
                            }
                        }else {
                            showToast("AircraftConnectedCurrent Error");
                        }
                    }
                }).start();*/
            }
        });

        while (!MApplication.isAircraftConnected() || !isLiveStreamManagerOn()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (MApplication.isAircraftConnected()) {
                        if (isLiveStreamManagerOn()) {
                            if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
                                showToast("already started!");
                            } else {
                                DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(liveShowUrl);
                                int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                                DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
                                if (isLiveStreamManagerOn()) {
                                    DJISDKManager.getInstance().getLiveStreamManager().registerListener(listener);
                                }
                                ToastUtils.setResultToToast("startLive:" + result +
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
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        //传感器数据获取
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(MApplication.isAircraftConnected()){
                        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                            FlightController flightController =
                                    ((Aircraft) MApplication.getProductInstance()).getFlightController();

                            flightController.setStateCallback(new FlightControllerState.Callback() {
                                @Override
                                public void onUpdate(@NonNull FlightControllerState djiFlightControllerCurrentState) {
                                    if (null != compass) {
                                        String description =
                                                "CalibrationStatus: " + compass.getCalibrationState() + "\n"
                                                        + "Heading: " + compass.getHeading() + "\n"
                                                        + "isCalibrating: " + compass.isCalibrating() + "\n";

                                        //changeDescription(description);
                                        //compassView.setText(description);
                                    }
                                }
                            });
                            if (ModuleVerificationUtil.isCompassAvailable()) {
                                compass = flightController.getCompass();
                                //compassView.setText(String.valueOf(compass.getHeading()));
                               showToast(String.valueOf(compass.getHeading()));

                            }
                        }else{
                            *//*showToast("FlightControllerCurrent Error");*//*
                        }
                    }else {
                        *//*showToast("AircraftconnectedCurrent Error");*//*
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();*/

       /* //避障数据获取
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(MApplication.isAircraftConnected()){
                        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                            FlightController flightController =
                                    ((Aircraft) MApplication.getProductInstance()).getFlightController();

                            FlightAssistant intelligentFlightAssistant = flightController.getFlightAssistant();
                            if (intelligentFlightAssistant != null) {
                                intelligentFlightAssistant.setVisionDetectionStateUpdatedCallback(new VisionDetectionState.Callback() {
                                    @Override
                                    public void onUpdate(VisionDetectionState visionDetectionState) {
                                        //VisionSensorPosition a = visionDetectionState.getPosition();
                                        ObstacleDetectionSector[] visionDetectionSectorArray =
                                                visionDetectionState.getDetectionSectors();
                                        showToast(String.valueOf(visionDetectionSectorArray[0].getObstacleDistanceInMeters()));

                                    }
                                });
                                }else {
                                showToast("onAttachedToWindow FC NOT Available");
                            }
                        }else{
                            showToast("FlightControllerCurrent Error");
                        }
                    }else {
                        showToast("AircraftconnectedCurrent Error");
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();*/

    }

    private void showToast(final String toastMsg) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isLiveStreamManagerOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            ToastUtils.setResultToToast("No live stream manager!");
            return false;
        }
        return true;
    }

}


package com.dji.uxsdkdemo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import dji.common.airlink.PhysicalSource;
import dji.common.product.Model;
import dji.sdk.camera.*;
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

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }

    private Compass compass;
    private static final String TAG = MainActivity.class.getName();
    private LiveStreamManager.OnLiveChangeListener listener;
    private VideoFeeder.VideoDataListener videoDataListener;
    private Camera.VideoDataCallback videoDataCallback;
    private VideoFeeder.PhysicalSourceListener sourceListener;
    private String liveShowUrl;
    private TextureView fpv;
    Boolean temp;
    byte[] bytes;
    int size;


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
                            Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA,
                    }
                    , 1);
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.start);
        Button cc = (Button) findViewById(R.id.c_calibration);
        fpv = (TextureView) findViewById(R.id.fpvWidget);
//        listener = new LiveStreamManager.OnLiveChangeListener() {
//            @Override
//            public void onStatusChanged(int i) {
//                showToast("status changed : " + i);
//            }
//        };

        //Camera Data
//        Camera camera =
//                ((Aircraft) MApplication.getProductInstance()).getCamera();


//        VideoFeeder.getInstance().addPhysicalSourceListener(sourceListener);
//        videoDataListener = new VideoFeeder.VideoDataListener() {
//            @Override
//            public void onReceive(byte[] bytes, int size) {
//                videoDataTest(bytes, size);
//            }
//        };

        //不能用
//        if(((Aircraft) MApplication.getProductInstance()).getModel() != null) {
//            sourceListener = new VideoFeeder.PhysicalSourceListener() {
//                @Override
//                public void onChange(VideoFeeder.VideoFeed videoFeed, PhysicalSource physicalSource) {
//                    VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);
//                    VideoFeeder.getInstance().addPhysicalSourceListener(sourceListener);
//
//                    videoDataListener = new VideoFeeder.VideoDataListener() {
//                        @Override
//                        public void onReceive(byte[] bytes, int i) {
//                            showToast("" + i);
//                            videoDataTest(bytes, size);
//                        }
//                    };
//
//                }
//            };
//        }


        //showToast(stringFromJNI());
        liveShowUrl = "rtmp://192.168.1.117:1935/live/home";

        cc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this,CameraCalibrationActivity.class);
//                startActivity(intent);
                Bitmap bitmap = fpv.getBitmap();
                if (bitmap == null) {
                    showToast("no fpv data!");
                } else {
                    showToast("get bitmap!");
                }
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* FlyingTask flyingTask = new FlyingTask(MainActivity.this);
                FlyingTask.FlyTask flyTask = flyingTask.new FlyTask();
                flyTask.start();*/

//                Intent intent = new Intent(MainActivity.this,InputActivity.class);
//                startActivityForResult(intent,1);

                bytes = "hello world".getBytes();
                if (bytes != null) {
                    Intent intent = new Intent(MainActivity.this, VideoDataActivity.class);
                    startActivityForResult(intent, 1);
                } else {
                    showToast("bytes is null!");
                }


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

        //直播流服务
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (!MApplication.isAircraftConnected() || !isLiveStreamManagerOn()) {}
//                if (MApplication.isAircraftConnected()) {
//                    if (isLiveStreamManagerOn()) {
//                        if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
//                            showToast("already started!");
//                        } else {
//                            DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(liveShowUrl);
//                            int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
//                            DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
//                            if (isLiveStreamManagerOn()) {
//                                DJISDKManager.getInstance().getLiveStreamManager().registerListener(listener);
//                            }
//                            ToastUtils.setResultToToast("startLive:" + result +
//                                    "\n isVideoStreamSpeedConfigurable:" + DJISDKManager.getInstance().getLiveStreamManager().isVideoStreamSpeedConfigurable() +
//                                    "\n isLiveAudioEnabled:" + DJISDKManager.getInstance().getLiveStreamManager().isLiveAudioEnabled());
//                        }
//                    } else {
//                        //showToast("onStreamError!");
//                    }
//                } else {
//                    //showToast("AircraftconnectedCurrent Error");
//                }
//            }
//        }).start();
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


        //传感器数据获取
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true){
//                    if(MApplication.isAircraftConnected()){
//                        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
//                            FlightController flightController =
//                                    ((Aircraft) MApplication.getProductInstance()).getFlightController();
//
//                            flightController.setStateCallback(new FlightControllerState.Callback() {
//                                @Override
//                                public void onUpdate(@NonNull FlightControllerState djiFlightControllerCurrentState) {
//                                    if (null != compass) {
//                                        String description =
//                                                "CalibrationStatus: " + compass.getCalibrationState() + "\n"
//                                                        + "Heading: " + compass.getHeading() + "\n"
//                                                        + "isCalibrating: " + compass.isCalibrating() + "\n";
//
//                                        //changeDescription(description);
//                                        //compassView.setText(description);
//                                    }
//                                }
//                            });
//                            if (ModuleVerificationUtil.isCompassAvailable()) {
//                                compass = flightController.getCompass();
//                                //compassView.setText(String.valueOf(compass.getHeading()));
//                               showToast(String.valueOf(compass.getHeading()));
//
//                            }
//                        }else{
//                            showToast("FlightControllerCurrent Error");
//                        }
//                    }else {
//                        showToast("AircraftconnectedCurrent Error");
//                    }
//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();

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

    private void videoDataTest(byte[] bytes, int size) {

        showToast("" + size);
        this.bytes = bytes;
        this.size = size;

    }

    private boolean isLiveStreamManagerOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            ToastUtils.setResultToToast("No live stream manager!");
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == 2) {
            String content = data.getStringExtra("data");
            showToast(content);
        }
    }
}


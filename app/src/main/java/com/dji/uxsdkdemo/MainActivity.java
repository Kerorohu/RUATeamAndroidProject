package com.dji.uxsdkdemo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import dji.common.airlink.PhysicalSource;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
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
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdksharedlib.DJISDKCache;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }

    private Handler mHandler;
    private boolean mRunning = true;

    Runnable mArucoDetectRunnable = new Runnable() {
        @Override
        public void run() {
            while (mRunning) {
                bitmap = fpv.getBitmap();
                if (bitmap != null) {
                    float[] points = calibratePoint(bitmap, bitmap.getWidth(), bitmap.getHeight());
                    app.setPoints(points);
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            //bitmap.recycle();
        }
    };

    private Compass compass;
    private WaypointMissionOperator waypointMissionOperator;
    private WaypointMission mission;
    private static final String TAG = MainActivity.class.getName();
    private LiveStreamManager.OnLiveChangeListener listener;
    private VideoFeeder.VideoDataListener videoDataListener;
    private Camera.VideoDataCallback videoDataCallback;
    private VideoFeeder.PhysicalSourceListener sourceListener;
    private String liveShowUrl;
    private TextureView fpv;
    private MApplication app;
    private Gimbal gimbal = null;
    private int currentGimbalId = 0;
    private Bitmap[] bitmaps;
    private Bitmap bitmap;
    private int count;

    //private native void cameraCalibrate(Bitmap[] bmps,long rvecs,long tvecs);
    private native float[] calibratePoint(Bitmap input, int width, int height);

    //xiaoweigeCode
    protected double nowLatitude;
    protected double nowLongitude;
    protected double nowAltitude;
    protected FlightMode flightState;
    protected String IP_Address;
    protected int port;
    double car_Latitude;
    double car_Longitude;
    double car_Updata;
    int loadready = 0;

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
        Button cc = (Button) findViewById(R.id.saveImg);
        Button cca = (Button) findViewById(R.id.c_calibration);
        fpv = (TextureView) findViewById(R.id.fpvWidget);
        bitmaps = new Bitmap[10];
        count = 0;
        app = (MApplication) getApplication();

        //points初始化
        float[] tempf = new float[2];
        tempf[0] = 5000;
        tempf[1] = 0;
        app.setPoints(tempf);

        nowLatitude = 181;
        nowLongitude = 181;
        nowAltitude = 0;
        flightState = null;
        IP_Address = "192.168.1.120";
        port = 54321;
        car_Latitude = 0;
        car_Longitude = 0;
        car_Updata = 0;
        loadready = 0;

//        listener = new LiveStreamManager.OnLiveChangeListener() {
//            @Override
//            public void onStatusChanged(int i) {
//                showToast("status changed : " + i);
//            }
//        };

        //showToast(stringFromJNI());
        liveShowUrl = "rtmp://192.168.1.117:1935/live/home";

        cc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this,CameraCalibrationActivity.class);
//                startActivity(intent);

                //Aruco测试
                bytes = "hello world".getBytes();
                if (bytes != null) {
                    Intent intent = new Intent(MainActivity.this, VideoDataActivity.class);
                    startActivityForResult(intent, 1);
                } else {
                    showToast("bytes is null!");
                }

                //保存图像
//                Bitmap bitmap = fpv.getBitmap();
//                if (bitmap == null) {
//                    showToast("no fpv data!");
//                }else {
//                    saveBitmap(bitmap, Environment.getExternalStorageDirectory().getAbsolutePath()+"/calibrateImg/");
//                }

//                if(count <= 9){
//                    bitmaps[count] = bitmap;
//                    showToast("get");
//                    count++;
//                }else {
//                    count = 0;
//                    Mat tempRvecs = new Mat();
//                    Mat tempTvecs = new Mat();
//                    cameraCalibrate(bitmaps,tempRvecs.getNativeObjAddr(),tempTvecs.getNativeObjAddr());
//                    bitmaps = new Bitmap[10];
//                }
            }
        });

        //Init Gimbal
        new Thread(new Runnable() {
            @Override
            public void run() {
                int temp = 0;
                while (true) {
                    if (MApplication.isAircraftConnected() && temp == 0) {
                        BaseProduct product = MApplication.getProductInstance();
                        if (product != null) {
                            if (product instanceof Aircraft) {
                                gimbal = ((Aircraft) product).getGimbals().get(currentGimbalId);
                            } else {
                                gimbal = product.getGimbal();
                            }
                            temp = 1;
                            showToast("Ok:gimbal");
                        } else {
                            showToast("fail:gimbal");
                        }
                    }
                }
            }
        }).start();

        //2.通信初始化
        showToast("tcp link");
        tcp_ip_link.sharedCenter().connect(IP_Address, port);
        tcp_ip_link.sharedCenter().setReceivedCallback(new tcp_ip_link.OnReceiveCallbackBlock() {
            @Override
            public void callback(String receicedMessage) {
                if (receicedMessage.contains("GPS")) {
                    String str_car_Latitude = receicedMessage.split(" ")[1];
                    String str_car_Longitude = receicedMessage.split(" ")[2];
                    car_Latitude = Float.parseFloat(str_car_Latitude);
                    car_Longitude = Float.parseFloat(str_car_Longitude);
                    if (car_Updata == 0) {
                        car_Updata = 1;
                    }
                }
            }
        });
        showToast("tcp link end");

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* FlyingTask flyingTask = new FlyingTask(MainActivity.this);
                FlyingTask.FlyTask flyTask = flyingTask.new FlyTask();
                flyTask.start();*/

//                Intent intent = new Intent(MainActivity.this,InputActivity.class);
//                startActivityForResult(intent,1);

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        //1.云台向下
                        Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
                        builder.pitch((float) (-90.0));
                        sendRotateGimbalCommand(builder.build());
                        builder.yaw((float) (0));
                        sendRotateGimbalCommand(builder.build());
                        builder.roll((float) (0));
                        sendRotateGimbalCommand(builder.build());

                        //3.飞行实例获取
                        if (MApplication.isAircraftConnected()) {
                            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                                FlightController flightController =
                                        ((Aircraft) MApplication.getProductInstance()).getFlightController();

                                //4.mission任务初始化
                                while (waypointMissionOperator == null) {
                                    waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
                                    if (waypointMissionOperator == null)
                                        showToast("start init waypointOperator");
                                }

                                //5.无人机状态
                                flightController.setStateCallback(new FlightControllerState.Callback() {
                                    @Override
                                    public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                                        nowLatitude = flightControllerState.getAircraftLocation().getLatitude();
                                        nowLongitude = flightControllerState.getAircraftLocation().getLongitude();
                                        nowAltitude = flightControllerState.getAircraftLocation().getAltitude();
                                        flightState = flightControllerState.getFlightMode();

                                    }
                                });

                                // 6. 起飞
                                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (null != djiError)
                                            showToast(djiError.getDescription());
                                    }
                                });

                                //7.启动二维码检测线程
                                HandlerThread thread = new HandlerThread("arucoThread");
                                thread.start();
                                mHandler = new Handler(thread.getLooper());
                                mHandler.post(mArucoDetectRunnable);

                                //8.waypoint执行
                                while (app.getPoints()[0] == 5000) {
                                    waypointMissionOperator.clearMission();
                                    if (car_Updata == 1) {
                                        car_Updata = 0;
                                        //showToast("car_Latitude: "+car_Latitude+" car_Longitude "+car_Longitude);
//                                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
//                                    @Override
//                                    public void onResult(DJIError djiError) {
//                                        showToast(djiError.getDescription());
//                                    }
//                                });
                                        mission = createWaypointMission(car_Latitude, car_Longitude, 6);
//                              mission = createRandomWaypointMission(1,1);

                                        if (mission != null) {
                                            DJIError djiError = waypointMissionOperator.loadMission(mission);
                                            if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                                                    || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {

                                                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                                                    @Override
                                                    public void onResult(DJIError djiError) {
                                                        if (djiError == null) {
                                                            loadready = 1;
                                                        } else {
                                                            showToast(djiError.getDescription());
                                                        }
                                                    }
                                                });
                                            } else {
                                                //showToast("Not ready!");
                                                loadready = 0;
                                            }
                                            if (loadready == 1 && mission != null) {
                                                try {
                                                    Thread.sleep(3000);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        } else {
                                            //showToast("null  mission!");
                                        }
                                        //showToast("loadready "+loadready);
                                        if (loadready == 1) {
                                            if (mission != null) {
                                                waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                                                    @Override
                                                    public void onResult(DJIError djiError) {
                                                        showToast(djiError.getDescription());
                                                    }
                                                });
                                            } else {
                                                showToast("Prepare Mission First!");
                                            }
//                                            waypointMissionOperator.destroy();


//                                    try {
//                                        Thread.sleep(6000);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }
                                        }
                                    }
                                }
                                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        showToast(djiError.getDescription());
                                    }
                                });
                                waypointMissionOperator.destroy();

                                // 9. 设置启动虚拟摇杆模式
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
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                // 10. 设置飞行模式
                                setFilghtMode(flightController);
//                                // 停 5 s
//                                try {
//                                    Thread.sleep(5000);
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }

                                // 11. 初始化 TimerTask 和 Timer
                                SendControlDataTask task = new SendControlDataTask(flightController);
                                setControlData(0, 0, 0, 0, task);
                                Timer timer = new Timer();
                                timer.schedule(task, 0, 30);
                                // 12. 寻找二维码并矫正位置并居中
                                try {
                                    showToast("Starting Task");
                                    //showToast(Double.toString(nowAltitude));
                                    float[] points;
                                    float[] control = {0, 0, 0};
                                    int count = 0;
                                    int temp = 0;
                                    boolean visualSticktmp = false;
                                    DJIError tmperror;
                                    setControlData(0, 0, 0, 0, task);
                                    while (count < 20) {
                                        points = app.getPoints();
                                        if (flightController.isVirtualStickControlModeAvailable()) {
                                            visualSticktmp = true;
                                        } else {
                                            visualSticktmp = false;
                                        }
                                        if (temp == 8) {
                                            showToast(Float.toString(points[0]) + " " + Float.toString(points[1]) + " ControlMode:" + Boolean.toString(visualSticktmp));
                                            if (task.getDjiError() != null) {
                                                tmperror = task.getDjiError();
                                                showToast(tmperror.getDescription());
                                            }
                                            temp = 0;
                                        }
                                        temp++;

                                        if ((int) points[0] == 5000 || (int) points[0] == 6000) {
                                            setControlData(0, 0, 0, 0, task);
                                        } else {
                                            //y
                                            if (points[1] > 300) {
                                                control[1] = (float) -0.1;
                                            } else if (points[1] < -300) {
                                                control[1] = (float) 0.1;
                                            } else if (points[1] > 0 && points[1] <= 300) {
                                                control[1] = (float) -0.1;
                                            } else if (points[1] < 0 && points[1] >= -300) {
                                                control[1] = (float) 0.1;
                                            } else {
                                                control[1] = 0;
                                            }

                                            //x
                                            if (points[0] > 200) {
                                                control[0] = (float) 0.1;
                                            } else if (points[0] < -200) {
                                                control[0] = (float) -0.1;
                                            } else if (points[0] > 0 && points[0] <= 200) {
                                                control[0] = (float) 0.1;
                                            } else if (points[0] < 0 && points[0] >= -200) {
                                                control[0] = (float) -0.1;
                                            } else {
                                                control[0] = 0;
                                            }

                                            if (nowAltitude > 1.6) {
                                                control[2] = (float) -0.1;
                                            } else {
                                                control[2] = 0;
                                            }

                                            setControlData(control[0], control[1], 0, control[2], task);
                                            if (points[0] < 100 && points[1] < 100 && nowAltitude < 1.8) {
                                                count++;
                                            } else {
                                                count = 0;
                                            }

                                        }
                                        Thread.sleep(300);
                                    }
//                                    showToast("Starting Task");
//                                    setControlData(0,1,0,0, task);
//                                    Thread.sleep(5000);
//                                    setControlData(0,0,0,0, task);
//                                    Thread.sleep(1000);
//
//                                    setControlData(1,0,0,0, task);
//                                    Thread.sleep(3000);
//                                    setControlData(0,0,0,0, task);
//                                    Thread.sleep(1000);
//
//                                    setControlData(0,-1,0,0, task);
//                                    Thread.sleep(5000);
//                                    setControlData(0,0,0,0, task);
//                                    Thread.sleep(1000);
//
//                                    setControlData(-1,0,0,0, task);
//                                    Thread.sleep(3000);
//                                    setControlData(0,0,0,0, task);
//                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    showToast(e.getMessage());
                                } finally {
                                    // 13. 删除 Timer 线程
                                    timer.cancel();
                                    timer.purge();
                                    timer = null;
                                    task = null;
                                    // 14. 降落
                                    flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (null != djiError)
                                                showToast(djiError.getDescription());
                                        }
                                    });

                                    //15.销毁二维码识别
                                    mHandler.removeCallbacks(mArucoDetectRunnable);
                                }
                            } else {
                                showToast("FlightControllerCurrent Error");
                            }
                        } else {
                            showToast("AircraftConnectedCurrent Error");
                        }
                    }
                }).start();
            }
        });

//        //直播流服务
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

    private WaypointMission createWaypointMission(double to_Latitude, double to_Longitude, float to_height) {
        WaypointMission.Builder builder = new WaypointMission.Builder();

        builder.autoFlightSpeed(5f);
        builder.maxFlightSpeed(10f);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.repeatTimes(1);

        List<Waypoint> waypointList = new ArrayList<>();
        final Waypoint eachWaypoint0 = new Waypoint(nowLatitude, nowLongitude, (float) nowAltitude);
        eachWaypoint0.addAction(new WaypointAction(WaypointActionType.STAY, 1));
        waypointList.add(eachWaypoint0);
        final Waypoint eachWaypoint1 = new Waypoint(to_Latitude, to_Longitude, to_height);
        eachWaypoint1.addAction(new WaypointAction(WaypointActionType.STAY, 1));
        waypointList.add(eachWaypoint1);


        builder.waypointList(waypointList).waypointCount(waypointList.size());
        //showToast("waypointCount "+ waypointList.size());
        return builder.build();
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

    private void setFilghtMode(FlightController flightController) {
        // 坐标系设置为机体坐标系
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        // 垂直方向控制模式为位置
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        // 水平方向控制模式为速度
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        // 偏航角控制模式设置为角度
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
    }

    private void setControlData(float pitch, float roll, float yaw, float throttle, SendControlDataTask task) {
        task.set_pitch(pitch);
        task.set_roll(roll);
        task.set_yaw(yaw);
        task.set_throttle(throttle);
    }

    private void saveBitmap(Bitmap bitmap, String sdCardDir) {
        try {
            File dirFile = new File(sdCardDir);
            if (!dirFile.exists()) {              //如果不存在，那就建立这个文件夹
                dirFile.mkdirs();
            }
            File file = new File(sdCardDir, System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRotateGimbalCommand(Rotation rotation) {

//        Gimbal gimbal = getGimbalInstance();
//        if (gimbal == null) {
//            return;
//        }

        gimbal.rotate(rotation, new CallbackHandlers.CallbackToastHandler());
    }
}


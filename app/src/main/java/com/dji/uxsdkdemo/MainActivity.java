package com.dji.uxsdkdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.gimbal.Rotation;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.camera.*;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;

//import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LATITUDE;
import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LONGITUDE;

public class MainActivity extends AppCompatActivity {
    private static final double ONE_METER_OFFSET = 0.00000899322;

    private Handler mHandler;
    private boolean mRunning = true;

    private Compass compass;
    private WaypointMissionOperator waypointMissionOperator;
    private WaypointMission mission;
    private HotpointMission hotpointMission;
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


    //xiaoweigeCode
    protected double nowLatitude;
    protected double nowLongitude;
    protected double nowAltitude;
    protected double aimLatitude = 0;
    protected double aimLongitude = 0;
    protected double aimAltitude = 0;
    protected double hotLatitude = 0;
    protected double hotLongitude = 0;
    protected double hotAltitude = 0;
    protected FlightMode flightState;
    protected String IP_Address;
    protected int port;
    double car_Latitude;
    double car_Longitude;
    double car_Updata;
    int loadready = 0;
    private FlightController flightController;
    private HotpointMissionOperator hotpointMissionOperator;

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
        Button setting = (Button) findViewById(R.id.setting);
        SlideButton RTMPButton = findViewById(R.id.RTMPButton);

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TO-DO
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        RTMPButton.setBigCircleModel(Color.parseColor("#cccccc"), Color.parseColor("#00000000"), Color.parseColor("#FF4040"), Color.parseColor("#cccccc"), Color.parseColor("#cccccc"));
        RTMPButton.setOnCheckedListener(new SlideButton.SlideButtonOnCheckedListener() {
            @Override
            public void onCheckedChangeListener(boolean isChecked) {
                if (isChecked) {
                    //打开直播流
                    liveShowUrl = "rtmp://192.168.1.97:1935/live/home";
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!MApplication.isAircraftConnected() || !isLiveStreamManagerOn()) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (isLiveStreamManagerOn()) {
                                listener = new LiveStreamManager.OnLiveChangeListener() {
                                    @Override
                                    public void onStatusChanged(int i) {
                                        //showToast("status changed : " + i);
                                    }
                                };
                                DJISDKManager.getInstance().getLiveStreamManager().registerListener(listener);
                            }
                            if (MApplication.isAircraftConnected()) {
                                if (isLiveStreamManagerOn()) {
                                    if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
                                        showToast("livestream already started!");
                                    } else {
                                        DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(liveShowUrl);
                                        DJISDKManager.getInstance().getLiveStreamManager().setAudioStreamingEnabled(false);
                                        int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                                        showToast(result == 0 ? "livestream start success" : "error code: " + result);
                                        DJISDKManager.getInstance().getLiveStreamManager().setStartTime();

                                        ToastUtils.setResultToToast("startLive:" + result +
                                                "\n isVideoStreamSpeedConfigurable:" + DJISDKManager.getInstance().getLiveStreamManager().isVideoStreamSpeedConfigurable() +
                                                "\n isLiveAudioEnabled:" + DJISDKManager.getInstance().getLiveStreamManager().isLiveAudioEnabled());
                                    }
                                } else {
                                    showToast("onStreamError!");
                                }
                            } else {
                                showToast("AircraftconnectedCurrent Error");
                            }

                        }
                    }).start();
                } else {
                    //关闭直播流
                    if (!isLiveStreamManagerOn()) {
                        return;
                    }
                    DJISDKManager.getInstance().getLiveStreamManager().stopStream();
                    ToastUtils.setResultToToast("Stop Live Show");
                }
            }
        });

        fpv = (TextureView) findViewById(R.id.fpvWidget);
        bitmaps = new Bitmap[10];
        count = 0;
        app = (MApplication) getApplication();

        //points初始化
        float[] tempf = new float[2];
        tempf[0] = 5000;
        tempf[1] = 0;
        app.setPoints(tempf);

        flightState = null;
        IP_Address = "192.168.1.120";
        port = 54321;
        car_Latitude = 0;
        car_Longitude = 0;
        car_Updata = 0;
        loadready = 0;

        //热点环绕任务
        cca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                View view2 = View.inflate(MainActivity.this, R.layout.input, null);
                final EditText lon = (EditText) view2.findViewById(R.id.username);
                final EditText lat = (EditText) view2.findViewById(R.id.password);
                final EditText alt = (EditText) view2.findViewById(R.id.alt);
                lon.setText(Double.toString(hotLongitude));
                lat.setText(Double.toString(hotLatitude));
                alt.setText(Double.toString(hotAltitude));
                builder.setTitle("Input").setIcon(R.drawable.ic_access_locker_info).setView(view2).setNegativeButton("取消", null);
                builder.setCancelable(true);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tlon = lon.getText().toString().trim();
                        String tlat = lat.getText().toString().trim();
                        String talt = alt.getText().toString().trim();
                        Double dlon;
                        Double dlat;
                        Double dalt;
                        try {
                            dlon = Double.valueOf(tlon);
                            dlat = Double.valueOf(tlat);
                            dalt = Double.valueOf(talt);
                            hotLatitude = dlat;
                            hotLongitude = dlon;
                            hotAltitude = dalt;
                        } catch (NumberFormatException e) {
                            showToast("输入数据有误");
                        }

                        if (MApplication.isAircraftConnected()) {
                            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                                if (flightController == null)
                                    flightController =
                                            ((Aircraft) MApplication.getProductInstance()).getFlightController();

                                //4.missionoperator任务初始化
                                while (hotpointMissionOperator == null) {
                                    hotpointMissionOperator = MissionControl.getInstance().getHotpointMissionOperator();
                                    if (hotpointMissionOperator == null)
                                        showToast("start init waypointOperator");
                                }

                                //6. 起飞
                                if (nowAltitude < 0.1) {
                                    flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            showToast(djiError == null ? "take off!" : djiError.getDescription());
                                        }
                                    });

                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    showToast("is already fly");
                                }

                                //创建hotpoint任务
                                hotpointMission = new HotpointMission();
                                hotpointMission.setHotpoint(new LocationCoordinate2D(hotLatitude, hotLongitude));
                                hotpointMission.setHeading(HotpointHeading.TOWARDS_HOT_POINT);
                                hotpointMission.setAltitude(15);
                                hotpointMission.setRadius(5);
                                hotpointMission.setClockwise(true);
//
                                if (hotpointMission != null) {
                                    if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                                            || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {

                                        hotpointMissionOperator.startMission(hotpointMission, new CommonCallbacks.CompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                ToastUtils.showToast(djiError == null ? "hotpoint mission success" : djiError.getDescription());
                                            }
                                        });
                                    } else {
                                        showToast("Not ready!");
                                    }

                                } else {
                                    //showToast("null  mission!");
                                }
                                //showToast("loadready "+loadready);

                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                showToast("FlightControllerCurrent Error");
                            }
                        } else {
                            showToast("AircraftConnectedCurrent Error");
                        }

                    }
                });
            }
        });

        cc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this,CameraCalibrationActivity.class);
//                startActivity(intent);

//                //Aruco测试
//                bytes = "hello world".getBytes();
//                if (bytes != null) {
//                    Intent intent = new Intent(MainActivity.this, VideoDataActivity.class);
//                    startActivityForResult(intent, 1);
//                } else {
//                    showToast("bytes is null!");
//                }

                //任务数据初始化
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                View view2 = View.inflate(MainActivity.this, R.layout.input, null);
                final EditText lon = (EditText) view2.findViewById(R.id.username);
                final EditText lat = (EditText) view2.findViewById(R.id.password);
                final EditText alt = (EditText) view2.findViewById(R.id.alt);
                lon.setText(Double.toString(aimLongitude));
                lat.setText(Double.toString(aimLatitude));
                alt.setText(Double.toString(aimAltitude));
                builder.setTitle("Input").setIcon(R.drawable.ic_access_locker_info).setView(view2).setNegativeButton("取消", null);
                builder.setCancelable(true);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tlon = lon.getText().toString().trim();
                        String tlat = lat.getText().toString().trim();
                        String talt = alt.getText().toString().trim();
                        Double dlon;
                        Double dlat;
                        Double dalt;
                        try {
                            dlon = Double.valueOf(tlon);
                            dlat = Double.valueOf(tlat);
                            dalt = Double.valueOf(talt);
                            aimLatitude = dlat;
                            aimLongitude = dlon;
                            aimAltitude = dalt;
                        } catch (NumberFormatException e) {
                            showToast("输入数据有误");
                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

//                        //1.云台向下
//                        Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
//                        builder.pitch((float) (-90.0));
//                        sendRotateGimbalCommand(builder.build());
//                        builder.yaw((float) (0));
//                        sendRotateGimbalCommand(builder.build());
//                        builder.roll((float) (0));
//                        sendRotateGimbalCommand(builder.build());

                                //3.飞行实例获取
                                if (MApplication.isAircraftConnected()) {
                                    if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                                        if (flightController == null)
                                            flightController =
                                                    ((Aircraft) MApplication.getProductInstance()).getFlightController();

                                        //4.mission任务初始化
                                        while (waypointMissionOperator == null) {
                                            waypointMissionOperator = MissionControl.getInstance().getWaypointMissionOperator();
                                            if (waypointMissionOperator == null)
                                                showToast("start init waypointOperator");
                                        }

                                        if (MApplication.isAircraftConnected()) {
                                            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                                                if (flightController != null) {
                                                    flightController.setStateCallback(new FlightControllerState.Callback() {
                                                        @Override
                                                        public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                                                            nowLatitude = flightControllerState.getAircraftLocation().getLatitude();
                                                            nowLongitude = flightControllerState.getAircraftLocation().getLongitude();
                                                            nowAltitude = flightControllerState.getAircraftLocation().getAltitude();
                                                            flightState = flightControllerState.getFlightMode();
                                                        }
                                                    });
                                                }
                                            }
                                        }


                                        //6. 起飞
                                        if (nowAltitude < 0.1) {
                                            flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                                                @Override
                                                public void onResult(DJIError djiError) {
                                                    showToast(djiError == null ? "take off!" : djiError.getDescription());
                                                }
                                            });

                                            try {
                                                Thread.sleep(5000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            showToast("is already fly");
                                        }

                                        //8.waypoint执行
//                                waypointMissionOperator.clearMission();
                                        //创建waypoint任务
                                        mission = createWaypointMission(aimLatitude, aimLongitude, (float) aimAltitude);
//                              mission = createRandomWaypointMission(1,1);

                                        if (mission != null) {
                                            DJIError djiError = waypointMissionOperator.loadMission(mission);
                                            if (djiError != null)
                                                showToast(djiError.getDescription());
                                            if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                                                    || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {

                                                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                                                    @Override
                                                    public void onResult(DJIError djiError) {
                                                        if (djiError == null) {
                                                            showToast("upload success");
                                                        } else {
                                                            showToast("retry");
                                                            waypointMissionOperator.retryUploadMission(new CommonCallbacks.CompletionCallback() {
                                                                @Override
                                                                public void onResult(DJIError djiError) {
                                                                    if (djiError == null) {
                                                                        showToast("upload success");
                                                                    } else {
                                                                        showToast("retryMission: " + djiError.getDescription());
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                            } else {
                                                showToast("Not ready!");
                                            }

                                        } else {
                                            //showToast("null  mission!");
                                        }
                                        //showToast("loadready "+loadready);

                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                showToast(djiError != null ? djiError.getDescription() : "start mission success");
                                            }
                                        });
//                               waypointMissionOperator.destroy();
//                                    try {
//                                        Thread.sleep(6000);
//                                    } catch (InterruptedException e) {
//                                        e.printStackTrace();
//                                    }


//                                waypointMissionOperator.stopMission(new CommonCallbacks.CompletionCallback() {
//                                    @Override
//                                    public void onResult(DJIError djiError) {
//                                        showToast(djiError.getDescription());
//                                    }
//                                });
//                                waypointMissionOperator.destroy();
//                                    // 14. 降落
//                                    flightController.startLanding(new CommonCallbacks.CompletionCallback() {
//                                        @Override
//                                        public void onResult(DJIError djiError) {
//                                            if (null != djiError)
//                                                showToast(djiError.getDescription());
//                                        }
//                                    });
//                                }
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
                builder.create().show();

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
                //无人机状态
                int tempp = 0;
                while (temp == 0) {
                    if (MApplication.isAircraftConnected() && temp == 0) {
                        BaseProduct product = MApplication.getProductInstance();
                        if (product != null) {
                            if (product instanceof Aircraft) {
                                gimbal = ((Aircraft) product).getGimbals().get(currentGimbalId);
                            } else {
                                gimbal = product.getGimbal();
                            }
                            temp = 1;
                            //showToast("Ok:gimbal");
                        } else {
                            showToast("fail:gimbal");
                        }
                    }
                }
            }
        }).start();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PicDownloadActivity.class);
                startActivity(intent);
            }
        });

//        Intent startRTMPService = new Intent(this, com.dji.uxsdkdemo.RTMPService.class);
//        startService(startRTMPService);

        //直播流服务

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
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.repeatTimes(1);
        builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);

        double baseLatitude = 22;
        double baseLongitude = 113;
        Object latitudeValue = KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LATITUDE)));
        Object longitudeValue =
                KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LONGITUDE)));
        if (latitudeValue != null && latitudeValue instanceof Double) {
            baseLatitude = (double) latitudeValue;
        }
        if (longitudeValue != null && longitudeValue instanceof Double) {
            baseLongitude = (double) longitudeValue;
        }

        List<Waypoint> waypointList = new ArrayList<>();
        Waypoint eachWaypoint0 = new Waypoint(baseLatitude, baseLongitude, (float) aimAltitude);
        eachWaypoint0.addAction(new WaypointAction(WaypointActionType.STAY, 1));
        waypointList.add(eachWaypoint0);

        final double v = (Math.floor(1 / 4) + 1) * 2 * ONE_METER_OFFSET * Math.pow(-1, 1) * Math.pow(0, 1 % 2);
//        Waypoint eachWaypoint1 = new Waypoint(baseLatitude+30*ONE_METER_OFFSET,
//                baseLongitude,
//                15f);

        Waypoint eachWaypoint1 = new Waypoint(baseLatitude + to_Latitude * ONE_METER_OFFSET,
                baseLongitude + to_Longitude * ONE_METER_OFFSET,
                (float) to_height);
        eachWaypoint1.addAction(new WaypointAction(WaypointActionType.STAY, 1));
        waypointList.add(eachWaypoint1);


        builder.waypointList(waypointList).waypointCount(waypointList.size());
//        showToast("baseLatiyude:"+Double.toString(baseLatitude));
//        showToast("aimLatitude:"+Double.toString(aimLatitude));
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

    @Override
    protected void onStart() {
        super.onStart();

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


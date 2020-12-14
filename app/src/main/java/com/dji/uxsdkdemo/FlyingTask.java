package com.dji.uxsdkdemo;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Timer;

import dji.common.error.DJIError;
import dji.common.flightcontroller.ObstacleDetectionSector;
import dji.common.flightcontroller.VisionDetectionState;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class FlyingTask {
    private final Object lock = new Object();
    private boolean pause = false;
    private MainActivity activity;

    public FlyingTask(MainActivity activity) {
        this.activity = activity;
    }

    //飞行任务类
    class FlyTask extends Thread {
        private Boolean temp;
        public Timer timer;
        public Boolean status;

        public FlyTask() {
            this.temp = false;
        }


        @Override
        public void run() {
            status = true;
            if (MApplication.isAircraftConnected()) {
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
                    timer = new Timer();
                    timer.schedule(task, 0, 1);
                    // 7. 飞行矩形轨迹
                    try {
                        showToast("Starting Task");
                        setControlData(0, 1, 0, 0, task);
                        Thread.sleep(5000);
                        setControlData(0, 0, 0, 0, task);
                        Thread.sleep(1000);

                        setControlData(1, 0, 0, 0, task);
                        Thread.sleep(3000);
                        setControlData(0, 0, 0, 0, task);
                        Thread.sleep(1000);

                        setControlData(0, -1, 0, 0, task);
                        Thread.sleep(5000);
                        setControlData(0, 0, 0, 0, task);
                        Thread.sleep(1000);

                        setControlData(-1, 0, 0, 0, task);
                        Thread.sleep(3000);
                        setControlData(0, 0, 0, 0, task);
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
                                if (null != djiError)
                                    showToast(djiError.getDescription());
                            }
                        });

                    }
                } else {
                    showToast("FlightControllerCurrent Error");
                }
            } else {
                //showToast("AircraftconnectedCurrent Error");
            }
            status = false;
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
    }

    //避障类
    class ObstacleAvoidance extends Thread {
        FlyTask flyTask;

        public ObstacleAvoidance(FlyTask flyTask) {
            this.flyTask = flyTask;
        }

        public void onPause() {
            try {
                if (flyTask.status == true)
                    flyTask.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void onResume() {
            if (flyTask.status == true)
                flyTask.notifyAll();
        }

        @Override
        public void run() {
            while (true) {
                if (MApplication.isAircraftConnected()) {
                    if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                        FlightController flightController =
                                ((Aircraft) MApplication.getProductInstance()).getFlightController();

                        FlightAssistant intelligentFlightAssistant = flightController.getFlightAssistant();
                        if (intelligentFlightAssistant != null) {
                            intelligentFlightAssistant.setVisionDetectionStateUpdatedCallback(new VisionDetectionState.Callback() {
                                @Override
                                public void onUpdate(VisionDetectionState visionDetectionState) {
                                    //showToast(visionDetectionState.getPosition().toString());
                                    ObstacleDetectionSector[] visionDetectionSectorArray =
                                            visionDetectionState.getDetectionSectors();
                                    float distance = visionDetectionSectorArray[0].getObstacleDistanceInMeters();
                                    if (distance < 1) {
                                        onPause();
                                    } else {
                                        onResume();
                                    }
                                }
                            });
                        } else {
                            showToast("onAttachedToWindow FC NOT Available");
                        }
                    } else {
                        //showToast("FlightControllerCurrent Error");
                    }
                } else {
                    //showToast("AircraftconnectedCurrent Error");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void showToast(final String toastMsg) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity.getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            }
        });

    }
}


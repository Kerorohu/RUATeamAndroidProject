package com.dji.uxsdkdemo;

import android.util.Log;

import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

public class SendControlDataTask extends TimerTask {
    private float _pitch;
    private float _roll;
    private float _yaw;
    private float _throttle;
    private DJIError djiError;
    FlightController flightController;
    private String TAG = this.getClass().getSimpleName();
    public SendControlDataTask(FlightController flightController) {
        this.flightController = flightController;
    }

    public void set_pitch(float _pitch) {
        this._pitch = _pitch;
    }

    public void set_roll(float _roll) {
        this._roll = _roll;
    }

    public void set_yaw(float _yaw) {
        this._yaw = _yaw;
    }

    public void set_throttle(float _throttle) {
        this._throttle = _throttle;
    }

    public DJIError getDjiError() {
        return djiError;
    }

    public void setDjiError(DJIError djiError) {
        this.djiError = djiError;
    }

    @Override
    public void run() {
        flightController.sendVirtualStickFlightControlData(new FlightControlData(_pitch,
                        _roll,
                        _yaw,
                        _throttle),
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null != djiError) {
                            Log.i(TAG, "an error occured while run Task: " + djiError.getDescription());
                            setDjiError(djiError);
                        }
                    }
                });
    }
}

package com.example.mapdemo;

import android.hardware.SensorEvent;
import android.hardware.SensorManager;

public class XYZAccelerometer extends Accelerometer {

    private static final int BUFFER_SIZE = 500;
    // calibration
    private  float dX = 0;
    private  float dY = 0;
    private  float dZ = 0;
    // buffer variables
    private float X;
    private float Y;
    private float Z;
    private int cnt = 0;


    @Override
    public Point getPoint() {
        return null;
    }

    // resets buffer
    public void reset(){
        cnt = 0;
        X = 0;
        Y = 0;
        Z = 0;
    }

    public  void setdX(float dX) {
        this.dX = dX;
    }

    public  void setdY(float dY) {
        this.dY = dY;
    }

    public  void setdZ(float dZ) {
        this.dZ = dZ;
    }
    @Override
    public void onSensorChanged(SensorEvent se) {
        float x = se.values[SensorManager.DATA_X] + dX;
        float y = se.values[SensorManager.DATA_Y] + dY;
        float z = se.values[SensorManager.DATA_Z] + dZ;

        lastX = x;
        lastY = y;
        lastZ = z;

        X+= x;
        Y+= y;
        Z+= z;

        if (cnt < BUFFER_SIZE-1) {
            cnt++;
        } else
        {
            reset();
        }
    }
}

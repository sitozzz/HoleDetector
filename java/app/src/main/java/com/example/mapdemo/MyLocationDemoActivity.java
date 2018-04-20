/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mapdemo;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

public class MyLocationDemoActivity extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        LocationSource.OnLocationChangedListener,
        //SensorEventListener,
        OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    public static String fileName;
    private boolean mPermissionDenied = false;
    public static Location locationbuffer;
    private GoogleMap mMap;
    private float last_speed = 0;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    //Очередь
    public List<Float> XArray;
    public List<Float> YArray;
    public List<Float> ZArray;
    public int count;
    //Вычисленные значения вектора гравитации
    public float medX;
    public float medY;
    public float medZ;
    //Ускорение вертикальной оси
    public float gAccel;
    public boolean logging = false;
    public static int sessionNumber = 0;
    private FileSaver saver;
    public FrameLayout layout;

    public MyLocation.LocationResult locationResult = new MyLocation.LocationResult(){
        @Override
        public void gotLocation(Location location){
            locationbuffer = location;
        }
    };
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_location_demo);
        layout = (FrameLayout) findViewById(R.id.layout);
        fileName = "";
        //Аккселерометр
        //senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //senSensorManager.registerListener(this, senAccelerometer, senSensorManager.SENSOR_DELAY_NORMAL);
        //SupportMapFragment mapFragment =
          //      (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);

        MyLocation myLocation = new MyLocation(getApplicationContext());
        myLocation.getLocation(this, locationResult);
        //Ask permission to write stornage
        getPermission();
        final Button holeBtn = (Button) findViewById(R.id.Hole);
        holeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng pos = new LatLng(locationbuffer.getLatitude(), locationbuffer.getLongitude());
                if(saver.isRecord()) {
                    saver.addToBuffer("Hole");
                }
            }
        });

        final Button policeBtn = (Button) findViewById(R.id.Police);
        policeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng pos = new LatLng(locationbuffer.getLatitude(), locationbuffer.getLongitude());
                if(saver.isRecord()) {
                    saver.addToBuffer("Police");
                }
            }
        });

        final Button railsBtn = (Button) findViewById(R.id.Rails);
        railsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng pos = new LatLng(locationbuffer.getLatitude(), locationbuffer.getLongitude());
                if(saver.isRecord()) {
                    saver.addToBuffer("Rails");
                }
            }
        });

        final Button startLog = (Button) findViewById(R.id.startWrite);
        startLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fileName == ""){
                    fileName = String.valueOf(Calendar.getInstance().getTime());
                }
                if (saver.isRecord()){
                    saver.stopRecord();
                    sessionNumber += 1;
                    startLog.setText("Stop logging");
                } else {
                    saver.startRecord();
                    startLog.setText("Logging");
                }
                changeBgColor();
            }
        });
        saver = new FileSaver(getApplicationContext());
    }

    @Override
    protected void onPause() {

        super.onPause();
        //senSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        //senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void getPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int canRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int canWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (canRead != PackageManager.PERMISSION_GRANTED || canWrite != PackageManager.PERMISSION_GRANTED) {

                //Нужно ли нам показывать объяснения , зачем нам нужно это разрешение
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //показываем объяснение
                } else {
                    //просим разрешение
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 5000);
                }
            } else {
                //ваш код
            }
        }
    }
    @Override
    public void onBackPressed() {
        LatLng pos = new LatLng(locationbuffer.getLatitude(), locationbuffer.getLongitude());
        //mMap.addMarker(new MarkerOptions().position(pos).title("Яма"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
        if(saver.isRecord()) {
            saver.addToBuffer("Hole");
            //saver.writeToFile("Human");
            Toast.makeText(this, "Яма записана пользователем", Toast.LENGTH_SHORT).show();
            Log.d("loc", "Write buffer to file");
        }
    }

    public void changeBgColor(){
        if (saver.isRecord()) {
            layout.setBackgroundColor(Color.RED);
        }
        else {
            layout.setBackgroundColor(Color.GREEN);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        //mMap = map;
        //mMap.setOnMyLocationButtonClickListener(this);
        //mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }


    @Override
    public void onLocationChanged(Location location) {

    }
}

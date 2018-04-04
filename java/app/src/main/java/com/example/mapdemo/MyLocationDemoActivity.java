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
import android.util.Log;
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

/**
 * This demo shows how GMS Location can be used to check for changes to the users location.  The
 * "My Location" button uses GMS Location to set the blue dot representing the users location.
 * Permission for {@link android.Manifest.permission#ACCESS_FINE_LOCATION} is requested at run
 * time. If the permission has not been granted, the Activity is finished with an error message.
 */

public class MyLocationDemoActivity extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        LocationSource.OnLocationChangedListener,
        SensorEventListener,
        OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;


    private boolean mPermissionDenied = false;
    public Location locationbuffer;
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
    public MyLocation.LocationResult locationResult = new MyLocation.LocationResult(){
        @Override
        public void gotLocation(Location location){
            Log.d("loc", "latt = " + String.valueOf(location.getLatitude()) + ", long = " +String.valueOf(location.getLongitude()));
            locationbuffer = location;
            //Toast.makeText(, "Got the location", Toast.LENGTH_LONG).show();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_location_demo);
        //Аккселерометр
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, senSensorManager.SENSOR_DELAY_NORMAL);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        MyLocation myLocation = new MyLocation(getApplicationContext());
        myLocation.getLocation(this, locationResult);

        XArray = new ArrayList<>();
        YArray = new ArrayList<>();
        ZArray = new ArrayList<>();
        //Счетчик измерений
        count = 0;
        medX = 0.0f;
        medY = 0.0f;
        medZ = 0.0f;
    }

    @Override
    protected void onPause() {

        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if(locationbuffer != null && last_speed != 0){

            LatLng pos = new LatLng(locationbuffer.getLatitude(), locationbuffer.getLongitude());
            mMap.addMarker(new MarkerOptions().position(pos).title("Яма"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
            writeToFile(locationbuffer, gAccel, "Human");
            Toast.makeText(this, "Яма записана пользователем", Toast.LENGTH_SHORT).show();
            Log.d("loc", "Write buffer to file");
        }

    }

//    public void writeFile() {
//        try {
//            // отрываем поток для записи
//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
//                    openFileOutput("holes.txt", MODE_APPEND)));
//            // пишем данные
//            bw.write("Содержимое файла");
//            // закрываем поток
//            bw.close();
//            Log.d("file", "Файл записан");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    public void writeToFile(Location location, float power, String author){

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


        String longt = String.valueOf(location.getLongitude());
        String latt = String.valueOf(location.getLatitude());

        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.d("file", "SD-карта не доступна: " + Environment.getExternalStorageState());
            return;
        }
        // получаем путь к SD
        File sdPath = Environment.getExternalStorageDirectory();
        // добавляем свой каталог к пути
        sdPath = new File(sdPath.getAbsolutePath() + "/" + "log/");
        Log.d("path", sdPath.getAbsolutePath());
        // создаем каталог
        sdPath.mkdirs();
        // формируем объект File, который содержит путь к файлу
        //File sdFile = new File("/mnt/sdcard/", "fileSD.txt");
        File sdFile = new File(sdPath, "log.txt");
        try {
            // открываем поток для записи
            BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile, true));
            // пишем данные
            //Дата долгота широта ускорение по вертикальной оси
            if(author == "Sensor"){
                bw.write(String.valueOf(Calendar.getInstance().getTime()) + " By" + author + " " + longt + " " + latt + " " + String.valueOf(power) + " ,");
            }
            else {
                bw.write(String.valueOf(Calendar.getInstance().getTime()) + " By" + author + " " + longt + " " + latt + " " + String.valueOf(power) + " ,");
            }
            // закрываем поток
            bw.close();
            Log.d("file", "Файл записан на SD: " + sdFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
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



        //        if (location != null) {
//            Toast.makeText(this, String.valueOf(location.getLongitude() + ";" + location.getLatitude()), Toast.LENGTH_LONG).show();
//        }
//        else {
//            Toast.makeText(this, "Location is null", Toast.LENGTH_LONG).show();
//        }
    }
    //Среднее арифметическое
    public float arMean(float a, float b){
        return (a + b) / 2;
    }

    public void firstOut(List<Float> list){
        float buffer = 0.0f;
        for (i = 0; i<49; i++){
            list.add(i, list.get(i + 1));
        }
    }

    //Запись вектора горизонтального ускорения
    public void getGVector(float x, float y, float z){
        if(count != 49) {
            XArray.add(count, x);
            //XArray[count] = x;
            YArray.add(count, y);
            ZArray.add(count, x);
            count++;

            Log.d("accel", "Count = " + String.valueOf(count));
        }
        else {
            //Записываем значение медианы
            medX = arMean(XArray.get(25), XArray.get(26));
            medY = arMean(YArray.get(25), YArray.get(26));
            medZ = arMean(ZArray.get(25), ZArray.get(26));
            //Удаляем последний элемент массива и сдвигаем на 1 позицию
            firstOut(XArray);
            firstOut(YArray);
            firstOut(ZArray);
            //Добавляем новый элемент в конец
            XArray.add(49, x);
            YArray.add(49, y);
            ZArray.add(49, z);

        }
    }

    public float scalarMultiply(float x1, float y1, float z1, float x2, float y2, float z2){
        return x1*x2 + y1*y2 + z1*z2;
    }
    public float vectorLength(float x, float y, float z){
        return (float) Math.sqrt(x*x + y*y + z*z);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastUpdate) > 100){
                long difftime = (currentTime - lastUpdate);
                lastUpdate = currentTime;
                //Вычисляем вектор гравитации
                getGVector(x, y, z);

                if(medX != 0.0f && medY != 0.0f && medZ != 0.0f){
                    //Вычисляем проекцию вектора
                    gAccel = scalarMultiply(last_x, last_y, last_z, medX, medY, medZ) / vectorLength(medX, medY, medZ);
                    writeToFile(locationbuffer, gAccel,"NaN");
                    //Log.d("accel", "accel = " + String.valueOf(accel));
                }

                float speed = Math.abs(y - last_y)/ difftime * 10000;
                if (speed > SHAKE_THRESHOLD) {
                    last_speed = speed;
                    //Log.d("acceldbg", "Speed = " + String.valueOf(speed));
                    //Log.d("acceldbg", "Z = " + String.valueOf(z));
                    Toast.makeText(this, "Опять яма!", Toast.LENGTH_SHORT).show();
                    //writeToFile(locationbuffer, last_speed, "Sensor");
                }

                last_x = x;
                last_y = y;
                last_z = z;
                //Log.d("acceldbg", "accelerometr x = "+String.valueOf(x));
                //Log.d("acceldbg", "accelerometr y = "+String.valueOf(y));
                //Log.d("acceldbg", "accelerometr z = "+String.valueOf(z));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

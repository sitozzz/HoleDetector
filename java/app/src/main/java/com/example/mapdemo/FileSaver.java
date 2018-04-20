package com.example.mapdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileSaver implements SensorEventListener, Runnable {
    private AtomicBoolean isRecord = new AtomicBoolean(false);
    List<String> buffer = new ArrayList<>();
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
    //public int sessionNumber = 0;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Context appContext;
    private float last_x, last_y, last_z;
    private long lastUpdate = 0;
    private static final Object mutex = new Object();

    public FileSaver(Context context) {
        appContext = context;
        // init_sensors
        XArray = new ArrayList<>();
        YArray = new ArrayList<>();
        ZArray = new ArrayList<>();
        //Счетчик измерений
        count = 0;
        medX = 0.0f;
        medY = 0.0f;
        medZ = 0.0f;
        //Аккселерометр
        senSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, senSensorManager.SENSOR_DELAY_NORMAL);

        Thread thread = new Thread(this);
        thread.start();
    }

    public synchronized void addToBuffer(String author) {
        synchronized (mutex) {
            String longt = String.valueOf(MyLocationDemoActivity.locationbuffer.getLongitude());
            String latt = String.valueOf(MyLocationDemoActivity.locationbuffer.getLatitude());
            buffer.add(MyLocationDemoActivity.sessionNumber + " " + String.valueOf(System.currentTimeMillis()) + " By" + author + " " + longt + " " + latt + " " + String.valueOf(gAccel) + ",");
        }
    }

    public void writeToFile(List<String> buffer){
        Log.d("THREAD", " write file " + Thread.currentThread().getName());
        //String longt = String.valueOf(MyLocationDemoActivity.locationbuffer.getLongitude());
        //String latt = String.valueOf(MyLocationDemoActivity.locationbuffer.getLatitude());

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
        // add
        // if > 100
            // write
            // clean
        //if(buffer.size()<100){
          //  buffer.add(MyLocationDemoActivity.sessionNumber + " " + String.valueOf(System.currentTimeMillis()) + " By" + author + " " + longt + " " + latt + " " + String.valueOf(gAccel) + ",");
        //}
        //else {
            try {
                // открываем поток для записи
                BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile, true));
                // пишем данные
                for(int i = 0; i < buffer.size(); i++) {
                    bw.write(buffer.get(i));
                }
                //Дата долгота широта ускорение по вертикальной оси
                //if (author == "Sensor") {
                    //old time
                    //Calendar.getInstance().getTime()
                    //bw.write(MyLocationDemoActivity.sessionNumber + " " + String.valueOf(System.currentTimeMillis()) + " By" + author + " " + longt + " " + latt + " " + String.valueOf(gAccel) + ",");
                //} else {
                    //old time
                    //Calendar.getInstance().getTime()
                    //bw.write(MyLocationDemoActivity.sessionNumber + " " + String.valueOf(System.currentTimeMillis()) + " By" + author + " " + longt + " " + latt + " " + String.valueOf(gAccel) + ",");
                //}
                // закрываем поток
                bw.close();
                Log.d("file", "Файл записан на SD: " + sdFile.getAbsolutePath());
                //buffer = new ArrayList<>();
            } catch (IOException e) {
                e.printStackTrace();
            }
       // }

    }
    //Среднее арифметическое
    public float arMean(float a, float b){
        return (a + b) / 2;
    }
    //Очередь
    public void firstOut(List<Float> list){
        float buffer = 0.0f;
        for (int i = 0; i<49; i++){
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

    public synchronized boolean isRecord(){
        return isRecord.get();
    }

    public synchronized void startRecord(){
        isRecord.set(true);
    }

    public synchronized void stopRecord(){
        isRecord.set(false);
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
        // put to buffer

        // get mediana

        // put to mediana buffer

        // if mediana buffer > 1000
            // write to file

        Log.d("THREAD", "sensor changed " + Thread.currentThread().getName());

        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastUpdate) > 50){
                long difftime = (currentTime - lastUpdate);
                lastUpdate = currentTime;
                //Вычисляем вектор гравитации
                getGVector(x, y, z);

                if(medX != 0.0f && medY != 0.0f && medZ != 0.0f){
                    //Вычисляем проекцию вектора
                    gAccel = scalarMultiply(last_x, last_y, last_z, medX, medY, medZ) / vectorLength(medX, medY, medZ);

                    //Log.d("loctime", String.valueOf(System.currentTimeMillis()));
                    if (isRecord()) {

                        //writeToFile( "NaN");
                        addToBuffer("Nan");
                        Log.d("THREAD", "write thread ------->" + Thread.currentThread().getName() + "size = " + buffer.size());
                        Log.d("gaccel", "gaccel = " + String.valueOf(gAccel));
                    }
                }

//                //Don't use anymore
//                float speed = Math.abs(y - last_y)/ difftime * 10000;
//                if (speed > SHAKE_THRESHOLD) {
//                    last_speed = speed;
//                    //Log.d("acceldbg", "Speed = " + String.valueOf(speed));
//                    //Log.d("acceldbg", "Z = " + String.valueOf(z));
//                    Toast.makeText(this, "Опять яма!", Toast.LENGTH_SHORT).show();
//                    //writeToFile(locationbuffer, last_speed, "Sensor");
//                }

                last_x = x;
                last_y = y;
                last_z = z;
                //Log.d("acceldbg", "accelerometr x = "+String.valueOf(x));
                //Log.d("acceldbg", "accelerometr y = "+String.valueOf(y));
                //Log.d("acceldbg", "accelerometr z = "+String.valueOf(z));
            }
        }


    }

    public synchronized List<String> getBuffer(){
        synchronized (mutex) {
            if (buffer.size() < 100) {
                return null;
            }
            List<String> new_buff = new ArrayList<>(buffer.size()+1);
            new_buff.addAll(buffer);
            //Collections.copy(new_buff, buffer);
            buffer.clear();
            //System.gc();
            return new_buff;
        }
    }

    @Override
    public synchronized void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void run() {
        while (true){
           sleep(10);
           List<String> buff = getBuffer();
           if (buff != null){
               Log.d("THREAD", "write thread " + Thread.currentThread().getName());
               writeToFile(buff);
               buff.clear();
           }
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

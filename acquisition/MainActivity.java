package com.example.ruslan.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    TextView tvText;
    String buf;
    int Rec = 0;

    SensorManager sensorManager;
    Sensor sensorAcceleration;

    Timer timer;


    float[] valuesAcceleration = new float[3];
    float[] gravity = new float[3];

    EditText mEdit;
    TextView letText;
    final String LOG_TAG = "myLogs";
    final String DataFile = "mnt/sdcard-ext/sensor_data";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvText = (TextView) findViewById(R.id.tvText);
        mEdit   = (EditText)findViewById(R.id.editText);
        letText = (TextView) findViewById(R.id.letter);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensorAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(listener, sensorAcceleration,
                SensorManager.SENSOR_DELAY_NORMAL);

        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showInfo();
                    }
                });
            }
        };
        timer.schedule(task, 0, 50);
    }

    private void writeFileSD(String line) {
        // проверяем доступность SD
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Log.d(LOG_TAG, "SD-карта не доступна: " + Environment.getExternalStorageState());
            return;
        }
        // формируем объект File, который содержит путь к файлу
        File sdFile = new File(DataFile);
        try {
            // открываем поток для записи
            BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile, true));
            // пишем данные
            bw.write(line);
            // закрываем поток
            bw.close();
            Log.d(LOG_TAG, "Файл записан на SD: " + sdFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showInfo(){
        String line1 = "Acceleration: \n"+String.valueOf(valuesAcceleration[0])+" \n"+String.valueOf(valuesAcceleration[1])+ " \n"+
                String.valueOf(valuesAcceleration[2])+"\n";

        letText.setText(mEdit.getText().toString());
        tvText.setText(line1);
        if (Rec == 1) {
            buf = buf + String.valueOf(valuesAcceleration[0]) + ", " +
                    String.valueOf(valuesAcceleration[1]) + ", " +
                    String.valueOf(valuesAcceleration[2]) + ", ";
        }

    }


    public void onClickStartRecord(View v){
        Rec = 1;
    }

    public void onClickStopRecord(View v){
        Rec = 0;
        buf = buf + mEdit.getText().toString() + "\n";
        writeFileSD(buf);
        buf = "";
    }


    public void onClickClear(View v) {
        File sdFile = new File(DataFile);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile));
            bw.close();
            Log.d(LOG_TAG, "Файл стерт");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickExit(View v){
        finish();
        System.exit(0);
    }
    @Override
    protected void onPause() {
        super.onPause();
        //sensorManager.unregisterListener(listenerLight, sensorLight);
    }

    SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    for (int i = 0; i < 3; i++) {
                        valuesAcceleration[i] = event.values[i];
                    }
                    float alpha = (float) 0.8;

                    // Isolate the force of gravity with the low-pass filter.
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                    // Remove the gravity contribution with the high-pass filter.
                    valuesAcceleration[0] = event.values[0] - gravity[0];
                    valuesAcceleration[1] = event.values[1] - gravity[1];
                    valuesAcceleration[2] = event.values[2] - gravity[2];
                    break;
            }
        }
    };


}

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    TextView tvText;
    int Rec = 0;

    SensorManager sensorManager;
    Sensor sensorAcceleration;

    Timer timer;


    float[] valuesAcceleration = new float[3];
    float[] gravity = new float[3];
    float[] X = new float[20];
    float[] Y = new float[20];
    List<Float> Xraw = new ArrayList<Float>();
    List<Float> Yraw = new ArrayList<Float>();
    EditText mEdit;
    TextView letText;
    TextView statusText;
    final String LOG_TAG = "myLogs";
    final String DataFile = "mnt/sdcard-ext/dataset.csv";
    float [][] dataset = new float[800][40];
    String [] labels = new String [800];
    int svm_flag = 0;
    int dtw_flag = 0;

    //float [][] dtw_expgram = new float[800][800];
    float [][] dtw_a = new float[25][744];
    String [] dtw_cs = new String [26];
    int [] dtw_start = new int[26];
    int [] dtw_end = new int[26];
    int [] dtw_sv = new int[744];
    float [] dtw_b = new float[325];
    float [][] euc_a = new float[25][744];
    String [] euc_cs = new String [26];
    int [] euc_start = new int[26];
    int [] euc_end = new int[26];
    int [] euc_sv = new int[744];
    float [] euc_b = new float[325];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readFileSD();
        readFileSD_svm_dtw();
        setContentView(R.layout.activity_main);
        tvText = (TextView) findViewById(R.id.tvText);

        letText = (TextView) findViewById(R.id.letter);
        statusText = (TextView) findViewById(R.id.status);
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

    private void readFileSD() {
        // проверяем доступность SD
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        // формируем объект File, который содержит путь к файлу
        try {
            br = new BufferedReader(new FileReader(DataFile));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < 40; i = i + 1) {
                    dataset[count][i] = Float.parseFloat(obs[i]);
                }
                labels[count] = obs[40];
                count = count + 1;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void readFileSD_svm_dtw() {
        // проверяем доступность SD
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/dtw_start_end.csv"));
            int count = 0;
            line = br.readLine();
            // use comma as separator
            String[] obs = line.split(cvsSplitBy);
            for (int i = 0; i < 26; i = i + 1) {
                dtw_start[i] = Integer.parseInt(obs[i]);
            }
            line = br.readLine();
            // use comma as separator
            obs = line.split(cvsSplitBy);
            for (int i = 0; i < 26; i = i + 1) {
                dtw_end[i] = Integer.parseInt(obs[i]);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/dtw_supvec_ind.csv"));
            int count = 0;
            line = br.readLine();
                // use comma as separator
            String[] obs = line.split(cvsSplitBy);
            for (int i = 0; i < obs.length; i = i + 1) {
                dtw_sv[i] = Integer.parseInt(obs[i]);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/dtw_intercept.csv"));
            int count = 0;
            line = br.readLine();
            String[] obs = line.split(cvsSplitBy);
            for (int i = 0; i < obs.length; i = i + 1) {
                dtw_b[i] = Float.parseFloat(obs[i]);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/dtw_classes.csv"));
            int count = 0;
            line = br.readLine();
            String[] obs = line.split(cvsSplitBy);
            dtw_cs = obs;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/dtw_dual_coef.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    dtw_a[count][i] = Float.parseFloat(obs[i]);
                }
                count = count + 1;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/euc_start_end.csv"));
            int count = 0;
            line = br.readLine();
            // use comma as separator
            String[] obs = line.split(cvsSplitBy);
            for (int i = 0; i < 26; i = i + 1) {
                euc_start[i] = Integer.parseInt(obs[i]);
            }
            line = br.readLine();
            // use comma as separator
            obs = line.split(cvsSplitBy);
            for (int i = 0; i < 26; i = i + 1) {
                euc_end[i] = Integer.parseInt(obs[i]);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/euc_supvec_ind.csv"));
            int count = 0;
            line = br.readLine();
            // use comma as separator
            String[] obs = line.split(cvsSplitBy);
            for (int i = 0; i < obs.length; i = i + 1) {
                euc_sv[i] = Integer.parseInt(obs[i]);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/euc_intercept.csv"));
            int count = 0;
            line = br.readLine();
            String[] obs = line.split(cvsSplitBy);
            for (int i = 0; i < obs.length; i = i + 1) {
                euc_b[i] = Float.parseFloat(obs[i]);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/euc_classes.csv"));
            int count = 0;
            line = br.readLine();
            String[] obs = line.split(cvsSplitBy);
            euc_cs = obs;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/euc_dual_coef.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    euc_a[count][i] = Float.parseFloat(obs[i]);
                }
                count = count + 1;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showInfo(){
        String line1 = "Acceleration: \n"+String.valueOf(valuesAcceleration[0])+" \n"+String.valueOf(valuesAcceleration[1])+ " \n"+
                String.valueOf(valuesAcceleration[2])+"\n";

        tvText.setText(line1);
        if (Rec == 1) {
            Xraw.add(valuesAcceleration[0]);
            Yraw.add(valuesAcceleration[1]);
        }

    }

    private double distance(float[] arr){
        double dist = 0;
        if (dtw_flag == 0) {
            for (int i = 0; i < 20; i = i + 1) {
                dist = dist + Math.sqrt((arr[i] - X[i]) * (arr[i] - X[i]) + (arr[i + 20] - Y[i]) * (arr[i + 20] - Y[i]));
            }
        }
        if (dtw_flag == 1) {
            float[][] arr1 = new float[20][2];
            for (int i = 0; i < 20; i = i + 1) {

                arr1[i][0] = X[i];
                arr1[i][1] = Y[i];
            }
            float[][] arr2 = new float[20][2];
            for (int i = 0; i < 20; i = i + 1) {

                arr2[i][0] = arr[i];
                arr2[i][1] = arr[i+20];
            }
            DTW dtw = new DTW(arr1, arr2);
            dist = dtw.getDistance();
        }
        return dist;
    }


    private void interpolate(){
        int len = Xraw.size();
        Float[] arr1 = new Float[len];
        Xraw.toArray(arr1);
        Float[] arr2 = new Float[len];
        Yraw.toArray(arr2);
        double step = len/20.0;
        //String buf = len+" ";
        int count = 0;
        for (double i = 0; i < len-1; i = i + step) {
            int a = (int) (i + 0.5);
            //buf = buf + arr1[a].toString()+" ";
            if (count<20) {
                float m1 = 0;
                float m2 = 0;
                if (i==0) {
                    m1 = (arr1[a] + arr1[a + 1]) / 2.0f;
                    m2 = (arr2[a] + arr2[a + 1]) / 2.0f;
                }
                if (i == len - 2) {
                    m1 = (arr1[a - 1] + arr1[a]) / 2.0f;
                    m2 = (arr2[a - 1] + arr2[a]) / 2.0f;
                }
                if ((i < len - 2) && (i>0)) {
                    m1 = (arr1[a-1]+arr1[a]+arr1[a+1])/3.0f;
                    m2 = (arr2[a-1]+arr2[a]+arr2[a+1])/3.0f;
                }

                X[count] = m1;
                Y[count] = m2;
            }
            count = count + 1;
        }

        //letText.setText(buf+count);
    }

    private String knn(){
        float []array = new float[800];
        for (int i=0; i<800; i=i+1) {
            double d = distance(dataset[i]);
            array[i] = (float)d;
        }
        TreeMap<Float, List<Integer>> map = new TreeMap<Float, List<Integer>>();
        for(int i = 0; i < array.length; i++) {
            List<Integer> ind = map.get(array[i]);
            if(ind == null){
                ind = new ArrayList<Integer>();
                map.put(array[i], ind);
            }
            ind.add(i);
        }

        List<Integer> indices = new ArrayList<Integer>();
        for(List<Integer> arr : map.values()) {
            indices.addAll(arr);
        }
        String fi = labels[indices.get(0)];
        String si = labels[indices.get(1)];
        String ti = labels[indices.get(2)];
        if (si == ti) {
            return si;
        }
        return fi;
    }
    public int getPopularElement(int[] a)
    {
        int count = 1, tempCount;
        int popular = a[0];
        int temp = 0;
        for (int i = 0; i < (a.length - 1); i++)
        {
            temp = a[i];
            tempCount = 0;
            for (int j = 1; j < a.length; j++)
            {
                if (temp == a[j])
                    tempCount++;
            }
            if (tempCount > count)
            {
                popular = temp;
                count = tempCount;
            }
        }
        return popular;
    }

    private String svm(float[][] a, int[] start, int[] end, int[] sv, float[] b, String[] cs, double gamma) {
        String buf = "";
        float[] k = new float[800];
        for (int i = 0; i < 800; i = i + 1) {
            double d = -gamma*distance(dataset[i]);
            k[i] = (float) Math.exp(d);
            //buf = buf+distance(dataset[i])+" ";
        }
        //k = dtw_expgram[100];
        int[] decision = new int[325];
        int count = 0;
        for (int i = 0; i < 26; i = i + 1) {
            for (int j = i+1; j < 26; j = j + 1) {
                float sum = 0;
                for (int p = start[j]; p < end[j]; p = p + 1) {
                    sum = sum + a[i][p]*k[sv[p]];
                }
                for (int p = start[i]; p < end[i]; p = p + 1) {
                    sum = sum + a[j-1][p]*k[sv[p]];
                }
                if ((sum + b[count])> 0) {
                    decision[count] = i;
                }
                if ((sum + b[count])<= 0) {
                    decision[count] = j;
                }
                count = count +1;
            }
        }
       return cs[getPopularElement(decision)];
        //return dtw_cs[findPopular(decision)];

    }


    public void onClickSVM(View v){
        svm_flag = 1;
        String buf = "";
        if (svm_flag == 1) {
            buf = buf + "SVM with ";
        }
        if (svm_flag == 0) {
            buf = buf + "1NN with ";
        }
        if (dtw_flag == 1) {
            buf = buf + "DTW distance";
        }
        if (dtw_flag == 0) {
            buf = buf + "Euclidean distance";
        }
        statusText.setText(buf);
    }

    public void onClickNN(View v){
        svm_flag = 0;
        String buf = "";
        if (svm_flag == 1) {
            buf = buf + "SVM with ";
        }
        if (svm_flag == 0) {
            buf = buf + "1NN with ";
        }
        if (dtw_flag == 1) {
            buf = buf + "DTW distance";
        }
        if (dtw_flag == 0) {
            buf = buf + "Euclidean distance";
        }
        statusText.setText(buf);
    }

    public void onClickEUC(View v){
        dtw_flag = 0;
        String buf = "";
        if (svm_flag == 1) {
            buf = buf + "SVM with ";
        }
        if (svm_flag == 0) {
            buf = buf + "1NN with ";
        }
        if (dtw_flag == 1) {
            buf = buf + "DTW distance";
        }
        if (dtw_flag == 0) {
            buf = buf + "Euclidean distance";
        }
        statusText.setText(buf);
    }

    public void onClickDTW(View v){
        dtw_flag = 1;
        String buf = "";
        if (svm_flag == 1) {
            buf = buf + "SVM with ";
        }
        if (svm_flag == 0) {
            buf = buf + "1NN with ";
        }
        if (dtw_flag == 1) {
            buf = buf + "DTW distance";
        }
        if (dtw_flag == 0) {
            buf = buf + "Euclidean distance";
        }
        statusText.setText(buf);
    }

    public void onClickStartRecord(View v){
        Rec = 1;
    }


    public void onClickStopRecord(View v){
        Rec = 0;
        interpolate();
        Xraw.clear();
        Yraw.clear();
        if (svm_flag == 0) {
            letText.setText(knn());
        }
        if (svm_flag == 1) {
            if (dtw_flag == 0) {
                letText.setText(svm(euc_a,euc_start,euc_end,euc_sv,euc_b,euc_cs,0.05));
            }
            if (dtw_flag == 1) {
                letText.setText(svm(dtw_a,dtw_start,dtw_end,dtw_sv,dtw_b,dtw_cs,0.067));
            }
        }
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

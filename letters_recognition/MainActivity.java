package com.example.ruslan.letterrecognizer;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    int Rec = 0;

    SensorManager sensorManager;
    Sensor sensorAcceleration;

    Timer timer;


    float[] valuesAcceleration = new float[3];
    float[] gravity = new float[3];
    Float[] X = new Float[20];
    Float[] Y = new Float[20];
    Float[] Z = new Float[20];
    List<Float> Xraw = new ArrayList<Float>();
    List<Float> Yraw = new ArrayList<Float>();
    List<Float> Zraw = new ArrayList<Float>();
    EditText mEdit;
    TextView letText;
    TextView next_lettersText;
    TextView prob_lettersText;
    TextView wordText;
    final String LOG_TAG = "myLogs";
    final String DataFile = "mnt/sdcard-ext/111/dataset1.csv";
    float [][] dataset = new float[3625][60];//1742
    String [] labels = new String [3625];
    ArrayList<float[]> pred_windows = new ArrayList<float[]>();
    String[] alphabet = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    //float [][] dtw_expgram = new float[800][800];
    String word = "";
    float [][] W_i = new float[16][64];
    float [][] W_f = new float[16][64];
    float [][] W_c = new float[16][64];
    float [][] W_o = new float[16][64];
    float [][] U_i = new float[64][64];
    float [][] U_f = new float[64][64];
    float [][] U_c = new float[64][64];
    float [][] U_o = new float[64][64];
    float [] b_i = new float[64];
    float [] b_f = new float[64];
    float [] b_c = new float[64];
    float [] b_o = new float[64];
    float [][] D_a = new float[64][26];
    float [] D_b = new float[26];

    String[] words = new String[97565];
    Long[] freqs = new Long[97565];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readFileSD();
        readFileSD_lstm();
        readWords();
        Log.v("words", ""+freqs[0]+freqs[1]);
        setContentView(R.layout.activity_main);

        letText = (TextView) findViewById(R.id.letter);
        next_lettersText = (TextView) findViewById(R.id.next_letters);
        prob_lettersText = (TextView) findViewById(R.id.prob_letters);
        wordText = (TextView) findViewById(R.id.word);
        //statusText = (TextView) findViewById(R.id.status);
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
                int n = obs.length / 3;
                Float [] Xtrain = new Float[n];
                Float [] Ytrain = new Float[n];
                Float [] Ztrain = new Float[n];
                for (int i = 0; i < n; i = i + 1) {
                    Xtrain[i] = Float.parseFloat(obs[i]);
                    Ytrain[i] = Float.parseFloat(obs[i+n]);
                    Ztrain[i] = Float.parseFloat(obs[i+2*n]);
                }

                for (int i = 0; i < 20; i = i + 1) {
                    dataset[count][i] = interpolate(Xtrain)[i];
                    dataset[count][i+20] = interpolate(Ytrain)[i];
                    dataset[count][i+40] = interpolate(Ztrain)[i];
                }
                //System.out.format());
                //System.out.format("%d%n", count);
                //Log.v("obs", obs[obs.length-1]);
                labels[count] = obs[obs.length-1];
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

    private void readFileSD_lstm() {
        // проверяем доступность SD
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/W_i.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);

                for (int i = 0; i < obs.length; i = i + 1) {
                    W_i[count][i] = Float.parseFloat(obs[i]);
                    //Log.v("obs", W_i[count][i]+"");
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/W_f.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    W_f[count][i] = Float.parseFloat(obs[i]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/W_c.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    W_c[count][i] = Float.parseFloat(obs[i]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/W_o.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    W_o[count][i] = Float.parseFloat(obs[i]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/U_i.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    U_i[count][i] = Float.parseFloat(obs[i]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/U_f.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    U_f[count][i] = Float.parseFloat(obs[i]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/U_c.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    U_c[count][i] = Float.parseFloat(obs[i]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/U_o.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    U_o[count][i] = Float.parseFloat(obs[i]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/b_i.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                b_i[count] = Float.parseFloat(obs[0]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/b_f.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                b_f[count] = Float.parseFloat(obs[0]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/b_c.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                b_c[count] = Float.parseFloat(obs[0]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/b_o.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                b_o[count] = Float.parseFloat(obs[0]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/D_a.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                for (int i = 0; i < obs.length; i = i + 1) {
                    D_a[count][i] = Float.parseFloat(obs[i]);
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
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/D_b.csv"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                D_b[count] = Float.parseFloat(obs[0]);
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

    private void readWords() {
        // проверяем доступность SD
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        // формируем объект File, который содержит путь к файлу
        try {
            br = new BufferedReader(new FileReader("mnt/sdcard-ext/111/100kwords"));
            int count = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] obs = line.split(cvsSplitBy);
                words[count] = obs[0];
                freqs[count] = Long.parseLong(obs[1]);
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
        //String line1 = "Acceleration: \n"+String.valueOf(valuesAcceleration[0])+" \n"+String.valueOf(valuesAcceleration[1])+ " \n"+
        //        String.valueOf(valuesAcceleration[2])+"\n";

        //tvText.setText(line1);
        if (Rec == 1) {
            Xraw.add(valuesAcceleration[0]);
            Yraw.add(valuesAcceleration[1]);
            Zraw.add(valuesAcceleration[2]);
        }

    }

    private double distance(float[] arr){
        double dist = 0;
        for (int i = 0; i < 20; i = i + 1) {
            dist = dist + ((arr[i] - X[i]) * (arr[i] - X[i]) +
                    (arr[i + 20] - Y[i]) * (arr[i + 20] - Y[i]) +
                    (arr[i + 40] - Z[i]) * (arr[i + 40] - Z[i]));
        }
        dist = Math.sqrt(dist);
        return dist;
    }

    private void windowing(Float[] arr1, Float[] arr2, Float[] arr3){
        for (int i = 0; i < arr1.length-20; i = i + 10) {
            X = Arrays.copyOfRange(arr1, i, i + 20);
            Y = Arrays.copyOfRange(arr2, i, i+20);
            Z = Arrays.copyOfRange(arr3, i, i+20);
            pred_windows.add(knn());
        }


    }

    private float[] sigmoid(float[] arr){
        float[] arr_sigmoid = new float[arr.length];
        for (int i=0; i<arr.length; i++){
            arr_sigmoid[i] = (float) (1 / (1 + Math.exp(-arr[i])));
        }
        return arr_sigmoid;
    }

    private float[] tanh(float[] arr){
        float[] arr_tanh = new float[arr.length];
        for (int i=0; i<arr.length; i++){
            arr_tanh[i] = (float) Math.tanh(arr[i]);
        }
        return arr_tanh;
    }

    private float[] softmax(float[] arr){
        float[] arr_softmax = new float[arr.length];
        float sum = 0;
        for (int i=0; i<arr.length; i++){
            arr_softmax[i] = (float) Math.exp(arr[i]);
            sum = sum + (float) Math.exp(arr[i]);
        }

        for (int i=0; i<arr.length; i++){
            arr_softmax[i] = arr_softmax[i]/sum;
        }
        return arr_softmax;
    }

    private float[] elemwise_multuply(float[] arr1, float[] arr2){
        float[] arr_multiply = new float[arr1.length];
        for (int i=0; i<arr1.length; i++){
            arr_multiply[i] = arr1[i]*arr2[i];
        }
        return arr_multiply;
    }

    private float[] elemwise_sum(float[] arr1, float[] arr2){
        float[] arr_sum = new float[arr1.length];
        for (int i=0; i<arr1.length; i++){
            arr_sum[i] = arr1[i]+arr2[i];
        }
        return arr_sum;
    }

    private float[] dot(float[][] arr1_to_transpose, float[] arr2){
        float[] arr_sum = new float[arr1_to_transpose[0].length];
        for (int i=0; i<arr1_to_transpose[0].length; i++) {
            float sum = 0;
            for (int j=0; j<arr1_to_transpose.length; j++){
                sum = sum + arr1_to_transpose[j][i]*arr2[j];
            }
            arr_sum[i] = sum;
        }
        return arr_sum;
    }

    private float[] logp1(Long[] arr){
        float[] arr_logp1 = new float[arr.length];
        for (int i=0; i<arr.length; i++){
            arr_logp1[i] = (float) Math.log(arr[i]+1);
        }
        return arr_logp1;
    }

    private float[] normalize(float[] arr){
        float[] arr_normalize = new float[arr.length];
        float sum = 0;
        for (int i=0; i<arr.length; i++){
            sum = sum + arr[i];
        }

        for (int i=0; i<arr.length; i++){
            arr_normalize[i] = arr[i]/sum;
        }
        return arr_normalize;
    }

    private String join(float[] arr) {
        String buf = "";

        for (int i=0; i<arr.length; i++) {
            buf = buf + arr[i] + " ";
        }
        return buf;
    }


    private int[] maxKIndex(float[] array, int top_k) {
        double[] max = new double[top_k];
        int[] maxIndex = new int[top_k];
        Arrays.fill(max, Double.NEGATIVE_INFINITY);
        Arrays.fill(maxIndex, -1);

        top: for(int i = 0; i < array.length; i++) {
            for(int j = 0; j < top_k; j++) {
                if(array[i] > max[j]) {
                    for(int x = top_k - 1; x > j; x--) {
                        maxIndex[x] = maxIndex[x-1]; max[x] = max[x-1];
                    }
                    maxIndex[j] = i; max[j] = array[i];
                    continue top;
                }
            }
        }
        return maxIndex;
    }

    private float[] next_letter_probs() {
        Long[] alpha_count = new Long[26];
        for (int i=0; i<alpha_count.length; i++) {
            alpha_count[i] = (long) 0;
        }
        float[] alpha_count1 = new float[26];
        for (int i=0; i<words.length; i++) {
            String word_part = "";
            for(int j = 0; j< word.length(); j++) {
                if (j<words[i].length()){
                    word_part += words[i].charAt(j);
                }
            }
            if (word_part.equals(word)) {
                if (words[i].length()>word.length())  {
                    char next_char = words[i].charAt(word.length());
                    int ind = 0;
                    for (int k=0; k<alphabet.length; k++) {
                        if (next_char == alphabet[k].charAt(0)) {
                            ind = k;
                        }
                    }
                    alpha_count[ind] += freqs[i];

                }
            }

        }
        alpha_count1 = logp1(alpha_count);
        return normalize(alpha_count1);
    }

    private float[] corrected_prediction(float[] pred) {
        //Log.v("pred", join(pred));
        //Log.v("freq", join(next_letter_probs()));
        pred = elemwise_multuply(pred, next_letter_probs());
        return normalize(pred);
    }

    private float[] predict(){
        float[] c_prev = new float[64];
        float[] h_prev = new float[64];
        for(int i=0;i<c_prev.length;i++) {
            c_prev[i] = 0;
            h_prev[i] = 0;
        }
        for (int k = 0; k < pred_windows.size(); k ++) {
            float[] xt = pred_windows.get(k);
            float[] i = sigmoid(elemwise_sum(elemwise_sum(dot(W_i, xt),dot(U_i, h_prev)),b_i));
            float[] c_ =tanh(elemwise_sum(elemwise_sum(dot(W_c, xt),dot(U_c, h_prev)),b_c));
            float[] f = sigmoid(elemwise_sum(elemwise_sum(dot(W_f, xt),dot(U_f, h_prev)),b_f));
            float[] c = elemwise_sum(elemwise_multuply(i,c_),elemwise_multuply(f,c_prev));
            float[] o = sigmoid(elemwise_sum(elemwise_sum(dot(W_o, xt),dot(U_o, h_prev)),b_o));
            h_prev = elemwise_multuply(o,tanh(c));
            c_prev = c;

        }

        //Log.v("h_prev", join(h_prev));

        float[] letter = softmax(elemwise_sum(dot(D_a, h_prev), D_b));



        String prob_letters = "";
        int[] next_ind = maxKIndex(letter, 3);
        for (int i=0; i<next_ind.length; i++) {
            if (next_ind[i]<0) {
                prob_letters = ".";
                break;
            }
            prob_letters += alphabet[next_ind[i]] + " ";
        }
        prob_lettersText.setText(prob_letters);

        Log.v("letter", join(letter));
        if (word.length()>0) {
            letter = corrected_prediction(letter);
        }
    return letter;
    }




    private float[] interpolate(Float[] arr1){

        float[] output = new float[20];
        int len = arr1.length;
        double step = len/20.0;
        //String buf = len+" ";
        int count = 0;
        for (double i = 0; i < len-1; i = i + step) {
            int a = (int) (i + 0.5);
            //buf = buf + arr1[a].toString()+" ";
            if (count<20) {
                float m1 = 0;
                if (a==0) {
                    m1 = (arr1[a] + arr1[a + 1]) / 2.0f;
                }
                if (a == len - 2) {
                    m1 = (arr1[a - 1] + arr1[a]) / 2.0f;
                }
                if ((a < len - 2) && (a>0)) {
                    m1 = (arr1[a-1]+arr1[a]+arr1[a+1])/3.0f;

                }

                output[count] = m1;

            }
            count = count + 1;
        }
        return output;
        //letText.setText(buf+count);
    }

    private float[] knn(){
        float []array = new float[dataset.length];
        for (int i=0; i<dataset.length; i=i+1) {
            double d = distance(dataset[i]);
            array[i] = (float) d;
            //Log.v("dist "+i, d+"");
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
        float [] proba = new float[16];
        for(int i = 0; i < 10; i++) {
            proba[Integer.parseInt(labels[indices.get(i)])-1] ++;
        }

        float sum = 0;
        for( float i : proba) {
            sum += i;
        }
        for (int i=0; i<proba.length; i=i+1) {
            proba[i]=proba[i]/sum;
        }

        String buf="";
        for (int i=0; i<proba.length; i=i+1) {
            buf = buf + proba[i] + ", ";
        }
        Log.v("proba", buf);
        Log.v("top", indices.get(0)+" "+indices.get(1)+" "+indices.get(2));
        return proba;
    }


    public void onClickWord(View v){
        word = "";
        wordText.setText(word);
    }

    public void onClickoLetter(View v){
        String word_part = "";
        if (word.length()>0) {
            for (int j = 0; j < word.length() - 1; j++) {
                char c = word.charAt(j);
                word_part += c;
            }
        }
        word = word_part;
        wordText.setText(word);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            if (Rec==0) {
                Rec = 1;
            } else {
                Rec = 0;
                int len = Xraw.size();
                Float[] arr1 = new Float[len];
                Xraw.toArray(arr1);
                Float[] arr2 = new Float[len];
                Yraw.toArray(arr2);
                Float[] arr3 = new Float[len];
                Zraw.toArray(arr3);
                Xraw.clear();
                Yraw.clear();
                Zraw.clear();
                String buf = "";

                for (int i=0; i<arr1.length; i=i+1) {
                    buf = buf + arr1[i] + ", ";
                }
                for (int i=0; i<arr2.length; i=i+1) {
                    buf = buf + arr2[i] + ", ";
                }
                for (int i=0; i<arr3.length; i=i+1) {
                    buf = buf + arr3[i] + ", ";
                }


                windowing(arr1, arr2, arr3);
                float[] pred = predict();
                int maxIndex = 0;
                float max = -1000;
                for (int i = 0; i < pred.length; i++) {
                    if (pred[i] > max) {
                        max = pred[i];
                        maxIndex = i;
                    }
                }
                word = word + alphabet[maxIndex];
                letText.setText(alphabet[maxIndex]);
                wordText.setText(word);
                String next_letters = "";
                int[] next_ind = maxKIndex(next_letter_probs(), 3);
                for (int i=0; i<next_ind.length; i++) {
                    if (next_ind[i]<0) {
                        next_letters = ".";
                        break;
                    }
                    next_letters += alphabet[next_ind[i]] + " ";
                }
                next_lettersText.setText(next_letters);
                Log.v("current",buf);
                Log.v("max",maxIndex+"");
                //letText.setText(buf);
                pred_windows.clear();
            }
        }
        return true;
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
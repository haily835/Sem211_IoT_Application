package com.example.helloworld;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/*
Fix error mqtt Load... library:
In /gradle.properties add: android.enableJetifier=true
In build.gradle add:
    // paho mqtt library
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0'
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'

Http request library using
implementation 'com.android.volley:volley:1.2.1'

How to create chart: https://viblo.asia/p/mpandroidchart-and-example-Az45badzlxY

Q.Hai
* */
public class MainActivity extends AppCompatActivity {

    // declare a pointer to MQTT helper class
    MQTTHelper mqttHelper;

    // link to TextView in res/layout/activity_main.xml
    Dictionary<String, TextView> UIElements = new Hashtable<String, TextView>();
    ToggleButton ledToggleButton;
    LineChart chart;

    Dictionary<String, String> weather = new Hashtable<String, String>();
    // Temperature data
    List<Entry> entries = new ArrayList<Entry>();
    String ledData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // binding
        ledToggleButton = findViewById(R.id.ledToggleButton);
        UIElements.put("bbc-hum", findViewById(R.id.humText));
        UIElements.put("bbc-switch", findViewById(R.id.switchText));
        UIElements.put("bbc-temp", findViewById(R.id.tempText));
        UIElements.put("bbc-weather", findViewById(R.id.weather));


        // get last_value from api
        fetchLastData("bbc-led");
        fetchLastData("bbc-hum");
        fetchLastData("bbc-switch");
        fetchLastData("bbc-temp");
        fetchLastData("bbc-weather");

        chart = (LineChart) findViewById(R.id.chart);

        ledToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                ledData = b ? "1" : "0";

                waitingPeriod = 3;

                // Go to the feed on Adafruit, find MQTT by key and paste in
                SpinKitView ledLoadingSpin = findViewById(R.id.ledLoadingSpin);
                ledLoadingSpin.setVisibility(View.VISIBLE);
                Log.d("mqtt", "Sending to haily835/feeds/bbc-led" + ledData);
                sendDataMQTT("haily835/feeds/bbc-led", ledData);
            }
        });


        setupScheduler();
        startMQTT();
    }

    // error to implement the Stop and Wait using the java timer
    private void setupScheduler() {
        Timer timer = new Timer();

        TimerTask scheduler = new TimerTask() {
            @Override
            public void run() {
                Log.d("mqtt", "Timer is ticking...");
                if (waitingPeriod > 0) {
                    waitingPeriod--;
                    if (waitingPeriod == 0) {
                        Log.d("mqtt", "Scheduler resend message");
                        sendDataMQTT("haily835/feeds/bbc-led", ledData);
                    }
                }
            }
        };

        // after 5s, wait 1s
        timer.schedule(scheduler, 5000,1000);
    }

    int waitingPeriod = 0;

    private void startMQTT() {

        // create unique id for different client; if use same ID may cause error
        UUID uuid = UUID.randomUUID();
        String uuidAsString = uuid. toString();

        // task assign a new ID by random otherwise if we install on other app we have to reassigned
        mqttHelper = new MQTTHelper(getApplicationContext(), uuidAsString);

        // ctrl space code suggestion
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("mqtt", "Connection is successful");
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("mqtt", "Received: " + message.toString() + " from " + topic);

                if (topic.equals("haily835/f/bbc-temp")) {
                    UIElements.get("bbc-temp").setText(message.toString());
                    updateChart(message.toString());
                } else if (topic.equals("haily835/f/bbc-led")) {
                    String data = message.toString();


                    if (waitingPeriod == 0) {
                        ledToggleButton.setChecked(data.equals("0") ? false : true);
                    }

                    // new feature
                    if (data.equals(ledData)) {
                        waitingPeriod = 0;
                        Log.d("mqtt", "Message is sent succesfully");
                        SpinKitView ledLoadingSpin = findViewById(R.id.ledLoadingSpin);
                        ledLoadingSpin.setVisibility(View.INVISIBLE);
                        ledToggleButton.setChecked(data.equals("0") ? false : true);
                    }
                } else if (topic.equals("haily835/f/bbc-hum")) {
                    UIElements.get("bbc-hum").setText(message.toString());
                } else if (topic.equals("haily835/f/bbc-switch")) {
                    UIElements.get("bbc-switch").setText(message.toString());
                } else if (topic.equals("haily835/f/bbc-weather")) {
                    updateWeatherText(message.toString());
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    private void sendDataMQTT(String topic, String value){
        MqttMessage msg = new MqttMessage();
        msg.setId(1234); // ko qtrong
        msg.setQos(0); // set cang cao thi thoi gian gui dai hon, nhung neu qos cang lon thi cang chinh xac
        // qos = 4 -> http luon khong phai tcp nua
        msg.setRetained(true); // moi khi mot client subscribe thi tu dong gui lastest_value cho. Adafruit ko ho tro viec nay

        byte[] b = value.getBytes(Charset.forName("UTF-8")); // chuyen het thanh byte
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        }catch (Exception e){}

    }

    private void fetchLastData(String topic) {
        // Instantiate the RequestQueue.
        String url = "https://io.adafruit.com/api/v2/haily835/feeds/" + topic;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            String lastValue = json.getString("last_value");

                            switch(topic) {
                                case "bbc-led":
                                    // ---------led toggle button ------------------
                                    // initial state of toggle button from the last value of LED
                                    if (lastValue.equals("0")) {
                                        ledToggleButton.setChecked(false);
                                    } else {
                                        ledToggleButton.setChecked(true);
                                    }
                                    break;
                                case "bbc-temp":
                                    UIElements.get("bbc-temp").setText(lastValue);
                                    updateChart(lastValue);
                                    break;
                                case "bbc-weather":
                                    updateWeatherText(lastValue);
                                    break;
                                default:
                                    UIElements.get(topic).setText(lastValue);
                                    break;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        };
                    }
                }, error -> error.printStackTrace());

        queue.add(stringRequest);
    }

    private void updateChart(String newPoint) {
        int nextPoint = entries.size();

        if (nextPoint > 10) {
            entries.remove(0);
        }

        entries.add(new Entry(nextPoint, Float.parseFloat(newPoint)));
        LineDataSet dataSet = new LineDataSet(entries, "Temp");

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // refresh
    }

    private void updateWeatherText(String data) {
        Dictionary<String, String> retMap = new Gson().fromJson(
                data, new TypeToken<Hashtable<String, String>>() {}.getType()
        );
        String txt = "Temperature: " + retMap.get("temp") + "\n";
        txt += "Status: " + retMap.get("status") + "\n";
        txt += retMap.get("location") + "\n";

        UIElements.get("bbc-weather").setText(txt);
    }

}
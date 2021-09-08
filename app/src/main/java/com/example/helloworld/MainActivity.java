package com.example.helloworld;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Dictionary;
import java.util.Hashtable;

/*
Fix error mqtt Load... library:
In /gradle.properties add: android.enableJetifier=true
In build.gradle add:
    // paho mqtt library
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0'
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'

Http call using
implementation 'com.android.volley:volley:1.2.1'

Q.Hai
* */
public class MainActivity extends AppCompatActivity {

    // declare a pointer to MQTT helper class
    MQTTHelper mqttHelper;

    // link to TextView in res/layout/activity_main.xml
    Dictionary<String, TextView> UIElements = new Hashtable<String, TextView>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        UIElements.put("bbc-led", findViewById(R.id.ledText));
        UIElements.put("bbc-hum", findViewById(R.id.humText));
        UIElements.put("bbc-switch", findViewById(R.id.switchText));
        UIElements.put("bbc-temp", findViewById(R.id.tempText));

        // get last_value from api
        fetchLastData("bbc-led");
        fetchLastData("bbc-hum");
        fetchLastData("bbc-switch");
        fetchLastData("bbc-temp");

        startMQTT();
    }

    private void startMQTT() {

        // task assign a new ID by random otherwise if we install on other app we have to reassigned
        mqttHelper = new MQTTHelper(getApplicationContext(), "123456789");

        // ctrl space -> auto added for you
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
                } else if (topic.equals("haily835/f/bbc-led")) {
                    UIElements.get("bbc-led").setText(message.toString());
                } else if (topic.equals("haily835/f/bbc-hum")) {
                    UIElements.get("bbc-hum").setText(message.toString());
                } else if (topic.equals("haily835/f/bbc-switch")) {
                    UIElements.get("bbc-switch").setText(message.toString());
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
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
                            UIElements.get(topic).setText(lastValue);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        };
                    }
                }, error -> error.printStackTrace());

        queue.add(stringRequest);
    }
}
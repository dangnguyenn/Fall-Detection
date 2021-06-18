package com.example.wear;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;


public class DataCollectionService extends Service implements SensorEventListener, MessageClient.OnMessageReceivedListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] values;
    JSONArray sensorData = new JSONArray();
    private String selectedActivity;
    DataClient dataClient;
    JSONObject obj;
    private Node mNode;
    int count = 0;

    public void connect() {
        Task<List<Node>> nodeListTask =  Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Node> nodeList = Tasks.await(nodeListTask);
                    for(Node node: nodeList){
                        if (node.isNearby()) {
                            mNode = node;
                            break;
                        }
                        mNode = node;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Wearable.getMessageClient(this).addListener(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("DataCollectionService :", "onStartCommand()");
        connect();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "ForegroundServiceChannel",
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "ForegroundServiceChannel")
                .setContentTitle("Foreground Service")
                .setContentText(selectedActivity)
                .setSmallIcon(R.drawable.ic_cc_checkmark)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        Log.d("DataCollectionService :", "onDestroy()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("DataCollectionService :", "onBind()");
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        values = event.values;

        try {
            obj = new JSONObject();
            obj.put("x", values[0] / -9.8);
            obj.put("y", values[1] / -9.8);
            obj.put("z", values[2] / -9.8);
            obj.put("timestamp", new Timestamp(System.currentTimeMillis()));

            Log.d("Message Created: ", obj.toString());
            count++;
            Log.d("Count: ", "" + count);

            Wearable.getMessageClient(getApplicationContext()).sendMessage(mNode.getId(), "/fall_data", obj.toString().getBytes());


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("DataCollectionService :", "onAccuracyChanged()");
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        try {
            JSONObject jsonObject = new JSONObject(new String(messageEvent.getData()));
            Log.d("Message from phone", "onMessageReceived: "+ jsonObject.toString());
            String message = jsonObject.getString("message");

            if(message.equals("start")) {
                Log.d("start sensor", String.valueOf(SensorManager.SENSOR_DELAY_GAME));
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
            else if(message.equals("fall")) {
                sensorManager.unregisterListener(this);
            }

        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }

    }
}

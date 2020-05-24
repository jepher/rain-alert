package com.gmail.jygtx.rainalert;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;

public class WebService extends IntentService {
    static WeatherApp weatherApp = new WeatherApp();

    public WebService() {
        super("WebService");
        setIntentRedelivery(false);
    }

    @Override
    public void onCreate() {
        Notification notification = new NotificationCompat.Builder(this, NotificationService.ALERT_CHANNEL_ID)
                .setContentTitle("Web Service")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.ic_cloud_black)
                .build();

        startForeground(1, notification);

        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Bundle extras = intent.getExtras();
        String request = extras.getString("Request");

        try {
            // determine which request is being made
            if ("Location".equals(request)) {
                String zipCode = extras.getString("Zip Code");
                handleLocationRequest(zipCode);
            } else if ("Data".equals(request)) {
                String locationKey = extras.getString("Location Key");
                handleDataRequest(locationKey);
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void handleLocationRequest(String zipCode) throws IOException, JSONException {
        Bundle extras = new Bundle();

        extras.putInt("Response Code", weatherApp.getLocationKey(zipCode));
        extras.putString("Location", weatherApp.getLocation());
        extras.putString("Location Key", weatherApp.getLocationKey());

        Intent updateIntent = new Intent("com.gmail.jygtx.rainalert.UPDATELOCATION");
        updateIntent.putExtras(extras);
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);
    }

    private void handleDataRequest(String locationKey) throws IOException, JSONException {
        Bundle extras = new Bundle();
        Intent sendDataIntent = new Intent("com.gmail.jygtx.rainalert.GETDATA");

        extras.putInt("Response Code", weatherApp.getWeather(locationKey));
        extras.putParcelableArray("Data", weatherApp.getData());

        sendDataIntent.putExtras(extras);

        LocalBroadcastManager.getInstance(this).sendBroadcast(sendDataIntent);
    }
}

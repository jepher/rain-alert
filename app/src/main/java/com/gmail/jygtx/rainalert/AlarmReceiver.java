package com.gmail.jygtx.rainalert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent alarmIntent = new Intent("com.gmail.jygtx.rainalert.ALARMRECEIVED");
        LocalBroadcastManager.getInstance(context).sendBroadcast(alarmIntent);
        System.out.println("Alarm received");
    }
}

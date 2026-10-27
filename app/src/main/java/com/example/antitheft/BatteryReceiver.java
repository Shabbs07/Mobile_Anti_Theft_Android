package com.example.antitheft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;

public class BatteryReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) return;
        SharedPreferences prefs = context.getSharedPreferences("anti_theft", Context.MODE_PRIVATE);
        String emergency = prefs.getString("emergency", "");
        if (emergency.matches("\\+?[0-9]{8,15}") &&
            context.checkSelfPermission(android.Manifest.permission.SEND_SMS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager.getDefault().sendTextMessage(emergency, null,
                    "Anti-Theft alert: Android reported that the device battery is low.", null, null);
            } catch (Exception ignored) { }
        }
    }
}

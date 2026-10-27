package com.example.antitheft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.telephony.SmsManager;

public class SimChangeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED.equals(intent.getAction()) &&
            !TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED.equals(intent.getAction()) &&
            !"android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("anti_theft", Context.MODE_PRIVATE);
        String emergency = prefs.getString("emergency", "");
        if (emergency.matches("\\+?[0-9]{8,15}") &&
            context.checkSelfPermission(android.Manifest.permission.SEND_SMS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager.getDefault().sendTextMessage(emergency, null,
                    "Anti-Theft notice: Android reported a SIM state change. Verify your device if unexpected.", null, null);
            } catch (Exception ignored) { }
        }
    }
}

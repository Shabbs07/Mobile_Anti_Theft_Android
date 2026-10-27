package com.example.antitheft;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_LOC = 10;
    private static final int REQ_SMS = 11;
    private static final int REQ_PHONE = 12;
    private EditText trustedPhone, emergencyPhone, pinInput;
    private TextView status;
    private SharedPreferences prefs;
    private ToneGenerator tone;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int blue = 0xff1769aa;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("anti_theft", MODE_PRIVATE);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(26, 30, 26, 28);
        page.setBackgroundColor(0xfff5f8fc);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("Mobile Anti-Theft Plus");
        title.setTextSize(27);
        title.setTextColor(blue);
        title.setGravity(Gravity.CENTER);
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Android security learning project • Java");
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(0xff555555);
        subtitle.setPadding(0, 6, 0, 18);
        page.addView(subtitle);

        addLabel(page, "Trusted number (include country code)");
        trustedPhone = addInput(page, "+911234567890", 3);
        addLabel(page, "Emergency contact number");
        emergencyPhone = addInput(page, "+911234567890", 3);
        addLabel(page, "App demo PIN (4–8 digits)");
        pinInput = addInput(page, "Enter a new PIN", 2);
        pinInput.setHint("Set a demo PIN");

        addButton(page, "Save settings", v -> saveSettings());
        addButton(page, "Check last known location", v -> checkLocation());
        addButton(page, "Send location to trusted number", v -> sendLocation());
        addButton(page, "Start siren alarm", v -> startSiren());
        addButton(page, "Stop siren", v -> stopSiren());
        addButton(page, "Test wrong-PIN alert", v -> testWrongPin());
        addButton(page, "Check battery level", v -> checkBattery());
        addButton(page, "Check SIM status", v -> checkSimStatus());

        TextView disclaimer = new TextView(this);
        disclaimer.setText("\nImportant limitations\n• Remote location is a user-triggered SMS request/response demo, not continuous covert tracking.\n• SIM-change broadcasts vary by device and do not reliably identify a changed SIM on every Android version.\n• Wrong-PIN monitoring applies only to this app's demo PIN screen; Android does not let ordinary apps observe system lock-screen PIN attempts.\n• Battery-low monitoring depends on Android's system broadcast and background limits.\n• SMS requires permission, a supported SIM, service, and may incur charges.\n• Test only with your own device and contacts. No stealth, bypass, or remote wipe is implemented.");
        disclaimer.setTextSize(13);
        disclaimer.setTextColor(0xff444444);
        disclaimer.setPadding(0, 18, 0, 12);
        page.addView(disclaimer);

        status = new TextView(this);
        status.setText("Status: Ready");
        status.setTextColor(blue);
        status.setTextSize(15);
        status.setPadding(0, 14, 0, 12);
        page.addView(status);
        setContentView(scroll);

        trustedPhone.setText(prefs.getString("trusted", ""));
        emergencyPhone.setText(prefs.getString("emergency", ""));
        pinInput.setText(prefs.getString("pin", ""));
    }

    private void addLabel(LinearLayout parent, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14);
        label.setTextColor(0xff222222);
        label.setPadding(0, 12, 0, 2);
        parent.addView(label);
    }

    private EditText addInput(LinearLayout parent, String hint, int type) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setInputType(type);
        parent.addView(input);
        return input;
    }

    private interface ClickAction { void run(View view); }
    private void addButton(LinearLayout parent, String text, ClickAction action) {
        Button button = new Button(this);
        button.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 7, 0, 0);
        parent.addView(button, lp);
        button.setOnClickListener(action::run);
    }

    private boolean validPhone(String value) {
        return value != null && value.trim().matches("\\+?[0-9]{8,15}");
    }

    private void saveSettings() {
        String trusted = trustedPhone.getText().toString().trim();
        String emergency = emergencyPhone.getText().toString().trim();
        String pin = pinInput.getText().toString().trim();
        if (!validPhone(trusted) || !validPhone(emergency)) {
            toast("Enter valid trusted and emergency phone numbers.");
            return;
        }
        if (!pin.matches("[0-9]{4,8}")) {
            toast("Set a PIN containing 4–8 digits.");
            return;
        }
        prefs.edit().putString("trusted", trusted).putString("emergency", emergency)
                .putString("pin", pin).apply();
        setStatus("Settings saved on this device.");
    }

    private boolean ensurePermission(String permission, int requestCode) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return true;
        requestPermissions(new String[]{permission}, requestCode);
        return false;
    }

    private void checkLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
            return;
        }
        try {
            LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
            Location loc = null;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) {
                setStatus("No cached location found. Enable Location and try again.");
            } else {
                setStatus(String.format(Locale.US, "Last known location: %.6f, %.6f",
                        loc.getLatitude(), loc.getLongitude()));
            }
        } catch (SecurityException e) {
            setStatus("Location permission denied.");
        } catch (Exception e) {
            setStatus("Could not read location. Check location settings.");
        }
    }

    private String getLocationText() {
        try {
            LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
            Location loc = null;
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (loc == null) return null;
            return String.format(Locale.US, "Location: https://maps.google.com/?q=%.6f,%.6f",
                    loc.getLatitude(), loc.getLongitude());
        } catch (Exception e) { return null; }
    }

    private void sendLocation() {
        String number = trustedPhone.getText().toString().trim();
        if (!validPhone(number)) { toast("Save a valid trusted number first."); return; }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
            return;
        }
        String location = getLocationText();
        if (location == null) { setStatus("No last known location. Enable location and try again."); return; }
        sendSms(number, "Mobile Anti-Theft location request (user-triggered): " + location);
    }

    private void sendSms(String number, String message) {
        if (!validPhone(number)) { setStatus("A valid contact number is required."); return; }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQ_SMS);
            return;
        }
        try {
            android.telephony.SmsManager.getDefault().sendTextMessage(number, null, message, null, null);
            setStatus("SMS submitted to carrier. Delivery is not guaranteed.");
        } catch (Exception e) {
            setStatus("SMS failed. Check SIM, service, number, and permission.");
        }
    }

    private void notifyEmergency(String message) {
        String emergency = prefs.getString("emergency", "");
        if (validPhone(emergency)) sendSms(emergency, "Anti-Theft alert: " + message);
        else setStatus("Alert generated, but emergency contact is not configured.");
    }

    private void startSiren() {
        stopSiren();
        try {
            tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 5000);
            setStatus("Siren started for 5 seconds. Tap Stop Siren to end early.");
            handler.postDelayed(this::stopSiren, 5000);
        } catch (Exception e) { setStatus("Siren could not start on this device."); }
    }

    private void stopSiren() {
        handler.removeCallbacksAndMessages(null);
        if (tone != null) {
            tone.stopTone();
            tone.release();
            tone = null;
        }
        if (status != null) setStatus("Siren stopped.");
    }

    private void testWrongPin() {
        String expected = prefs.getString("pin", "");
        if (expected.isEmpty()) { toast("Save settings and create a demo PIN first."); return; }
        final EditText entry = new EditText(this);
        entry.setHint("Enter demo PIN");
        entry.setInputType(2);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Demo PIN check")
            .setView(entry)
            .setPositiveButton("Verify", (dialog, which) -> {
                if (!expected.equals(entry.getText().toString())) {
                    int attempts = prefs.getInt("wrong_attempts", 0) + 1;
                    prefs.edit().putInt("wrong_attempts", attempts).apply();
                    setStatus("Wrong app PIN. Failed attempts: " + attempts);
                    notifyEmergency("Wrong PIN entered in the app demo (" + attempts + " failed attempt(s)).");
                } else {
                    prefs.edit().putInt("wrong_attempts", 0).apply();
                    setStatus("Correct app PIN. Failed-attempt counter reset.");
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void checkBattery() {
        android.content.IntentFilter filter = new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = registerReceiver(null, filter);
        if (battery == null) { setStatus("Battery state unavailable."); return; }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pct = (level >= 0 && scale > 0) ? (int)(level * 100f / scale) : -1;
        setStatus(pct >= 0 ? "Battery level: " + pct + "%" : "Battery level unavailable.");
        if (pct >= 0 && pct <= 15) notifyEmergency("Battery is low (" + pct + "%).");
    }

    private void checkSimStatus() {
        if (!ensurePermission(Manifest.permission.READ_PHONE_STATE, REQ_PHONE)) return;
        try {
            TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
            String state = tm.getSimState() == TelephonyManager.SIM_STATE_READY ? "SIM ready" :
                    "SIM state code: " + tm.getSimState();
            setStatus(state + ". Note: SIM-change detection support varies by device.");
        } catch (Exception e) { setStatus("SIM state unavailable on this device."); }
    }

    private void setStatus(String message) {
        if (status != null) status.setText("Status: " + message);
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (results.length == 0 || results[0] != PackageManager.PERMISSION_GRANTED) {
            setStatus("Permission denied. Relevant feature may not work.");
            return;
        }
        if (requestCode == REQ_LOC) checkLocation();
        else if (requestCode == REQ_PHONE) checkSimStatus();
        else if (requestCode == REQ_SMS) setStatus("SMS permission granted. Tap the desired alert action again.");
    }

    @Override protected void onDestroy() {
        stopSiren();
        super.onDestroy();
    }
}

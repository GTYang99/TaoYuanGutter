package com.example.taoyuangutter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

/** Uses only framework APIs so the selected test APK can publish a mock GPS fix in its own UID. */
public final class MockLocationProviderReceiver extends BroadcastReceiver {
    private static final String TAG = "DBG1007MockLocation";

    @Override
    public void onReceive(Context context, Intent intent) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String action = intent.getStringExtra("action");
        try {
            if ("disable".equals(action)) {
                try {
                    manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                } catch (RuntimeException ignored) {
                    // The provider may not have been added if setup failed earlier.
                }
                try {
                    manager.removeTestProvider(LocationManager.GPS_PROVIDER);
                } catch (RuntimeException ignored) {
                    // No provider to remove.
                }
            } else {
                manager.addTestProvider(
                        LocationManager.GPS_PROVIDER,
                        false,
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        Criteria.POWER_LOW,
                        Criteria.ACCURACY_FINE);
                manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
                if (intent.hasExtra("lat") && intent.hasExtra("lng")) {
                    Location mock = new Location(LocationManager.GPS_PROVIDER);
                    mock.setLatitude(intent.getFloatExtra("lat", 0f));
                    mock.setLongitude(intent.getFloatExtra("lng", 0f));
                    mock.setAccuracy(5f);
                    mock.setTime(System.currentTimeMillis());
                    mock.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mock);
                }
            }
            Log.i(TAG, "Mock provider action completed: " + action);
            setResultCode(0);
        } catch (RuntimeException failure) {
            Log.e(TAG, "Mock provider action failed: " + action, failure);
            setResultCode(1);
        }
    }
}

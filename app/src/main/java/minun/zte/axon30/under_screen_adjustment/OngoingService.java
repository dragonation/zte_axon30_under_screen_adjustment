package minun.zte.axon30.under_screen_adjustment;

import android.app.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.res.Configuration;

import android.database.ContentObserver;

import android.hardware.SensorManager;

import android.hardware.display.DisplayManager;

import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import android.provider.Settings;

import android.util.Log;

import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class OngoingService extends Service {

    private static final int ONGOING_ID = 110;

    public enum DisplayOrientation {
        PORTRAIT,
        PORTRAIT_UPSIDE_DOWN,
        LANDSCAPE_UPSIDE_LEFT,
        LANDSCAPE_UPSIDE_RIGHT
    };

    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            float temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -2550) / 10f;
            OngoingService.this.batteryTemperature = temperature;
            OngoingService.this.autorefreshAdjustment();
        }
    }

    private class BrightnessObserver extends ContentObserver {
        public BrightnessObserver() {
            super(new Handler(Looper.getMainLooper()));
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            OngoingService.this.autorefreshAdjustment();
        }
    }

    private static OngoingService ongoingService;

    private DisplayManager displayManager;

    private DisplayOrientation displayOrientation;
    private OrientationEventListener orientationEventListener;
    private boolean orientationListenerEnabled;

    private BatteryReceiver batteryReceiver;
    private float batteryTemperature;

    private BrightnessObserver brightnessObserver;

    private float r;
    private float g;
    private float b;
    private float a;

    private IBinder iBinder;
    private AdjustmentDatabase adjustmentDatabase;

    public static OngoingService getInstance() {
        return ongoingService;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        ongoingService = this;

        this.displayOrientation = DisplayOrientation.PORTRAIT;

        // default preset adjustment
        this.r = 0.361f;
        this.g = 0.0f;
        this.b = 0.266f;
        this.a = 0.025f;

        // load data from database
        this.adjustmentDatabase = new AdjustmentDatabase(this);
        String latestAdjustment = this.adjustmentDatabase.getPreference("latest_adjustment");
        if (latestAdjustment != null) {
            try {
                JSONObject jsonObject = new JSONObject(latestAdjustment);
                this.r = (float)jsonObject.getDouble("r");
                this.g = (float)jsonObject.getDouble("g");
                this.b = (float)jsonObject.getDouble("b");
                this.a = (float)jsonObject.getDouble("a");
            } catch (JSONException exception) {
                Log.e("minun", "Latest adjustment data damaged, using default preset");
            }
        }

        // battery initialization
        this.batteryTemperature = -255f;
        this.batteryReceiver = new BatteryReceiver();
        this.registerReceiver(this.batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // brightness initialization
        this.brightnessObserver = new BrightnessObserver();
        this.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                true, this.brightnessObserver);

        // orientation initialization
        this.refreshDisplayOrientation();
        this.orientationListenerEnabled = false;
        this.orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                OngoingService.this.refreshDisplayOrientation();
            }
        };

        try {

            NotificationChannel channel = new NotificationChannel("minun", "minun", NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

            manager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(this, "minun");

            builder.setContentTitle("A30屏下微调");
            builder.setContentText("点按对重新调整屏下微调参数");
            builder.setSmallIcon(R.mipmap.notification_icon);

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, AdjustmentActivity.class), 0);
            builder.setContentIntent(contentIntent);

            Notification notification = builder.build();

            this.startForeground(ONGOING_ID, notification);

        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, START_STICKY, startId);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE: {
                if (!this.orientationListenerEnabled) {
                    this.orientationListenerEnabled = true;
                    this.orientationEventListener.enable();
                }
                break;
            }
            case Configuration.ORIENTATION_PORTRAIT: {
                // seldom occasion for portrait upside down, no monitoring for this situation
                if (this.orientationListenerEnabled) {
                    this.orientationListenerEnabled = false;
                    this.orientationEventListener.disable();
                }
                break;
            }
            case Configuration.ORIENTATION_UNDEFINED:
            default: {
                // do nothing, it is impossible usually
                break;
            }
        }

        this.refreshDisplayOrientation();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        this.stopForeground(true);

        if (this.orientationListenerEnabled) {
            this.orientationEventListener.disable();
        }
        this.unregisterReceiver(this.batteryReceiver);
        this.getContentResolver().unregisterContentObserver(this.brightnessObserver);

        if (ongoingService == this) {
            ongoingService = null;
        }

        super.onDestroy();

    }

    public void refreshDisplayOrientation() {

        if (displayManager == null) {
            displayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
        }

        Display[] displays = displayManager.getDisplays();

        Display mainDisplay = null;
        for (Display display : displays) {
            String name = display.getName();
            if ((mainDisplay == null) && "内置屏幕".equals(name)) {
                mainDisplay = display;
            }
        }
        if (mainDisplay == null) {
            mainDisplay = displayManager.getDisplay(0);
        }

        int rotation = mainDisplay.getRotation();

        DisplayOrientation oldDisplayOrientation = this.displayOrientation;

        switch (rotation) {
            case Surface.ROTATION_0: { displayOrientation = DisplayOrientation.PORTRAIT; break; }
            case Surface.ROTATION_90: { displayOrientation = DisplayOrientation.LANDSCAPE_UPSIDE_LEFT; break; }
            case Surface.ROTATION_180: { displayOrientation = DisplayOrientation.PORTRAIT_UPSIDE_DOWN; break; }
            case Surface.ROTATION_270: { displayOrientation = DisplayOrientation.LANDSCAPE_UPSIDE_RIGHT; break; }
            default: {
                // impossible
                break;
            }
        }

        if (oldDisplayOrientation != this.displayOrientation) {
            this.syncDisplayOrientation();
        }

    }

    public void hideAdjustmentOverlay() {

        Intent intent = new Intent(AdjustmentService.ACCESSIBILITY_ADJUST);
        intent.putExtra("action", AdjustmentService.HIDE_ADJUSTMENT_OVERLAY);

        sendBroadcast(intent);

    }

    public void showAdjustmentOverlay(boolean clear) {

        Intent intent = new Intent(AdjustmentService.ACCESSIBILITY_ADJUST);
        intent.putExtra("action", AdjustmentService.SHOW_ADJUSTMENT_OVERLAY);

        sendBroadcast(intent);

    }

    public void syncDisplayOrientation() {

        Intent intent = new Intent(AdjustmentService.ACCESSIBILITY_ADJUST);
        intent.putExtra("action", AdjustmentService.SYNC_DISPLAY_ORIENTATION);
        switch (this.displayOrientation) {
            case PORTRAIT: { intent.putExtra("rotation", AdjustmentService.ORIENTATION_PORTRAIT); break; }
            case PORTRAIT_UPSIDE_DOWN: { intent.putExtra("rotation", AdjustmentService.ORIENTATION_PORTRAIT_UPSIDE_DOWN); break; }
            case LANDSCAPE_UPSIDE_LEFT: { intent.putExtra("rotation", AdjustmentService.ORIENTATION_LANDSCAPE_UPSIDE_LEFT); break; }
            case LANDSCAPE_UPSIDE_RIGHT: { intent.putExtra("rotation", AdjustmentService.ORIENTATION_LANDSCAPE_UPSIDE_RIGHT); break; }
            default: { break; }
        }

        sendBroadcast(intent);

    }

    public void setAdjustment(float r, float g, float b, float a, boolean update) {

        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("r", this.r);
            jsonObject.put("g", this.g);
            jsonObject.put("b", this.b);
            jsonObject.put("a", this.a);
            this.adjustmentDatabase.updatePreference("latest_adjustment", jsonObject.toString());
        } catch (JSONException exception) {
            Log.e("minun", "Failed to generate preferences");
        }

        this.adjustmentDatabase.recordData(
                new Date().getTime() / 1000f,
                this.getBatteryTemperature(),
                this.getSystemBrightness(),
                this.r,
                this.g,
                this.b,
                this.a);

        Intent intent = new Intent(AdjustmentService.ACCESSIBILITY_ADJUST);

        intent.putExtra("action", AdjustmentService.SET_ADJUSTMENT);
        intent.putExtra("r", this.r);
        intent.putExtra("g", this.g);
        intent.putExtra("b", this.b);
        intent.putExtra("a", this.a);
        intent.putExtra("update", update);

        this.sendBroadcast(intent);

    }

    public void clearAdjustment() {

        Intent intent = new Intent(AdjustmentService.ACCESSIBILITY_ADJUST);
        intent.putExtra("action", AdjustmentService.CLEAR_ADJUSTMENT);

        sendBroadcast(intent);

    }

    public void restoreAdjustment() {

        {
            Intent intent = new Intent(AdjustmentService.ACCESSIBILITY_ADJUST);
            intent.putExtra("action", AdjustmentService.SET_ADJUSTMENT);
            intent.putExtra("r", this.r);
            intent.putExtra("g", this.g);
            intent.putExtra("b", this.b);
            intent.putExtra("a", this.a);
            intent.putExtra("update", true);
            this.sendBroadcast(intent);
        }

        {
            Intent intent = new Intent(AdjustmentService.ACCESSIBILITY_ADJUST);
            intent.putExtra("action", AdjustmentService.RESTORE_ADJUSTMENT);
            sendBroadcast(intent);
        }

    }

    public float[] getCurrentAdjustment() {

        return new float[] {this.r, this.g, this.b, this.a};

    }

    private float getSystemBrightness() {

        int systemBrightness = 0;
        try {
            systemBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException exception) {
            Log.e("minun", "Failed to get system brightness");
            return -1;
        }

        return systemBrightness / 255.0f;

    }

    private float getBatteryTemperature() {

        return this.batteryTemperature;

    }

    private void autorefreshAdjustment() {

        float temperature = this.getBatteryTemperature();
        float brightness = this.getSystemBrightness();

        // TODO: using data exists to get smart adjustment according to temperature and brightness;

    }

}

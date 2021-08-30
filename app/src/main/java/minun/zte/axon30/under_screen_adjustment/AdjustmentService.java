package minun.zte.axon30.under_screen_adjustment;

import android.annotation.SuppressLint;

import android.app.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.ActivityInfo;

import android.content.res.Configuration;

import android.database.ContentObserver;

import android.graphics.PixelFormat;

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
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

// TODO: make the service as accessibility service

public class AdjustmentService extends Service {

    public enum DisplayOrientation {
        PORTRAIT,
        PORTRAIT_UPSIDE_DOWN,
        LANDSCAPE_UPSIDE_LEFT,
        LANDSCAPE_UPSIDE_RIGHT
    };

    public class LocalBinder extends Binder {
        public AdjustmentService getService() {
            return AdjustmentService.this;
        }
    }

    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            float temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -2550) / 10f;
            AdjustmentService.this.batteryTemperature = temperature;
            AdjustmentService.this.autorefreshAdjustment();
        }
    }

    private class BrightnessObserver extends ContentObserver {
        public BrightnessObserver() {
            super(new Handler(Looper.getMainLooper()));
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            AdjustmentService.this.autorefreshAdjustment();
        }
    }

    private IBinder iBinder;

    private DisplayManager displayManager;
    private WindowManager windowManager;

    private WindowManager.LayoutParams layoutParams;
    private AdjustmentView adjustmentView;

    private DisplayOrientation displayOrientation;
    private OrientationEventListener orientationEventListener;
    private boolean orientationListenerEnabled;

    private BatteryReceiver batteryReceiver;
    private float batteryTemperature;

    private BrightnessObserver brightnessObserver;

    private AdjustmentDatabase adjustmentDatabase;

    private float r;
    private float g;
    private float b;
    private float a;

    @Override
    public void onCreate() {

        this.r = 0.361f;
        this.g = 0.0f;
        this.b = 0.266f;
        this.a = 0.025f;

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

        this.iBinder = new LocalBinder();

        this.batteryTemperature = -255f;

        this.batteryReceiver = new BatteryReceiver();
        this.registerReceiver(this.batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        this.brightnessObserver = new BrightnessObserver();
        this.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                true, this.brightnessObserver);

        this.refreshDisplayOrientation();

        this.orientationListenerEnabled = false;
        this.orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                AdjustmentService.this.refreshDisplayOrientation();
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (this.orientationListenerEnabled) {
            this.orientationEventListener.disable();
        }
        this.hideAdjustmentOverlay();
        this.unregisterReceiver(this.batteryReceiver);
        this.getContentResolver().unregisterContentObserver(this.brightnessObserver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.iBinder;
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
            this.showAdjustmentOverlay();
        }

    }

    public void hideAdjustmentOverlay() {

        if (adjustmentView != null) {
            windowManager.removeView(adjustmentView);
            adjustmentView = null;
        }

    }

    @SuppressLint("RtlHardcoded")
    public void showAdjustmentOverlay() {

        if (!Settings.canDrawOverlays(this)) {
            Log.e("minun", "Permission denied for overlay window creation");
            return;
        }

        if (windowManager == null) {
            windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        }

        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams();
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            layoutParams.format = PixelFormat.RGBA_8888;
            layoutParams.flags = (
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            layoutParams.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT;
            layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
            layoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            layoutParams.x = 0;
            layoutParams.y = 0;
        }

        switch (this.displayOrientation) {
            case PORTRAIT: {
                layoutParams.gravity = Gravity.TOP;
                layoutParams.width = 102;
                layoutParams.height = 60;
                break;
            }
            case PORTRAIT_UPSIDE_DOWN: {
                layoutParams.gravity = Gravity.BOTTOM;
                layoutParams.width = 102;
                layoutParams.height = 60;
                break;
            }
            case LANDSCAPE_UPSIDE_LEFT: {
                layoutParams.gravity = Gravity.LEFT;
                layoutParams.width = 60;
                layoutParams.height = 102;
                break;
            }
            case LANDSCAPE_UPSIDE_RIGHT: {
                layoutParams.gravity = Gravity.RIGHT;
                layoutParams.width = 60;
                layoutParams.height = 102;
                break;
            }
        }

        if (adjustmentView == null) {
            adjustmentView = new AdjustmentView(this);
        }
        if (adjustmentView.getParent() != null) {
            windowManager.updateViewLayout(adjustmentView, layoutParams);
            adjustmentView.setVisibility(View.VISIBLE);
        } else {
            windowManager.addView(adjustmentView, layoutParams);
        }

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

        if (update) {
            this.restoreAdjustment();
        }

    }

    public float[] getCurrentAdjustment() {

        return new float[] {this.r, this.g, this.b, this.a};

    }

    public void clearAdjustment() {

        if (this.adjustmentView == null) {
            return;
        }

        this.adjustmentView.setAdjustment(0, 0 , 0, 0);

    }

    public void restoreAdjustment() {

        if (this.adjustmentView == null) {
            return;
        }

        this.adjustmentView.setAdjustment(r, g, b, a);

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

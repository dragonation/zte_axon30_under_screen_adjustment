package minun.zte.axon30.under_screen_adjustment;

import android.accessibilityservice.AccessibilityService;

import android.annotation.SuppressLint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.ActivityInfo;

import android.graphics.PixelFormat;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import android.view.accessibility.AccessibilityEvent;

public class AdjustmentService extends AccessibilityService {

    public static final String ONGOING_INTENT = "minun.zte.axon30.under_screen_adjustment.adjustment";

    private static AdjustmentService adjustmentService;

    private class OngoingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("minun", "test");
//            float temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -2550) / 10f;
//            OngoingService.this.batteryTemperature = temperature;
//            OngoingService.this.autorefreshAdjustment();
        }
    }

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private AdjustmentView adjustmentView;

    private OngoingReceiver ongoingReceiver;

    private OngoingService.DisplayOrientation displayOrientation;

    float r;
    float g;
    float b;
    float a;

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        adjustmentService = this;

        if (this.ongoingReceiver == null) {
            this.ongoingReceiver = new OngoingReceiver();
            this.registerReceiver(this.ongoingReceiver, new IntentFilter(ONGOING_INTENT));
        }

        this.displayOrientation = OngoingService.DisplayOrientation.PORTRAIT;

        this.r = 0;
        this.g = 0;
        this.b = 0;
        this.a = 0;

        this.showAdjustmentOverlay(true);

    }

    @Override
    public void onDestroy() {

        if (adjustmentService == this) {
            adjustmentService = null;
        }

        this.hideAdjustmentOverlay();

        if (this.ongoingReceiver != null) {
            OngoingReceiver ongoingReceiver = this.ongoingReceiver;
            this.ongoingReceiver = null;
            this.unregisterReceiver(ongoingReceiver);
        }

        super.onDestroy();

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Do nothing
    }

    @Override
    public void onInterrupt() {
        // Do nothing
    }

    // TODO: find a better solution to communicate between accessibility service and
    //       foreground
    public static AdjustmentService getInstance() {

        return adjustmentService;

    }

    public void hideAdjustmentOverlay() {

        if (adjustmentView != null) {
            windowManager.removeView(adjustmentView);
            adjustmentView = null;
        }

    }

    public void syncDisplayOrientation(OngoingService.DisplayOrientation displayOrientation) {

        this.displayOrientation = displayOrientation;

        this.showAdjustmentOverlay(false);

    }

    @SuppressLint("RtlHardcoded")
    public void showAdjustmentOverlay(boolean clear) {

        if (windowManager == null) {
            windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        }

        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams();
            layoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
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

        if (clear) {
            this.clearAdjustment();
        }

    }

    public void setAdjustment(float r, float g, float b, float a, boolean update) {

        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

        if (update) {
            this.restoreAdjustment();
        }

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

        this.adjustmentView.setAdjustment(this.r, this.g, this.b, this.a);

    }

}

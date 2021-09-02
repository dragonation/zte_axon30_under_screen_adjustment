package minun.zte.axon30.under_screen_adjustment;

import android.accessibilityservice.AccessibilityService;

import android.annotation.SuppressLint;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;

import android.graphics.PixelFormat;

import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import android.view.accessibility.AccessibilityEvent;

public class AdjustmentService extends AccessibilityService {

    public static final String ACCESSIBILITY_ADJUST = "minun.zte.axon30.under_screen_adjustment.adjust";

    public static final int HIDE_ADJUSTMENT_OVERLAY = 1;
    public static final int SHOW_ADJUSTMENT_OVERLAY = 2;

    public static final int SET_ADJUSTMENT = 10;
    public static final int CLEAR_ADJUSTMENT = 11;
    public static final int RESTORE_ADJUSTMENT = 12;
    public static final int SYNC_DISPLAY_ORIENTATION = 13;

    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_PORTRAIT_UPSIDE_DOWN = 2;
    public static final int ORIENTATION_LANDSCAPE_UPSIDE_LEFT = 3;
    public static final int ORIENTATION_LANDSCAPE_UPSIDE_RIGHT = 4;

    private class AdjustmentServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            AdjustmentService service = AdjustmentService.this;
            switch (intent.getIntExtra("action", 0)) {
                case HIDE_ADJUSTMENT_OVERLAY: { service.hideAdjustmentOverlay(); break; }
                case SHOW_ADJUSTMENT_OVERLAY: {
                    boolean clear = intent.getBooleanExtra("clear", false);
                    service.showAdjustmentOverlay(clear);
                    break;
                }
                case SET_ADJUSTMENT: {
                    float r = intent.getFloatExtra("r", 0f);
                    float g = intent.getFloatExtra("g", 0f);
                    float b = intent.getFloatExtra("b", 0f);
                    float a = intent.getFloatExtra("a", 0f);
                    float notch = intent.getFloatExtra("notch", 0f);
                    boolean update = intent.getBooleanExtra("update", false);
                    service.setAdjustment(r, g, b, a, notch, update);
                    break;
                }
                case CLEAR_ADJUSTMENT: { service.clearAdjustment(); break; }
                case RESTORE_ADJUSTMENT: { service.restoreAdjustment(); break; }
                case SYNC_DISPLAY_ORIENTATION: {
                    switch (intent.getIntExtra("rotation", 0)) {
                        case ORIENTATION_PORTRAIT: {
                            service.syncDisplayOrientation(OngoingService.DisplayOrientation.PORTRAIT);
                            break;
                        }
                        case ORIENTATION_PORTRAIT_UPSIDE_DOWN: {
                            service.syncDisplayOrientation(OngoingService.DisplayOrientation.PORTRAIT_UPSIDE_DOWN);
                            break;
                        }
                        case ORIENTATION_LANDSCAPE_UPSIDE_LEFT: {
                            service.syncDisplayOrientation(OngoingService.DisplayOrientation.LANDSCAPE_UPSIDE_LEFT);
                            break;
                        }
                        case ORIENTATION_LANDSCAPE_UPSIDE_RIGHT: {
                            service.syncDisplayOrientation(OngoingService.DisplayOrientation.LANDSCAPE_UPSIDE_RIGHT);
                            break;
                        }
                        default: {
                            service.syncDisplayOrientation(OngoingService.DisplayOrientation.PORTRAIT);
                            break;
                        }
                    }
                    service.showAdjustmentOverlay(false);
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    private AdjustmentServiceReceiver receiver;

    private WindowManager windowManager;

    private WindowManager.LayoutParams adjustmentLayoutParams;
    private AdjustmentView adjustmentView;

    private WindowManager.LayoutParams notchLayoutParams;
    private NotchView notchView;

    private OngoingService.DisplayOrientation displayOrientation;

    float r;
    float g;
    float b;
    float a;
    float notch;

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        this.displayOrientation = OngoingService.DisplayOrientation.PORTRAIT;

        this.r = 0;
        this.g = 0;
        this.b = 0;
        this.a = 0;
        this.notch = 0;

        if (this.receiver == null) {
            this.receiver = new AdjustmentServiceReceiver();
            registerReceiver(this.receiver, new IntentFilter(ACCESSIBILITY_ADJUST));
        }
    }

    @Override
    public void onDestroy() {

        this.hideAdjustmentOverlay();

        if (this.receiver != null) {
            this.unregisterReceiver(this.receiver);
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

    public void hideAdjustmentOverlay() {

        if (adjustmentView != null) {
            windowManager.removeView(adjustmentView);
            adjustmentView = null;
        }

        if (notchView != null) {
            windowManager.removeView(notchView);
            notchView = null;
        }

    }

    public void syncDisplayOrientation(OngoingService.DisplayOrientation displayOrientation) {

        this.displayOrientation = displayOrientation;

    }

    @SuppressLint("RtlHardcoded")
    public void showAdjustmentOverlay(boolean clear) {

        if (windowManager == null) {
            windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        }

        if (adjustmentLayoutParams == null) {
            adjustmentLayoutParams = new WindowManager.LayoutParams();
            adjustmentLayoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            adjustmentLayoutParams.format = PixelFormat.RGBA_8888;
            adjustmentLayoutParams.flags = (
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            adjustmentLayoutParams.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT;
            adjustmentLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
            adjustmentLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            adjustmentLayoutParams.x = 0;
            adjustmentLayoutParams.y = 0;
        }

        if (notchLayoutParams == null) {
            notchLayoutParams = new WindowManager.LayoutParams();
            notchLayoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            notchLayoutParams.format = PixelFormat.RGBA_8888;
            notchLayoutParams.flags = (
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            notchLayoutParams.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT;
            notchLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
            notchLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            notchLayoutParams.x = 0;
            notchLayoutParams.y = 0;
        }

        switch (this.displayOrientation) {
            case PORTRAIT: {
                adjustmentLayoutParams.gravity = Gravity.TOP;
                adjustmentLayoutParams.width = 102;
                adjustmentLayoutParams.height = 60;
                notchLayoutParams.gravity = Gravity.TOP;
                notchLayoutParams.width = 1080;
                notchLayoutParams.height = 204;
                break;
            }
            case PORTRAIT_UPSIDE_DOWN: {
                adjustmentLayoutParams.gravity = Gravity.BOTTOM;
                adjustmentLayoutParams.width = 102;
                adjustmentLayoutParams.height = 60;
                notchLayoutParams.gravity = Gravity.BOTTOM;
                notchLayoutParams.width = 1080;
                notchLayoutParams.height = 204;
                break;
            }
            case LANDSCAPE_UPSIDE_LEFT: {
                adjustmentLayoutParams.gravity = Gravity.LEFT;
                adjustmentLayoutParams.width = 60;
                adjustmentLayoutParams.height = 102;
                notchLayoutParams.gravity = Gravity.LEFT;
                notchLayoutParams.width = 204;
                notchLayoutParams.height = 1080;
                break;
            }
            case LANDSCAPE_UPSIDE_RIGHT: {
                adjustmentLayoutParams.gravity = Gravity.RIGHT;
                adjustmentLayoutParams.width = 60;
                adjustmentLayoutParams.height = 102;
                notchLayoutParams.gravity = Gravity.RIGHT;
                notchLayoutParams.width = 204;
                notchLayoutParams.height = 1080;
                break;
            }
        }

        if (adjustmentView == null) {
            adjustmentView = new AdjustmentView(this);
        }
        if (adjustmentView.getParent() != null) {
            windowManager.updateViewLayout(adjustmentView, adjustmentLayoutParams);
            adjustmentView.setVisibility(View.VISIBLE);
        } else {
            windowManager.addView(adjustmentView, adjustmentLayoutParams);
        }

        if (notchView == null) {
            notchView = new NotchView(this);
        }
        if (notchView.getParent() != null) {
            windowManager.updateViewLayout(notchView, notchLayoutParams);
            notchView.setVisibility(View.VISIBLE);
        } else {
            windowManager.addView(notchView, notchLayoutParams);
        }

        if (clear) {
            this.clearAdjustment();
        }

    }

    public void setAdjustment(float r, float g, float b, float a, float notch, boolean update) {

        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.notch = notch;

        if (update) {
            this.restoreAdjustment();
        }

    }

    public void clearAdjustment() {

        if (this.adjustmentView != null) {
            this.adjustmentView.setAdjustment(0, 0 , 0, 0);
        }

        if (this.notchView != null) {
            this.notchView.setNotch(0);
        }

    }

    public void restoreAdjustment() {

        if (this.adjustmentView != null) {
            this.adjustmentView.setAdjustment(this.r, this.g, this.b, this.a);
        }

        if (this.notchView != null) {
            this.notchView.setNotch(this.notch);
        }

    }

}

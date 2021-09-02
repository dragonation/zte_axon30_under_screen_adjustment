package minun.zte.axon30.under_screen_adjustment;

import android.accessibilityservice.AccessibilityServiceInfo;

import android.annotation.SuppressLint;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;

import android.net.Uri;

import android.os.Bundle;

import android.provider.Settings;

import android.util.Log;

import android.view.accessibility.AccessibilityManager;

import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjustmentActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 23763;

    public final int OK = 0;
    public final int ERROR_API_NOT_FOUND = 1;
    public final int ERROR_API_NOT_IMPL = 2;
    public final int ERROR_JSON_ERROR = 3;
    public final int ERROR_NATIVE = 4;

    private class AdjustmentJavaScriptInterface {

        @JavascriptInterface
        public int callAPI(String api, String parameters, String callback) {
            AdjustmentJavaScriptAPI javaScriptAPI = AdjustmentActivity.this.adjustmentJavaScriptAPIs.get(api);
            if (javaScriptAPI == null) {
                return ERROR_API_NOT_FOUND;
            }
            JSONObject jsonParameters;
            try {
                jsonParameters = new JSONObject(parameters);
            } catch (JSONException exception) {
                return ERROR_JSON_ERROR;
            }
            try {
                javaScriptAPI.invoke(api, jsonParameters, new AdjustmentJavaScriptCallback(callback));
            } catch (Exception exception) {
                return ERROR_NATIVE;
            }
            return OK;
        }

    }

    private class AdjustmentJavaScriptCallback {

        private final String callbackAPI;

        public AdjustmentJavaScriptCallback(String callbackAPI) {
            this.callbackAPI = callbackAPI;
        }

        public void response(int error) {

            String script = this.callbackAPI + "(" + error + ", null)";

            AdjustmentActivity.this.evaluateJavaScript(script);

        }

        public void response(int error, JSONObject result) {

            String script = this.callbackAPI + "(" + error + "," + result.toString() + ")";

            AdjustmentActivity.this.evaluateJavaScript(script);

        }

        public void response(int error, boolean result) {

            String script = this.callbackAPI + "(" + error + ", " + (result ? "true" : "false") + ")";

            AdjustmentActivity.this.evaluateJavaScript(script);

        }

        public void response(int error, int result) {

            String script = this.callbackAPI + "(" + error + ", " + result + ")";

            AdjustmentActivity.this.evaluateJavaScript(script);

        }

        public void response(int error, double result) {

            String script = this.callbackAPI + "(" + error + ", " + result + ")";

            AdjustmentActivity.this.evaluateJavaScript(script);

        }

        public void response(int error, String result) {

            JSONArray array = new JSONArray();
            array.put(result);

            String jsonString = array.toString();

            String script = this.callbackAPI + "(" + error + ", " + jsonString.substring(1, jsonString.length() - 1)+ ")";

            AdjustmentActivity.this.evaluateJavaScript(script);

        }

    }

    private interface AdjustmentJavaScriptAPI {
        void invoke(String api, JSONObject parameters, AdjustmentJavaScriptCallback callback);
    }

    private Map<String, AdjustmentJavaScriptAPI> adjustmentJavaScriptAPIs;

    private WebView webView;

    private String permissionCheckCallbackScript;

    @Override
    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.adjustmentJavaScriptAPIs = new HashMap<>();

        this.installJavaScriptAPIs();

//        WebView.setWebContentsDebuggingEnabled(true);

        this.webView = new WebView(this);
        this.webView.setWebViewClient(new WebViewClient());
        this.webView.addJavascriptInterface(new AdjustmentJavaScriptInterface(), ".MinunZTEAxon30API");
//        this.webView.loadUrl("http://192.168.1.8:24724");
//        this.webView.loadUrl("http://192.168.1.105:24724");
        this.webView.loadUrl("file:///android_asset/ui/index.html");

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        this.setContentView(this.webView);

        try {
            this.startForegroundService(new Intent(this, OngoingService.class));
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        this.showAdjustmentOverlay(true);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (!Settings.canDrawOverlays(this)) {
                    Log.e("minun", "User denied for permission creating overlay");
                } else {
                    this.showAdjustmentOverlay(true);
                }
                if (this.permissionCheckCallbackScript != null) {
                    this.evaluateJavaScript(this.permissionCheckCallbackScript);
                }
                break;
            }
            default: { break; }
        }

        this.showAdjustmentOverlay(true);

    }

    @Override
    public void onBackPressed() {

        this.moveTaskToBack(true);

    }

    @Override
    protected void onResume() {

        super.onResume();

        this.clearAdjustmentOverlay();

        if (this.permissionCheckCallbackScript != null) {
            this.evaluateJavaScript(this.permissionCheckCallbackScript);
        }

    }

    @Override
    protected void onPause() {

        super.onPause();

        this.restoreAdjustmentOverlay();

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

    }

    private void installJavaScriptAPIs() {

        this.adjustmentJavaScriptAPIs.put("getCurrentAdjustment", (api, parameters, callback) -> {

            float[] adjustment = AdjustmentActivity.this.getCurrentAdjustment();
            if (adjustment == null) {
                callback.response(OK);
                return;
            }

            JSONObject result = new JSONObject();
            try {
                result.put("r", adjustment[0]);
                result.put("g", adjustment[1]);
                result.put("b", adjustment[2]);
                result.put("a", adjustment[3]);
                result.put("notch", adjustment[4]);
            } catch (JSONException exception) {
                callback.response(ERROR_JSON_ERROR);
                return;
            }

            callback.response(OK, result);

        });

        this.adjustmentJavaScriptAPIs.put("setAdjustment", (api, parameters, callback) -> {
            float r, g, b, a, notch;
            boolean update;
            try {
                JSONArray arguments = parameters.getJSONArray("arguments");
                r = (float)arguments.getDouble(0);
                g = (float)arguments.getDouble(1);
                b = (float)arguments.getDouble(2);
                a = (float)arguments.getDouble(3);
                notch = (float)arguments.getDouble(4);
                update = (boolean)arguments.getBoolean(5);
            } catch (JSONException exception) {
                callback.response(ERROR_JSON_ERROR);
                return;
            }
            AdjustmentActivity.this.setAdjustment(r, g, b, a, notch, update);
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("clearAdjustmentOverlay", (api, parameters, callback) -> {
            AdjustmentActivity.this.clearAdjustmentOverlay();
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("restoreAdjustmentOverlay", (api, parameters, callback) -> {
            AdjustmentActivity.this.restoreAdjustmentOverlay();
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("getCurrentDeviceStates", (api, parameters, callback) -> {
            float brightness = AdjustmentActivity.this.getSystemBrightness();
            float temperature = AdjustmentActivity.this.getBatteryTemperature();
            String deviceId = AdjustmentActivity.this.getDeviceId();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("brightness", brightness);
                jsonObject.put("temperature", temperature);
                jsonObject.put("device", deviceId);
            } catch (JSONException exception) {
                callback.response(ERROR_JSON_ERROR);
                return;
            }
            callback.response(OK, jsonObject);
        });

        this.adjustmentJavaScriptAPIs.put("hideAdjustmentOverlay", (api, parameters, callback) -> {
            AdjustmentActivity.this.hideAdjustmentOverlay();
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("showAdjustmentOverlay", (api, parameters, callback) -> {
            AdjustmentActivity.this.showAdjustmentOverlay(true);
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("isOverlayPermissionGranted", (api, parameters, callback) -> {
            boolean granted = AdjustmentActivity.this.isOverlayPermissionGranted();
            callback.response(OK, granted);
        });

        this.adjustmentJavaScriptAPIs.put("navigateToPermissionManager", (api, parameters, callback) -> {
            AdjustmentActivity.this.navigateToPermissionManager();
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("setPermissionCheckCallbackScript", (api, parameters, callback) -> {
            String callbackScript;
            try {
                JSONArray arguments = parameters.getJSONArray("arguments");
                callbackScript = arguments.getString(0);
            } catch (JSONException exception) {
                callback.response(ERROR_JSON_ERROR);
                return;
            }
            AdjustmentActivity.this.permissionCheckCallbackScript = callbackScript;
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("isWritingSystemPermissionGranted", (api, parameters, callback) -> {
            boolean granted = AdjustmentActivity.this.isWritingSystemPermissionGranted();
            callback.response(OK, granted);
        });

        this.adjustmentJavaScriptAPIs.put("navigateToSettingPermission", (api, parameters, callback) -> {
            AdjustmentActivity.this.navigateToSettingPermission();
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("autoenableAccessibilityService", (api, parameters, callback) -> {
            AdjustmentActivity.this.autoenableAccessibilityService();
            callback.response(OK);
        });

    }

    private void evaluateJavaScript(String script) {
        this.runOnUiThread(() -> {
            AdjustmentActivity.this.webView.evaluateJavascript(script, (value) -> {
                // Do nothing
            });
        });
    }

    private void hideAdjustmentOverlay() {
        this.runOnUiThread(() -> {
            OngoingService service = OngoingService.getInstance();
            if (service == null) {
                Log.e("minun", "Adjustment service not available");
                return;
            }
            service.hideAdjustmentOverlay();
        });
    }

    private void showAdjustmentOverlay(boolean clear) {
        this.runOnUiThread(() -> {
            OngoingService service = OngoingService.getInstance();
            if (service == null) {
                Log.e("minun", "Adjustment service not available");
                return;
            }
            service.showAdjustmentOverlay(clear);
        });
    }

    private void setAdjustment(float r, float g, float b, float a, float notch, boolean update) {
        this.runOnUiThread(() -> {
            OngoingService service = OngoingService.getInstance();
            if (service == null) {
                Log.e("minun", "Adjustment service not available");
                return;
            }
            service.setAdjustment(r, g, b, a, notch, update);
            Toast.makeText(AdjustmentActivity.this, "补偿数据已保存", Toast.LENGTH_SHORT).show();
        });
    }

    private void clearAdjustmentOverlay() {
        this.runOnUiThread(() -> {
            OngoingService service = OngoingService.getInstance();
            if (service == null) {
                Log.e("minun", "Adjustment service not available");
                return;
            }
            service.clearAdjustment();
        });
    }

    private void restoreAdjustmentOverlay() {
        this.runOnUiThread(() -> {
            OngoingService service = OngoingService.getInstance();
            if (service == null) {
                Log.e("minun", "Adjustment service not available");
                return;
            }
            service.restoreAdjustment();
        });
    }

    private float[] getCurrentAdjustment() {
        OngoingService service = OngoingService.getInstance();
        if (service == null) {
            Log.e("minun", "Adjustment service not available");
            return null;
        }
        return service.getCurrentAdjustment();
    }

    private float getBatteryTemperature() {
        OngoingService service = OngoingService.getInstance();
        if (service == null) {
            Log.e("minun", "Adjustment service not available");
            return -255f;
        }
        return service.getBatteryTemperature();
    }

    private String getDeviceId() {
        OngoingService service = OngoingService.getInstance();
        if (service == null) {
            Log.e("minun", "Adjustment service not available");
            return null;
        }
        return service.getDeviceId();
    }

    private float getSystemBrightness() {
        OngoingService service = OngoingService.getInstance();
        if (service == null) {
            Log.e("minun", "Adjustment service not available");
            return -1;
        }
        return service.getSystemBrightness();
    }

    private boolean isWritingSystemPermissionGranted() {
        return Settings.System.canWrite(this);
    }

    private void navigateToSettingPermission() {

        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivityForResult(intent, AdjustmentActivity.PERMISSION_REQUEST_CODE);

    }

    private void autoenableAccessibilityService() {

        if (!this.isWritingSystemPermissionGranted()) {
            return;
        }

        try {
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    "minun.zte.axon30.under_screen_adjustment/minun.zte.axon30.under_screen_adjustment.AdjustmentService");
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("minun", "Failed to autoenable accessiblity service");
        }

    }

    private boolean isOverlayPermissionGranted() {

        boolean granted = OngoingService.isOverlayPermissionGranted(this);

        OngoingService service = OngoingService.getInstance();
        if (service != null) {
            service.refreshNotification();
        }

        return granted;

    }

    private void navigateToPermissionManager() {

        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivityForResult(intent, AdjustmentActivity.PERMISSION_REQUEST_CODE);

    }

}

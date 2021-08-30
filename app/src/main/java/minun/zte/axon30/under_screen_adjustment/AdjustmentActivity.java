package minun.zte.axon30.under_screen_adjustment;

import android.annotation.SuppressLint;

import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import android.net.Uri;

import android.os.Bundle;
import android.os.IBinder;

import android.provider.Settings;

import android.util.Log;

import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
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

    private class AdjustmentServiceConnection implements ServiceConnection {

        private boolean boundApplied = false;

        private AdjustmentService boundService = null;

        public AdjustmentServiceConnection() {
            this.bind();
        }

        public AdjustmentService getBoundService() {
            return boundService;
        }

        public void bind() {

            if (this.boundApplied) {
                return;
            }

            Intent intent = new Intent(AdjustmentActivity.this, AdjustmentService.class);

            this.boundApplied = AdjustmentActivity.this.bindService(intent, this, Context.BIND_AUTO_CREATE);

        }

        public void unbind() {
            if (this.boundApplied) {
                this.boundApplied = false;
                AdjustmentActivity.this.unbindService(this);
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.boundService = ((AdjustmentService.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            this.boundService = null;
        }

    }

    private Map<String, AdjustmentJavaScriptAPI> adjustmentJavaScriptAPIs;

    private WebView webView;

    private AdjustmentServiceConnection adjustmentServiceConnection;

    private String permissionCheckCallbackScript;

    @Override
    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.adjustmentServiceConnection = new AdjustmentServiceConnection();

        this.adjustmentJavaScriptAPIs = new HashMap<>();

        this.installJavaScriptAPIs();

        WebView.setWebContentsDebuggingEnabled(true);

        this.webView = new WebView(this);
        this.webView.setWebViewClient(new WebViewClient());
        this.webView.addJavascriptInterface(new AdjustmentJavaScriptInterface(), ".MinunZTEAxon30API");
//        this.webView.loadUrl("http://192.168.1.105:24724");
        this.webView.loadUrl("file:///android_asset/ui/index.html");

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        this.setContentView(this.webView);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (!Settings.canDrawOverlays(this)) {
                    Log.e("minun", "User denied for permission creating overlay");
                } else {
                    this.showAdjustmentOverlay();
                }
                if (this.permissionCheckCallbackScript != null) {
                    this.evaluateJavaScript(this.permissionCheckCallbackScript);
                }
                break;
            }
            default: { break; }
        }

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

        this.adjustmentServiceConnection.unbind();

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
            } catch (JSONException exception) {
                callback.response(ERROR_JSON_ERROR);
                return;
            }

            callback.response(OK, result);

        });

        this.adjustmentJavaScriptAPIs.put("setAdjustment", (api, parameters, callback) -> {
            float r, g, b, a;
            boolean update;
            try {
                JSONArray arguments = parameters.getJSONArray("arguments");
                r = (float)arguments.getDouble(0);
                g = (float)arguments.getDouble(1);
                b = (float)arguments.getDouble(2);
                a = (float)arguments.getDouble(3);
                update = (boolean)arguments.getBoolean(4);
            } catch (JSONException exception) {
                callback.response(ERROR_JSON_ERROR);
                return;
            }
            AdjustmentActivity.this.setAdjustment(r, g, b, a, update);
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

        this.adjustmentJavaScriptAPIs.put("getCurrentDeviceStates", (api, parameters, callback) -> callback.response(ERROR_API_NOT_IMPL));

        this.adjustmentJavaScriptAPIs.put("hideAdjustmentOverlay", (api, parameters, callback) -> {
            AdjustmentActivity.this.hideAdjustmentOverlay();
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("showAdjustmentOverlay", (api, parameters, callback) -> {
            AdjustmentActivity.this.showAdjustmentOverlay();
            callback.response(OK);
        });

        this.adjustmentJavaScriptAPIs.put("isOverlayPermissionGranted", (api, parameters, callback) -> {
            boolean granted = Settings.canDrawOverlays(AdjustmentActivity.this);
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
            AdjustmentService service = AdjustmentActivity.this.adjustmentServiceConnection.getBoundService();
            if (service == null) {
                return;
            }
            service.hideAdjustmentOverlay();
        });
    }

    private void showAdjustmentOverlay() {
        this.runOnUiThread(() -> {
            AdjustmentService service = AdjustmentActivity.this.adjustmentServiceConnection.getBoundService();
            if (service == null) {
                return;
            }
            service.showAdjustmentOverlay();
        });
    }

    private void setAdjustment(float r, float g, float b, float a, boolean update) {
        this.runOnUiThread(() -> {
            AdjustmentService service = AdjustmentActivity.this.adjustmentServiceConnection.getBoundService();
            if (service == null) {
                return;
            }
            service.setAdjustment(r, g, b, a, update);
            Toast.makeText(AdjustmentActivity.this, "补偿数据已保存", Toast.LENGTH_SHORT).show();
        });
    }

    private void clearAdjustmentOverlay() {
        this.runOnUiThread(() -> {
            AdjustmentService service = AdjustmentActivity.this.adjustmentServiceConnection.getBoundService();
            if (service == null) {
                return;
            }
            service.clearAdjustment();
        });
    }

    private void restoreAdjustmentOverlay() {
        this.runOnUiThread(() -> {
            AdjustmentService service = AdjustmentActivity.this.adjustmentServiceConnection.getBoundService();
            if (service == null) {
                return;
            }
            service.restoreAdjustment();
        });
    }

    private float[] getCurrentAdjustment() {
        AdjustmentService service = AdjustmentActivity.this.adjustmentServiceConnection.getBoundService();
        if (service == null) {
            return null;
        }
        return service.getCurrentAdjustment();
    }

    private void navigateToPermissionManager() {

        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));

        startActivityForResult(intent, AdjustmentActivity.PERMISSION_REQUEST_CODE);

    }

}

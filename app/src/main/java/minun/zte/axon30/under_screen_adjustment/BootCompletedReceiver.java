package minun.zte.axon30.under_screen_adjustment;

import android.annotation.SuppressLint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver
        extends BroadcastReceiver {

    @Override
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    public void onReceive(Context context, Intent intent) {

        context.startService(new Intent(context, AdjustmentService.class));

    }

}
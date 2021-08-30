package minun.zte.axon30.under_screen_adjustment;

import android.content.Context;

import android.graphics.Color;

import android.view.View;

public class AdjustmentView extends View {

    public AdjustmentView(Context context) {
        super(context);
        this.setBackgroundColor(Color.TRANSPARENT);
    }

    public void setAdjustment(float r, float g, float b, float a) {
        this.setBackgroundColor(Color.argb(a, r, g, b));
    }

}

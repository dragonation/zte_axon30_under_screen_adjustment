package minun.zte.axon30.under_screen_adjustment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

public class NotchView extends View {

    Paint paint;
    Path path;

    private float notch;

    public NotchView(Context context) {

        super(context);

        this.notch = 0;

        this.paint = new Paint();
        this.path = new Path();

    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (notch < 0.1f) {
            this.paint.setColor(Color.argb(notch * 10, 0, 0, 0));
            canvas.drawCircle(540, 33, 27, this.paint);
        } else if (notch > 0.65f) {
            this.paint.setColor(Color.BLACK);
            float radius = (notch - 0.65f) * 100f * 3f;
            path.reset();
            path.moveTo(0f, 0f);
            path.lineTo(0f, 99f + radius);
            path.arcTo(0, 99f, radius * 2f, 99f + 2f * radius, 180, 90, false);
            path.lineTo(1080f - radius, 99f);
            path.arcTo(1080f - 2f * radius, 99f, 1080f, 99f + 2f * radius, -90, 90, false);
            path.lineTo(1080f, 0);
            path.close();
            canvas.drawPath(path, this.paint);
        } else {
            this.paint.setColor(Color.BLACK);
            float width = this.notch * 360f * 3 + 99f;
            float left = (1080f - width) / 2f;
            path.reset();
            path.moveTo(left, 0f);
            path.arcTo(left - 25f, 0f, left + 25f, 50f, -90, 90, false);
            path.lineTo(left + 25f, 99f - 25f);
            path.arcTo(left + 25f, 49f, left + 75f, 99f, 180, -90, false);
            path.lineTo(left + width - 50f, 99f);
            path.arcTo(left + width - 75f, 49f, left + width - 25f, 99f, 90, -90, false);
            path.lineTo(left + width - 25f, 25f);
            path.arcTo(left + width - 25f, 0f, left + width + 25f, 50f, -180, 90, false);
            path.close();
            canvas.drawPath(path, this.paint);
        }

    }

    public void setNotch(float notch) {

        this.notch = notch;

        this.invalidate();

    }

}

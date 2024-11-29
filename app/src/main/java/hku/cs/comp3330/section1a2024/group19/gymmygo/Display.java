package hku.cs.comp3330.section1a2024.group19.gymmygo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class Display extends View {

    private Bitmap bitmap;
    private final Rect destRect = new Rect();

    public Display(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Receives the processed bitmap and triggers a redraw.
     *
     * @param bitmap The bitmap with drawn pose landmarks.
     */
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate(); // Trigger onDraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            // Define destination rectangle to fill the entire view
            destRect.set(0, 0, getWidth(), getHeight());
            // Draw the bitmap scaled to the view's size
            canvas.drawBitmap(bitmap, null, destRect, null);
        }
    }
}

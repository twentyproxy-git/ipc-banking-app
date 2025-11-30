package com.example.ipcbanking.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class MagnifierView extends View {

    private Bitmap bitmap;
    private BitmapShader shader;
    private Matrix matrix;
    private Paint paint;
    private Paint borderPaint;

    private float zoomFactor = 2.0f;
    private int radius = 180;
    private float pointX = 0;
    private float pointY = 0;
    private boolean isMagnifying = false;

    public MagnifierView(Context context) {
        super(context);
        init();
    }

    public MagnifierView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(8);
        borderPaint.setAntiAlias(true);
        borderPaint.setShadowLayer(10.0f, 0.0f, 2.0f, Color.DKGRAY);

        matrix = new Matrix();
    }

    public void setupBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        if (bitmap != null) { // <--- THÊM DÒNG NÀY VÀO ĐÂY
            shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            paint.setShader(shader);
        } else {
            // Nếu bitmap null, reset shader để không vẽ gì
            paint.setShader(null);
        }
        invalidate(); // Luôn gọi invalidate để vẽ lại
    }

    public void setPoint(float x, float y) {
        this.pointX = x;
        this.pointY = y;
        this.isMagnifying = true;
        invalidate();
    }

    public void hide() {
        this.isMagnifying = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || !isMagnifying || shader == null) {
            return;
        }

        matrix.reset();
        matrix.postTranslate(-pointX, -pointY);
        matrix.postScale(zoomFactor, zoomFactor);

        float drawX = pointX;
        float drawY = pointY - radius - 50;

        matrix.postTranslate(drawX, drawY);
        shader.setLocalMatrix(matrix);

        canvas.drawCircle(drawX, drawY, radius, paint);
        canvas.drawCircle(drawX, drawY, radius, borderPaint);
    }
}
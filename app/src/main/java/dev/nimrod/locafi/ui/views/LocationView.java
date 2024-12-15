package dev.nimrod.locafi.ui.views;

import static android.view.View.resolveSize;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dev.nimrod.locafi.models.WifiPoint;

public class LocationView extends View {
    private final List<WifiPoint> wifiPoints;
    private final Paint pointPaint;
    private final Paint textPaint;
    private final Paint rangePaint;
    private final RectF tempRect;

    // Constants for visualization
    private static final float MAX_RADIUS = 300f;
    private static final float MIN_RADIUS = 20f;
    private static final float TEXT_SIZE = 30f;
    private static final float POINT_RADIUS = 15f;
    private static final int MAX_SIGNAL_STRENGTH = -30; // dBm
    private static final int MIN_SIGNAL_STRENGTH = -90; // dBm

    public LocationView(Context context) {
        super(context);
        wifiPoints = new ArrayList<>();

        // Initialize paint objects
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.CENTER);

        rangePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rangePaint.setStyle(Paint.Style.STROKE);
        rangePaint.setStrokeWidth(2f);

        tempRect = new RectF();
    }

    public void addWifiPoint(@NonNull WifiPoint point) {
        wifiPoints.add(point);
        invalidate(); // Trigger redraw
    }

    public void clearPoints() {
        wifiPoints.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (wifiPoints.isEmpty()) {
            return;
        }

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Draw device position (center point)
        pointPaint.setColor(Color.BLACK);
        canvas.drawCircle(centerX, centerY, POINT_RADIUS, pointPaint);
        canvas.drawText("Your Device", centerX, centerY - 30f, textPaint);

        // Draw WiFi points
        for (int i = 0; i < wifiPoints.size(); i++) {
            WifiPoint point = wifiPoints.get(i);
            drawWifiPoint(canvas, point, centerX, centerY, i);
        }
    }

    private void drawWifiPoint(Canvas canvas, WifiPoint point, float centerX, float centerY, int index) {
        // Calculate position based on signal strength and index
        double angle = (2 * Math.PI * index) / wifiPoints.size();
        float radius = calculateRadius(point.getRssi());

        float x = centerX + (float) (radius * Math.cos(angle));
        float y = centerY + (float) (radius * Math.sin(angle));

        // Draw signal range circle
        rangePaint.setColor(getSignalColor(point.getRssi()));
        rangePaint.setAlpha(50);
        tempRect.set(x - radius/2, y - radius/2, x + radius/2, y + radius/2);
        canvas.drawOval(tempRect, rangePaint);

        // Draw point
        pointPaint.setColor(getSignalColor(point.getRssi()));
        canvas.drawCircle(x, y, POINT_RADIUS, pointPaint);

        // Draw SSID and signal strength
        String text = String.format("%s\n%d dBm", point.getSsid(), point.getRssi());
        canvas.drawText(text, x, y - 20f, textPaint);
    }

    private float calculateRadius(int rssi) {
        // Convert RSSI to a radius between MIN_RADIUS and MAX_RADIUS
        float signalRange = MIN_SIGNAL_STRENGTH - MAX_SIGNAL_STRENGTH;
        float normalizedSignal = (rssi - MAX_SIGNAL_STRENGTH) / signalRange;
        return MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * normalizedSignal;
    }

    private int getSignalColor(int rssi) {
        // Return color based on signal strength (red = weak, green = strong)
        float signalRange = MIN_SIGNAL_STRENGTH - MAX_SIGNAL_STRENGTH;
        float normalizedSignal = (rssi - MAX_SIGNAL_STRENGTH) / signalRange;

        int red = (int) (255 * (1 - normalizedSignal));
        int green = (int) (255 * normalizedSignal);
        return Color.rgb(red, green, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int desiredWidth = getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight();
        int desiredHeight = getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(
                resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec)
        );
    }
}
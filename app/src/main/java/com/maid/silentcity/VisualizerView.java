package com.maid.silentcity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom View для відображення динамічних хвиль шуму, подібних до ефекту Shazam.
 * Анімація залежить від отриманого рівня амплітуди.
 */
public class VisualizerView extends View {

    private Paint wavePaint;
    private float amplitude = 0f; // Поточна сира амплітуда (getMaxAmplitude())
    private float normalizedAmplitude = 0f; // Нормалізована амплітуда (0..1)
    private Handler handler = new Handler(Looper.getMainLooper());

    // Константи для контролю анімації
    // ЗМЕНШЕНО для збільшення чутливості до звичайних звуків
    private static final int MAX_AMPLITUDE = 20000;
    private static final int NUM_WAVES = 5;
    // ЗБІЛЬШЕНО: Кола можуть розширюватися далі
    private static final float MAX_SCALE_FACTOR = 1.5f;
    private static final long ANIMATION_INTERVAL_MS = 30;
    private static final float BASE_RADIUS_MULTIPLIER = 0.7f;

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        wavePaint = new Paint();
        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setAntiAlias(true);

        handler.post(animationRunnable);
    }

    /**
     * Встановлює нове значення амплітуди, отримане від MediaRecorder.
     * @param rawAmplitude Сире значення амплітуди (getMaxAmplitude()).
     */
    public void setAmplitude(int rawAmplitude) {
        this.amplitude = rawAmplitude;

        // Нормалізуємо амплітуду до діапазону 0.0 - 1.0.
        // ВИПРАВЛЕНО: Використовуємо Math.cbrt (кубічний корінь) для більш агресивної чутливості
        this.normalizedAmplitude = (float) Math.cbrt(Math.min(1.0f, this.amplitude / MAX_AMPLITUDE));

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        float baseRadius = Math.min(centerX, centerY) * BASE_RADIUS_MULTIPLIER;

        long currentTime = System.currentTimeMillis();

        // Масштаб від амплітуди
        float noiseScale = normalizedAmplitude;

        // 1. Малюємо центральне пульсуюче коло
        wavePaint.setColor(Color.rgb(52, 66, 120));
        wavePaint.setAlpha(255);
        canvas.drawCircle(centerX, centerY, baseRadius * (1.0f + noiseScale * 0.1f), wavePaint);


        // 2. Малюємо розбіжні хвилі
        for (int i = 0; i < NUM_WAVES; i++) {

            // Якщо шум занадто тихий, не малюємо розбіжні кола
            if (noiseScale < 0.05f) continue;

            // Фаза: час розбіжності. Цикл 2000 мс.
            float phase = ((currentTime % 2000) / 2000.0f) + (i * (1.0f / NUM_WAVES));
            if (phase > 1.0f) phase -= 1.0f;

            // Радіус: починається від baseRadius і розширюється.
            // ВИПРАВЛЕНО: Максимальний радіус збільшено через MAX_SCALE_FACTOR = 1.5f
            float maxWaveRadius = baseRadius * (1.0f + MAX_SCALE_FACTOR * noiseScale);
            float radius = baseRadius + (maxWaveRadius - baseRadius) * phase;

            // Прозорість: коло зникає (альфа зменшується), коли воно розширюється.
            // ПРОЗОРІСТЬ ЗБІЛЬШЕНО: Множник 200 замість 150 для кращої видимості
            int alpha = (int) (200 * noiseScale * (1.0f - phase));

            wavePaint.setColor(Color.rgb(52, 66, 120));
            wavePaint.setAlpha(alpha);

            canvas.drawCircle(centerX, centerY, radius, wavePaint);
        }
    }

    // Runnable для циклу анімації
    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            // Перемальовуємо View для руху хвиль
            invalidate();
            handler.postDelayed(this, ANIMATION_INTERVAL_MS);
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(animationRunnable);
    }
}
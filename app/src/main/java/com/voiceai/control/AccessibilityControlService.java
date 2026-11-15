
package com.voiceai.control;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.util.DisplayMetrics;

public class AccessibilityControlService extends AccessibilityService {

    private BroadcastReceiver receiver;
    private boolean receiverRegistered = false;

    private static final String COMMAND_ACTION =
            "com.voiceai.control.COMMAND";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        if (receiverRegistered) return;

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent == null) return;

                String command =
                        intent.getStringExtra("command");

                if (command != null) {
                    executeCommand(command.trim().toUpperCase());
                }
            }
        };

        IntentFilter filter =
                new IntentFilter(COMMAND_ACTION);

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(
                        receiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );
            } else {
                registerReceiver(receiver, filter);
            }

            receiverRegistered = true;

        } catch (Exception ignored) {
            receiverRegistered = false;
        }
    }

    private void executeCommand(String command) {

        if (command == null || command.isEmpty()) return;

        switch (command) {

            case "BACK":
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;

            case "HOME":
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;

            case "RECENTS":
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;

            case "SCROLL_DOWN":
                scrollScreen(false);
                break;

            case "SCROLL_UP":
                scrollScreen(true);
                break;

            case "PLAY_PAUSE":
                sendMediaKey(
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                );
                break;

            case "NEXT":
                sendMediaKey(
                        KeyEvent.KEYCODE_MEDIA_NEXT
                );
                break;

            case "PREVIOUS":
                sendMediaKey(
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS
                );
                break;

            case "FAST_FORWARD":
                sendMediaKey(
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                );
                break;

            case "REWIND":
                sendMediaKey(
                        KeyEvent.KEYCODE_MEDIA_REWIND
                );
                break;
        }
    }

    private void sendMediaKey(int keyCode) {

        AudioManager audio =
                (AudioManager) getSystemService(
                        AUDIO_SERVICE
                );

        if (audio == null) return;

        long downTime = SystemClock.uptimeMillis();

        KeyEvent down = new KeyEvent(
                downTime,
                downTime,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0
        );

        KeyEvent up = new KeyEvent(
                downTime,
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP,
                keyCode,
                0
        );

        try {
            audio.dispatchMediaKeyEvent(down);
            audio.dispatchMediaKeyEvent(up);
        } catch (Exception ignored) {
        }
    }

    private void scrollScreen(boolean scrollUp) {

        DisplayMetrics metrics =
                getResources().getDisplayMetrics();

        float x = metrics.widthPixels / 2f;
        float top = metrics.heightPixels * 0.25f;
        float bottom = metrics.heightPixels * 0.75f;

        Path path = new Path();

        if (scrollUp) {
            path.moveTo(x, bottom);
            path.lineTo(x, top);
        } else {
            path.moveTo(x, top);
            path.lineTo(x, bottom);
        }

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(
                        path,
                        0,
                        450
                );

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        try {
            dispatchGesture(gesture, null, null);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // इस ऐप को स्क्रीन की सामग्री पढ़ने की जरूरत नहीं है।
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {

        if (receiver != null && receiverRegistered) {

            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
        }

        receiver = null;
        receiverRegistered = false;

        super.onDestroy();
    }
}

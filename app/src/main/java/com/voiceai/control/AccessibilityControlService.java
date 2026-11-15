
package com.voiceai.control;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.util.DisplayMetrics;

public class AccessibilityControlService extends AccessibilityService {

    private BroadcastReceiver receiver;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra("command");

                if (command != null) {
                    executeCommand(command);
                }
            }
        };

        IntentFilter filter =
            new IntentFilter("com.voiceai.control.COMMAND");

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(receiver, filter);
        }
    }

    private void executeCommand(String command) {

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
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;

            case "NEXT":
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;

            case "PREVIOUS":
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;

            case "FAST_FORWARD":
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                break;

            case "REWIND":
                sendMediaKey(KeyEvent.KEYCODE_MEDIA_REWIND);
                break;
        }
    }

    private void sendMediaKey(int keyCode) {

        AudioManager audio =
            (AudioManager) getSystemService(AUDIO_SERVICE);

        if (audio == null) return;

        long downTime = android.os.SystemClock.uptimeMillis();

        KeyEvent down = new KeyEvent(
            downTime,
            downTime,
            KeyEvent.ACTION_DOWN,
            keyCode,
            0
        );

        KeyEvent up = new KeyEvent(
            downTime,
            android.os.SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP,
            keyCode,
            0
        );

        audio.dispatchMediaKeyEvent(down);
        audio.dispatchMediaKeyEvent(up);
    }

    private void scrollScreen(boolean up) {

        DisplayMetrics metrics =
            getResources().getDisplayMetrics();

        float x = metrics.widthPixels / 2f;
        float top = metrics.heightPixels * 0.25f;
        float bottom = metrics.heightPixels * 0.75f;

        Path path = new Path();

        if (up) {
            path.moveTo(x, top);
            path.lineTo(x, bottom);
        } else {
            path.moveTo(x, bottom);
            path.lineTo(x, top);
        }

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(
                path,
                0,
                500
            );

        GestureDescription gesture =
            new GestureDescription.Builder()
                .addStroke(stroke)
                .build();

        dispatchGesture(gesture, null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {

        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
            receiver = null;
        }

        super.onDestroy();
    }
}

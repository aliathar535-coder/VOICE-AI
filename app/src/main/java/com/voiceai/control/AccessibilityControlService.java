
package com.voiceai.control;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Bundle;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

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

        IntentFilter filter = new IntentFilter("com.voiceai.control.COMMAND");

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
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
        }
    }

    private void sendMediaKey(int keyCode) {
        AudioManager audio =
            (AudioManager) getSystemService(AUDIO_SERVICE);

        long now = android.os.SystemClock.uptimeMillis();

        audio.dispatchMediaKeyEvent(
            new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        );

        audio.dispatchMediaKeyEvent(
            new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
        );
    }

    private void scrollScreen(boolean up) {
        Path path = new Path();

        if (up) {
            path.moveTo(500, 300);
            path.lineTo(500, 1100);
        } else {
            path.moveTo(500, 1100);
            path.lineTo(500, 300);
        }

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(path, 0, 500);

        GestureDescription gesture =
            new GestureDescription.Builder().addStroke(stroke).build();

        dispatchGesture(gesture, null, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        super.onDestroy();
    }
}

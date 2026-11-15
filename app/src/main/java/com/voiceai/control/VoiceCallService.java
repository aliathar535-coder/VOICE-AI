
package com.voiceai.control;

import android.telecom.Call;
import android.telecom.InCallService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class VoiceCallService extends InCallService {

    public static volatile Call currentCall;

    private static final String CHANNEL_ID = "voice_ai_calls";
    private static final int NOTIFICATION_ID = 701;

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        currentCall = call;

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call changedCall, int state) {
                if (state == Call.STATE_DISCONNECTED) {
                    removeCallNotification();
                } else {
                    showCallNotification(changedCall);
                }
            }
        });

        showCallNotification(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);

        if (currentCall == call) {
            currentCall = null;
        }

        removeCallNotification();
    }

    private void showCallNotification(Call call) {
        createChannel();

        Intent openIntent = new Intent(this, IncomingCallActivity.class);
        openIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent fullScreenIntent = PendingIntent.getActivity(
            this,
            701,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT |
            PendingIntent.FLAG_IMMUTABLE
        );

        String caller = "अज्ञात नंबर";

        if (call.getDetails() != null &&
            call.getDetails().getHandle() != null) {
            caller = call.getDetails().getHandle()
                .getSchemeSpecificPart();
        }

        boolean ringing = call.getState() == Call.STATE_RINGING;

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(ringing ? "आने वाली कॉल" : "कॉल जारी है")
            .setContentText(caller)
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(Notification.PRIORITY_MAX)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent);

        if (ringing) {
            builder.setFullScreenIntent(fullScreenIntent, true);
        }

        NotificationManager manager =
            (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE
            );

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                new NotificationChannel(
                    CHANNEL_ID,
                    "VOICE AI Phone Calls",
                    NotificationManager.IMPORTANCE_HIGH
                );

            channel.setDescription(
                "आने वाली फोन कॉल की सूचना"
            );

            channel.setLockscreenVisibility(
                Notification.VISIBILITY_PUBLIC
            );

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void removeCallNotification() {
        NotificationManager manager =
            (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE
            );

        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }
}

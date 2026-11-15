package com.voiceai.control;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class HandsFreeListeningService extends Service
        implements RecognitionListener {

    public static final String ACTION_STOP =
            "com.voiceai.control.STOP_HANDS_FREE";

    private static final String CHANNEL_ID = "voice_ai_listening";
    private static final int NOTIFICATION_ID = 501;

    private SpeechRecognizer recognizer;
    private Handler handler;
    private CustomCommandStore commandStore;

    private boolean running = false;
    private boolean waitingForCommand = false;

    private final Runnable restartTask = () -> {
        if (running) {
            startListening();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        commandStore = new CustomCommandStore(this);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null &&
                ACTION_STOP.equals(intent.getAction())) {
            stopListeningService();
            return START_NOT_STICKY;
        }

        if (running) {
            return START_STICKY;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            startForegroundNotification();

            running = true;
            waitingForCommand = false;

            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);

            handler.postDelayed(restartTask, 500);

        } catch (Exception e) {
            stopListeningService();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "VOICE AI Hands-Free",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription(
                    "VOICE AI आवाज़ सुनने की सेवा"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundNotification() {

        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("VOICE AI चालू है")
                    .setContentText(
                            "जगाने के लिए बोलें: VOICE AI"
                    )
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setOngoing(true)
                    .build();

        } else {

            notification = new Notification.Builder(this)
                    .setContentTitle("VOICE AI चालू है")
                    .setContentText("आवाज़ सुन रहा हूँ")
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setOngoing(true)
                    .build();
        }

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void startListening() {

        if (!running || recognizer == null) {
            return;
        }

        try {
            Intent intent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "hi-IN"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    "hi-IN"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
            );

            recognizer.startListening(intent);

        } catch (Exception e) {
            scheduleRestart(2000);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onError(int error) {

        if (!running) return;

        scheduleRestart(1500);
    }

    @Override
    public void onResults(Bundle results) {

        if (!running) return;

        ArrayList<String> matches =
                results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                );

        if (matches != null && !matches.isEmpty()) {

            String spoken = matches.get(0).trim();

            processSpokenText(spoken);
        }

        scheduleRestart(900);
    }

    private void processSpokenText(String spoken) {

        String compact = spoken
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}]", "");

        boolean wakeWord =
                compact.contains("voiceai") ||
                compact.contains("वॉइसएआई") ||
                compact.contains("वॉइसआई");

        if (!waitingForCommand) {

            if (wakeWord) {

                waitingForCommand = true;

                String remaining = spoken
                        .replaceAll("(?i)voice\\s*ai", "")
                        .replace("वॉइस एआई", "")
                        .replace("वॉइसएआई", "")
                        .replace("वॉइसआई", "")
                        .trim();

                if (!remaining.isEmpty()) {
                    executeSpokenCommand(remaining);
                    waitingForCommand = false;
                }
            }

            return;
        }

        waitingForCommand = false;

        executeSpokenCommand(spoken);
    }

    private void executeSpokenCommand(String spoken) {

        String command = spoken
                .trim()
                .toLowerCase(Locale.ROOT);

        if (command.isEmpty()) return;

        String customAction = commandStore.findAction(command);

        if (customAction != null) {
            executeAction(customAction);
            return;
        }

        if (command.contains("यूट्यूब")) {
            executeAction("YOUTUBE");
        }
        else if (command.contains("व्हाट्सएप") ||
                 command.contains("व्हाट्सऐप")) {
            executeAction("WHATSAPP");
        }
        else if (command.contains("एमएक्स प्लेयर") ||
                 command.contains("एम एक्स प्लेयर")) {
            executeAction("MX_PLAYER");
        }
        else if (command.contains("आवाज़ बढ़ाओ") ||
                 command.contains("वॉल्यूम बढ़ाओ")) {
            executeAction("VOLUME_UP");
        }
        else if (command.contains("आवाज़ कम") ||
                 command.contains("वॉल्यूम कम")) {
            executeAction("VOLUME_DOWN");
        }
        else if (command.contains("अगला गाना") ||
                 command.equals("अगला")) {
            executeAction("NEXT");
        }
        else if (command.contains("पिछला गाना") ||
                 command.equals("पिछला")) {
            executeAction("PREVIOUS");
        }
        else if (command.contains("आगे चलाओ") ||
                 command.contains("फास्ट फॉरवर्ड")) {
            executeAction("FAST_FORWARD");
        }
        else if (command.contains("पीछे चलाओ") ||
                 command.contains("रिवाइंड")) {
            executeAction("REWIND");
        }
        else if (command.contains("चलाओ") ||
                 command.contains("रोक दो") ||
                 command.contains("पॉज")) {
            executeAction("PLAY_PAUSE");
        }
        else if (command.contains("नीचे स्क्रॉल")) {
            executeAction("SCROLL_DOWN");
        }
        else if (command.contains("ऊपर स्क्रॉल")) {
            executeAction("SCROLL_UP");
        }
        else if (command.contains("वापस जाओ")) {
            executeAction("BACK");
        }
        else if (command.contains("होम पर जाओ")) {
            executeAction("HOME");
        }
        else if (command.contains("हाल के ऐप")) {
            executeAction("RECENTS");
        }
    }

    private void executeAction(String action) {

        switch (action) {

            case "YOUTUBE":
                openApp("com.google.android.youtube");
                break;

            case "WHATSAPP":
                openApp("com.whatsapp");
                break;

            case "MX_PLAYER":
                openMXPlayer();
                break;

            case "VOLUME_UP":
                changeVolume(true);
                break;

            case "VOLUME_DOWN":
                changeVolume(false);
                break;

            case "PLAY_PAUSE":
            case "NEXT":
            case "PREVIOUS":
            case "FAST_FORWARD":
            case "REWIND":
            case "SCROLL_UP":
            case "SCROLL_DOWN":
            case "BACK":
            case "HOME":
            case "RECENTS":
                sendAccessibilityCommand(action);
                break;
        }
    }

    private void sendAccessibilityCommand(String command) {

        Intent intent = new Intent(
                "com.voiceai.control.COMMAND"
        );

        intent.setPackage(getPackageName());
        intent.putExtra("command", command);

        sendBroadcast(intent);
    }

    private void changeVolume(boolean increase) {

        AudioManager audio =
                (AudioManager) getSystemService(
                        Context.AUDIO_SERVICE
                );

        if (audio == null) return;

        audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                increase
                        ? AudioManager.ADJUST_RAISE
                        : AudioManager.ADJUST_LOWER,
                0
        );
    }

    private void openApp(String packageName) {

        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(packageName);

        if (intent != null) {

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void openMXPlayer() {

        String[] packages = {
                "com.mxtech.videoplayer.ad",
                "com.mxtech.videoplayer.pro"
        };

        for (String packageName : packages) {

            Intent intent = getPackageManager()
                    .getLaunchIntentForPackage(packageName);

            if (intent != null) {

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
        }
    }

    private void scheduleRestart(long delay) {

        if (!running || handler == null) return;

        handler.removeCallbacks(restartTask);
        handler.postDelayed(restartTask, delay);
    }

    private void stopListeningService() {

        running = false;
        waitingForCommand = false;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (recognizer != null) {

            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception ignored) {
            }

            recognizer = null;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }

        stopSelf();
    }

    @Override
    public void onDestroy() {

        running = false;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (recognizer != null) {

            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception ignored) {
            }

            recognizer = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
              }

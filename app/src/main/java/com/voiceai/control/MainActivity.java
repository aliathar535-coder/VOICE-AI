
package com.voiceai.control;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.media.AudioManager;
import android.content.Context;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView status, result;
    Button micButton, accessButton;
    SpeechRecognizer recognizer;
    AudioManager audio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(25, 35, 25, 35);
        layout.setBackgroundColor(Color.rgb(7, 13, 30));

        TextView title = new TextView(this);
        title.setText("VOICE AI");
        title.setTextSize(36);
        title.setTextColor(Color.CYAN);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("● READY TO LISTEN");
        status.setTextColor(Color.GREEN);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(10, 30, 10, 30);

        micButton = new Button(this);
        micButton.setText("🎙 START VOICE CONTROL");

        accessButton = new Button(this);
        accessButton.setText("ENABLE MOBILE CONTROL");

        result = new TextView(this);
        result.setText("Say a command...");
        result.setTextColor(Color.WHITE);
        result.setTextSize(17);
        result.setGravity(Gravity.CENTER);
        result.setPadding(10, 30, 10, 30);

        layout.addView(title);
        layout.addView(status);
        layout.addView(micButton);
        layout.addView(accessButton);
        layout.addView(result);

        setContentView(layout);

        accessButton.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );

        micButton.setOnClickListener(v -> checkPermission());

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupRecognizer();
        } else {
            status.setText("Speech recognition unavailable");
        }
    }

    private void checkPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, 101
            );
        } else {
            listen();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        if (requestCode == 101 && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            listen();
        } else {
            status.setText("Microphone permission required");
        }
    }

    private void setupRecognizer() {
        recognizer.setRecognitionListener(new RecognitionListener() {

            public void onReadyForSpeech(Bundle b) {
                status.setText("● LISTENING...");
                status.setTextColor(Color.YELLOW);
            }

            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}

            public void onError(int error) {
                status.setText("● READY");
                status.setTextColor(Color.GREEN);
            }

            public void onResults(Bundle b) {
                ArrayList<String> list =
                    b.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );

                if (list != null && !list.isEmpty()) {
                    String command = list.get(0).toLowerCase(Locale.ROOT);
                    result.setText("You said: " + command);
                    runCommand(command);
                }

                status.setText("● READY");
                status.setTextColor(Color.GREEN);
            }

            public void onPartialResults(Bundle b) {}
            public void onEvent(int e, Bundle b) {}
        });
    }

    private void listen() {
        if (recognizer == null) return;

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        recognizer.startListening(i);
    }

    private void runCommand(String c) {

        if (c.contains("volume") &&
            (c.contains("badhao") || c.contains("badhाओ")
             || c.contains("up"))) {

            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            );
            result.setText("Volume increased");
        }

        else if (c.contains("volume") &&
            (c.contains("kam") || c.contains("down"))) {

            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            );
            result.setText("Volume decreased");
        }

        else if (c.contains("youtube")) {
            openApp("com.google.android.youtube");
        }

        else if (c.contains("whatsapp")) {
            openApp("com.whatsapp");
        }

        else if (c.contains("mx player") || c.contains("mxplayer")) {
            openApp("com.mxtech.videoplayer.ad");
        }

        else if (c.contains("play") || c.contains("pause")
                 || c.contains("रोक") || c.contains("चलाओ")) {
            sendCommand("PLAY_PAUSE");
        }

        else if (c.contains("next") || c.contains("अगला")) {
            sendCommand("NEXT");
        }

        else if (c.contains("previous") || c.contains("पिछला")) {
            sendCommand("PREVIOUS");
        }

        else if (c.contains("आगे") || c.contains("fast forward")) {
            sendCommand("FAST_FORWARD");
        }

        else if (c.contains("पीछे") || c.contains("rewind")) {
            sendCommand("REWIND");
        }

        else if (c.contains("back")) {
            sendCommand("BACK");
        }

        else if (c.contains("home")) {
            sendCommand("HOME");
        }

        else if (c.contains("recent")) {
            sendCommand("RECENTS");
        }

        else if (c.contains("scroll down")) {
            sendCommand("SCROLL_DOWN");
        }

        else if (c.contains("scroll up")) {
            sendCommand("SCROLL_UP");
        }

        else {
            result.setText("Command not recognized");
        }
    }

    private void sendCommand(String command) {
        Intent i = new Intent("com.voiceai.control.COMMAND");
        i.setPackage(getPackageName());
        i.putExtra("command", command);
        sendBroadcast(i);
        result.setText("Command sent: " + command);
    }

    private void openApp(String packageName) {
        Intent i = getPackageManager().getLaunchIntentForPackage(packageName);

        if (i != null) {
            startActivity(i);
            result.setText("Opening app...");
        } else {
            result.setText("App not installed");
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        super.onDestroy();
    }
}

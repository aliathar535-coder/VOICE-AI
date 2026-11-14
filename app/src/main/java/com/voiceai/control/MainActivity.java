
package com.voiceai.control;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.media.AudioManager;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView status;
    TextView result;
    Button micButton;
    SpeechRecognizer speechRecognizer;
    AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audioManager = (AudioManager)
                getSystemService(Context.AUDIO_SERVICE);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setGravity(Gravity.CENTER);
        main.setPadding(28, 35, 28, 35);
        main.setBackgroundColor(Color.rgb(7, 13, 30));

        TextView title = new TextView(this);
        title.setText("VOICE AI");
        title.setTextSize(34);
        title.setTextColor(Color.CYAN);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("HANDS FREE MOBILE CONTROL");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("● READY TO LISTEN");
        status.setTextColor(Color.GREEN);
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);
        status.setPadding(10, 35, 10, 25);

        micButton = new Button(this);
        micButton.setText("🎙 START VOICE");
        micButton.setTextSize(20);

        result = new TextView(this);
        result.setText("Your voice command will appear here");
        result.setTextColor(Color.WHITE);
        result.setTextSize(17);
        result.setGravity(Gravity.CENTER);
        result.setPadding(15, 35, 15, 35);

        TextView help = new TextView(this);
        help.setText(
            "Try saying:\n\n" +
            "Volume badhao\n" +
            "Volume kam karo\n" +
            "YouTube kholo\n" +
            "WhatsApp kholo\n" +
            "Flashlight on\n" +
            "Flashlight off"
        );
        help.setTextColor(Color.LTGRAY);
        help.setTextSize(16);
        help.setGravity(Gravity.CENTER);

        main.addView(title);
        main.addView(subtitle);
        main.addView(status);
        main.addView(micButton);
        main.addView(result);
        main.addView(help);

        setContentView(main);

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupSpeech();
        } else {
            status.setText("Speech recognition not available");
        }

        micButton.setOnClickListener(v -> startListening());
    }

    private void setupSpeech() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("● LISTENING...");
                status.setTextColor(Color.YELLOW);
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                status.setText("● PROCESSING...");
            }

            @Override
            public void onError(int error) {
                status.setText("● READY");
                status.setTextColor(Color.GREEN);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> words =
                    results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );

                if (words != null && !words.isEmpty()) {
                    String command = words.get(0).toLowerCase(Locale.ROOT);
                    result.setText("You said: " + command);
                    runCommand(command);
                }

                status.setText("● READY");
                status.setTextColor(Color.GREEN);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (speechRecognizer == null) return;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                        "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                        "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        speechRecognizer.startListening(intent);
    }

    private void runCommand(String command) {

        if (command.contains("volume") &&
            (command.contains("badhao") ||
             command.contains("up"))) {

            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            );
            result.setText("Volume increased");
        }

        else if (command.contains("volume") &&
                (command.contains("kam") ||
                 command.contains("down"))) {

            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            );
            result.setText("Volume decreased");
        }

        else if (command.contains("youtube")) {
            openApp("com.google.android.youtube");
        }

        else if (command.contains("whatsapp")) {
            openApp("com.whatsapp");
        }

        else if (command.contains("mx player") ||
                 command.contains("mxplayer")) {
            openApp("com.mxtech.videoplayer.ad");
        }

        else {
            result.setText("Command not supported yet");
        }
    }

    private void openApp(String packageName) {
        Intent launch = getPackageManager()
            .getLaunchIntentForPackage(packageName);

        if (launch != null) {
            startActivity(launch);
            result.setText("Opening app...");
        } else {
            result.setText("App not installed");
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
    }

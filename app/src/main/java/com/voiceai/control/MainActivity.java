
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

        TextView subtitle = new TextView(this);
        subtitle.setText("आवाज़ से मोबाइल चलाएँ");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("● सुनने के लिए तैयार");
        status.setTextColor(Color.GREEN);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(10, 25, 10, 25);

        micButton = new Button(this);
        micButton.setText("🎙 आवाज़ से चलाना शुरू करें");

        accessButton = new Button(this);
        accessButton.setText("मोबाइल कंट्रोल की अनुमति दें");

        result = new TextView(this);
        result.setText("अपना आदेश बोलें...");
        result.setTextColor(Color.WHITE);
        result.setTextSize(17);
        result.setGravity(Gravity.CENTER);
        result.setPadding(10, 25, 10, 25);

        layout.addView(title);
        layout.addView(subtitle);
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
            status.setText("आवाज़ पहचानने की सुविधा उपलब्ध नहीं है");
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
            status.setText("माइक्रोफोन की अनुमति जरूरी है");
        }
    }

    private void setupRecognizer() {
        recognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle b) {
                status.setText("● सुन रहा हूँ...");
                status.setTextColor(Color.YELLOW);
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float r) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                status.setText("● फिर से शुरू करें");
                status.setTextColor(Color.GREEN);
            }

            @Override
            public void onResults(Bundle b) {
                ArrayList<String> list =
                    b.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );

                if (list != null && !list.isEmpty()) {
                    String command = list.get(0).toLowerCase(Locale.ROOT);
                    result.setText("आपने कहा: " + command);
                    runCommand(command);
                }

                status.setText("● सुनने के लिए तैयार");
                status.setTextColor(Color.GREEN);
            }

            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int e, Bundle b) {}
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

        if (c.contains("आवाज़ बढ़ाओ") || c.contains("वॉल्यूम बढ़ाओ")) {
            changeVolume(true);
        }

        else if (c.contains("आवाज़ कम करो") || c.contains("वॉल्यूम कम करो")) {
            changeVolume(false);
        }

        else if (c.contains("यूट्यूब")) {
            openApp("com.google.android.youtube");
        }

        else if (c.contains("व्हाट्सऐप") || c.contains("व्हाट्सएप")) {
            openApp("com.whatsapp");
        }

        else if (c.contains("एमएक्स प्लेयर") || c.contains("एम एक्स प्लेयर")) {
            openMXPlayer();
        }

        else if (c.contains("आगे चलाओ") || c.contains("फास्ट फॉरवर्ड")) {
            sendCommand("FAST_FORWARD");
        }

        else if (c.contains("पीछे चलाओ") || c.contains("रिवाइंड")) {
            sendCommand("REWIND");
        }

        else if (c.contains("अगला गाना") || c.contains("अगला")) {
            sendCommand("NEXT");
        }

        else if (c.contains("पिछला गाना") || c.contains("पिछला")) {
            sendCommand("PREVIOUS");
        }

        else if (c.contains("चलाओ") || c.contains("रोक दो") || c.contains("पॉज")) {
            sendCommand("PLAY_PAUSE");
        }

        else if (c.contains("नीचे स्क्रॉल")) {
            sendCommand("SCROLL_DOWN");
        }

        else if (c.contains("ऊपर स्क्रॉल")) {
            sendCommand("SCROLL_UP");
        }

        else if (c.contains("वापस जाओ")) {
            sendCommand("BACK");
        }

        else if (c.contains("होम पर जाओ")) {
            sendCommand("HOME");
        }

        else if (c.contains("हाल के ऐप")) {
            sendCommand("RECENTS");
        }

        else {
            result.setText("यह आदेश समझ नहीं आया");
        }
    }

    private void changeVolume(boolean increase) {
        if (audio == null) return;

        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        );

        result.setText(increase ? "आवाज़ बढ़ा दी" : "आवाज़ कम कर दी");
    }

    private void sendCommand(String command) {
        Intent i = new Intent("com.voiceai.control.COMMAND");
        i.setPackage(getPackageName());
        i.putExtra("command", command);
        sendBroadcast(i);
        result.setText("आदेश भेज दिया: " + command);
    }

    private void openApp(String packageName) {
        Intent i = getPackageManager().getLaunchIntentForPackage(packageName);

        if (i != null) {
            startActivity(i);
            result.setText("ऐप खोल रहा हूँ...");
        } else {
            result.setText("यह ऐप फोन में नहीं मिला");
        }
    }

    private void openMXPlayer() {
        String[] packages = {
            "com.mxtech.videoplayer.ad",
            "com.mxtech.videoplayer.pro"
        };

        for (String p : packages) {
            Intent i = getPackageManager().getLaunchIntentForPackage(p);

            if (i != null) {
                startActivity(i);
                result.setText("एमएक्स प्लेयर खोल रहा हूँ...");
                return;
            }
        }

        result.setText("एमएक्स प्लेयर फोन में नहीं मिला");
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        super.onDestroy();
    }
}

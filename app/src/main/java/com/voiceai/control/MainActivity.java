
package com.voiceai.control;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.media.AudioManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView status, result, history;
    private Button micButton;
    private SpeechRecognizer recognizer;
    private AudioManager audio;
    private LinearLayout mainLayout;
    private boolean listening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setPadding(24, 35, 24, 25);
        mainLayout.setBackgroundColor(Color.rgb(7, 13, 30));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(mainLayout);

        TextView title = new TextView(this);
        title.setText("VOICE AI");
        title.setTextSize(36);
        title.setTextColor(Color.rgb(0, 220, 255));
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        add(title, 75);

        TextView subtitle = new TextView(this);
        subtitle.setText("आवाज़ से मोबाइल चलाएँ");
        subtitle.setTextSize(18);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);
        add(subtitle, 55);

        status = new TextView(this);
        status.setText("● तैयार हूँ");
        status.setTextSize(17);
        status.setTextColor(Color.GREEN);
        status.setGravity(Gravity.CENTER);
        add(status, 65);

        micButton = new Button(this);
        micButton.setText("🎙 बोलना शुरू करें");
        micButton.setTextSize(18);
        add(micButton, 75);

        micButton.setOnClickListener(v -> checkPermission());

        result = new TextView(this);
        result.setText("आपका आदेश यहाँ दिखाई देगा");
        result.setTextColor(Color.WHITE);
        result.setTextSize(17);
        result.setGravity(Gravity.CENTER);
        add(result, 75);

        TextView commands = new TextView(this);
        commands.setText(
            "आवाज़ के आदेश\n\n" +
            "• आवाज़ बढ़ाओ\n" +
            "• आवाज़ कम करो\n" +
            "• YouTube खोलो\n" +
            "• WhatsApp खोलो\n" +
            "• MX Player खोलो\n" +
            "• अगला गाना\n" +
            "• पिछला गाना\n" +
            "• रोक दो / चलाओ\n" +
            "• आगे चलाओ / पीछे चलाओ\n" +
            "• ऊपर स्क्रॉल / नीचे स्क्रॉल\n" +
            "• वापस जाओ\n" +
            "• होम पर जाओ\n" +
            "• हाल के ऐप दिखाओ"
        );
        commands.setTextColor(Color.LTGRAY);
        commands.setTextSize(15);
        commands.setGravity(Gravity.CENTER);
        add(commands, 300);

        Button accessButton = new Button(this);
        accessButton.setText("मोबाइल कंट्रोल सेटिंग");
        add(accessButton, 60);
        accessButton.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );

        history = new TextView(this);
        history.setText("कमांड हिस्ट्री\nअभी कोई आदेश नहीं");
        history.setTextColor(Color.CYAN);
        history.setTextSize(15);
        history.setGravity(Gravity.CENTER);
        add(history, 100);

        setContentView(scroll);

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupRecognizer();
        } else {
            status.setText("इस फोन में आवाज़ पहचान उपलब्ध नहीं है");
        }
    }

    private void add(View view, int height) {
        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height
            );
        params.setMargins(0, 8, 0, 8);
        mainLayout.addView(view, params);
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
            status.setText("माइक्रोफोन अनुमति नहीं मिली");
        }
    }

    private void setupRecognizer() {
        recognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                listening = true;
                status.setText("● सुन रहा हूँ...");
                status.setTextColor(Color.YELLOW);
                micButton.setText("🎙 सुन रहा हूँ");
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                listening = false;
                status.setText("● फिर से बोलने के लिए बटन दबाएँ");
                status.setTextColor(Color.GREEN);
                micButton.setText("🎙 बोलना शुरू करें");
            }

            @Override
            public void onResults(Bundle results) {
                listening = false;

                ArrayList<String> matches =
                    results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    );

                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase(Locale.ROOT);
                    result.setText("आपने कहा: " + command);
                    history.setText("आखिरी आदेश:\n" + command);
                    runCommand(command);
                }

                status.setText("● तैयार हूँ");
                status.setTextColor(Color.GREEN);
                micButton.setText("🎙 बोलना शुरू करें");
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void listen() {
        if (recognizer == null || listening) return;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        recognizer.startListening(intent);
    }

    private void runCommand(String c) {

        if (c.contains("आवाज़ बढ़ाओ") || c.contains("वॉल्यूम बढ़ाओ")) {
            changeVolume(true);
        }
        else if (c.contains("आवाज़ कम") || c.contains("वॉल्यूम कम")) {
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
        else if (c.contains("अगला गाना") || c.equals("अगला")) {
            sendCommand("NEXT");
        }
        else if (c.contains("पिछला गाना") || c.equals("पिछला")) {
            sendCommand("PREVIOUS");
        }
        else if (c.contains("फास्ट फॉरवर्ड") || c.contains("आगे चलाओ")) {
            sendCommand("FAST_FORWARD");
        }
        else if (c.contains("रिवाइंड") || c.contains("पीछे चलाओ")) {
            sendCommand("REWIND");
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
            result.setText("यह आदेश अभी समझ नहीं आया");
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
        Intent intent = new Intent("com.voiceai.control.COMMAND");
        intent.setPackage(getPackageName());
        intent.putExtra("command", command);
        sendBroadcast(intent);
        result.setText("आदेश भेजा: " + command);
    }

    private void openApp(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

        if (intent != null) {
            startActivity(intent);
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
            Intent intent = getPackageManager().getLaunchIntentForPackage(p);

            if (intent != null) {
                startActivity(intent);
                result.setText("MX Player खोल रहा हूँ...");
                return;
            }
        }

        result.setText("MX Player फोन में नहीं मिला");
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

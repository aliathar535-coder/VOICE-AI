
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
    private CustomCommandStore commandStore;

    private boolean listening = false;
    private boolean learningCommand = false;
    private String pendingPhrase = "";

    private static final String[] ACTIONS = {
        "YOUTUBE",
        "WHATSAPP",
        "MX_PLAYER",
        "VOLUME_UP",
        "VOLUME_DOWN",
        "PLAY_PAUSE",
        "NEXT",
        "PREVIOUS",
        "FAST_FORWARD",
        "REWIND",
        "SCROLL_UP",
        "SCROLL_DOWN",
        "BACK",
        "HOME",
        "RECENTS"
    };

    private static final String[] ACTION_NAMES = {
        "YouTube खोलो",
        "WhatsApp खोलो",
        "MX Player खोलो",
        "आवाज़ बढ़ाओ",
        "आवाज़ कम करो",
        "गाना चलाओ / रोक दो",
        "अगला गाना",
        "पिछला गाना",
        "आगे चलाओ",
        "पीछे चलाओ",
        "ऊपर स्क्रॉल",
        "नीचे स्क्रॉल",
        "वापस जाओ",
        "होम पर जाओ",
        "हाल के ऐप"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        commandStore = new CustomCommandStore(this);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setPadding(24, 30, 24, 25);
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
        add(status, 60);

        micButton = new Button(this);
        micButton.setText("🎙 बोलना शुरू करें");
        micButton.setTextSize(18);
        add(micButton, 70);
        micButton.setOnClickListener(v -> {
            learningCommand = false;
            checkPermission();
        });

        result = new TextView(this);
        result.setText("आपका आदेश यहाँ दिखाई देगा");
        result.setTextColor(Color.WHITE);
        result.setTextSize(17);
        result.setGravity(Gravity.CENTER);
        add(result, 65);

        Button learnButton = new Button(this);
        learnButton.setText("＋ नया कमांड सिखाएँ");
        add(learnButton, 60);
        learnButton.setOnClickListener(v -> startLearning());

        Button listButton = new Button(this);
        listButton.setText("मेरे सेव किए कमांड");
        add(listButton, 60);
        listButton.setOnClickListener(v -> showSavedCommands());

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
        params.setMargins(0, 7, 0, 7);
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
                    String spoken = matches.get(0).trim()
                        .toLowerCase(Locale.ROOT);

                    if (learningCommand) {
                        pendingPhrase = spoken;
                        learningCommand = false;
                        showActionChoice(spoken);
                    } else {
                        result.setText("आपने कहा: " + spoken);
                        history.setText("आखिरी आदेश:\n" + spoken);
                        runCommand(spoken);
                    }
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

    private void startLearning() {
        learningCommand = true;
        pendingPhrase = "";
        result.setText("अब अपना नया आदेश बोलिए");
        checkPermission();
    }

    private void showActionChoice(String phrase) {
        String suggestion = suggestAction(phrase);

        String[] options = new String[ACTION_NAMES.length];
        for (int i = 0; i < ACTION_NAMES.length; i++) {
            options[i] = ACTION_NAMES[i];
        }

        new AlertDialog.Builder(this)
            .setTitle("कमांड का एक्शन चुनें")
            .setMessage("आपका वाक्य: " + phrase +
                "\nऐप का सुझाव: " + getActionName(suggestion))
            .setSingleChoiceItems(options, findActionIndex(suggestion),
                (dialog, which) -> {
                    String selected = ACTIONS[which];

                    new AlertDialog.Builder(this)
                        .setTitle("कमांड सेव करें?")
                        .setMessage("वाक्य: " + phrase +
                            "\nएक्शन: " + ACTION_NAMES[which])
                        .setNegativeButton("नहीं", (d, w) -> d.dismiss())
                        .setPositiveButton("हाँ, सेव करें", (d, w) -> {
                            boolean saved = commandStore.saveCommand(
                                phrase, selected
                            );
                            result.setText(saved ?
                                "कमांड सेव हो गया: " + phrase :
                                "कमांड सेव नहीं हुआ");
                        })
                        .show();

                    dialog.dismiss();
                })
            .setNegativeButton("रद्द करें", null)
            .show();
    }

    private String suggestAction(String phrase) {
        if (phrase.contains("यूट्यूब")) return "YOUTUBE";
        if (phrase.contains("व्हाट्स") || phrase.contains("व्हाट्सएप"))
            return "WHATSAPP";
        if (phrase.contains("एमएक्स") || phrase.contains("एम एक्स"))
            return "MX_PLAYER";
        if (phrase.contains("बढ़ाओ")) return "VOLUME_UP";
        if (phrase.contains("कम करो")) return "VOLUME_DOWN";
        if (phrase.contains("अगला")) return "NEXT";
        if (phrase.contains("पिछला")) return "PREVIOUS";
        if (phrase.contains("आगे")) return "FAST_FORWARD";
        if (phrase.contains("पीछे")) return "REWIND";
        if (phrase.contains("ऊपर")) return "SCROLL_UP";
        if (phrase.contains("नीचे")) return "SCROLL_DOWN";
        if (phrase.contains("वापस")) return "BACK";
        if (phrase.contains("होम")) return "HOME";
        if (phrase.contains("हाल के")) return "RECENTS";
        if (phrase.contains("रोक") || phrase.contains("चलाओ"))
            return "PLAY_PAUSE";
        return "PLAY_PAUSE";
    }

    private String getActionName(String action) {
        for (int i = 0; i < ACTIONS.length; i++) {
            if (ACTIONS[i].equals(action)) return ACTION_NAMES[i];
        }
        return "एक्शन चुनें";
    }

    private int findActionIndex(String action) {
        for (int i = 0; i < ACTIONS.length; i++) {
            if (ACTIONS[i].equals(action)) return i;
        }
        return -1;
    }

    private void showSavedCommands() {
        ArrayList<String> phrases = commandStore.getAllPhrases();

        if (phrases.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("मेरे कमांड")
                .setMessage("अभी कोई कमांड सेव नहीं है")
                .setPositiveButton("ठीक है", null)
                .show();
            return;
        }

        String[] items = phrases.toArray(new String[0]);

        new AlertDialog.Builder(this)
            .setTitle("कमांड चुनें")
            .setItems(items, (dialog, which) -> {
                String phrase = items[which];
                String action = commandStore.findAction(phrase);

                new AlertDialog.Builder(this)
                    .setTitle(phrase)
                    .setMessage("एक्शन: " + getActionName(action))
                    .setPositiveButton("हटाएँ", (d, w) -> {
                        commandStore.deleteCommand(phrase);
                        result.setText("कमांड हटा दिया गया");
                    })
                    .setNegativeButton("बंद करें", null)
                    .show();
            })
            .setNegativeButton("बंद करें", null)
            .show();
    }

    private void runCommand(String c) {
        String customAction = commandStore.findAction(c);

        if (customAction != null) {
            executeAction(customAction);
            return;
        }

        if (c.contains("आवाज़ बढ़ाओ") || c.contains("वॉल्यूम बढ़ाओ")) {
            changeVolume(true);
        }
        else if (c.contains("आवाज़ कम") || c.contains("वॉल्यूम कम")) {
            changeVolume(false);
        }
        else if (c.contains("यूट्यूब")) {
            executeAction("YOUTUBE");
        }
        else if (c.contains("व्हाट्सऐप") || c.contains("व्हाट्सएप")) {
            executeAction("WHATSAPP");
        }
        else if (c.contains("एमएक्स प्लेयर") || c.contains("एम एक्स प्लेयर")) {
            executeAction("MX_PLAYER");
        }
        else if (c.contains("अगला गाना") || c.equals("अगला")) {
            executeAction("NEXT");
        }
        else if (c.contains("पिछला गाना") || c.equals("पिछला")) {
            executeAction("PREVIOUS");
        }
        else if (c.contains("फास्ट फॉरवर्ड") || c.contains("आगे चलाओ")) {
            executeAction("FAST_FORWARD");
        }
        else if (c.contains("रिवाइंड") || c.contains("पीछे चलाओ")) {
            executeAction("REWIND");
        }
        else if (c.contains("चलाओ") || c.contains("रोक दो") || c.contains("पॉज")) {
            executeAction("PLAY_PAUSE");
        }
        else if (c.contains("नीचे स्क्रॉल")) {
            executeAction("SCROLL_DOWN");
        }
        else if (c.contains("ऊपर स्क्रॉल")) {
            executeAction("SCROLL_UP");
        }
        else if (c.contains("वापस जाओ")) {
            executeAction("BACK");
        }
        else if (c.contains("होम पर जाओ")) {
            executeAction("HOME");
        }
        else if (c.contains("हाल के ऐप")) {
            executeAction("RECENTS");
        }
        else {
            result.setText("यह आदेश अभी समझ नहीं आया");
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
                sendCommand(action);
                break;
            default:
                result.setText("यह एक्शन उपलब्ध नहीं है");
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


package com.voiceai.control;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class CustomCommandStore {

    private static final String PREFS = "voice_ai_custom_commands";
    private static final String KEY_COMMANDS = "commands";

    private final SharedPreferences preferences;

    public CustomCommandStore(Context context) {
        preferences = context.getSharedPreferences(
            PREFS, Context.MODE_PRIVATE
        );
    }

    // नया आदेश सेव करना या पुराने आदेश का एक्शन बदलना
    public boolean saveCommand(String phrase, String action) {
        if (phrase == null || action == null) return false;

        phrase = phrase.trim().toLowerCase(Locale.ROOT);
        action = action.trim();

        if (phrase.isEmpty() || action.isEmpty()) return false;

        try {
            JSONObject commands = getCommands();
            commands.put(phrase, action);

            return preferences.edit()
                .putString(KEY_COMMANDS, commands.toString())
                .commit();

        } catch (Exception e) {
            return false;
        }
    }

    // बोले गए वाक्य का सेव किया हुआ एक्शन ढूँढना
    public String findAction(String spokenPhrase) {
        if (spokenPhrase == null) return null;

        String phrase = spokenPhrase.trim()
            .toLowerCase(Locale.ROOT);

        JSONObject commands = getCommands();

        Iterator<String> keys = commands.keys();

        while (keys.hasNext()) {
            String savedPhrase = keys.next();

            if (phrase.equals(savedPhrase)) {
                return commands.optString(savedPhrase, null);
            }
        }

        return null;
    }

    // किसी आदेश को हटाना
    public boolean deleteCommand(String phrase) {
        if (phrase == null) return false;

        try {
            JSONObject commands = getCommands();
            String key = phrase.trim().toLowerCase(Locale.ROOT);

            if (!commands.has(key)) return false;

            commands.remove(key);

            return preferences.edit()
                .putString(KEY_COMMANDS, commands.toString())
                .commit();

        } catch (Exception e) {
            return false;
        }
    }

    // सभी सेव किए गए आदेशों की सूची
    public ArrayList<String> getAllPhrases() {
        ArrayList<String> list = new ArrayList<>();
        JSONObject commands = getCommands();

        Iterator<String> keys = commands.keys();

        while (keys.hasNext()) {
            list.add(keys.next());
        }

        return list;
    }

    // सेव किए गए आदेशों की संख्या
    public int getCommandCount() {
        return getCommands().length();
    }

    // सभी कस्टम आदेश हटाना
    public boolean clearAllCommands() {
        return preferences.edit()
            .remove(KEY_COMMANDS)
            .commit();
    }

    private JSONObject getCommands() {
        String saved = preferences.getString(KEY_COMMANDS, "{}");

        try {
            return new JSONObject(saved);
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}

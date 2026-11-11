package com.gameenginepro.mousemapper;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

final class MappingStore {
    static final String[] IDS = {"left", "right", "middle", "x1", "x2", "wheel_up", "wheel_down"};
    static final String[] LABELS = {"Left click", "Right click", "Middle click", "X1", "X2", "Scroll up", "Scroll down"};
    static final String[] ACTIONS = {"Tap", "Repeated tap", "Swipe"};

    static final class Mapping {
        boolean enabled;
        String action;
        int x;
        int y;
        int endX;
        int endY;
        int repeat;

        Mapping(boolean enabled, String action, int x, int y, int endX, int endY, int repeat) {
            this.enabled = enabled;
            this.action = action;
            this.x = x;
            this.y = y;
            this.endX = endX;
            this.endY = endY;
            this.repeat = repeat;
        }

        static Mapping defaults(String id) {
            return new Mapping(false, "Tap", 50, 50, 70, 50, 3);
        }
    }

    private final SharedPreferences preferences;

    MappingStore(Context context) {
        preferences = context.getSharedPreferences("mouse_mappings", Context.MODE_PRIVATE);
    }

    Mapping[] load(String profile) {
        Mapping[] result = new Mapping[IDS.length];
        String stored = preferences.getString(profileKey(profile), null);
        JSONObject root = parse(stored);
        for (int i = 0; i < IDS.length; i++) {
            String id = IDS[i];
            Mapping fallback = Mapping.defaults(id);
            JSONObject item = root.optJSONObject(id);
            if (item == null) {
                result[i] = fallback;
                continue;
            }
            String action = item.optString("action", fallback.action);
            if (!contains(ACTIONS, action)) action = fallback.action;
            result[i] = new Mapping(
                    item.optBoolean("enabled", fallback.enabled),
                    action,
                    clamp(item.optInt("x", fallback.x)),
                    clamp(item.optInt("y", fallback.y)),
                    clamp(item.optInt("endX", fallback.endX)),
                    clamp(item.optInt("endY", fallback.endY)),
                    Math.max(1, Math.min(20, item.optInt("repeat", fallback.repeat))));
        }
        return result;
    }

    void save(String profile, Mapping[] mappings) throws JSONException {
        JSONObject root = new JSONObject();
        for (int i = 0; i < IDS.length; i++) {
            Mapping mapping = mappings[i];
            JSONObject item = new JSONObject();
            item.put("enabled", mapping.enabled);
            item.put("action", mapping.action);
            item.put("x", clamp(mapping.x));
            item.put("y", clamp(mapping.y));
            item.put("endX", clamp(mapping.endX));
            item.put("endY", clamp(mapping.endY));
            item.put("repeat", Math.max(1, Math.min(20, mapping.repeat)));
            root.put(IDS[i], item);
        }
        preferences.edit()
                .putString(profileKey(profile), root.toString())
                .putLong("revision", preferences.getLong("revision", 0) + 1)
                .apply();
    }

    boolean hasProfile(String profile) {
        return preferences.contains(profileKey(profile));
    }

    long revision() {
        return preferences.getLong("revision", 0);
    }

    static boolean contains(String[] values, String value) {
        for (String candidate : values) {
            if (candidate.equals(value)) return true;
        }
        return false;
    }

    private static String profileKey(String profile) {
        return "profile:" + profile.trim();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static JSONObject parse(String stored) {
        if (stored == null) return new JSONObject();
        try {
            return new JSONObject(stored);
        } catch (JSONException ignored) {
            android.util.Log.w("MouseMapper", "Stored profile is invalid; using empty defaults", ignored);
            return new JSONObject();
        }
    }
}

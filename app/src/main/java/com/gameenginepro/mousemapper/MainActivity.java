package com.gameenginepro.mousemapper;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.List;

public class MainActivity extends Activity {
    private final MappingStore.Mapping[] mappings = new MappingStore.Mapping[MappingStore.IDS.length];
    private final EditText[][] fields = new EditText[MappingStore.IDS.length][5];
    private final CheckBox[] enabled = new CheckBox[MappingStore.IDS.length];
    private final Spinner[] actions = new Spinner[MappingStore.IDS.length];
    private EditText profileField;
    private TextView serviceStatus;
    private MappingStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new MappingStore(this);
        buildScreen();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serviceStatus != null) updateServiceStatus();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(28));
        page.setBackgroundColor(Color.rgb(15, 23, 42));
        scroll.addView(page);

        TextView title = text("Mouse Mapper", 28, Color.WHITE);
        title.setTypeface(null, 1);
        page.addView(title);
        page.addView(text("Android 14 • External mouse to touch controls", 14, Color.rgb(148, 163, 184)));
        page.addView(gap(14));

        TextView notice = text("Unrooted mode uses Android AccessibilityService gestures. Input delivery and gesture injection depend on the foreground app; some games may block or duplicate mapped actions.", 14, Color.rgb(254, 226, 150));
        notice.setPadding(dp(12), dp(12), dp(12), dp(12));
        notice.setBackgroundColor(Color.rgb(51, 45, 28));
        page.addView(notice);
        page.addView(gap(12));

        serviceStatus = text("", 15, Color.WHITE);
        page.addView(serviceStatus);
        Button accessibilityButton = button("Enable accessibility service");
        accessibilityButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        page.addView(accessibilityButton);

        LinearLayout profileRow = new LinearLayout(this);
        profileRow.setGravity(Gravity.CENTER_VERTICAL);
        profileField = edit("Profile package (or * for all apps)");
        profileField.setText(getSharedPreferences("mouse_mappings", MODE_PRIVATE)
                .getString("last_edit_profile", "*"));
        profileRow.addView(profileField, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button loadButton = button("Load");
        loadButton.setOnClickListener(v -> loadProfile());
        profileRow.addView(loadButton);
        page.addView(profileRow);
        page.addView(text("Use the target app's package name for an app-specific profile, or * for a global profile.", 13, Color.rgb(148, 163, 184)));
        page.addView(gap(8));

        for (int i = 0; i < MappingStore.IDS.length; i++) addMappingCard(page, i);

        Button saveButton = button("Save profile");
        saveButton.setOnClickListener(v -> saveProfile());
        page.addView(saveButton);

        setContentView(scroll);
        updateServiceStatus();
    }

    private void addMappingCard(LinearLayout page, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(12));
        card.setBackgroundColor(Color.rgb(30, 41, 59));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        page.addView(card, cardParams);

        enabled[index] = new CheckBox(this);
        enabled[index].setText(MappingStore.LABELS[index]);
        enabled[index].setTextColor(Color.WHITE);
        card.addView(enabled[index]);

        actions[index] = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, MappingStore.ACTIONS);
        actions[index].setAdapter(adapter);
        card.addView(actions[index]);

        LinearLayout coords = new LinearLayout(this);
        coords.setOrientation(LinearLayout.HORIZONTAL);
        String[] placeholders = {"X %", "Y %", "End X %", "End Y %", "Taps"};
        for (int field = 0; field < fields[index].length; field++) {
            fields[index][field] = edit(placeholders[field]);
            fields[index][field].setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            coords.addView(fields[index][field], params);
        }
        card.addView(coords);
        card.addView(text("Coordinates are percentages of the screen. Swipe uses start and end coordinates.", 12, Color.rgb(148, 163, 184)));
    }

    private void loadProfile() {
        String profile = profileField == null ? "*" : profileField.getText().toString().trim();
        if (profile.isEmpty()) profile = "*";
        if (profileField != null && profileField.getText().toString().trim().isEmpty()) profileField.setText(profile);
        MappingStore.Mapping[] loaded = store.load(profile);
        for (int i = 0; i < loaded.length; i++) {
            mappings[i] = loaded[i];
            if (enabled[i] == null) continue;
            enabled[i].setChecked(loaded[i].enabled);
            actions[i].setSelection(Math.max(0, indexOf(MappingStore.ACTIONS, loaded[i].action)));
            setField(i, 0, loaded[i].x);
            setField(i, 1, loaded[i].y);
            setField(i, 2, loaded[i].endX);
            setField(i, 3, loaded[i].endY);
            setField(i, 4, loaded[i].repeat);
        }
    }

    private void saveProfile() {
        String profile = profileField.getText().toString().trim();
        if (profile.isEmpty()) {
            Toast.makeText(this, "Enter a package name or *", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < mappings.length; i++) {
            mappings[i] = new MappingStore.Mapping(
                    enabled[i].isChecked(),
                    String.valueOf(actions[i].getSelectedItem()),
                    number(i, 0, 50), number(i, 1, 50), number(i, 2, 70), number(i, 3, 50),
                    Math.max(1, Math.min(20, number(i, 4, 3))));
        }
        try {
            store.save(profile, mappings);
            getSharedPreferences("mouse_mappings", MODE_PRIVATE).edit()
                    .putString("last_edit_profile", profile).apply();
            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
        } catch (JSONException exception) {
            Toast.makeText(this, "Could not save profile: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int number(int row, int field, int fallback) {
        String value = fields[row][field].getText().toString().trim();
        if (value.isEmpty()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void setField(int row, int field, int value) {
        fields[row][field].setText(String.valueOf(value));
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
        return 0;
    }

    private void updateServiceStatus() {
        if (serviceStatus == null) return;
        boolean active = false;
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (manager != null) {
            List<AccessibilityServiceInfo> services =
                    manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (AccessibilityServiceInfo service : services) {
                if (service.getId() != null && service.getId().contains(getPackageName())) active = true;
            }
        }
        serviceStatus.setText(active ? "Service: enabled" : "Service: disabled");
        serviceStatus.setTextColor(active ? Color.rgb(134, 239, 172) : Color.rgb(252, 165, 165));
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private EditText edit(String hint) {
        EditText view = new EditText(this);
        view.setSingleLine(true);
        view.setTextSize(14);
        view.setHint(hint);
        view.setHintTextColor(Color.rgb(148, 163, 184));
        view.setTextColor(Color.WHITE);
        view.setPadding(dp(8), 0, dp(8), 0);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        return button;
    }

    private View gap(int height) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

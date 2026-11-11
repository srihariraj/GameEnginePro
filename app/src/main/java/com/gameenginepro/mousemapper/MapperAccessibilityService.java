package com.gameenginepro.mousemapper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.WindowMetrics;
import android.view.WindowManager;

public class MapperAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String foregroundPackage = "";
    private MappingStore store;
    private String loadedProfile;
    private long loadedRevision = -1;
    private MappingStore.Mapping[] mappings;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        store = new MappingStore(this);
        refreshMappings();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.setMotionEventSources(InputDevice.SOURCE_MOUSE);
            setServiceInfo(info);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        foregroundPackage = event.getPackageName().toString();
        refreshMappings();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (store == null || event == null || !event.isFromSource(InputDevice.SOURCE_MOUSE)) return;
        refreshMappings();

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_BUTTON_PRESS) {
            int button = event.getActionButton();
            switch (button) {
                case MotionEvent.BUTTON_PRIMARY:
                    trigger(0);
                    break;
                case MotionEvent.BUTTON_SECONDARY:
                    trigger(1);
                    break;
                case MotionEvent.BUTTON_TERTIARY:
                    trigger(2);
                    break;
                case MotionEvent.BUTTON_BACK:
                    trigger(3);
                    break;
                case MotionEvent.BUTTON_FORWARD:
                    trigger(4);
                    break;
                default:
                    break;
            }
        } else if (action == MotionEvent.ACTION_SCROLL) {
            float vertical = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (vertical > 0) trigger(5);
            else if (vertical < 0) trigger(6);
        }
    }

    private void refreshMappings() {
        if (store == null) return;
        String targetProfile = !foregroundPackage.isEmpty() && store.hasProfile(foregroundPackage)
                ? foregroundPackage : "*";
        long revision = store.revision();
        if (!targetProfile.equals(loadedProfile) || revision != loadedRevision) {
            loadedProfile = targetProfile;
            loadedRevision = revision;
            mappings = store.load(loadedProfile);
        }
    }

    private void trigger(int index) {
        if (index >= mappings.length) return;
        MappingStore.Mapping mapping = mappings[index];
        if (!mapping.enabled) return;

        int width;
        int height;
        WindowManager windowManager = getSystemService(WindowManager.class);
        if (windowManager == null) {
            android.util.Log.e("MouseMapper", "Window manager unavailable; cannot map mouse input");
            return;
        }
        WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
        Rect bounds = metrics.getBounds();
        width = bounds.width();
        height = bounds.height();
        float startX = (width - 1) * mapping.x / 100f;
        float startY = (height - 1) * mapping.y / 100f;
        float endX = (width - 1) * mapping.endX / 100f;
        float endY = (height - 1) * mapping.endY / 100f;
        int count = "Repeated tap".equals(mapping.action) ? mapping.repeat : 1;
        dispatchMappedGesture(mapping.action, startX, startY, endX, endY, count);
    }

    private void dispatchMappedGesture(String action, float startX, float startY,
                                       float endX, float endY, int remaining) {
        Path path = new Path();
        path.moveTo(startX, startY);
        if ("Swipe".equals(action)) path.lineTo(endX, endY);

        long duration = "Swipe".equals(action) ? 250 : 60;
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, duration))
                .build();
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription completedGesture) {
                if (remaining > 1) {
                    handler.postDelayed(() ->
                            dispatchMappedGesture("Tap", startX, startY, endX, endY, remaining - 1), 70);
                }
            }

            @Override
            public void onCancelled(GestureDescription cancelledGesture) {
                android.util.Log.w("MouseMapper", "Android cancelled a mapped gesture");
            }
        }, handler);
        if (!accepted) {
            android.util.Log.w("MouseMapper", "Android rejected a mapped gesture");
        }
    }
}

package app.lawnchair.prime.drawer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.logging.StatsLogManager.LauncherEvent;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.OptionsPopupView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import app.lawnchair.preferences.PreferenceManager;

/** Prime folder long-press menu and drag bridge for drawer and workspace folders. */
public final class PrimeFolderLongPressHelper {
    private static final Map<FolderInfo, PrimeFolderRef> PRIME_FOLDERS = new WeakHashMap<>();
    private final FolderIcon mIcon;
    private final int mTouchSlop;
    private float mDownX;
    private float mDownY;
    private boolean mLongPressActive;
    private OptionsPopupView<?> mPopup;
    private boolean mDragStarted;

    public PrimeFolderLongPressHelper(FolderIcon icon) {
        mIcon = icon;
        mTouchSlop = ViewConfiguration.get(icon.getContext()).getScaledTouchSlop();
    }

    public static void registerPrimeFolder(FolderInfo info, String tabId, String folderId) {
        synchronized (PRIME_FOLDERS) {
            PRIME_FOLDERS.put(info, new PrimeFolderRef(tabId, folderId));
        }
    }

    public static boolean shouldHandle(FolderIcon icon) {
        if (!icon.isInAppDrawer()) return true;
        return PreferenceManager.getInstance(icon.getContext()).getDrawerTabsEnabled().get()
                && getPrimeRef(icon.mInfo) != null;
    }

    public boolean onLongClick(View view) {
        if (!shouldHandle(mIcon)) return false;
        mIcon.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        mLongPressActive = true;
        // Keep the gesture owned by the folder icon after the long press. Showing the popup here
        // would move touch handling to DragLayer, allowing workspace swipes/notification gestures
        // to steal the MOVE event before we can turn it into a drag.
        mIcon.getParent().requestDisallowInterceptTouchEvent(true);
        return true;
    }

    public void onTouchEvent(MotionEvent event) {
        if (!shouldHandle(mIcon)) return;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                mLongPressActive = false;
                mDragStarted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mLongPressActive
                        && Math.hypot(event.getX() - mDownX, event.getY() - mDownY) > mTouchSlop) {
                    mLongPressActive = false;
                    if (mPopup != null) {
                        mPopup.close(false);
                        mPopup = null;
                    }
                    startDrag();
                    mDragStarted = true;
                } else if (mDragStarted) {
                    Launcher launcher = Launcher.getLauncher(mIcon.getContext());
                    launcher.getDragController().onControllerTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mLongPressActive) {
                    mLongPressActive = false;
                    showMenu();
                } else if (mDragStarted) {
                    Launcher launcher = Launcher.getLauncher(mIcon.getContext());
                    launcher.getDragController().onControllerTouchEvent(event);
                    mDragStarted = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mDragStarted) {
                    Launcher launcher = Launcher.getLauncher(mIcon.getContext());
                    launcher.getDragController().onControllerTouchEvent(event);
                    mDragStarted = false;
                }
                mLongPressActive = false;
                break;
        }
    }

    private void startDrag() {
        // The long-press phase blocks parent interception so workspace gestures cannot steal the
        // gesture. Once the drag starts, DragLayer must receive MOVE events again; otherwise the
        // DragView is created but remains frozen at its initial position.
        Launcher launcher = Launcher.getLauncher(mIcon.getContext());
        if (!ItemLongClickListener.canStartDrag(launcher)) return;
        // Projected drawer folders have container == NO_ID, but once the same FolderInfo is
        // dropped on Workspace that value is not a reliable way to choose the drag pipeline.
        // The actual parent tells us whether this icon currently lives in All Apps.
        if (isAttachedToAllApps()) {
            launcher.getWorkspace().beginDragShared(mIcon, launcher.getAppsView(), new DragOptions());
        } else {
            launcher.setWaitingForResult(null);
            ItemLongClickListener.beginDrag(mIcon, launcher, mIcon.mInfo, new DragOptions());
        }
    }

    private boolean isAttachedToAllApps() {
        android.view.ViewParent parent = mIcon.getParent();
        while (parent instanceof View) {
            if (parent instanceof com.android.launcher3.allapps.AllAppsRecyclerView) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void showMenu() {
        ActivityContext activity = ActivityContext.lookupContext(mIcon.getContext());
        if (activity == null) return;
        ArrayList<OptionsPopupView.OptionItem> items = new ArrayList<>();
        items.add(option(mIcon.getContext().getString(R.string.prime_tab_rename), v -> {
            showRenameDialog();
            return true;
        }));
        items.add(option(mIcon.getContext().getString(R.string.prime_tab_apps), v -> {
            showAppsDialog();
            return true;
        }));
        items.add(option(mIcon.getContext().getString(R.string.prime_tab_advanced) + "*", v -> true));

        int[] location = new int[2];
        mIcon.getLocationOnScreen(location);
        RectF target = new RectF(location[0], location[1],
                location[0] + mIcon.getWidth(), location[1] + mIcon.getHeight());
        mPopup = OptionsPopupView.show(activity, target, items, false);
    }

    private OptionsPopupView.OptionItem option(String label, View.OnLongClickListener action) {
        return new OptionsPopupView.OptionItem(
                label, new ColorDrawable(android.graphics.Color.TRANSPARENT),
                LauncherEvent.IGNORE, action);
    }

    private void showRenameDialog() {
        EditText input = new EditText(mIcon.getContext());
        input.setText(mIcon.mInfo.title);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int padding = dp(24);
        LinearLayout container = new LinearLayout(mIcon.getContext());
        container.setPadding(padding, 0, padding, 0);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(mIcon.getContext())
                .setTitle(R.string.prime_tab_rename).setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.prime_tab_rename_action, null).create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) return;
                    PrimeFolderRef ref = getPrimeRef(mIcon.mInfo);
                    if (mIcon.isInAppDrawer() && ref != null) {
                        new PrimeDrawerTabsRepository(mIcon.getContext())
                                .renameFolder(ref.tabId, ref.folderId, title);
                        mIcon.mInfo.setTitle(title, null);
                    } else {
                        Launcher launcher = Launcher.getLauncher(mIcon.getContext());
                        mIcon.mInfo.setTitle(title, launcher.getModelWriter());
                    }
                    mIcon.onTitleChanged(title);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showAppsDialog() {
        Launcher launcher = Launcher.getLauncher(mIcon.getContext());
        AppInfo[] apps = launcher.getAppsView().getAppsStore().getApps().clone();
        Arrays.sort(apps, Comparator.comparing(
                app -> app.title == null ? "" : app.title.toString(),
                String.CASE_INSENSITIVE_ORDER));

        PrimeFolderRef ref = getPrimeRef(mIcon.mInfo);
        PrimeDrawerTabsRepository repository = new PrimeDrawerTabsRepository(mIcon.getContext());
        Set<String> selected = new HashSet<>();
        for (ItemInfo item : mIcon.mInfo.getContents()) {
            if (item.getTargetComponent() != null) {
                selected.add(new ComponentKey(item.getTargetComponent(), item.user).toString());
            }
        }

        LinearLayout list = new LinearLayout(mIcon.getContext());
        list.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        list.setPadding(padding, dp(8), padding, dp(8));
        for (AppInfo app : apps) {
            String key = app.toComponentKey().toString();
            if (mIcon.isInAppDrawer() && ref != null
                    && !repository.isAppInTab(app.toComponentKey(), ref.tabId)) continue;
            CheckBox checkBox = new CheckBox(mIcon.getContext());
            checkBox.setText(app.title);
            android.graphics.drawable.Drawable icon = app.newIcon(mIcon.getContext());
            int iconSize = dp(32);
            icon.setBounds(0, 0, iconSize, iconSize);
            checkBox.setCompoundDrawablesRelative(icon, null, null, null);
            checkBox.setCompoundDrawablePadding(dp(12));
            TypedValue textColor = new TypedValue();
            if (mIcon.getContext().getTheme().resolveAttribute(
                    android.R.attr.textColorPrimary, textColor, true)) {
                if (textColor.resourceId != 0) {
                    checkBox.setTextColor(mIcon.getContext().getColorStateList(textColor.resourceId));
                } else {
                    checkBox.setTextColor(textColor.data);
                }
            }
            checkBox.setTag(key);
            checkBox.setChecked(selected.contains(key));
            checkBox.setPadding(dp(8), dp(4), dp(8), dp(4));
            list.addView(checkBox, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        ScrollView scroll = new ScrollView(mIcon.getContext());
        scroll.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(mIcon.getContext())
                .setTitle(mIcon.mInfo.title).setView(scroll)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    Set<ComponentKey> keys = new HashSet<>();
                    for (int i = 0; i < list.getChildCount(); i++) {
                        View child = list.getChildAt(i);
                        if (child instanceof CheckBox && ((CheckBox) child).isChecked()) {
                            ComponentKey key = ComponentKey.fromString((String) child.getTag());
                            if (key != null) keys.add(key);
                        }
                    }
                    if (mIcon.isInAppDrawer() && ref != null) {
                        repository.setFolderApps(ref.tabId, ref.folderId, keys);
                        if (launcher.getAppsView().getFloatingHeaderView() != null) {
                            launcher.getAppsView().getFloatingHeaderView().onPrimeDrawerTabSelected();
                        }
                    } else {
                        mIcon.getFolder().setAppsFromPrimeMenu(keys, apps);
                    }
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * mIcon.getResources().getDisplayMetrics().density);
    }

    private static PrimeFolderRef getPrimeRef(FolderInfo info) {
        synchronized (PRIME_FOLDERS) {
            return PRIME_FOLDERS.get(info);
        }
    }

    private static final class PrimeFolderRef {
        final String tabId;
        final String folderId;
        PrimeFolderRef(String tabId, String folderId) {
            this.tabId = tabId;
            this.folderId = folderId;
        }
    }
}

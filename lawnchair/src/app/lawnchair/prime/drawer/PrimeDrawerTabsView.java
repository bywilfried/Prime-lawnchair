package app.lawnchair.prime.drawer;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;

import androidx.annotation.Nullable;
import android.app.AlertDialog;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;

import com.android.launcher3.R;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.allapps.FloatingHeaderRow;
import com.android.launcher3.allapps.FloatingHeaderView;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.OptionsPopupView;
import com.android.launcher3.logging.StatsLogManager.LauncherEvent;

import java.util.List;
import java.util.ArrayList;

import app.lawnchair.preferences.PreferenceManager;

/** Prime's independent horizontal drawer tab row. */
public class PrimeDrawerTabsView extends HorizontalScrollView implements FloatingHeaderRow {

    private final PreferenceManager mPrefs;
    private final PrimeDrawerTabsRepository mRepository;
    private final LinearLayout mTabsContainer;
    private boolean mIsScrolledOut;
    private OptionsPopupView<?> mTabPopup;
    private String mDraggingTabId;
    private float mLongPressDownX;
    private float mLastTouchRawX;
    private boolean mLongPressActive;
    private final int mTouchSlop;

    public PrimeDrawerTabsView(Context context) {
        this(context, null);
    }

    public PrimeDrawerTabsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPrefs = PreferenceManager.getInstance(context);
        mRepository = new PrimeDrawerTabsRepository(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTabsContainer = new LinearLayout(context);
        mTabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        mTabsContainer.setGravity(Gravity.CENTER_VERTICAL);
        addView(mTabsContainer, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        setHorizontalScrollBarEnabled(false);
    }

    @Override
    public void setup(FloatingHeaderView parent, FloatingHeaderRow[] rows, boolean tabsHidden) {
        refresh(parent);
    }

    private void refresh(FloatingHeaderView parent) {
        boolean enabled = mPrefs.getDrawerTabsEnabled().get();
        setVisibility(enabled && !mIsScrolledOut ? VISIBLE : enabled ? INVISIBLE : GONE);
        mTabsContainer.removeAllViews();
        if (!enabled) return;

        PrimeDrawerTabsConfiguration configuration = mRepository.getConfiguration();
        List<PrimeDrawerTab> tabs = configuration.getTabs();
        for (PrimeDrawerTab tab : tabs) {
            String label;
            if (PrimeDrawerTabsRepository.ALL_TAB_ID.equals(tab.getId())) {
                label = getContext().getString(R.string.prime_tab_all);
            } else if (PrimeDrawerTabsRepository.UNCLASSIFIED_TAB_ID.equals(tab.getId())) {
                label = getContext().getString(R.string.prime_tab_unclassified);
            } else {
                label = tab.getTitle();
            }
            final String tabId = tab.getId();
            TextView pill = addPill(label, tabId.equals(configuration.getSelectedTabId()), () -> {
                mRepository.setSelectedTab(tabId);
                refresh(parent);
                parent.onPrimeDrawerTabSelected();
            });
            pill.setTag(tabId);
            pill.setOnTouchListener((v, event) -> {
                mLastTouchRawX = event.getRawX();
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mLongPressDownX = event.getRawX();
                    mLongPressActive = false;
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                        && mLongPressActive
                        && Math.abs(event.getRawX() - mLongPressDownX) > mTouchSlop) {
                    if (mTabPopup != null) {
                        mTabPopup.close(false);
                        mTabPopup = null;
                    }
                    mLongPressActive = false;
                    mDraggingTabId = tabId;
                    v.startDragAndDrop(null, new DragShadowBuilder(v), tabId, 0);
                    return true;
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    mLongPressActive = false;
                }
                return false;
            });
            pill.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                mLongPressActive = true;
                mLongPressDownX = mLastTouchRawX;
                showTabMenu(parent, tab, pill);
                return true;
            });
            pill.setOnDragListener((v, event) -> handleTabDrag(event));
        }
        addPill("+", false, () -> showCreateTabDialog(parent));
    }

    private boolean handleTabDrag(DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
            if (mTabPopup != null) {
                mTabPopup.close(false);
                mTabPopup = null;
            }
            View target = findTabAt(event.getX() + getScrollX());
            Object localState = event.getLocalState();
            if (target != null && localState instanceof String) {
                moveTabView((String) localState, target);
            }
        } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            persistCurrentOrder();
            mDraggingTabId = null;
        }
        return true;
    }

    private View findTabAt(float x) {
        for (int i = 0; i < mTabsContainer.getChildCount(); i++) {
            View child = mTabsContainer.getChildAt(i);
            if (!(child.getTag() instanceof String)) continue;
            if (x >= child.getLeft() && x <= child.getRight()) return child;
        }
        return null;
    }

    private void moveTabView(String draggedId, View target) {
        View dragged = null;
        for (int i = 0; i < mTabsContainer.getChildCount(); i++) {
            View child = mTabsContainer.getChildAt(i);
            if (draggedId.equals(child.getTag())) {
                dragged = child;
                break;
            }
        }
        if (dragged == null || dragged == target) return;
        int from = mTabsContainer.indexOfChild(dragged);
        int to = mTabsContainer.indexOfChild(target);
        if (from < 0 || to < 0) return;
        mTabsContainer.removeViewAt(from);
        if (from < to) to--;
        mTabsContainer.addView(dragged, to);
    }

    private void persistCurrentOrder() {
        ArrayList<String> ids = new ArrayList<>();
        for (int i = 0; i < mTabsContainer.getChildCount(); i++) {
            Object tag = mTabsContainer.getChildAt(i).getTag();
            if (tag instanceof String) ids.add((String) tag);
        }
        mRepository.reorderTabs(ids);
    }

    private void showTabMenu(FloatingHeaderView parent, PrimeDrawerTab tab, View anchor) {
        ActivityContext activityContext = ActivityContext.lookupContext(getContext());
        if (activityContext == null) return;
        ArrayList<OptionsPopupView.OptionItem> items = new ArrayList<>();
        if (!tab.isSystem()) {
            items.add(option(R.string.prime_tab_rename, v -> true));
            items.add(option(R.string.prime_tab_reorganize, v -> true));
        }
        items.add(option(R.string.prime_tab_set_default, v -> {
            mRepository.setDefaultTab(tab.getId());
            return true;
        }));
        if (!tab.isSystem()) {
            items.add(option(R.string.prime_tab_apps, v -> true));
            items.add(option(R.string.prime_tab_advanced, v -> true));
            items.add(option(R.string.prime_tab_delete, v -> true));
        }

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        RectF target = new RectF(location[0], location[1],
                location[0] + anchor.getWidth(), location[1] + anchor.getHeight());
        mTabPopup = OptionsPopupView.show(activityContext, target, items, false);
    }

    private OptionsPopupView.OptionItem option(int labelRes, View.OnLongClickListener action) {
        return new OptionsPopupView.OptionItem(
                getContext().getString(labelRes),
                new ColorDrawable(android.graphics.Color.TRANSPARENT),
                LauncherEvent.IGNORE,
                action);
    }

    private String getSystemTabLabel(PrimeDrawerTab tab) {
        return PrimeDrawerTabsRepository.ALL_TAB_ID.equals(tab.getId())
                ? getContext().getString(R.string.prime_tab_all)
                : getContext().getString(R.string.prime_tab_unclassified);
    }

    private void showCreateTabDialog(FloatingHeaderView parent) {
        EditText input = new EditText(getContext());
        input.setHint(R.string.prime_tab_name_hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        int horizontalPadding = dp(24);
        LinearLayout container = new LinearLayout(getContext());
        container.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        container.addView(input, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.prime_tab_create)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.prime_tab_create_action, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = input.getText().toString().trim();
                if (title.isEmpty()) {
                    input.setError(getContext().getString(R.string.prime_tab_name_required));
                    return;
                }
                PrimeDrawerTab tab = mRepository.createTab(title);
                mRepository.setSelectedTab(tab.getId());
                dialog.dismiss();
                refresh(parent);
                parent.onPrimeDrawerTabSelected();
                post(() -> fullScroll(FOCUS_RIGHT));
            });
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        dialog.show();
    }

    private TextView addPill(String label, boolean selected, Runnable action) {
        TextView pill = new TextView(getContext());
        pill.setText(label);
        pill.setGravity(Gravity.CENTER);
        pill.setMinHeight(dp(40));
        pill.setPadding(dp(16), 0, dp(16), 0);
        pill.setTextColor(Themes.getAttrColor(
                getContext(), selected ? android.R.attr.colorBackground : android.R.attr.textColorPrimary));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(20));
        if (selected) {
            background.setColor(Themes.getAttrColor(getContext(), android.R.attr.colorAccent));
        } else {
            background.setColor(0x00000000);
            background.setStroke(dp(1),
                    Themes.getAttrColor(getContext(), android.R.attr.textColorSecondary));
        }
        pill.setBackground(background);
        pill.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(40));
        params.setMarginEnd(dp(8));
        mTabsContainer.addView(pill, params);
        return pill;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public int getExpectedHeight() {
        return mPrefs.getDrawerTabsEnabled().get() ? dp(48) : 0;
    }

    @Override
    public boolean shouldDraw() {
        return mPrefs.getDrawerTabsEnabled().get();
    }

    @Override
    public boolean hasVisibleContent() {
        return shouldDraw();
    }

    @Override
    public void setVerticalScroll(int scroll, boolean isScrolledOut) {
        setTranslationY(scroll);
        mIsScrolledOut = isScrolledOut;
        boolean enabled = mPrefs.getDrawerTabsEnabled().get();
        setVisibility(enabled && !isScrolledOut ? VISIBLE : enabled ? INVISIBLE : GONE);
    }

    @Override
    public Class<PrimeDrawerTabsView> getTypeClass() {
        return PrimeDrawerTabsView.class;
    }

    @Override
    public View getFocusedChild() {
        return null;
    }
}

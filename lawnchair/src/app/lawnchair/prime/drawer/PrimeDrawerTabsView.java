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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;

import androidx.annotation.Nullable;
import android.app.AlertDialog;

import com.android.launcher3.R;
import com.android.launcher3.allapps.FloatingHeaderRow;
import com.android.launcher3.allapps.FloatingHeaderView;
import com.android.launcher3.util.Themes;

import java.util.List;
import java.util.ArrayList;

import app.lawnchair.preferences.PreferenceManager;

/** Prime's independent horizontal drawer tab row. */
public class PrimeDrawerTabsView extends HorizontalScrollView implements FloatingHeaderRow {

    private final PreferenceManager mPrefs;
    private final PrimeDrawerTabsRepository mRepository;
    private final LinearLayout mTabsContainer;
    private boolean mIsScrolledOut;

    public PrimeDrawerTabsView(Context context) {
        this(context, null);
    }

    public PrimeDrawerTabsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPrefs = PreferenceManager.getInstance(context);
        mRepository = new PrimeDrawerTabsRepository(context);
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
            pill.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                showTabMenu(parent, tab, pill);
                v.startDragAndDrop(null, new DragShadowBuilder(v), tabId, 0);
                return true;
            });
            pill.setOnDragListener((v, event) -> handleTabDrag(event, parent));
        }
        addPill("+", false, () -> showCreateTabDialog(parent));
    }

    private boolean handleTabDrag(DragEvent event, FloatingHeaderView parent) {
        if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
            View target = findTabAt(event.getX() + getScrollX());
            Object localState = event.getLocalState();
            if (target != null && localState instanceof String) {
                moveTab((String) localState, (String) target.getTag(), parent);
            }
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

    private void moveTab(String draggedId, String targetId, FloatingHeaderView parent) {
        if (draggedId.equals(targetId)) return;
        PrimeDrawerTabsConfiguration configuration = mRepository.getConfiguration();
        ArrayList<String> ids = new ArrayList<>();
        for (PrimeDrawerTab tab : configuration.getTabs()) ids.add(tab.getId());
        int from = ids.indexOf(draggedId);
        int to = ids.indexOf(targetId);
        if (from < 0 || to < 0) return;
        ids.remove(from);
        ids.add(to, draggedId);
        mRepository.reorderTabs(ids);
        refresh(parent);
    }

    private void showTabMenu(FloatingHeaderView parent, PrimeDrawerTab tab, View anchor) {
        boolean system = tab.isSystem();
        String[] items = system
                ? new String[] {getContext().getString(R.string.prime_tab_set_default)}
                : new String[] {
                        getContext().getString(R.string.prime_tab_rename),
                        getContext().getString(R.string.prime_tab_reorganize),
                        getContext().getString(R.string.prime_tab_set_default),
                        getContext().getString(R.string.prime_tab_apps),
                        getContext().getString(R.string.prime_tab_advanced),
                        getContext().getString(R.string.prime_tab_delete)
                };
        new AlertDialog.Builder(getContext())
                .setTitle(tab.isSystem() ? getSystemTabLabel(tab) : tab.getTitle())
                .setItems(items, (dialog, which) -> {
                    int defaultIndex = system ? 0 : 2;
                    if (which == defaultIndex) mRepository.setDefaultTab(tab.getId());
                })
                .show();
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

package app.lawnchair.prime.drawer;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.android.launcher3.allapps.FloatingHeaderRow;
import com.android.launcher3.allapps.FloatingHeaderView;
import com.android.launcher3.util.Themes;

import java.util.List;

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
            addPill(label, tabId.equals(configuration.getSelectedTabId()), () -> {
                mRepository.setSelectedTab(tabId);
                refresh(parent);
                parent.onPrimeDrawerTabSelected();
            });
        }
        addPill("+", false, () -> {
            // Creation UI is intentionally implemented with the CRUD step.
        });
    }

    private void addPill(String label, boolean selected, Runnable action) {
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

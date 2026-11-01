package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.IconHeaderItemPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.dialog.ErrorDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.browse.NavigateTitleView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed SmartTube BrowseFragment using androidx.leanback.app.BrowseSupportFragment
 * with Permanent Expandable Icon Sidebar (72dp icon rail -> 240dp expanded drawer).
 */
public class BrowseFragment extends BrowseSupportFragment implements BrowseView {
    private static final String TAG = BrowseFragment.class.getSimpleName();
    private static final String SELECTED_HEADER_INDEX = "SelectedHeaderIndex";
    private static final String SELECTED_VIDEO = "SelectedVideo";
    private static final String IS_PLAYER_IN_FOREGROUND = "IsPlayerInForeground";

    private ArrayObjectAdapter mSectionRowAdapter;
    private BrowsePresenter mBrowsePresenter;
    private Map<Integer, BrowseSection> mSections;
    private BrowseSectionFragmentFactory mFragmentFactory;
    private ErrorDialogFragment mErrorFragment;
    private NavigateTitleView mTitleView;
    private Handler mHandler;

    private boolean mIsFragmentCreated;
    private int mSelectedHeaderIndex = -1;
    private Video mSelectedVideo;
    private boolean mIsPlayerInForeground;
    private boolean mFocusOnContent;
    private boolean mIsProgressBarShowing;

    private PermanentSidebarController mSidebarController;
    private View mBrowseContainerDock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSelectedHeaderIndex = savedInstanceState.getInt(SELECTED_HEADER_INDEX, -1);
            mSelectedVideo = Video.fromString(savedInstanceState.getString(SELECTED_VIDEO));
            mIsPlayerInForeground = savedInstanceState.getBoolean(IS_PLAYER_IN_FOREGROUND, false);
        }
        mIsFragmentCreated = true;

        mSections = new LinkedHashMap<>();
        mHandler = new Handler(Looper.getMainLooper());
        mBrowsePresenter = BrowsePresenter.instance(getContext());
        mBrowsePresenter.setView(this);

        setupAdapter();
        setupFragmentFactory();
        setupUi();

        enableMainFragmentScaling(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SELECTED_HEADER_INDEX, getSelectedPosition());
        if (mBrowsePresenter != null && mBrowsePresenter.getCurrentVideo() != null) {
            outState.putString(SELECTED_VIDEO, mBrowsePresenter.getCurrentVideo().toString());
            outState.putBoolean(IS_PLAYER_IN_FOREGROUND, ViewManager.instance(getContext()).isPlayerInForeground());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        updateWaitTime();

        mTitleView = root.findViewById(R.id.browse_title_group);
        mBrowseContainerDock = root.findViewById(R.id.browse_container_dock);
        View headersDock = root.findViewById(R.id.browse_headers_dock);

        // Hide default Leanback header dock offscreen while keeping PageRow fragment switching active
        if (headersDock != null) {
            headersDock.setAlpha(0f);
            headersDock.setTranslationX(-4000f);
            headersDock.setFocusable(false);
            if (headersDock instanceof ViewGroup) {
                ((ViewGroup) headersDock).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            }
        }

        // Keep 72dp left margin on video container so permanent icon rail never overlaps video cards
        if (mBrowseContainerDock != null) {
            final int collapsedPx = dpToPx(requireContext(), PermanentSidebarController.COLLAPSED_WIDTH_DP);
            enforceContainerMargin(collapsedPx);
            mBrowseContainerDock.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                enforceContainerMargin(collapsedPx);
            });
        }

        // Attach Permanent Sidebar Controller
        if (root instanceof ViewGroup) {
            mSidebarController = new PermanentSidebarController(requireContext(), this);
            ((ViewGroup) root).addView(mSidebarController.getSidebarView());
            mSidebarController.syncSections(mSections);
        }

        return root;
    }

    private void enforceContainerMargin(int leftMarginPx) {
        if (mBrowseContainerDock == null) return;
        mBrowseContainerDock.setTranslationX(0f);
        ViewGroup.LayoutParams lp = mBrowseContainerDock.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            if (mlp.leftMargin != leftMarginPx || mlp.getMarginStart() != leftMarginPx) {
                mlp.leftMargin = leftMarginPx;
                mlp.setMarginStart(leftMarginPx);
                mBrowseContainerDock.setLayoutParams(mlp);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupEventListeners();

        prepareEntranceTransition();

        mBrowsePresenter.onViewInitialized();

        if (mSelectedHeaderIndex != -1) {
            selectSection(mSelectedHeaderIndex, true);
            mSelectedHeaderIndex = -1;

            if (mSelectedVideo != null) {
                if (mIsPlayerInForeground) {
                    SplashPresenter.instance(getContext()).openVideo(mSelectedVideo);
                } else {
                    selectSectionItem(mSelectedVideo);
                }
                mSelectedVideo = null;
            }
        }
    }

    private void setupAdapter() {
        mSectionRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mSectionRowAdapter);
    }

    private void setupFragmentFactory() {
        mFragmentFactory = new BrowseSectionFragmentFactory(
                (row) -> {
                    focusOnContentIfNeeded();
                    mBrowsePresenter.onSectionFocused(getSelectedHeaderId());
                }
        );

        getMainFragmentRegistry().registerFragment(PageRow.class, mFragmentFactory);
    }

    private void setupUi() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        setBrandColor(ContextCompat.getColor(requireContext(), R.color.fastlane_background));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter(getHeaderResId(o), getIconUrl(o));
            }
        });
    }

    private int getHeaderResId(Object obj) {
        if (obj instanceof PageRow) {
            return ((SectionHeaderItem) ((PageRow) obj).getHeaderItem()).getResId();
        }
        return -1;
    }

    private String getIconUrl(Object obj) {
        if (obj instanceof PageRow) {
            return ((SectionHeaderItem) ((PageRow) obj).getHeaderItem()).getIconUrl();
        }
        return null;
    }

    private void setupEventListeners() {
        getHeadersSupportFragment().setOnHeaderClickedListener(
                (viewHolder, row) -> {
                    long headerId = row.getHeaderItem().getId();
                    mBrowsePresenter.onSectionFocused((int) headerId);
                }
        );

        setBrowseTransitionListener(new BrowseTransitionListener() {
            @Override
            public void onHeadersTransitionStart(boolean withHeaders) {
                if (withHeaders && mSidebarController != null) {
                    mSidebarController.focusSelectedRow();
                    startHeadersTransition(false);
                }
            }

            @Override
            public void onHeadersTransitionStop(boolean withHeaders) {
                if (withHeaders) {
                    startHeadersTransition(false);
                }
            }
        });

        if (getView() != null) {
            BrowseFrameLayout browseFrame = getView().findViewById(R.id.browse_frame);
            if (browseFrame != null) {
                browseFrame.setOnFocusSearchListener((focused, direction) -> {
                    if (mSidebarController == null) return null;

                    if (mSidebarController.isSidebarChild(focused)) {
                        if (direction == View.FOCUS_RIGHT) {
                            mSidebarController.setExpanded(false);
                            View mainView = getMainFragment() != null ? getMainFragment().getView() : null;
                            if (mainView != null) {
                                return mainView;
                            }
                        }
                        return null;
                    } else if (direction == View.FOCUS_LEFT) {
                        mSidebarController.setExpanded(true);
                        return mSidebarController.getSelectedFocusTarget();
                    }
                    return null;
                });
            }
        }
    }

    private void updateWaitTime() {
        if (getProgressBarManager() != null) {
            getProgressBarManager().setInitialDelay(0);
        }
    }

    // =========================================================================
    // BrowseView Methods
    // =========================================================================

    public void addSection(int index, BrowseSection section) {
        if (section == null) return;

        mSections.put(section.getId(), section);
        createHeader(index, section);

        if (mSidebarController != null) {
            mSidebarController.syncSections(mSections);
        }
    }

    public void removeSection(BrowseSection section) {
        if (section == null) return;

        mSections.remove(section.getId());
        removeHeader(section);

        if (mSidebarController != null) {
            mSidebarController.syncSections(mSections);
        }
    }

    public void removeAllSections() {
        mSections.clear();
        mSectionRowAdapter.clear();

        if (mSidebarController != null) {
            mSidebarController.syncSections(mSections);
        }
    }

    public void clearSection(BrowseSection section) {
        if (mFragmentFactory != null) {
            mFragmentFactory.clearCurrentFragment();
        }
    }

    public void updateSection(VideoGroup group) {
        restoreMainFragment();
        mFragmentFactory.updateFragment(group);
        fixPlayerSurface();
    }

    public void updateSection(SettingsGroup group) {
        restoreMainFragment();
        mFragmentFactory.updateFragment(group);
    }

    public void updateErrorIfEmpty(ErrorFragmentData data) {
        if (isEmpty()) {
            replaceMainFragment(new ErrorDialogFragment(data));
        }
    }

    private void replaceMainFragment(ErrorDialogFragment fragment) {
        Object forceOrNull = Helpers.getField(this, "mMainFragment");
        if (forceOrNull instanceof Fragment && mErrorFragment == null && !((Fragment) forceOrNull).isHidden()) {
            mErrorFragment = fragment;
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.hide((Fragment) forceOrNull);
            ft.add(R.id.scale_frame, mErrorFragment);
            ft.commitAllowingStateLoss();
        }
    }

    private void restoreMainFragment() {
        Object forceOrNull = Helpers.getField(this, "mMainFragment");
        if (forceOrNull instanceof Fragment && mErrorFragment != null) {
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.remove(mErrorFragment);
            ft.show((Fragment) forceOrNull);
            ft.commitAllowingStateLoss();
            mErrorFragment = null;
        }
    }

    private void fixPlayerSurface() {
        if (getView() != null) {
            getView().invalidate();
        }
    }

    private void createHeader(int index, BrowseSection header) {
        HeaderItem headerItem = new SectionHeaderItem(header);
        PageRow pageRow = new PageRow(headerItem);

        if (index == -1 || mSectionRowAdapter.size() < index) {
            mSectionRowAdapter.add(pageRow);
        } else {
            mSectionRowAdapter.add(index, pageRow);
        }
    }

    private void removeHeader(BrowseSection header) {
        Object foundHeader = null;
        for (Object item : mSectionRowAdapter.unmodifiableList()) {
            if (((PageRow) item).getHeaderItem().getId() == header.getId()) {
                foundHeader = item;
                break;
            }
        }
        if (foundHeader != null) {
            mSectionRowAdapter.remove(foundHeader);
        }
    }

    public void selectSection(int index, boolean focusOnContent) {
        if (index >= 0 && index < mSectionRowAdapter.size()) {
            mFocusOnContent = focusOnContent;

            if (mSidebarController != null) {
                mSidebarController.setSelectedSectionIndex(index, !focusOnContent);
            }

            if (getSelectedPosition() == index) {
                focusOnContentIfNeeded();
                mBrowsePresenter.onSectionFocused(getSelectedHeaderId());
            } else {
                setSelectedPosition(index, false);
            }
        }
    }

    public void selectSectionFromSidebar(int sectionIndex, int sectionId, boolean moveFocusToContent) {
        if (sectionIndex >= 0 && sectionIndex < mSectionRowAdapter.size()) {
            mFocusOnContent = moveFocusToContent;
            if (getSelectedPosition() != sectionIndex) {
                setSelectedPosition(sectionIndex, false);
            }
            mBrowsePresenter.onSectionFocused(sectionId);
            if (moveFocusToContent) {
                focusOnContent();
            }
        }
    }

    public void onSectionLongPressedFromSidebar(int sectionId) {
        if (mBrowsePresenter != null) {
            mBrowsePresenter.onSectionLongPressed(sectionId);
        }
    }

    public void focusOnContent() {
        startHeadersTransition(false);
        if (mSidebarController != null) {
            mSidebarController.setExpanded(false);
        }
        if (getMainFragment() != null && getMainFragment().getView() != null) {
            getMainFragment().getView().requestFocus();
        }
    }

    private void focusOnContentIfNeeded() {
        if (mFocusOnContent) {
            focusOnContent();
            mFocusOnContent = false;
        } else {
            startHeadersTransition(false);
        }
    }

    private int getSelectedHeaderId() {
        if (getSelectedPosition() >= mSectionRowAdapter.size() || getSelectedPosition() < 0) {
            return -1;
        }
        Object item = mSectionRowAdapter.get(getSelectedPosition());
        return (int) ((PageRow) item).getHeaderItem().getId();
    }

    public void selectSectionItem(int index) {
        if (index >= 0) {
            mFragmentFactory.setCurrentFragmentItemIndex(index);
        }
    }

    public void selectSectionItem(Video item) {
        if (item != null) {
            mFragmentFactory.selectFragmentItem(item);
        }
    }

    public void showProgressBar(boolean show) {
        mIsProgressBarShowing = show;
        Runnable callback = () -> {
            if (!isAdded() || getProgressBarManager() == null) return;
            if (show) {
                getProgressBarManager().show();
            } else {
                getProgressBarManager().hide();
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.run();
        } else {
            mHandler.post(callback);
        }
    }

    public boolean isProgressBarShowing() {
        return mIsProgressBarShowing;
    }

    public boolean isEmpty() {
        return mFragmentFactory == null || mFragmentFactory.isEmpty();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFragmentFactory.cleanup();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBrowsePresenter.onViewDestroyed();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mIsFragmentCreated) {
            mBrowsePresenter.onViewResumed();
        }
        mIsFragmentCreated = false;

        if (mTitleView != null) {
            mTitleView.update();
        }
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    // =========================================================================
    // Permanent Expandable Icon Sidebar Controller
    // =========================================================================

    private static class PermanentSidebarController {
        static final int COLLAPSED_WIDTH_DP = 72;
        static final int EXPANDED_WIDTH_DP = 240;

        private static final int ACTION_PROFILE = -100;
        private static final int ACTION_SEARCH = -101;

        private final Context mContext;
        private final BrowseFragment mFragment;
        private final Handler mUiHandler = new Handler(Looper.getMainLooper());

        private final FrameLayout mSidebarWrapper;
        private final LinearLayout mItemsContainer;
        private final List<RowHolder> mHolders = new ArrayList<>();

        private boolean mIsExpanded = false;
        private int mSelectedRowIndex = 2;
        private ValueAnimator mWidthAnimator;
        private Runnable mPendingSectionLoad;

        private static class SidebarEntry {
            final int sectionIndex;
            final int sectionId;
            final String title;
            final int iconResId;
            final boolean isHeader;

            SidebarEntry(int sectionIndex, int sectionId, String title, int iconResId, boolean isHeader) {
                this.sectionIndex = sectionIndex;
                this.sectionId = sectionId;
                this.title = title;
                this.iconResId = iconResId;
                this.isHeader = isHeader;
            }
        }

        private static class RowHolder {
            final LinearLayout rowView;
            final ImageView iconView;
            final TextView titleView;
            final SidebarEntry entry;

            RowHolder(LinearLayout rowView, ImageView iconView, TextView titleView, SidebarEntry entry) {
                this.rowView = rowView;
                this.iconView = iconView;
                this.titleView = titleView;
                this.entry = entry;
            }
        }

        PermanentSidebarController(Context context, BrowseFragment fragment) {
            mContext = context;
            mFragment = fragment;

            mSidebarWrapper = new FrameLayout(context);
            FrameLayout.LayoutParams wrapperParams = new FrameLayout.LayoutParams(
                    dpToPx(context, COLLAPSED_WIDTH_DP),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.START
            );
            mSidebarWrapper.setLayoutParams(wrapperParams);
            mSidebarWrapper.setBackgroundColor(Color.parseColor("#F20F0F0F"));
            mSidebarWrapper.setElevation(dpToPx(context, 16));

            ScrollView scrollView = new ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(false);
            scrollView.setFillViewport(true);

            mItemsContainer = new LinearLayout(context);
            mItemsContainer.setOrientation(LinearLayout.VERTICAL);
            mItemsContainer.setGravity(Gravity.CENTER_VERTICAL);
            int vPad = dpToPx(context, 16);
            mItemsContainer.setPadding(dpToPx(context, 10), vPad, dpToPx(context, 10), vPad);

            scrollView.addView(mItemsContainer);
            mSidebarWrapper.addView(scrollView);
        }

        View getSidebarView() {
            return mSidebarWrapper;
        }

        boolean isSidebarChild(View v) {
            if (v == null) return false;
            for (RowHolder holder : mHolders) {
                if (holder.rowView == v) return true;
            }
            return false;
        }

        View getSelectedFocusTarget() {
            if (mSelectedRowIndex >= 0 && mSelectedRowIndex < mHolders.size()) {
                return mHolders.get(mSelectedRowIndex).rowView;
            }
            return mHolders.size() > 2 ? mHolders.get(2).rowView : (mHolders.isEmpty() ? null : mHolders.get(0).rowView);
        }

        void focusSelectedRow() {
            setExpanded(true);
            View target = getSelectedFocusTarget();
            if (target != null) {
                target.requestFocus();
            }
        }

        void setSelectedSectionIndex(int sectionIndex, boolean expandAndFocus) {
            for (int i = 0; i < mHolders.size(); i++) {
                if (mHolders.get(i).entry.sectionIndex == sectionIndex) {
                    mSelectedRowIndex = i;
                    break;
                }
            }
            if (expandAndFocus) {
                focusSelectedRow();
            } else {
                setExpanded(false);
                updateVisualSelection();
            }
        }

        void syncSections(Map<Integer, BrowseSection> sectionsMap) {
            boolean hadFocus = isAnySidebarItemFocused();
            mItemsContainer.removeAllViews();
            mHolders.clear();

            int accountIconId = mContext.getResources().getIdentifier("header_account", "drawable", mContext.getPackageName());
            if (accountIconId == 0) accountIconId = android.R.drawable.sym_def_app_icon;

            int searchIconId = mContext.getResources().getIdentifier("action_search", "drawable", mContext.getPackageName());
            if (searchIconId == 0) searchIconId = android.R.drawable.ic_menu_search;

            List<SidebarEntry> entries = new ArrayList<>();
            entries.add(new SidebarEntry(-1, ACTION_PROFILE, "Profile", accountIconId, true));
            entries.add(new SidebarEntry(-1, ACTION_SEARCH, "Search", searchIconId, false));

            int secIdx = 0;
            for (BrowseSection section : sectionsMap.values()) {
                int iconRes = section.getResId() > 0 ? section.getResId() : android.R.drawable.ic_media_play;
                entries.add(new SidebarEntry(secIdx, section.getId(), section.getTitle(), iconRes, false));
                secIdx++;
            }

            for (int i = 0; i < entries.size(); i++) {
                RowHolder holder = createRowView(entries.get(i), i);
                mHolders.add(holder);
                mItemsContainer.addView(holder.rowView);
            }

            if (mSelectedRowIndex >= mHolders.size()) {
                mSelectedRowIndex = Math.min(2, mHolders.size() - 1);
            }

            if (mIsExpanded) {
                for (RowHolder holder : mHolders) {
                    holder.titleView.setVisibility(View.VISIBLE);
                    holder.titleView.setAlpha(1f);
                }
                if (hadFocus && mSelectedRowIndex >= 0 && mSelectedRowIndex < mHolders.size()) {
                    mHolders.get(mSelectedRowIndex).rowView.requestFocus();
                }
            } else {
                for (RowHolder holder : mHolders) {
                    holder.titleView.setVisibility(View.GONE);
                    holder.titleView.setAlpha(0f);
                }
            }

            updateVisualSelection();
        }

        private RowHolder createRowView(SidebarEntry entry, final int rowIndex) {
            LinearLayout row = new LinearLayout(mContext);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dpToPx(mContext, entry.isHeader ? 50 : 44)
            );
            rowParams.topMargin = dpToPx(mContext, entry.isHeader ? 0 : 3);
            rowParams.bottomMargin = dpToPx(mContext, entry.isHeader ? 10 : 2);
            row.setLayoutParams(rowParams);
            row.setPadding(dpToPx(mContext, 12), 0, dpToPx(mContext, 14), 0);

            ImageView icon = new ImageView(mContext);
            int iconSize = dpToPx(mContext, entry.isHeader ? 26 : 24);
            icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));

            Drawable drawable = null;
            try {
                drawable = ContextCompat.getDrawable(mContext, entry.iconResId);
            } catch (Exception ignored) {
            }
            if (drawable == null) {
                drawable = ContextCompat.getDrawable(mContext, android.R.drawable.sym_def_app_icon);
            }
            icon.setImageDrawable(drawable != null ? drawable.mutate() : null);

            TextView title = new TextView(mContext);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            titleParams.leftMargin = dpToPx(mContext, 18);
            title.setLayoutParams(titleParams);
            title.setText(entry.title);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, entry.isHeader ? 16 : 15);
            title.setTypeface(null, entry.isHeader ? Typeface.BOLD : Typeface.NORMAL);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);

            row.addView(icon);
            row.addView(title);

            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (!mIsExpanded) {
                        setExpanded(true);
                    }
                    if (entry.sectionIndex >= 0) {
                        mSelectedRowIndex = rowIndex;
                        if (mPendingSectionLoad != null) {
                            mUiHandler.removeCallbacks(mPendingSectionLoad);
                        }
                        mPendingSectionLoad = () -> mFragment.selectSectionFromSidebar(
                                entry.sectionIndex, entry.sectionId, false
                        );
                        mUiHandler.postDelayed(mPendingSectionLoad, 180);
                    }
                } else {
                    mSidebarWrapper.post(() -> {
                        if (!isAnySidebarItemFocused()) {
                            setExpanded(false);
                        }
                    });
                }
                updateVisualSelection();
            });

            row.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (entry.sectionId == ACTION_SEARCH) {
                        SearchPresenter.instance(mContext).startSearch(null);
                        return true;
                    } else if (entry.sectionId == ACTION_PROFILE) {
                        AccountSelectionPresenter.instance(mContext).show(true);
                        return true;
                    } else if (entry.sectionIndex >= 0) {
                        if (mPendingSectionLoad != null) {
                            mUiHandler.removeCallbacks(mPendingSectionLoad);
                        }
                        mFragment.selectSectionFromSidebar(entry.sectionIndex, entry.sectionId, true);
                        return true;
                    }
                }
                return false;
            });

            row.setOnClickListener(v -> {
                if (entry.sectionId == ACTION_PROFILE) {
                    AccountSelectionPresenter.instance(mContext).show(true);
                } else if (entry.sectionId == ACTION_SEARCH) {
                    SearchPresenter.instance(mContext).startSearch(null);
                } else if (entry.sectionIndex >= 0) {
                    mSelectedRowIndex = rowIndex;
                    if (mPendingSectionLoad != null) {
                        mUiHandler.removeCallbacks(mPendingSectionLoad);
                    }
                    mFragment.selectSectionFromSidebar(entry.sectionIndex, entry.sectionId, true);
                }
            });

            row.setOnLongClickListener(v -> {
                if (entry.sectionIndex >= 0) {
                    mFragment.onSectionLongPressedFromSidebar(entry.sectionId);
                    return true;
                }
                return false;
            });

            return new RowHolder(row, icon, title, entry);
        }

        private boolean isAnySidebarItemFocused() {
            for (RowHolder holder : mHolders) {
                if (holder.rowView.hasFocus()) {
                    return true;
                }
            }
            return false;
        }

        void setExpanded(boolean expanded) {
            if (mIsExpanded == expanded) return;
            mIsExpanded = expanded;

            int startWidth = mSidebarWrapper.getWidth() > 0
                    ? mSidebarWrapper.getWidth()
                    : dpToPx(mContext, expanded ? COLLAPSED_WIDTH_DP : EXPANDED_WIDTH_DP);
            int targetWidth = dpToPx(mContext, expanded ? EXPANDED_WIDTH_DP : COLLAPSED_WIDTH_DP);

            if (mWidthAnimator != null && mWidthAnimator.isRunning()) {
                mWidthAnimator.cancel();
            }

            mWidthAnimator = ValueAnimator.ofInt(startWidth, targetWidth);
            mWidthAnimator.setDuration(180);
            mWidthAnimator.setInterpolator(new DecelerateInterpolator());
            mWidthAnimator.addUpdateListener(anim -> {
                ViewGroup.LayoutParams lp = mSidebarWrapper.getLayoutParams();
                lp.width = (int) anim.getAnimatedValue();
                mSidebarWrapper.setLayoutParams(lp);
            });
            mWidthAnimator.start();

            for (RowHolder holder : mHolders) {
                if (expanded) {
                    holder.titleView.setVisibility(View.VISIBLE);
                    holder.titleView.animate().alpha(1f).setDuration(160).start();
                } else {
                    holder.titleView.animate().alpha(0f).setDuration(90).withEndAction(() -> {
                        if (!mIsExpanded) {
                            holder.titleView.setVisibility(View.GONE);
                        }
                    }).start();
                }
            }
            updateVisualSelection();
        }

        private void updateVisualSelection() {
            for (int i = 0; i < mHolders.size(); i++) {
                RowHolder holder = mHolders.get(i);
                boolean isFocused = holder.rowView.hasFocus();
                boolean isSelected = (i == mSelectedRowIndex);

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dpToPx(mContext, 24));

                if (isFocused) {
                    bg.setColor(Color.WHITE);
                    holder.rowView.setBackground(bg);
                    holder.iconView.setColorFilter(Color.BLACK);
                    holder.titleView.setTextColor(Color.BLACK);
                    holder.titleView.setTypeface(null, Typeface.BOLD);
                } else if (isSelected) {
                    bg.setColor(Color.parseColor("#2AFFFFFF"));
                    holder.rowView.setBackground(bg);
                    holder.iconView.setColorFilter(Color.WHITE);
                    holder.titleView.setTextColor(Color.WHITE);
                    holder.titleView.setTypeface(null, Typeface.BOLD);
                } else {
                    bg.setColor(Color.TRANSPARENT);
                    holder.rowView.setBackground(bg);
                    holder.iconView.setColorFilter(Color.parseColor("#AAAAAA"));
                    holder.titleView.setTextColor(Color.parseColor("#AAAAAA"));
                    holder.titleView.setTypeface(null, Typeface.NORMAL);
                }
            }
        }
    }
}

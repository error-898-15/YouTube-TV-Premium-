// File Path: smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/browse/BrowseFragment.java
package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.app.HeadersSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.misc.CrashRestorer;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.IconHeaderItemPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.dialog.ErrorDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.headers.ExtendedHeadersSupportFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.misc.ProgressBarManager;

import java.util.HashMap;
import java.util.Map;

public class BrowseFragment extends BrowseSupportFragment implements BrowseView {
    private static final String TAG = BrowseFragment.class.getSimpleName();
    private ArrayObjectAdapter mSectionRowAdapter;
    private BrowsePresenter mBrowsePresenter;
    private Map<Integer, BrowseSection> mSections;
    private BrowseSectionFragmentFactory mSectionFragmentFactory;
    private Handler mHandler;
    private ProgressBarManager mProgressBarManager;
    private boolean mIsFragmentCreated;
    private boolean mFocusOnContent;
    private CrashRestorer mCrashRestorer;
    private YouTubeSidebarUiHelper mSidebarUiHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);

        if (getContext() == null) {
            return;
        }

        mCrashRestorer = new CrashRestorer(getContext(), savedInstanceState);
        mIsFragmentCreated = true;

        mSections = new HashMap<>();
        mHandler = new Handler(Looper.getMainLooper());
        mBrowsePresenter = BrowsePresenter.instance(getContext());
        mBrowsePresenter.setView(this);
        mProgressBarManager = new ProgressBarManager();
        mSidebarUiHelper = new YouTubeSidebarUiHelper(this);

        setupAdapter();
        setupFragmentFactory();
        setupUi();

        enableMainFragmentScaling(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mCrashRestorer.persistHeaderIndex(outState, getSelectedPosition());
        mCrashRestorer.persistVideo(outState, mBrowsePresenter.getCurrentVideo());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        mProgressBarManager.setRootView((ViewGroup) root);

        if (mSidebarUiHelper != null && root instanceof ViewGroup) {
            mSidebarUiHelper.attachToBrowseRoot((ViewGroup) root);
            root.post(() -> mSidebarUiHelper.enforceSlimRailLayout(false));
        }

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupEventListeners();
        prepareEntranceTransition();
        mBrowsePresenter.onViewInitialized();

        mCrashRestorer.restoreHeader((idx, video) -> {
            selectSection(idx, true);
            selectSectionItem(video);
        });
        mCrashRestorer.restorePlayback();
    }

    @Override
    public HeadersSupportFragment onCreateHeadersSupportFragment() {
        return new ExtendedHeadersSupportFragment();
    }

    private void setupEventListeners() {
        getHeadersSupportFragment().setOnHeaderClickedListener(
                (viewHolder, row) -> {
                    long headerId = row.getHeaderItem().getId();
                    int newPosition = indexOf(headerId);

                    if (getHeadersSupportFragment().getSelectedPosition() != newPosition) {
                        getHeadersSupportFragment().setSelectedPosition(newPosition);
                    } else {
                        mBrowsePresenter.onSectionFocused((int) headerId);
                        startHeadersTransitionSafe(false);
                    }
                }
        );

        ((ExtendedHeadersSupportFragment) getHeadersSupportFragment()).setOnHeaderLongPressedListener(
                (viewHolder, row) -> {
                    long headerId = row.getHeaderItem().getId();
                    mBrowsePresenter.onSectionLongPressed((int) headerId);
                }
        );

        setBrowseTransitionListener(new BrowseTransitionListener() {
            @Override
            public void onHeadersTransitionStart(boolean withHeaders) {
                super.onHeadersTransitionStart(withHeaders);
                if (mSidebarUiHelper != null) {
                    mSidebarUiHelper.enforceSlimRailLayout(false);
                }
            }

            @Override
            public void onHeadersTransitionStop(boolean withHeaders) {
                super.onHeadersTransitionStop(withHeaders);
                if (mSidebarUiHelper != null) {
                    mSidebarUiHelper.enforceSlimRailLayout(false);
                }
            }
        });

        setOnSearchClickedListener(view -> SearchPresenter.instance(getContext()).startSearch(null));
    }

    private void setupFragmentFactory() {
        mSectionFragmentFactory = new BrowseSectionFragmentFactory(
                (row) -> {
                    focusOnContentIfNeeded();
                    mBrowsePresenter.onSectionFocused(getSelectedHeaderId());
                    if (mSidebarUiHelper != null && row != null && row.getHeaderItem() != null) {
                        mSidebarUiHelper.updateActiveSectionHeader(row.getHeaderItem().getName());
                        mSidebarUiHelper.enforceSlimRailLayout(false);
                    }
                }
        );

        getMainFragmentRegistry().registerFragment(PageRow.class, mSectionFragmentFactory);
    }

    public void selectSettingsSection() {
        for (int i = 0; i < mSectionRowAdapter.size(); i++) {
            PageRow row = (PageRow) mSectionRowAdapter.get(i);
            if (row.getHeaderItem().getId() == MediaGroup.TYPE_SETTINGS) {
                selectSection(i, true);
                return;
            }
        }
    }

    private int indexOf(long headerId) {
        for (int i = 0; i < mSectionRowAdapter.size(); i++) {
            PageRow row = (PageRow) mSectionRowAdapter.get(i);
            HeaderItem header = row.getHeaderItem();
            if (header.getId() == headerId) {
                return i;
            }
        }
        return 0;
    }

    private void setupAdapter() {
        mSectionRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mSectionRowAdapter);
    }

    private void setupUi() {
        if (getContext() == null) {
            return;
        }

        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        int brandAccentColorRes = Helpers.getThemeAttr(getContext(), R.attr.brandAccentColor);
        updateBadge();

        setBrandColor(0xFF0F0F0F);
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), brandAccentColorRes));

        setHeaderPresenterSelector(new PresenterSelector() {
            private final Map<Integer, Presenter> mPresenterMap = new HashMap<>();

            @Override
            public Presenter getPresenter(Object o) {
                Presenter presenter = mPresenterMap.get(o.hashCode());

                if (presenter == null) {
                    presenter = new IconHeaderItemPresenter(getHeaderResId(o), getIconUrl(o));
                    mPresenterMap.put(o.hashCode(), presenter);
                }

                return presenter;
            }

            private int getHeaderResId(Object o) {
                if (o instanceof PageRow) {
                    return ((SectionHeaderItem) ((PageRow) o).getHeaderItem()).getResId();
                }
                return -1;
            }

            private String getIconUrl(Object o) {
                if (o instanceof PageRow) {
                    return ((SectionHeaderItem) ((PageRow) o).getHeaderItem()).getIconUrl();
                }
                return null;
            }
        });
    }

    private int getSelectedHeaderId() {
        if (getSelectedPosition() >= mSectionRowAdapter.size() || getSelectedPosition() < 0) {
            return -1;
        }
        return (int) ((PageRow) mSectionRowAdapter.get(getSelectedPosition())).getHeaderItem().getId();
    }

    public void updateErrorIfEmpty(ErrorFragmentData data) {
        mHandler.postDelayed(() -> showErrorIfEmpty(data), 500);
    }

    @Override
    public void showError(ErrorFragmentData data) {
        replaceMainFragment(new ErrorDialogFragment(data));
    }

    private void showErrorIfEmpty(ErrorFragmentData data) {
        if (isEmpty()) {
            replaceMainFragment(new ErrorDialogFragment(data));
        }
    }

    private void replaceMainFragment(Fragment fragment) {
        Fragment mainFragment = getMainFragment();

        if (mainFragment != null && fragment != null && mainFragment != fragment) {
            Helpers.setField(this, "mMainFragment", fragment);

            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.replace(R.id.scale_frame, fragment);
            mFocusOnContent = hasFocus();
            ft.runOnCommit(this::focusOnContentIfNeeded);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void addSection(int index, BrowseSection section) {
        if (section == null) {
            return;
        }

        if (mSections.get(section.getId()) != null && (index == -1 || indexOf(section.getId()) == index)) {
            return;
        }

        removeSection(section);

        mSections.put(section.getId(), section);
        createHeader(index, section);
    }

    @Override
    public void removeSection(BrowseSection section) {
        if (section == null) {
            return;
        }

        mSections.remove(section.getId());
        removeHeader(section);
    }

    @Override
    public void removeAllSections() {
        mSections.clear();
        mSectionRowAdapter.clear();
    }

    @Override
    public void updateSection(VideoGroup group) {
        restoreMainFragment();
        mSectionFragmentFactory.updateCurrentFragment(group);
        if (mSidebarUiHelper != null) {
            mSidebarUiHelper.enforceSlimRailLayout(false);
        }
    }

    @Override
    public void updateSection(SettingsGroup group) {
        restoreMainFragment();
        mSectionFragmentFactory.updateCurrentFragment(group);
        if (mSidebarUiHelper != null) {
            mSidebarUiHelper.enforceSlimRailLayout(false);
        }
    }

    @Override
    public void selectSection(int index, boolean focusOnContent) {
        if (index >= 0 && mSectionRowAdapter.size() > 0) {
            mFocusOnContent = focusOnContent;

            if (getSelectedPosition() == index) {
                focusOnContentIfNeeded();
                mBrowsePresenter.onSectionFocused(getSelectedHeaderId());
            }

            int targetIdx = index < mSectionRowAdapter.size() ? index : mSectionRowAdapter.size() - 1;
            setSelectedPosition(targetIdx, false);
        }
    }

    @Override
    public void focusOnContent() {
        startHeadersTransitionSafe(false);
        if (mSidebarUiHelper != null) {
            mSidebarUiHelper.enforceSlimRailLayout(false);
        }
        if (getMainFragment() != null && getMainFragment().getView() != null) {
            getMainFragment().getView().requestFocus();
        }
    }

    private void focusOnContentIfNeeded() {
        if (mFocusOnContent) {
            focusOnContent();
            mFocusOnContent = false;
        }
    }

    private boolean hasFocus() {
        if (getMainFragment() == null || getMainFragment().getView() == null) {
            return false;
        }
        return getMainFragment().getView().hasFocus();
    }

    @Override
    public void selectSectionItem(int index) {
        if (index >= 0) {
            mSectionFragmentFactory.setCurrentFragmentItemIndex(index);
        }
    }

    @Override
    public void selectSectionItem(Video item) {
        if (item != null) {
            mSectionFragmentFactory.selectCurrentFragmentItem(item);
        }
    }

    private void startHeadersTransitionSafe(boolean withHeaders) {
        if (!Utils.checkActivity(getActivity())) {
            return;
        }

        try {
            startHeadersTransition(withHeaders);
            if (mSidebarUiHelper != null) {
                mHandler.post(() -> mSidebarUiHelper.enforceSlimRailLayout(false));
            }
        } catch (IllegalStateException e) {
            // NOP
        }
    }

    private void restoreMainFragment() {
        Fragment currentFragment = mSectionFragmentFactory.getCurrentFragment();

        if (currentFragment != null) {
            replaceMainFragment(currentFragment);
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

    @Override
    public void clearSection(BrowseSection section) {
        mSectionFragmentFactory.clearCurrentFragment();
    }

    @Override
    public void onDestroyView() {
        mSectionFragmentFactory.cleanup();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBrowsePresenter.onViewDestroyed();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mIsFragmentCreated) {
            mBrowsePresenter.onViewPaused();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsFragmentCreated) {
            mBrowsePresenter.onViewResumed();
        }
        mIsFragmentCreated = false;
        if (mSidebarUiHelper != null) {
            mSidebarUiHelper.loadAccountAvatar();
            mSidebarUiHelper.enforceSlimRailLayout(false);
        }
    }

    @Override
    public void showProgressBar(boolean show) {
        Runnable callback = show ? mProgressBarManager::show : mProgressBarManager::hide;
        new Handler(Looper.getMainLooper()).post(callback);
    }

    @Override
    public boolean isProgressBarShowing() {
        return mProgressBarManager.isShowing();
    }

    @Override
    public boolean isEmpty() {
        return mSectionFragmentFactory == null || mSectionFragmentFactory.isEmpty();
    }

    @Override
    public void updateBadge() {
        if (getContext() == null) {
            return;
        }

        SplashPresenter splashPresenter = SplashPresenter.instance(getContext());
        if (splashPresenter == null) {
            return;
        }

        int appLogoRes = Helpers.getThemeAttr(getContext(), R.attr.appLogo);
        Drawable bridgeIcon = Utils.getDrawable(getContext(), splashPresenter.getBridgePackageName(), "app_icon");
        setBadgeDrawable(bridgeIcon != null ? bridgeIcon : appLogoRes > 0 ? ContextCompat.getDrawable(getContext(), appLogoRes) : null);
    }
    }

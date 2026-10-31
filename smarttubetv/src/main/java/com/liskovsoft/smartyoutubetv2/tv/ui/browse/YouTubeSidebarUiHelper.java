// File Path: smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/browse/YouTubeSidebarUiHelper.java
package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.leanback.app.HeadersSupportFragment;
import androidx.leanback.widget.VerticalGridView;

import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class YouTubeSidebarUiHelper {
    private static final int SIDEBAR_COLLAPSED_WIDTH_DP = 72;
    private static final int SIDEBAR_EXPANDED_WIDTH_DP = 240;
    private static final int CONTENT_MARGIN_START_DP = 82;
    private static final int CONTENT_MARGIN_TOP_DP = 76;

    private final BrowseFragment mFragment;
    private final Context mContext;
    private View mHeadersDock;
    private View mContainerDock;
    private ImageView mAvatarView;
    private ImageView mSearchBtn;
    private ImageView mSettingsBtn;
    private TextView mTopCategoryLabel;
    private TextView mTopSubHeaderLabel;
    private boolean mSidebarExpanded = false;

    public YouTubeSidebarUiHelper(BrowseFragment fragment) {
        mFragment = fragment;
        mContext = fragment.requireContext();
    }

    public void attachToBrowseRoot(ViewGroup browseRoot) {
        if (browseRoot == null) {
            return;
        }

        browseRoot.setBackgroundColor(Color.parseColor("#0F0F0F"));

        mHeadersDock = browseRoot.findViewById(androidx.leanback.R.id.browse_headers_dock);
        mContainerDock = browseRoot.findViewById(androidx.leanback.R.id.browse_container_dock);

        hideDefaultTitleView(browseRoot);
        setupTopHeaderOverlay(browseRoot);
        setupPersistentSlimSidebar(browseRoot);
        loadAccountAvatar();
    }

    private void hideDefaultTitleView(ViewGroup root) {
        View titleGroup = root.findViewById(R.id.browse_title_group);
        if (titleGroup != null) {
            titleGroup.setVisibility(View.GONE);
        }
    }

    private void setupTopHeaderOverlay(ViewGroup browseRoot) {
        RelativeLayout topBar = new RelativeLayout(mContext);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68));
        topLp.setMarginStart(dp(CONTENT_MARGIN_START_DP));
        topLp.topMargin = dp(10);
        topBar.setLayoutParams(topLp);
        topBar.setPadding(dp(24), dp(6), dp(36), 0);

        // Left stack: "HOME" + "Recommended"
        LinearLayout leftStack = new LinearLayout(mContext);
        leftStack.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams leftLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftLp.addRule(RelativeLayout.ALIGN_PARENT_START);
        leftLp.addRule(RelativeLayout.CENTER_VERTICAL);
        leftStack.setLayoutParams(leftLp);

        mTopCategoryLabel = new TextView(mContext);
        mTopCategoryLabel.setText("HOME");
        mTopCategoryLabel.setTextColor(Color.parseColor("#AAAAAA"));
        mTopCategoryLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        mTopCategoryLabel.setTypeface(Typeface.DEFAULT_BOLD);
        mTopCategoryLabel.setLetterSpacing(0.08f);

        mTopSubHeaderLabel = new TextView(mContext);
        mTopSubHeaderLabel.setText("Recommended");
        mTopSubHeaderLabel.setTextColor(Color.parseColor("#F1F1F1"));
        mTopSubHeaderLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mTopSubHeaderLabel.setTypeface(Typeface.DEFAULT_BOLD);

        leftStack.addView(mTopCategoryLabel);
        leftStack.addView(mTopSubHeaderLabel);
        topBar.addView(leftStack);

        // Right brand badge: Red Play Box + "YouTube"
        LinearLayout brandBox = new LinearLayout(mContext);
        brandBox.setOrientation(LinearLayout.HORIZONTAL);
        brandBox.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams rightLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        rightLp.addRule(RelativeLayout.CENTER_VERTICAL);
        brandBox.setLayoutParams(rightLp);

        FrameLayout redBadge = new FrameLayout(mContext);
        GradientDrawable redBg = new GradientDrawable();
        redBg.setShape(GradientDrawable.RECTANGLE);
        redBg.setCornerRadius(dp(6));
        redBg.setColor(Color.parseColor("#FF0000"));
        redBadge.setBackground(redBg);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(30), dp(21));
        badgeLp.setMarginEnd(dp(6));
        redBadge.setLayoutParams(badgeLp);

        TextView playIcon = new TextView(mContext);
        playIcon.setText("▶");
        playIcon.setTextColor(Color.WHITE);
        playIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        playIcon.setGravity(Gravity.CENTER);
        redBadge.addView(playIcon, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView ytText = new TextView(mContext);
        ytText.setText("YouTube");
        ytText.setTextColor(Color.WHITE);
        ytText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        ytText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        ytText.setLetterSpacing(-0.03f);

        brandBox.addView(redBadge);
        brandBox.addView(ytText);
        topBar.addView(brandBox);

        browseRoot.addView(topBar);
    }

    private void setupPersistentSlimSidebar(ViewGroup browseRoot) {
        if (mHeadersDock instanceof ViewGroup) {
            ViewGroup dock = (ViewGroup) mHeadersDock;
            dock.setPadding(0, dp(96), 0, dp(68));
            dock.setBackgroundColor(Color.parseColor("#0F0F0F"));
        }

        // Top Avatar + Search Button Overlay inside Left Rail
        LinearLayout topRailOverlay = new LinearLayout(mContext);
        topRailOverlay.setOrientation(LinearLayout.VERTICAL);
        topRailOverlay.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams topRailLp = new FrameLayout.LayoutParams(
                dp(SIDEBAR_COLLAPSED_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.TOP);
        topRailLp.topMargin = dp(18);
        topRailOverlay.setLayoutParams(topRailLp);
        topRailOverlay.setElevation(dp(12));

        mAvatarView = new ImageView(mContext);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        avatarLp.bottomMargin = dp(14);
        mAvatarView.setLayoutParams(avatarLp);
        mAvatarView.setFocusable(true);
        mAvatarView.setClickable(true);
        mAvatarView.setPadding(dp(2), dp(2), dp(2), dp(2));
        mAvatarView.setBackground(createCircleFocusDrawable());
        mAvatarView.setImageResource(R.drawable.browse_title_account);
        mAvatarView.setOnClickListener(v -> AccountSelectionPresenter.instance(mContext).show(true));
        topRailOverlay.addView(mAvatarView);

        mSearchBtn = new ImageView(mContext);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        mSearchBtn.setLayoutParams(searchLp);
        mSearchBtn.setPadding(dp(9), dp(9), dp(9), dp(9));
        mSearchBtn.setFocusable(true);
        mSearchBtn.setClickable(true);
        mSearchBtn.setBackground(createCircleFocusDrawable());
        mSearchBtn.setImageResource(R.drawable. lb_ic_in_app_search);
        ImageViewCompat.setImageTintList(mSearchBtn,
                ContextCompat.getColorStateList(mContext, R.color.header_content_color));
        mSearchBtn.setOnClickListener(v -> SearchPresenter.instance(mContext).startSearch(null));
        topRailOverlay.addView(mSearchBtn);

        browseRoot.addView(topRailOverlay);

        // Bottom Settings Icon pinned to bottom-left of Rail
        FrameLayout bottomRailOverlay = new FrameLayout(mContext);
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(
                dp(SIDEBAR_COLLAPSED_WIDTH_DP), dp(56), Gravity.START | Gravity.BOTTOM);
        bottomLp.bottomMargin = dp(14);
        bottomRailOverlay.setLayoutParams(bottomLp);
        bottomRailOverlay.setElevation(dp(12));

        mSettingsBtn = new ImageView(mContext);
        FrameLayout.LayoutParams settingsLp = new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER);
        mSettingsBtn.setLayoutParams(settingsLp);
        mSettingsBtn.setPadding(dp(9), dp(9), dp(9), dp(9));
        mSettingsBtn.setFocusable(true);
        mSettingsBtn.setClickable(true);
        mSettingsBtn.setBackground(createCircleFocusDrawable());
        mSettingsBtn.setImageResource(R.drawable.header_settings);
        ImageViewCompat.setImageTintList(mSettingsBtn,
                ContextCompat.getColorStateList(mContext, R.color.header_content_color));
        mSettingsBtn.setOnClickListener(v -> mFragment.selectSettingsSection());
        bottomRailOverlay.addView(mSettingsBtn);

        browseRoot.addView(bottomRailOverlay);
    }

    public void enforceSlimRailLayout(boolean expanded) {
        mSidebarExpanded = expanded;
        HeadersSupportFragment headersFragment = mFragment.getHeadersSupportFragment();
        if (headersFragment != null && headersFragment.getView() != null) {
            View headersRoot = headersFragment.getView();
            headersRoot.setVisibility(View.VISIBLE);
            headersRoot.setTranslationX(0f);

            ViewGroup.LayoutParams lp = headersRoot.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                mlp.setMarginStart(0);
                mlp.width = dp(expanded ? SIDEBAR_EXPANDED_WIDTH_DP : SIDEBAR_COLLAPSED_WIDTH_DP);
                headersRoot.setLayoutParams(mlp);
            }

            View fadeEdge = headersRoot.findViewById(androidx.leanback.R.id.fade_out_edge);
            if (fadeEdge != null) {
                fadeEdge.setVisibility(View.GONE);
            }

            VerticalGridView gridView = headersFragment.getVerticalGridView();
            if (gridView != null) {
                ViewGroup.LayoutParams glp = gridView.getLayoutParams();
                if (glp != null) {
                    glp.width = dp(expanded ? SIDEBAR_EXPANDED_WIDTH_DP : SIDEBAR_COLLAPSED_WIDTH_DP);
                    gridView.setLayoutParams(glp);
                }
                updateVisibleHeaderLabels(gridView, expanded);
            }
        }

        if (mContainerDock != null) {
            ViewGroup.LayoutParams clp = mContainerDock.getLayoutParams();
            if (clp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mclp = (ViewGroup.MarginLayoutParams) clp;
                mclp.setMarginStart(dp(CONTENT_MARGIN_START_DP));
                mclp.topMargin = dp(CONTENT_MARGIN_TOP_DP);
                mContainerDock.setLayoutParams(mclp);
            }
        }
    }

    private void updateVisibleHeaderLabels(VerticalGridView gridView, boolean showLabels) {
        int childCount = gridView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = gridView.getChildAt(i);
            if (child != null) {
                View label = child.findViewById(R.id.header_label);
                if (label != null) {
                    label.setVisibility(showLabels ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    public void updateActiveSectionHeader(String categoryName) {
        if (!TextUtils.isEmpty(categoryName) && mTopCategoryLabel != null) {
            mTopCategoryLabel.setText(categoryName.toUpperCase());
            if (mTopSubHeaderLabel != null) {
                if ("HOME".equalsIgnoreCase(categoryName)) {
                    mTopSubHeaderLabel.setText("Recommended");
                } else {
                    mTopSubHeaderLabel.setText(categoryName);
                }
            }
        }
    }

    public void loadAccountAvatar() {
        if (mAvatarView == null) {
            return;
        }
        MediaServiceManager.instance().loadAccounts(accounts -> {
            if (accounts == null || accounts.isEmpty()) {
                return;
            }
            for (Account account : accounts) {
                if (account.isSelected() && account.getAvatarImageUrl() != null) {
                    Glide.with(mContext)
                            .load(account.getAvatarImageUrl())
                            .apply(ViewUtil.glideOptions().circleCrop())
                            .into(mAvatarView);
                    break;
                }
            }
        });
    }

    private GradientDrawable createCircleFocusDrawable() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.TRANSPARENT);
        return drawable;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, mContext.getResources().getDisplayMetrics());
    }
  }

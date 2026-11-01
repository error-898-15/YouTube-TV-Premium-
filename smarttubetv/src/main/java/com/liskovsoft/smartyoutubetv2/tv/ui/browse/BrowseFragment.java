package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Ekta file-er moddhei YouTube TV style Permanent Icon Sidebar + Expandable Drawer.
 * Video grid-e focus thakle sudhu Icon dekhabe (72dp), ar Left chaple expand hoye Text + Icon dekhabe (240dp).
 */
public class BrowseFragment extends Fragment {

    private PermanentSidebarController mSidebarController;
    private FrameLayout mRootContainer;
    private FrameLayout mContentContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = requireContext();

        // Root Container
        mRootContainer = new FrameLayout(context);
        mRootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        mRootContainer.setBackgroundColor(Color.parseColor("#0F0F0F"));

        // ডানদিকের মেইন কনটেন্ট কন্টেইনার (যেখানে ভিডিও গ্রিড ও সার্চ রেজাল্ট থাকবে)
        // বামপাশে সবসময় 72dp জায়গা ফাঁকা রাখা হয়েছে যাতে আইকন বারের নিচে ভিডিও ঢেকে না যায়
        mContentContainer = new FrameLayout(context);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        contentParams.leftMargin = dpToPx(context, PermanentSidebarController.COLLAPSED_WIDTH_DP);
        mContentContainer.setLayoutParams(contentParams);
        mRootContainer.addView(mContentContainer);

        // এক ফাইলের ভেতরেই পার্মানেন্ট সাইডবার তৈরি এবং যুক্ত করা
        mSidebarController = new PermanentSidebarController(context, mContentContainer, item -> {
            // এখানে সাইডবারের যেকোনো অপশন সিলেক্ট হলে আপনার সেকশন পরিবর্তন হবে
            onSidebarItemSelected(item);
        });

        mRootContainer.addView(mSidebarController.getSidebarView());

        return mRootContainer;
    }

    /**
     * আপনার আগের যেকোনো существующий (existing) BrowseSupportFragment বা Activity-র View-তেও
     * চাইলে শুধু এই একটি মেথড কল করে সাইডবারটি বসিয়ে দিতে পারেন:
     * BrowseFragment.attachPermanentSidebar(rootViewGroup, mainContentView, listener);
     */
    public static PermanentSidebarController attachPermanentSidebar(
            @NonNull ViewGroup rootViewGroup,
            @NonNull View mainContentView,
            @Nullable OnSidebarItemClickListener listener) {

        Context context = rootViewGroup.getContext();

        // মেইন কনটেন্টকে বামপাশ থেকে 72dp সরিয়ে রাখা যেন আইকন সবসময় দেখা যায়
        if (mainContentView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mainContentView.getLayoutParams();
            params.leftMargin = dpToPx(context, PermanentSidebarController.COLLAPSED_WIDTH_DP);
            mainContentView.setLayoutParams(params);
        } else {
            mainContentView.setPadding(
                    dpToPx(context, PermanentSidebarController.COLLAPSED_WIDTH_DP),
                    mainContentView.getPaddingTop(),
                    mainContentView.getPaddingRight(),
                    mainContentView.getPaddingBottom()
            );
        }

        PermanentSidebarController controller = new PermanentSidebarController(context, mainContentView, listener);
        rootViewGroup.addView(controller.getSidebarView());
        return controller;
    }

    private void onSidebarItemSelected(SidebarItem item) {
        // আপনার অ্যাপের সেকশন/ক্যাটাগরি লোড করার লজিক এখানে কাজ করবে
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    public interface OnSidebarItemClickListener {
        void onItemSelected(SidebarItem item);
    }

    public static class SidebarItem {
        public final int id;
        public final String title;
        public final int iconResId;
        public final boolean isHeader;

        public SidebarItem(int id, String title, int iconResId, boolean isHeader) {
            this.id = id;
            this.title = title;
            this.iconResId = iconResId;
            this.isHeader = isHeader;
        }
    }

    /**
     * সম্পূর্ণ সাইডবার ইউআই, ফোকাস হ্যান্ডলিং এবং অ্যানিমেশন কন্ট্রোলার (একই ফাইলের ভেতর)
     */
    public static class PermanentSidebarController {
        public static final int COLLAPSED_WIDTH_DP = 72; // সবসময় আইকন দেখানোর প্রস্থ
        public static final int EXPANDED_WIDTH_DP = 240; // অপশনে গেলে বড় হওয়ার প্রস্থ

        private final Context mContext;
        private final View mMainContentView;
        private final OnSidebarItemClickListener mListener;

        private final FrameLayout mSidebarWrapper;
        private final LinearLayout mItemsContainer;
        private final List<ViewHolder> mHolders = new ArrayList<>();

        private boolean mIsExpanded = false;
        private int mSelectedIndex = 2; // Default: Home সিলেক্টেড থাকবে
        private ValueAnimator mWidthAnimator;

        private static class ViewHolder {
            final LinearLayout rowView;
            final ImageView iconView;
            final TextView titleView;
            final SidebarItem item;

            ViewHolder(LinearLayout rowView, ImageView iconView, TextView titleView, SidebarItem item) {
                this.rowView = rowView;
                this.iconView = iconView;
                this.titleView = titleView;
                this.item = item;
            }
        }

        public PermanentSidebarController(Context context, View mainContentView, OnSidebarItemClickListener listener) {
            mContext = context;
            mMainContentView = mainContentView;
            mListener = listener;

            mSidebarWrapper = new FrameLayout(context);
            FrameLayout.LayoutParams wrapperParams = new FrameLayout.LayoutParams(
                    dpToPx(context, COLLAPSED_WIDTH_DP),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.START
            );
            mSidebarWrapper.setLayoutParams(wrapperParams);
            mSidebarWrapper.setBackgroundColor(Color.parseColor("#E60F0F0F"));
            mSidebarWrapper.setElevation(dpToPx(context, 12));

            ScrollView scrollView = new ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(false);
            scrollView.setFillViewport(true);

            mItemsContainer = new LinearLayout(context);
            mItemsContainer.setOrientation(LinearLayout.VERTICAL);
            mItemsContainer.setGravity(Gravity.CENTER_VERTICAL);
            int verticalPad = dpToPx(context, 16);
            mItemsContainer.setPadding(dpToPx(context, 10), verticalPad, dpToPx(context, 10), verticalPad);

            scrollView.addView(mItemsContainer);
            mSidebarWrapper.addView(scrollView);

            setupDefaultYoutubeItems();
            applyCollapsedStateImmediate();
        }

        public View getSidebarView() {
            return mSidebarWrapper;
        }

        private void setupDefaultYoutubeItems() {
            List<SidebarItem> items = new ArrayList<>();
            // ভিডিওর মতো হুবহু মেনু অপশনগুলো
            items.add(new SidebarItem(0, "Profile", android.R.drawable.sym_def_app_icon, true));
            items.add(new SidebarItem(1, "Search", android.R.drawable.ic_menu_search, false));
            items.add(new SidebarItem(2, "Home", android.R.drawable.ic_menu_compass, false));
            items.add(new SidebarItem(3, "Shorts", android.R.drawable.ic_media_play, false));
            items.add(new SidebarItem(4, "Subscriptions", android.R.drawable.ic_menu_slideshow, false));
            items.add(new SidebarItem(5, "Library", android.R.drawable.ic_menu_recent_history, false));
            items.add(new SidebarItem(6, "Gaming", android.R.drawable.ic_menu_manage, false));
            items.add(new SidebarItem(7, "Music", android.R.drawable.ic_lock_silent_mode_off, false));
            items.add(new SidebarItem(8, "News", android.R.drawable.ic_menu_info_details, false));
            items.add(new SidebarItem(9, "Sports", android.R.drawable.ic_menu_myplaces, false));
            items.add(new SidebarItem(10, "Settings", android.R.drawable.ic_menu_preferences, false));

            for (int i = 0; i < items.size(); i++) {
                final int index = i;
                SidebarItem item = items.get(i);
                ViewHolder holder = createRowView(item, index);
                mHolders.add(holder);
                mItemsContainer.addView(holder.rowView);
            }
            updateVisualSelection();
        }

        private ViewHolder createRowView(SidebarItem item, final int index) {
            LinearLayout row = new LinearLayout(mContext);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dpToPx(mContext, item.isHeader ? 52 : 44)
            );
            rowParams.topMargin = dpToPx(mContext, item.isHeader ? 0 : 4);
            rowParams.bottomMargin = dpToPx(mContext, item.isHeader ? 12 : 2);
            row.setLayoutParams(rowParams);
            row.setPadding(dpToPx(mContext, 12), 0, dpToPx(mContext, 14), 0);

            ImageView icon = new ImageView(mContext);
            int iconSize = dpToPx(mContext, item.isHeader ? 28 : 24);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            icon.setLayoutParams(iconParams);
            icon.setImageDrawable(ContextCompat.getDrawable(mContext, item.iconResId));
            icon.setColorFilter(Color.WHITE);

            TextView title = new TextView(mContext);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            titleParams.leftMargin = dpToPx(mContext, 18);
            title.setLayoutParams(titleParams);
            title.setText(item.title);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.isHeader ? 16 : 15);
            title.setTypeface(null, item.isHeader ? Typeface.BOLD : Typeface.NORMAL);
            title.setTextColor(Color.WHITE);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);

            row.addView(icon);
            row.addView(title);

            // Focus গেলে সাইডবার নিজে থেকেই Expand হবে, আর Focus সরে গেলে শুধু Icon দেখাবে
            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    mSelectedIndex = index;
                    if (!mIsExpanded) {
                        setExpanded(true);
                    }
                    if (mListener != null) {
                        mListener.onItemSelected(item);
                    }
                } else {
                    // সাইডবারের অন্য কোনো আইটেমে ফোকাস আছে কি না চেক করা
                    mSidebarWrapper.post(() -> {
                        if (!isAnySidebarItemFocused()) {
                            setExpanded(false);
                        }
                    });
                }
                updateVisualSelection();
            });

            // রিমোটের D-Pad Right চাপলে ভিডিও গ্রিডে ফোকাস চলে যাবে এবং সাইডবার ছোট হয়ে শুধু আইকন দেখাবে
            row.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (mMainContentView != null) {
                        mMainContentView.requestFocus();
                        setExpanded(false);
                        return true;
                    }
                }
                return false;
            });

            row.setOnClickListener(v -> {
                mSelectedIndex = index;
                updateVisualSelection();
                if (mListener != null) {
                    mListener.onItemSelected(item);
                }
            });

            return new ViewHolder(row, icon, title, item);
        }

        private boolean isAnySidebarItemFocused() {
            for (ViewHolder holder : mHolders) {
                if (holder.rowView.hasFocus()) {
                    return true;
                }
            }
            return false;
        }

        public void setExpanded(boolean expanded) {
            if (mIsExpanded == expanded) return;
            mIsExpanded = expanded;

            int startWidth = mSidebarWrapper.getWidth();
            int targetWidth = dpToPx(mContext, expanded ? EXPANDED_WIDTH_DP : COLLAPSED_WIDTH_DP);

            if (mWidthAnimator != null && mWidthAnimator.isRunning()) {
                mWidthAnimator.cancel();
            }

            mWidthAnimator = ValueAnimator.ofInt(startWidth, targetWidth);
            mWidthAnimator.setDuration(200);
            mWidthAnimator.setInterpolator(new DecelerateInterpolator());
            mWidthAnimator.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams params = mSidebarWrapper.getLayoutParams();
                params.width = val;
                mSidebarWrapper.setLayoutParams(params);
            });
            mWidthAnimator.start();

            for (ViewHolder holder : mHolders) {
                if (expanded) {
                    holder.titleView.setVisibility(View.VISIBLE);
                    holder.titleView.animate().alpha(1f).setDuration(180).start();
                } else {
                    holder.titleView.animate().alpha(0f).setDuration(100).withEndAction(() -> {
                        if (!mIsExpanded) {
                            holder.titleView.setVisibility(View.GONE);
                        }
                    }).start();
                }
            }
            updateVisualSelection();
        }

        private void applyCollapsedStateImmediate() {
            mIsExpanded = false;
            for (ViewHolder holder : mHolders) {
                holder.titleView.setAlpha(0f);
                holder.titleView.setVisibility(View.GONE);
            }
            updateVisualSelection();
        }

        private void updateVisualSelection() {
            for (int i = 0; i < mHolders.size(); i++) {
                ViewHolder holder = mHolders.get(i);
                boolean isFocused = holder.rowView.hasFocus();
                boolean isSelected = (i == mSelectedIndex);

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dpToPx(mContext, 24));

                if (isFocused) {
                    // ভিডিওর মতো সাদা হাইলাইট পিল যখন অপশনের ওপর যাওয়া হবে
                    bg.setColor(Color.WHITE);
                    holder.rowView.setBackground(bg);
                    holder.iconView.setColorFilter(Color.BLACK);
                    holder.titleView.setTextColor(Color.BLACK);
                    holder.titleView.setTypeface(null, Typeface.BOLD);
                } else if (isSelected) {
                    // যখন ডানপাশে ভিডিও দেখা হচ্ছে, তখন সিলেক্টেড আইকনের নিচে হালকা হাইলাইট থাকবে
                    bg.setColor(Color.parseColor("#28FFFFFF"));
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

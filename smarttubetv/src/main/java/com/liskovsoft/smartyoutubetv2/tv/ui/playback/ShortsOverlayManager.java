package com.liskovsoft.smartyoutubetv2.tv.ui.playback;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.listener.PlayerEventListener;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerConstants;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.tv.R;

public class ShortsOverlayManager {
    private final PlaybackFragment mFragment;
    private final Context mContext;
    private View mOverlayRoot;
    private View mVideoBounds;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private View mChannelContainer;
    private ImageView mChannelAvatar;
    private TextView mChannelName;
    private TextView mSubscribeBtn;
    private TextView mLikeBtn;
    private TextView mCommentBtn;
    private TextView mLoopBtn;
    private TextView mMoreBtn;

    private boolean mIsShortsActive;
    private boolean mIsSubscribed;
    private boolean mIsLiked;
    private boolean mIsLoopEnabled = true;

    public ShortsOverlayManager(PlaybackFragment fragment) {
        mFragment = fragment;
        mContext = fragment.requireContext();
    }

    public void attachToRoot(ViewGroup rootView) {
        if (mOverlayRoot != null || rootView == null) {
            return;
        }
        mOverlayRoot = LayoutInflater.from(mContext).inflate(R.layout.fragment_shorts_overlay, rootView, false);
        rootView.addView(mOverlayRoot);

        mVideoBounds = mOverlayRoot.findViewById(R.id.shorts_video_bounds);
        mTitleView = mOverlayRoot.findViewById(R.id.shorts_title);
        mSubtitleView = mOverlayRoot.findViewById(R.id.shorts_subtitle);
        mChannelContainer = mOverlayRoot.findViewById(R.id.shorts_channel_container);
        mChannelAvatar = mOverlayRoot.findViewById(R.id.shorts_channel_avatar);
        mChannelName = mOverlayRoot.findViewById(R.id.shorts_channel_name);
        mSubscribeBtn = mOverlayRoot.findViewById(R.id.btn_shorts_subscribe);
        mLikeBtn = mOverlayRoot.findViewById(R.id.btn_shorts_like);
        mCommentBtn = mOverlayRoot.findViewById(R.id.btn_shorts_comment);
        mLoopBtn = mOverlayRoot.findViewById(R.id.btn_shorts_loop);
        mMoreBtn = mOverlayRoot.findViewById(R.id.btn_shorts_more);

        setupStylesAndFocus();
        setupClickListeners();
    }

    private void setupStylesAndFocus() {
        // Channel row focus style
        applyPillStyle(mChannelContainer, false, Color.TRANSPARENT, Color.parseColor("#33FFFFFF"));
        mChannelContainer.setOnFocusChangeListener((v, hasFocus) -> {
            applyPillStyle(v, hasFocus, Color.TRANSPARENT, Color.parseColor("#33FFFFFF"));
            animateFocus(v, hasFocus, 1.05f);
        });

        // + Subscribe button style (Unfocused: Dark translucent pill, Focused: Solid White pill with dark text)
        updateSubscribeButtonVisual(false);
        mSubscribeBtn.setOnFocusChangeListener((v, hasFocus) -> {
            updateSubscribeButtonVisual(hasFocus);
            animateFocus(v, hasFocus, 1.08f);
        });

        // 4 Circular Action Buttons (Like, Comment, Loop, More)
        setupCircleButtonFocus(mLikeBtn);
        setupCircleButtonFocus(mCommentBtn);
        setupCircleButtonFocus(mLoopBtn);
        setupCircleButtonFocus(mMoreBtn);

        // Video bounds click to pause/play
        mVideoBounds.setOnClickListener(v -> {
            mFragment.setPlayWhenReady(!mFragment.getPlayWhenReady());
        });
    }

    private void setupCircleButtonFocus(TextView button) {
        updateCircleButtonVisual(button, false);
        button.setOnFocusChangeListener((v, hasFocus) -> {
            updateCircleButtonVisual((TextView) v, hasFocus);
            animateFocus(v, hasFocus, 1.12f);
        });
    }

    private void updateSubscribeButtonVisual(boolean hasFocus) {
        if (mSubscribeBtn == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(20));
        if (hasFocus) {
            bg.setColor(Color.WHITE);
            mSubscribeBtn.setTextColor(Color.parseColor("#0F0F0F"));
        } else {
            bg.setColor(mIsSubscribed ? Color.parseColor("#38FFFFFF") : Color.parseColor("#26FFFFFF"));
            mSubscribeBtn.setTextColor(Color.WHITE);
        }
        mSubscribeBtn.setBackground(bg);
        mSubscribeBtn.setText(mIsSubscribed ? "✓  Subscribed" : "+  Subscribe");
    }

    private void updateCircleButtonVisual(TextView button, boolean hasFocus) {
        if (button == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        if (hasFocus) {
            bg.setColor(Color.WHITE);
            if (button == mLikeBtn && mIsLiked) {
                button.setTextColor(Color.parseColor("#E53935"));
            } else {
                button.setTextColor(Color.parseColor("#0F0F0F"));
            }
        } else {
            if (button == mLikeBtn && mIsLiked) {
                bg.setColor(Color.parseColor("#33FF4E45"));
                button.setTextColor(Color.parseColor("#FF4E45"));
            } else {
                bg.setColor(Color.parseColor("#26FFFFFF"));
                button.setTextColor(Color.WHITE);
            }
        }
        button.setBackground(bg);
    }

    private void applyPillStyle(View view, boolean hasFocus, int normalColor, int focusedColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(18));
        bg.setColor(hasFocus ? focusedColor : normalColor);
        view.setBackground(bg);
    }

    private void animateFocus(View v, boolean hasFocus, float scale) {
        v.animate()
                .scaleX(hasFocus ? scale : 1.0f)
                .scaleY(hasFocus ? scale : 1.0f)
                .setDuration(140)
                .start();
    }

    private void setupClickListeners() {
        PlayerEventListener listener = mFragment.getEventListener();

        mChannelContainer.setOnClickListener(v -> {
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onChannelClicked();
            }
        });

        mSubscribeBtn.setOnClickListener(v -> {
            mIsSubscribed = !mIsSubscribed;
            updateSubscribeButtonVisual(mSubscribeBtn.hasFocus());
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onSubscribeClicked(mIsSubscribed);
            }
            Toast.makeText(mContext, mIsSubscribed ? "Subscribed" : "Unsubscribed", Toast.LENGTH_SHORT).show();
        });

        mLikeBtn.setOnClickListener(v -> {
            mIsLiked = !mIsLiked;
            mLikeBtn.setText(mIsLiked ? "♥" : "♡");
            updateCircleButtonVisual(mLikeBtn, mLikeBtn.hasFocus());
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onLikeClicked(mIsLiked);
            }
        });

        mCommentBtn.setOnClickListener(v -> {
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onChatClicked(true);
            }
        });

        mLoopBtn.setOnClickListener(v -> {
            mIsLoopEnabled = !mIsLoopEnabled;
            int mode = mIsLoopEnabled ? PlayerConstants.PLAYBACK_MODE_ONE : PlayerConstants.PLAYBACK_MODE_ALL;
            PlayerData.instance(mContext).setPlaybackMode(mode);
            Toast.makeText(mContext, mIsLoopEnabled ? "Loop Short: ON" : "Auto-Next Short: ON", Toast.LENGTH_SHORT).show();
        });

        mMoreBtn.setOnClickListener(v -> {
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onVideoStatsClicked(true);
            }
        });
    }

    /**
     * শর্টস ভিডিও প্লে হলে ফুল-হাইট (Uncropped) লেআউট এবং নিচের অপশন প্যানেল সেটআপ করে
     */
    public void updateShortsMode(Video video, PlayerView playerView) {
        if (mOverlayRoot == null || playerView == null) {
            return;
        }

        boolean isShorts = video != null && video.isShorts;
        mIsShortsActive = isShorts;

        if (!isShorts) {
            mOverlayRoot.setVisibility(View.GONE);
            restoreNormalPlayerLayout(playerView);
            return;
        }

        // স্বাভাবিক প্লেব্যাক ওভারলে লুকিয়ে শর্টস ওভারলে দেখানো
        mFragment.showControlsOverlay(false);
        mOverlayRoot.setVisibility(View.VISIBLE);

        // ১. Cropped সমস্যা সমাধান: RESIZE_MODE_FIT এবং 9:16 ফুল-হাইট সেট করা
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        alignPlayerViewToShortsBounds(playerView);

        // ২. ভিডিওর তথ্য আপডেট
        mTitleView.setText(!TextUtils.isEmpty(video.title) ? video.title : "");

        String subInfo = !TextUtils.isEmpty(video.secondTitle) ? video.secondTitle : video.getAuthor();
        mSubtitleView.setText("▶  " + (subInfo != null ? subInfo : ""));

        String author = video.getAuthor();
        if (TextUtils.isEmpty(author) && !TextUtils.isEmpty(video.title)) {
            author = "Channel";
        }
        String handle = author != null && !author.startsWith("@") ? "@" + author.replace(" ", "") : author;
        mChannelName.setText(handle != null ? handle : "@Channel");

        String avatarUrl = video.cardImageUrl;
        Glide.with(mContext)
                .load(avatarUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(mChannelAvatar);

        // ডিফল্টভাবে Like বাটনে কার্সর দেওয়া যাতে ডানে/বামে বা উপরে (+ Subscribe) কার্সর নেওয়া যায়
        mOverlayRoot.post(() -> {
            if (mIsShortsActive && mLikeBtn != null && !hasAnyShortsButtonFocus()) {
                mLikeBtn.requestFocus();
            }
        });
    }

    private void alignPlayerViewToShortsBounds(PlayerView playerView) {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        int screenHeight = dm.heightPixels;
        int verticalMargin = dpToPx(10);
        int targetHeight = screenHeight - (verticalMargin * 2);
        int targetWidth = (int) (targetHeight * (9f / 16f));

        // বাম-মাঝখানে ভিডিও এবং ডানপাশে অপশন প্যানেলের ভারসাম্য বজায় রাখা
        ViewGroup.LayoutParams boundsLp = mVideoBounds.getLayoutParams();
        boundsLp.width = targetWidth;
        boundsLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mVideoBounds.setLayoutParams(boundsLp);

        mVideoBounds.post(() -> {
            if (!mIsShortsActive) return;
            int[] loc = new int[2];
            mVideoBounds.getLocationInWindow(loc);

            FrameLayout.LayoutParams playerLp = new FrameLayout.LayoutParams(targetWidth, targetHeight);
            playerLp.gravity = Gravity.TOP | Gravity.START;
            playerLp.leftMargin = loc[0];
            playerLp.topMargin = verticalMargin;
            playerView.setLayoutParams(playerLp);

            // হালকা গোল কোণা (Rounded Corners) কিন্তু কোনো অংশ কাটবে না
            final int radius = dpToPx(12);
            playerView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            playerView.setClipToOutline(true);
        });
    }

    private void restoreNormalPlayerLayout(PlayerView playerView) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        lp.gravity = Gravity.CENTER;
        lp.setMargins(0, 0, 0, 0);
        playerView.setLayoutParams(lp);
        playerView.setClipToOutline(false);
    }

    public void setSubscribeState(boolean isSubscribed) {
        mIsSubscribed = isSubscribed;
        if (mSubscribeBtn != null) {
            updateSubscribeButtonVisual(mSubscribeBtn.hasFocus());
        }
    }

    public void setLikeState(boolean isLiked) {
        mIsLiked = isLiked;
        if (mLikeBtn != null) {
            mLikeBtn.setText(mIsLiked ? "♥" : "♡");
            updateCircleButtonVisual(mLikeBtn, mLikeBtn.hasFocus());
        }
    }

    public boolean isShortsActive() {
        return mIsShortsActive;
    }

    private boolean hasAnyShortsButtonFocus() {
        return (mSubscribeBtn != null && mSubscribeBtn.hasFocus())
                || (mChannelContainer != null && mChannelContainer.hasFocus())
                || (mLikeBtn != null && mLikeBtn.hasFocus())
                || (mCommentBtn != null && mCommentBtn.hasFocus())
                || (mLoopBtn != null && mLoopBtn.hasFocus())
                || (mMoreBtn != null && mMoreBtn.hasFocus());
    }

    private boolean isBottomActionRowFocused() {
        return (mLikeBtn != null && mLikeBtn.hasFocus())
                || (mCommentBtn != null && mCommentBtn.hasFocus())
                || (mLoopBtn != null && mLoopBtn.hasFocus())
                || (mMoreBtn != null && mMoreBtn.hasFocus());
    }

    /**
     * রিমোট কন্ট্রোল (Up/Down দিয়ে শর্টস পরিবর্তন এবং Subscribe বাটনে কার্সর নেভিগেশন)
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mIsShortsActive) {
            return false;
        }

        // Leanback এর ডিফল্ট কন্ট্রোল ওভারলে বন্ধ রাখা
        mFragment.showControlsOverlay(false);

        if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP) {
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onPreviousClicked();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onNextClicked();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            // নিচের বাটন রো-তে থাকলে UP চাপলে সরাসরি + Subscribe বাটনে কার্সর যাবে
            if (isBottomActionRowFocused()) {
                mSubscribeBtn.requestFocus();
                return true;
            }
            // + Subscribe বাটনে থাকা অবস্থায় UP চাপলে @Channel-এ যাবে
            if (mSubscribeBtn != null && mSubscribeBtn.hasFocus()) {
                mChannelContainer.requestFocus();
                return true;
            }
            // @Channel বা ভিডিও ফোকাসে থাকা অবস্থায় UP চাপলে আগের শর্টস (Previous Short) চালু হবে
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onPreviousClicked();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            // @Channel-এ থাকলে DOWN চাপলে + Subscribe বাটনে কার্সর যাবে
            if (mChannelContainer != null && mChannelContainer.hasFocus()) {
                mSubscribeBtn.requestFocus();
                return true;
            }
            // + Subscribe বাটনে থাকলে DOWN চাপলে নিচের Like বাটনে কার্সর যাবে
            if (mSubscribeBtn != null && mSubscribeBtn.hasFocus()) {
                mLikeBtn.requestFocus();
                return true;
            }
            // নিচের বাটন রো বা ভিডিওতে থাকা অবস্থায় DOWN চাপলে পরবর্তী শর্টস (Next Short) চালু হবে
            if (mFragment.getEventListener() != null) {
                mFragment.getEventListener().onNextClicked();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (mLikeBtn != null && mLikeBtn.hasFocus()) {
                mSubscribeBtn.requestFocus();
                return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (mSubscribeBtn != null && mSubscribeBtn.hasFocus()) {
                mLikeBtn.requestFocus();
                return true;
            }
            if (mChannelContainer != null && mChannelContainer.hasFocus()) {
                mSubscribeBtn.requestFocus();
                return true;
            }
        }

        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * mContext.getResources().getDisplayMetrics().density);
    }
  }

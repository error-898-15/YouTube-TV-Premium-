// File Path: smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/playback/ShortsOverlayHelper.java
package com.liskovsoft.smartyoutubetv2.tv.ui.playback;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Build.VERSION;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.bumptech.glide.Glide;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu.VideoMenuPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class ShortsOverlayHelper {
    /** Width / height of the rounded Shorts frame (taken from the reference screenshots, ~3:4). */
    private static final float SHORTS_FRAME_RATIO = 3f / 4f;
    /** Frame height relative to the screen height. */
    private static final float SHORTS_FRAME_HEIGHT_PERCENT = 0.88f;
    /** Frame left margin relative to the screen width. */
    private static final float SHORTS_FRAME_LEFT_PERCENT = 0.16f;
    private final PlaybackFragment mFragment;
    private View mShortsOverlay;
    private View mSurfaceRoot;
    private View mControlsDock;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private TextView mChannelNameView;
    private ImageView mChannelIconView;
    private TextView mSubscribeBtn;
    private TextView mLikeBtn;
    private ImageButton mCommentBtn;
    private ImageButton mAutoScrollBtn;
    private TextView mMoreBtn;
    private boolean mIsShortsActive;
    private boolean mIsLiked;
    private boolean mIsSubscribed;

    public ShortsOverlayHelper(PlaybackFragment fragment) {
        mFragment = fragment;
    }

    public void init(View root) {
        if (root == null) {
            return;
        }
        mShortsOverlay = root.findViewById(R.id.yt_shorts_overlay);
        mSurfaceRoot = root.findViewById(R.id.surface_root);
        mControlsDock = root.findViewById(R.id.playback_controls_dock);
        mTitleView = root.findViewById(R.id.shorts_title);
        mSubtitleView = root.findViewById(R.id.shorts_subtitle);
        mChannelNameView = root.findViewById(R.id.shorts_channel_name);
        mChannelIconView = root.findViewById(R.id.shorts_channel_icon);
        mSubscribeBtn = root.findViewById(R.id.shorts_btn_subscribe);
        mLikeBtn = root.findViewById(R.id.shorts_btn_like);
        mCommentBtn = root.findViewById(R.id.shorts_btn_comment);
        mAutoScrollBtn = root.findViewById(R.id.shorts_btn_autoscroll);
        mMoreBtn = root.findViewById(R.id.shorts_btn_more);

        setupListeners(root.getContext());
    }

    private void setupListeners(Context context) {
        if (mShortsOverlay == null) {
            return;
        }

        setupTextButtonFocus(mSubscribeBtn);
        setupTextButtonFocus(mLikeBtn);
        setupTextButtonFocus(mMoreBtn);
        setupImageButtonFocus(mCommentBtn);
        setupImageButtonFocus(mAutoScrollBtn);

        View channelRow = mShortsOverlay.findViewById(R.id.shorts_channel_row);
        if (channelRow != null) {
            channelRow.setOnClickListener(v ->
                    PlaybackPresenter.instance(context).onButtonClicked(R.id.action_channel, PlayerUI.BUTTON_OFF));
        }

        if (mSubscribeBtn != null) {
            mSubscribeBtn.setOnClickListener(v -> {
                int state = mIsSubscribed ? PlayerUI.BUTTON_ON : PlayerUI.BUTTON_OFF;
                PlaybackPresenter.instance(context).onButtonClicked(R.id.action_subscribe, state);
                updateSubscribeState(!mIsSubscribed);
            });
        }

        if (mLikeBtn != null) {
            mLikeBtn.setOnClickListener(v -> {
                int state = mIsLiked ? PlayerUI.BUTTON_ON : PlayerUI.BUTTON_OFF;
                PlaybackPresenter.instance(context).onButtonClicked(R.id.action_thumbs_up, state);
                updateLikeState(!mIsLiked);
            });
        }

        if (mCommentBtn != null) {
            mCommentBtn.setOnClickListener(v ->
                    PlaybackPresenter.instance(context).onButtonClicked(R.id.action_chat, PlayerUI.BUTTON_OFF));
        }

        if (mAutoScrollBtn != null) {
            mAutoScrollBtn.setOnClickListener(v -> {
                PlayerTweaksData tweaks = PlayerTweaksData.instance(context);
                boolean newLoopState = !tweaks.isLoopShortsEnabled();
                tweaks.setLoopShortsEnabled(newLoopState);
                updateAutoScrollIcon(context);
                MessageHelpers.showMessage(context, newLoopState ? "Shorts Mode: Loop Current" : "Shorts Mode: Auto-Scroll Next");
            });
        }

        if (mMoreBtn != null) {
            mMoreBtn.setOnClickListener(v -> {
                if (mFragment.getVideo() != null) {
                    VideoMenuPresenter.instance(context).showMenu(mFragment.getVideo());
                }
            });
        }
    }

    private void setupTextButtonFocus(TextView btn) {
        if (btn == null) return;
        btn.setOnFocusChangeListener((v, hasFocus) ->
                btn.setTextColor(hasFocus ? Color.parseColor("#0F0F0F") : Color.WHITE));
    }

    private void setupImageButtonFocus(ImageButton btn) {
        if (btn == null) return;
        btn.setOnFocusChangeListener((v, hasFocus) ->
                btn.setColorFilter(hasFocus ? Color.parseColor("#0F0F0F") : Color.WHITE));
    }

    public void updateVideo(Video video) {
        if (mShortsOverlay == null || mSurfaceRoot == null) {
            return;
        }

        boolean wasShortsActive = mIsShortsActive;
        mIsShortsActive = video != null && (video.isShorts || video.belongsToShortsGroup());

        if (mIsShortsActive) {
            mShortsOverlay.setVisibility(View.VISIBLE);
            if (mControlsDock != null) {
                mControlsDock.setVisibility(View.GONE);
            }

            // Position vertical video on the left-center with rounded outline (like Image 13 & 14)
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSurfaceRoot.getLayoutParams();
            int rootHeight = mSurfaceRoot.getResources().getDisplayMetrics().heightPixels;
            int targetHeight = (int) (rootHeight * SHORTS_FRAME_HEIGHT_PERCENT);
            int targetWidth = (int) (targetHeight * SHORTS_FRAME_RATIO);
            lp.width = targetWidth;
            lp.height = targetHeight;
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.leftMargin = (int) (mSurfaceRoot.getResources().getDisplayMetrics().widthPixels * SHORTS_FRAME_LEFT_PERCENT);
            mSurfaceRoot.setLayoutParams(lp);

            // The video must fill the whole rounded frame (no black bars, no user zoom/resize interference)
            mFragment.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            mFragment.setZoomPercents(100);

            if (VERSION.SDK_INT >= 21) {
                mSurfaceRoot.setClipToOutline(true);
                mSurfaceRoot.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 32f);
                    }
                });
                if (VERSION.SDK_INT >= 23) {
                    mSurfaceRoot.setForeground(ContextCompat.getDrawable(mSurfaceRoot.getContext(), R.drawable.yt_shorts_video_frame));
                }
            }

            String title = video.getTitleFull() != null ? video.getTitleFull() : video.getTitle();
            if (mTitleView != null) {
                mTitleView.setText(title != null ? title : "");
            }

            CharSequence subText = video.getSecondTitleFull() != null ? video.getSecondTitleFull() : video.getSecondTitle();
            String sub = subText != null ? subText.toString() : null;
            if (mSubtitleView != null) {
                mSubtitleView.setText(sub != null ? "▶  " + sub : "▶  Shorts");
            }

            String author = video.getAuthor();
            if (mChannelNameView != null) {
                if (author != null && !author.isEmpty()) {
                    mChannelNameView.setText(author.startsWith("@") ? author : "@" + author.replace(" ", ""));
                } else {
                    mChannelNameView.setText("@channel");
                }
            }

            updateSubscribeState(video.isSubscribed);
            updateAutoScrollIcon(mSurfaceRoot.getContext());
        } else {
            mShortsOverlay.setVisibility(View.GONE);
            if (mControlsDock != null) {
                mControlsDock.setVisibility(View.VISIBLE);
            }

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSurfaceRoot.getLayoutParams();
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
            lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
            lp.gravity = Gravity.CENTER;
            lp.leftMargin = 0;
            mSurfaceRoot.setLayoutParams(lp);

            // Restore user's regular player settings after leaving Shorts
            if (wasShortsActive) {
                Context ctx = mSurfaceRoot.getContext();
                mFragment.setResizeMode(PlayerData.instance(ctx).getResizeMode());
                mFragment.setZoomPercents(PlayerData.instance(ctx).getZoomPercents());
            }

            if (VERSION.SDK_INT >= 21) {
                mSurfaceRoot.setClipToOutline(false);
                if (VERSION.SDK_INT >= 23) {
                    mSurfaceRoot.setForeground(null);
                }
            }
        }
    }

    public void updateButtonState(int buttonId, int buttonState) {
        if (buttonId == R.id.action_thumbs_up) {
            updateLikeState(buttonState == PlayerUI.BUTTON_ON);
        } else if (buttonId == R.id.action_subscribe) {
            updateSubscribeState(buttonState == PlayerUI.BUTTON_ON);
        }
    }

    private void updateLikeState(boolean liked) {
        mIsLiked = liked;
        if (mLikeBtn != null) {
            mLikeBtn.setText(liked ? "♥" : "♡");
        }
    }

    private void updateSubscribeState(boolean subscribed) {
        mIsSubscribed = subscribed;
        if (mSubscribeBtn != null) {
            mSubscribeBtn.setText(subscribed ? "Subscribed" : "+ Subscribe");
        }
    }

    private void updateAutoScrollIcon(Context context) {
        if (mAutoScrollBtn == null) return;
        boolean isLoop = PlayerTweaksData.instance(context).isLoopShortsEnabled();
        mAutoScrollBtn.setImageResource(isLoop ? R.drawable.action_mode_one : R.drawable.action_mode_all);
    }

    public void updateChannelIcon(String iconUrl) {
        if (mChannelIconView == null || iconUrl == null || mFragment.getContext() == null) {
            return;
        }
        Glide.with(mFragment.getContext())
                .load(iconUrl)
                .apply(ViewUtil.glideOptions())
                .circleCrop()
                .into(mChannelIconView);
    }

    public boolean isShortsActive() {
        return mIsShortsActive;
    }

    /**
     * Remote control handling for Shorts.<br/>
     * UP/DOWN: previous/next Short (only while focus is NOT inside the right panel;
     * inside the panel they move focus between the buttons as usual).<br/>
     * RIGHT: move focus into the panel. LEFT: leave the panel. OK: pause/resume.
     * @return true if the event is consumed (both ACTION_DOWN and ACTION_UP)
     */
    public boolean onKeyDown(KeyEvent event) {
        if (!mIsShortsActive) {
            return false;
        }

        boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean panelFocused = mShortsOverlay != null && mShortsOverlay.hasFocus();

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (panelFocused) {
                    return false; // regular focus movement inside the panel
                }
                // Single step per key press: ignore auto-repeat so a held key doesn't skip many Shorts
                if (isDown && event.getRepeatCount() == 0) {
                    PlaybackPresenter presenter = PlaybackPresenter.instance(mFragment.getContext());
                    if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                        presenter.onNextClicked();
                    } else {
                        presenter.onPreviousClicked();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!panelFocused && mSubscribeBtn != null) {
                    if (isDown) {
                        mSubscribeBtn.requestFocus();
                    }
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (panelFocused && (mSubscribeBtn != null && mSubscribeBtn.hasFocus() ||
                        isChannelRowFocused())) {
                    if (isDown) {
                        leavePanel();
                    }
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!panelFocused) {
                    if (isDown && event.getRepeatCount() == 0) {
                        mFragment.setPlayWhenReady(!mFragment.getPlayWhenReady());
                    }
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private boolean isChannelRowFocused() {
        View row = mShortsOverlay != null ? mShortsOverlay.findViewById(R.id.shorts_channel_row) : null;
        return row != null && row.hasFocus();
    }

    /**
     * Take focus away from the panel and give it back to the player root,
     * otherwise the first focusable panel child grabs it again and UP/DOWN never change the Short.
     */
    private void leavePanel() {
        mShortsOverlay.clearFocus();
        View root = mFragment.getView();
        if (root != null) {
            root.requestFocus();
        }
    }
    }

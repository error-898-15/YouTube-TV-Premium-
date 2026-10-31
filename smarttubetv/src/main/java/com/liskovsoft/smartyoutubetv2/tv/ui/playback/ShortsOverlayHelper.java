// File Path: smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/playback/ShortsOverlayHelper.java
package com.liskovsoft.smartyoutubetv2.tv.ui.playback;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
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
    // ---- Shorts frame geometry (measured from the reference screenshots) ----
    /** Width / height of the rounded Shorts frame (reference: ~0.80 - 0.82). */
    private static final float SHORTS_FRAME_RATIO = 4f / 5f;
    /** Frame height relative to the screen height (the reference frame is full height). */
    private static final float SHORTS_FRAME_HEIGHT_PERCENT = 1.0f;
    /** Frame left edge relative to the screen width. */
    private static final float SHORTS_FRAME_LEFT_PERCENT = 0.16f;
    /** Corner radius of the frame (must match res/drawable/yt_shorts_video_frame.xml). */
    private static final int SHORTS_FRAME_CORNER_DP = 12;
    // ---- Info / actions panel (right of the frame, bottom aligned) ----
    private static final int SHORTS_PANEL_WIDTH_DP = 290;
    private static final int SHORTS_PANEL_GAP_DP = 36;
    private static final int SHORTS_PANEL_BOTTOM_DP = 44;
    private static final int SHORTS_PANEL_SCREEN_EDGE_DP = 16;

    private final PlaybackFragment mFragment;
    private View mRoot;
    private View mShortsOverlay;
    private View mVideoFrame;
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
    private int mLastRootWidth;
    private int mLastRootHeight;

    public ShortsOverlayHelper(PlaybackFragment fragment) {
        mFragment = fragment;
    }

    public void init(View root) {
        if (root == null) {
            return;
        }
        mRoot = root;
        mShortsOverlay = root.findViewById(R.id.yt_shorts_overlay);
        mVideoFrame = root.findViewById(R.id.shorts_video_frame);
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

        // The panel must never grab the focus on its own (Android TV gives the focus to the first
        // focusable view that appears). Otherwise UP/DOWN would move inside the panel instead of
        // switching Shorts. The focus is given to the panel only when the user presses RIGHT.
        blockPanelFocus();

        // Keep the rounded outline in sync with the frame size
        if (mVideoFrame != null) {
            mVideoFrame.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (VERSION.SDK_INT >= 21 && mIsShortsActive) {
                    v.invalidateOutline();
                }
            });
        }

        // Re-apply the layout if the screen/window size changes while Shorts is shown
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int w = r - l;
            int h = b - t;
            if (mIsShortsActive && w > 0 && h > 0 && (w != mLastRootWidth || h != mLastRootHeight)) {
                applyShortsLayout();
            }
        });

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
        if (mShortsOverlay == null || mSurfaceRoot == null || mVideoFrame == null) {
            return;
        }

        boolean wasShortsActive = mIsShortsActive;
        mIsShortsActive = video != null && (video.isShorts || video.belongsToShortsGroup());

        // New video: the focus must go back to the player so UP/DOWN keep switching Shorts
        leavePanel();

        if (mIsShortsActive) {
            mShortsOverlay.setVisibility(View.VISIBLE);
            if (mControlsDock != null) {
                mControlsDock.setVisibility(View.GONE);
            }

            // Fixed-size rounded frame on the left + info panel on its right (like the reference)
            applyShortsLayout();

            // The video must fill the whole rounded frame (no black bars, no user zoom/resize interference)
            mFragment.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            mFragment.setZoomPercents(100);

            if (VERSION.SDK_INT >= 21) {
                mVideoFrame.setClipToOutline(true);
                mVideoFrame.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        float radius = SHORTS_FRAME_CORNER_DP * view.getResources().getDisplayMetrics().density;
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                    }
                });
                mVideoFrame.invalidateOutline();
                if (VERSION.SDK_INT >= 23 && mVideoFrame instanceof FrameLayout) {
                    ((FrameLayout) mVideoFrame).setForeground(
                            ContextCompat.getDrawable(mVideoFrame.getContext(), R.drawable.yt_shorts_video_frame));
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

            // Frame container goes back to full screen
            FrameLayout.LayoutParams frameLp = (FrameLayout.LayoutParams) mVideoFrame.getLayoutParams();
            frameLp.width = FrameLayout.LayoutParams.MATCH_PARENT;
            frameLp.height = FrameLayout.LayoutParams.MATCH_PARENT;
            frameLp.gravity = Gravity.CENTER;
            frameLp.leftMargin = 0;
            frameLp.topMargin = 0;
            frameLp.rightMargin = 0;
            frameLp.bottomMargin = 0;
            mVideoFrame.setLayoutParams(frameLp);

            if (wasShortsActive) {
                FrameLayout.LayoutParams surfaceLp = (FrameLayout.LayoutParams) mSurfaceRoot.getLayoutParams();
                surfaceLp.width = FrameLayout.LayoutParams.MATCH_PARENT;
                surfaceLp.height = FrameLayout.LayoutParams.MATCH_PARENT;
                surfaceLp.gravity = Gravity.CENTER;
                mSurfaceRoot.setLayoutParams(surfaceLp);

                // Restore user's regular player settings after leaving Shorts
                Context ctx = mSurfaceRoot.getContext();
                mFragment.setResizeMode(PlayerData.instance(ctx).getResizeMode());
                mFragment.setZoomPercents(PlayerData.instance(ctx).getZoomPercents());
            }

            if (VERSION.SDK_INT >= 21) {
                mVideoFrame.setClipToOutline(false);
                if (VERSION.SDK_INT >= 23 && mVideoFrame instanceof FrameLayout) {
                    ((FrameLayout) mVideoFrame).setForeground(null);
                }
            }
        }
    }

    /**
     * Sizes/positions the Shorts frame and the info panel.<br/>
     * ROOT CAUSE of the old "small + cropped" bug: the frame size was applied to the
     * AspectRatioFrameLayout (surface_root) itself, but that view re-measures itself from the video aspect
     * ratio (ZOOM makes it taller than the frame), so the frame/outline/border were wrong.<br/>
     * Now a dedicated container (shorts_video_frame) has the fixed frame size, and surface_root fills it
     * and zooms (center-crop) inside; the container clips the overflow.
     */
    private void applyShortsLayout() {
        if (mVideoFrame == null || mSurfaceRoot == null || mShortsOverlay == null) {
            return;
        }

        DisplayMetrics dm = mVideoFrame.getResources().getDisplayMetrics();
        float density = dm.density;
        int rootWidth = mRoot != null && mRoot.getWidth() > 0 ? mRoot.getWidth() : dm.widthPixels;
        int rootHeight = mRoot != null && mRoot.getHeight() > 0 ? mRoot.getHeight() : dm.heightPixels;
        mLastRootWidth = rootWidth;
        mLastRootHeight = rootHeight;

        int frameHeight = Math.round(rootHeight * SHORTS_FRAME_HEIGHT_PERCENT);
        int frameWidth = Math.round(frameHeight * SHORTS_FRAME_RATIO);
        int gap = Math.round(SHORTS_PANEL_GAP_DP * density);
        int panelWidth = Math.round(SHORTS_PANEL_WIDTH_DP * density);
        int edge = Math.round(SHORTS_PANEL_SCREEN_EDGE_DP * density);

        int frameLeft = Math.round(rootWidth * SHORTS_FRAME_LEFT_PERCENT);
        int groupWidth = frameWidth + gap + panelWidth;
        if (frameLeft + groupWidth + edge > rootWidth) {
            // Narrow screen: center the frame + panel group instead of overflowing
            frameLeft = Math.max(0, (rootWidth - groupWidth) / 2);
        }

        // 1) Frame container: fixed size, left aligned, vertically centered
        FrameLayout.LayoutParams frameLp = (FrameLayout.LayoutParams) mVideoFrame.getLayoutParams();
        frameLp.width = frameWidth;
        frameLp.height = frameHeight;
        frameLp.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        frameLp.leftMargin = frameLeft;
        frameLp.topMargin = 0;
        frameLp.rightMargin = 0;
        frameLp.bottomMargin = 0;
        mVideoFrame.setLayoutParams(frameLp);

        // 2) Video view: always fills the container and is centered inside (zoom/crop happens inside it)
        FrameLayout.LayoutParams surfaceLp = (FrameLayout.LayoutParams) mSurfaceRoot.getLayoutParams();
        surfaceLp.width = FrameLayout.LayoutParams.MATCH_PARENT;
        surfaceLp.height = FrameLayout.LayoutParams.MATCH_PARENT;
        surfaceLp.gravity = Gravity.CENTER;
        surfaceLp.leftMargin = 0;
        surfaceLp.topMargin = 0;
        surfaceLp.rightMargin = 0;
        surfaceLp.bottomMargin = 0;
        mSurfaceRoot.setLayoutParams(surfaceLp);

        // 3) Info/actions panel: right of the frame, bottom aligned
        int panelLeft = frameLeft + frameWidth + gap;
        FrameLayout.LayoutParams panelLp = (FrameLayout.LayoutParams) mShortsOverlay.getLayoutParams();
        panelLp.width = Math.max(0, Math.min(panelWidth, rootWidth - panelLeft - edge));
        panelLp.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        panelLp.gravity = Gravity.LEFT | Gravity.BOTTOM;
        panelLp.leftMargin = panelLeft;
        panelLp.rightMargin = 0;
        panelLp.bottomMargin = Math.round(SHORTS_PANEL_BOTTOM_DP * density);
        mShortsOverlay.setLayoutParams(panelLp);
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
     * UP/DOWN: previous/next Short. Only when the focus is inside the right panel (after RIGHT) they move
     * the focus between the panel buttons, and switch the Short again once there is nothing further
     * up/down in the panel.<br/>
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
                boolean down = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN;
                if (panelFocused && canMoveFocusInPanel(down ? View.FOCUS_DOWN : View.FOCUS_UP)) {
                    return false; // regular focus movement inside the panel
                }
                // Single step per key press: ignore auto-repeat so a held key doesn't skip many Shorts
                if (isDown && event.getRepeatCount() == 0) {
                    PlaybackPresenter presenter = PlaybackPresenter.instance(mFragment.getContext());
                    if (down) {
                        presenter.onNextClicked();
                    } else {
                        presenter.onPreviousClicked();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!panelFocused && mSubscribeBtn != null) {
                    if (isDown) {
                        enterPanel();
                    }
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (panelFocused && !canMoveFocusInPanel(View.FOCUS_LEFT)) {
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

    /**
     * @return true if the currently focused panel view has another panel view in the given direction
     */
    private boolean canMoveFocusInPanel(int direction) {
        if (mShortsOverlay == null) {
            return false;
        }

        View focused = mShortsOverlay.findFocus();
        if (focused == null) {
            return false;
        }

        View next = focused.focusSearch(direction);
        return next != null && next != focused && isInsidePanel(next);
    }

    private boolean isInsidePanel(View view) {
        View current = view;
        while (current != null) {
            if (current == mShortsOverlay) {
                return true;
            }
            if (!(current.getParent() instanceof View)) {
                return false;
            }
            current = (View) current.getParent();
        }
        return false;
    }

    /**
     * Give the focus to the panel (only on user request: RIGHT).
     */
    private void enterPanel() {
        if (mShortsOverlay instanceof ViewGroup) {
            ((ViewGroup) mShortsOverlay).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        }
        if (mSubscribeBtn != null) {
            mSubscribeBtn.requestFocus();
        }
    }

    /**
     * Take the focus away from the panel and forbid the panel to grab it again,
     * otherwise the first focusable panel child takes it back and UP/DOWN never change the Short.
     */
    private void leavePanel() {
        if (mShortsOverlay == null) {
            return;
        }
        blockPanelFocus(); // block first, then clear: clearFocus() re-assigns focus to the first focusable view
        mShortsOverlay.clearFocus();
    }

    private void blockPanelFocus() {
        if (mShortsOverlay instanceof ViewGroup) {
            ((ViewGroup) mShortsOverlay).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
    }
    }

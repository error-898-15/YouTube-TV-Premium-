package com.liskovsoft.smartyoutubetv2.tv.ui.dialogs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ekta file-er moddhei:
 * 1. YouTube TV Modern Shorts Options Box UI (Card boxes + Circular bottom action buttons)
 * 2. YouTube TV Modern Comments Card UI ("Comment as..." input bar + Unified Comment Cards)
 * Kono XML layout ba external icon drawable charai sorasori kaj korbe.
 */
public class ShortsAndCommentsUiManager {

    private static final int COLOR_PANEL_BG = Color.parseColor("#0F0F0F");
    private static final int COLOR_CARD_BG = Color.parseColor("#272727");
    private static final int COLOR_CARD_FOCUSED_BG = Color.parseColor("#F1F1F1");
    private static final int COLOR_COMMENT_FOCUSED_BG = Color.parseColor("#3A3A3A");
    private static final int COLOR_TEXT_PRIMARY = Color.parseColor("#FFFFFF");
    private static final int COLOR_TEXT_SECONDARY = Color.parseColor("#AAAAAA");
    private static final int COLOR_TEXT_FOCUSED_DARK = Color.parseColor("#0F0F0F");

    // =========================================================================
    // 1. SHORTS VIDEO OPTIONS BOX UI MODEL & BUILDER
    // =========================================================================

    public static class ShortsMetadata {
        public final String title;
        public final String musicTitle;
        public final String relatedVideoTitle;
        public final String channelHandle;
        @Nullable public final Bitmap channelAvatar;
        public final boolean isSubscribed;

        public ShortsMetadata(String title, String musicTitle, String relatedVideoTitle,
                              String channelHandle, @Nullable Bitmap channelAvatar, boolean isSubscribed) {
            this.title = title;
            this.musicTitle = musicTitle;
            this.relatedVideoTitle = relatedVideoTitle;
            this.channelHandle = channelHandle;
            this.channelAvatar = channelAvatar;
            this.isSubscribed = isSubscribed;
        }
    }

    public interface ShortsOptionsListener {
        void onTitleBoxClicked();
        void onRelatedVideoClicked();
        void onChannelClicked();
        void onSubscribeClicked();
        void onLikeClicked();
        void onCommentsClicked();
        void onRemixClicked();
        void onMoreOptionsClicked();
    }

    /**
     * Shorts-er ডানপাশের অপশনগুলো বক্স আকারে (Image 1-er moto) তৈরি করার মেথড
     */
    public static View createShortsOptionsBoxView(@NonNull Context context,
                                                  @NonNull ShortsMetadata data,
                                                  @Nullable ShortsOptionsListener listener) {
        FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                dp(context, 380),
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(COLOR_PANEL_BG);
        root.setPadding(dp(context, 24), dp(context, 32), dp(context, 24), dp(context, 32));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams colParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
        );
        column.setLayoutParams(colParams);

        // --- BOX 1: Title + Hashtags + Music Info Box ---
        LinearLayout titleBox = new LinearLayout(context);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        LinearLayout.LayoutParams box1Params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        box1Params.bottomMargin = dp(context, 8);
        titleBox.setLayoutParams(box1Params);

        TextView titleTv = new TextView(context);
        titleTv.setText(data.title);
        titleTv.setTextColor(COLOR_TEXT_PRIMARY);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleTv.setTypeface(null, Typeface.BOLD);
        titleTv.setMaxLines(4);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);
        titleTv.setLineSpacing(dp(context, 2), 1.0f);
        titleBox.addView(titleTv);

        if (!TextUtils.isEmpty(data.musicTitle)) {
            LinearLayout musicRow = new LinearLayout(context);
            musicRow.setOrientation(LinearLayout.HORIZONTAL);
            musicRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams musicParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            musicParams.topMargin = dp(context, 10);
            musicRow.setLayoutParams(musicParams);

            ImageView musicIcon = new ImageView(context);
            musicIcon.setImageDrawable(new VectorIconDrawable(VectorIconDrawable.ICON_MUSIC_NOTE, COLOR_TEXT_SECONDARY));
            LinearLayout.LayoutParams mIconParams = new LinearLayout.LayoutParams(dp(context, 16), dp(context, 16));
            mIconParams.rightMargin = dp(context, 8);
            musicIcon.setLayoutParams(mIconParams);

            TextView musicTv = new TextView(context);
            musicTv.setText(data.musicTitle);
            musicTv.setTextColor(COLOR_TEXT_SECONDARY);
            musicTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            musicTv.setSingleLine(true);
            musicTv.setEllipsize(TextUtils.TruncateAt.END);

            musicRow.addView(musicIcon);
            musicRow.addView(musicTv);
            titleBox.addView(musicRow);
        }

        applyInteractiveBoxStyle(context, titleBox, 12, false, titleTv, null);
        titleBox.setOnClickListener(v -> {
            if (listener != null) listener.onTitleBoxClicked();
        });
        column.addView(titleBox);

        // --- BOX 2: Related Video Box (► The Position Is Called...) ---
        if (!TextUtils.isEmpty(data.relatedVideoTitle)) {
            LinearLayout relatedBox = new LinearLayout(context);
            relatedBox.setOrientation(LinearLayout.HORIZONTAL);
            relatedBox.setGravity(Gravity.CENTER_VERTICAL);
            relatedBox.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
            LinearLayout.LayoutParams box2Params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            box2Params.bottomMargin = dp(context, 8);
            relatedBox.setLayoutParams(box2Params);

            ImageView playIcon = new ImageView(context);
            playIcon.setImageDrawable(new VectorIconDrawable(VectorIconDrawable.ICON_PLAY, COLOR_TEXT_PRIMARY));
            LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(dp(context, 18), dp(context, 18));
            playParams.rightMargin = dp(context, 12);
            playIcon.setLayoutParams(playParams);

            TextView relatedTv = new TextView(context);
            relatedTv.setText(data.relatedVideoTitle);
            relatedTv.setTextColor(COLOR_TEXT_PRIMARY);
            relatedTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            relatedTv.setTypeface(null, Typeface.BOLD);
            relatedTv.setSingleLine(true);
            relatedTv.setEllipsize(TextUtils.TruncateAt.END);

            relatedBox.addView(playIcon);
            relatedBox.addView(relatedTv);

            applyInteractiveBoxStyle(context, relatedBox, 10, true, relatedTv, playIcon);
            relatedBox.setOnClickListener(v -> {
                if (listener != null) listener.onRelatedVideoClicked();
            });
            column.addView(relatedBox);
        }

        // --- BOX 3: Channel Box (Avatar + @REX_AMV) ---
        LinearLayout channelBox = new LinearLayout(context);
        channelBox.setOrientation(LinearLayout.HORIZONTAL);
        channelBox.setGravity(Gravity.CENTER_VERTICAL);
        channelBox.setPadding(dp(context, 14), dp(context, 10), dp(context, 16), dp(context, 10));
        LinearLayout.LayoutParams box3Params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        box3Params.bottomMargin = dp(context, 8);
        channelBox.setLayoutParams(box3Params);

        ImageView avatarView = new ImageView(context);
        LinearLayout.LayoutParams avParams = new LinearLayout.LayoutParams(dp(context, 28), dp(context, 28));
        avParams.rightMargin = dp(context, 12);
        avatarView.setLayoutParams(avParams);
        avatarView.setImageBitmap(getCircularAvatar(data.channelAvatar, data.channelHandle, dp(context, 28)));

        TextView channelTv = new TextView(context);
        channelTv.setText(data.channelHandle);
        channelTv.setTextColor(COLOR_TEXT_PRIMARY);
        channelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        channelTv.setTypeface(null, Typeface.BOLD);
        channelTv.setSingleLine(true);

        channelBox.addView(avatarView);
        channelBox.addView(channelTv);

        applyInteractiveBoxStyle(context, channelBox, 10, true, channelTv, null);
        channelBox.setOnClickListener(v -> {
            if (listener != null) listener.onChannelClicked();
        });
        column.addView(channelBox);

        // --- BOX 4: + Subscribe Box (Focus korle White pill hoye jabe) ---
        LinearLayout subscribeBox = new LinearLayout(context);
        subscribeBox.setOrientation(LinearLayout.HORIZONTAL);
        subscribeBox.setGravity(Gravity.CENTER_VERTICAL);
        subscribeBox.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
        LinearLayout.LayoutParams box4Params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        box4Params.bottomMargin = dp(context, 12);
        subscribeBox.setLayoutParams(box4Params);

        ImageView plusIcon = new ImageView(context);
        plusIcon.setImageDrawable(new VectorIconDrawable(VectorIconDrawable.ICON_PLUS, COLOR_TEXT_PRIMARY));
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(dp(context, 18), dp(context, 18));
        plusParams.rightMargin = dp(context, 12);
        plusIcon.setLayoutParams(plusParams);

        TextView subTv = new TextView(context);
        subTv.setText(data.isSubscribed ? "Subscribed" : "Subscribe");
        subTv.setTextColor(COLOR_TEXT_PRIMARY);
        subTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        subTv.setTypeface(null, Typeface.BOLD);

        subscribeBox.addView(plusIcon);
        subscribeBox.addView(subTv);

        applyInteractiveBoxStyle(context, subscribeBox, 10, true, subTv, plusIcon);
        subscribeBox.setOnClickListener(v -> {
            if (listener != null) listener.onSubscribeClicked();
        });
        column.addView(subscribeBox);

        // --- ROW 5: Circular Bottom Action Buttons (Like, Comment, Remix, More) ---
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        actionsRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        View likeBtn = createCircularActionButton(context, VectorIconDrawable.ICON_HEART, () -> {
            if (listener != null) listener.onLikeClicked();
        });
        View commentBtn = createCircularActionButton(context, VectorIconDrawable.ICON_COMMENT_LINES, () -> {
            if (listener != null) listener.onCommentsClicked();
        });
        View remixBtn = createCircularActionButton(context, VectorIconDrawable.ICON_REMIX, () -> {
            if (listener != null) listener.onRemixClicked();
        });
        View moreBtn = createCircularActionButton(context, VectorIconDrawable.ICON_MORE_VERT, () -> {
            if (listener != null) listener.onMoreOptionsClicked();
        });

        actionsRow.addView(likeBtn);
        actionsRow.addView(commentBtn);
        actionsRow.addView(remixBtn);
        actionsRow.addView(moreBtn);

        column.addView(actionsRow);
        root.addView(column);

        // Default focus Subscribe button-e (Image 1-er moto)
        subscribeBox.post(subscribeBox::requestFocus);

        return root;
    }

    private static View createCircularActionButton(Context context, int iconType, Runnable onClick) {
        FrameLayout btn = new FrameLayout(context);
        int size = dp(context, 42);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.rightMargin = dp(context, 12);
        btn.setLayoutParams(params);
        btn.setFocusable(true);
        btn.setFocusableInTouchMode(true);

        ImageView icon = new ImageView(context);
        int iconSize = dp(context, 20);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
        icon.setLayoutParams(iconParams);
        VectorIconDrawable drawable = new VectorIconDrawable(iconType, COLOR_TEXT_PRIMARY);
        icon.setImageDrawable(drawable);
        btn.addView(icon);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(COLOR_CARD_BG);
        btn.setBackground(bg);

        btn.setOnFocusChangeListener((v, hasFocus) -> {
            bg.setColor(hasFocus ? COLOR_CARD_FOCUSED_BG : COLOR_CARD_BG);
            drawable.setColor(hasFocus ? COLOR_TEXT_FOCUSED_DARK : COLOR_TEXT_PRIMARY);
            btn.invalidate();
        });

        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    private static void applyInteractiveBoxStyle(Context context, View box, int radiusDp,
                                                 boolean invertOnFocus,
                                                 @Nullable TextView primaryText,
                                                 @Nullable ImageView iconView) {
        box.setFocusable(true);
        box.setFocusableInTouchMode(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(context, radiusDp));
        bg.setColor(COLOR_CARD_BG);
        box.setBackground(bg);

        box.setOnFocusChangeListener((v, hasFocus) -> {
            if (invertOnFocus) {
                bg.setColor(hasFocus ? COLOR_CARD_FOCUSED_BG : COLOR_CARD_BG);
                if (primaryText != null) {
                    primaryText.setTextColor(hasFocus ? COLOR_TEXT_FOCUSED_DARK : COLOR_TEXT_PRIMARY);
                }
                if (iconView != null && iconView.getDrawable() instanceof VectorIconDrawable) {
                    ((VectorIconDrawable) iconView.getDrawable()).setColor(
                            hasFocus ? COLOR_TEXT_FOCUSED_DARK : COLOR_TEXT_PRIMARY
                    );
                }
            } else {
                bg.setColor(hasFocus ? COLOR_COMMENT_FOCUSED_BG : COLOR_CARD_BG);
            }
        });
    }

    // =========================================================================
    // 2. COMMENTS BOX UI MODEL & BUILDER (IMAGE 4 STYLE)
    // =========================================================================

    public static class CommentItemData {
        public final String authorHandle;
        public final String publishedTime;
        public final String commentText;
        public final String likeCount;
        public final String replyCount;
        public final boolean isPinned;
        @Nullable public final Bitmap authorAvatar;

        public CommentItemData(String authorHandle, String publishedTime, String commentText,
                               String likeCount, String replyCount, boolean isPinned,
                               @Nullable Bitmap authorAvatar) {
            this.authorHandle = authorHandle;
            this.publishedTime = publishedTime;
            this.commentText = commentText;
            this.likeCount = likeCount;
            this.replyCount = replyCount;
            this.isPinned = isPinned;
            this.authorAvatar = authorAvatar;
        }
    }

    public interface CommentsPanelListener {
        void onWriteCommentClicked();
        void onCommentCardClicked(CommentItemData comment, int position);
    }

    /**
     * Comments Box-er সম্পূর্ণ নতুন Card UI (Image 4-er moto) তৈরি করার মেথড
     */
    public static View createCommentsPanelView(@NonNull Context context,
                                               @NonNull String totalCommentsCount,
                                               @NonNull String currentUserHandle,
                                               @Nullable Bitmap currentUserAvatar,
                                               @NonNull List<CommentItemData> comments,
                                               @Nullable CommentsPanelListener listener) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                dp(context, 420),
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(COLOR_PANEL_BG);
        root.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), 0);

        // --- Header: "Comments 136" ---
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        headerParams.bottomMargin = dp(context, 14);
        headerRow.setLayoutParams(headerParams);

        TextView headerTitle = new TextView(context);
        headerTitle.setText("Comments ");
        headerTitle.setTextColor(COLOR_TEXT_PRIMARY);
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        headerTitle.setTypeface(null, Typeface.BOLD);

        TextView headerCount = new TextView(context);
        headerCount.setText(totalCommentsCount);
        headerCount.setTextColor(COLOR_TEXT_SECONDARY);
        headerCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        headerCount.setTypeface(null, Typeface.BOLD);

        headerRow.addView(headerTitle);
        headerRow.addView(headerCount);
        root.addView(headerRow);

        // --- Scrollable List (Input Box + Comment Cards) ---
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, 0, 0, dp(context, 28));

        // 1. "Comment as @Just_A_Modder..." Input Bar
        LinearLayout inputBar = new LinearLayout(context);
        inputBar.setOrientation(LinearLayout.HORIZONTAL);
        inputBar.setGravity(Gravity.CENTER_VERTICAL);
        inputBar.setPadding(dp(context, 14), dp(context, 12), dp(context, 16), dp(context, 12));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.bottomMargin = dp(context, 12);
        inputBar.setLayoutParams(inputParams);

        ImageView userAvatarView = new ImageView(context);
        LinearLayout.LayoutParams uAvParams = new LinearLayout.LayoutParams(dp(context, 30), dp(context, 30));
        uAvParams.rightMargin = dp(context, 12);
        userAvatarView.setLayoutParams(uAvParams);
        userAvatarView.setImageBitmap(getCircularAvatar(currentUserAvatar, currentUserHandle, dp(context, 30)));

        TextView promptTv = new TextView(context);
        LinearLayout.LayoutParams promptParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        promptTv.setLayoutParams(promptParams);
        promptTv.setText("Comment as " + currentUserHandle + "...");
        promptTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        promptTv.setSingleLine(true);
        promptTv.setEllipsize(TextUtils.TruncateAt.END);

        ImageView commentPromptIcon = new ImageView(context);
        commentPromptIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 20), dp(context, 20)));
        VectorIconDrawable promptIconDrawable = new VectorIconDrawable(VectorIconDrawable.ICON_COMMENT_LINES, COLOR_TEXT_PRIMARY);
        commentPromptIcon.setImageDrawable(promptIconDrawable);

        inputBar.addView(userAvatarView);
        inputBar.addView(promptTv);
        inputBar.addView(commentPromptIcon);

        inputBar.setFocusable(true);
        inputBar.setFocusableInTouchMode(true);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dp(context, 12));
        inputBg.setColor(COLOR_CARD_BG);
        inputBar.setBackground(inputBg);

        inputBar.setOnFocusChangeListener((v, hasFocus) -> {
            inputBg.setColor(hasFocus ? COLOR_CARD_FOCUSED_BG : COLOR_CARD_BG);
            promptTv.setTextColor(hasFocus ? Color.parseColor("#555555") : COLOR_TEXT_SECONDARY);
            promptIconDrawable.setColor(hasFocus ? COLOR_TEXT_FOCUSED_DARK : COLOR_TEXT_PRIMARY);
        });
        inputBar.setOnClickListener(v -> {
            if (listener != null) listener.onWriteCommentClicked();
        });

        listContainer.addView(inputBar);

        // 2. Individual Comment Cards (Avatar inside card + @User • time + Pin + Body + Like/Reply row)
        for (int i = 0; i < comments.size(); i++) {
            final int pos = i;
            CommentItemData item = comments.get(i);
            View card = createSingleCommentCard(context, item, () -> {
                if (listener != null) listener.onCommentCardClicked(item, pos);
            });
            listContainer.addView(card);
        }

        scrollView.addView(listContainer);
        root.addView(scrollView);

        // Default focus uporer "Comment as..." bar-e thakbe (Image 4-er moto)
        inputBar.post(inputBar::requestFocus);

        return root;
    }

    private static View createSingleCommentCard(Context context, CommentItemData item, Runnable onClick) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(context, 10);
        card.setLayoutParams(cardParams);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(context, 12));
        cardBg.setColor(COLOR_CARD_BG);
        card.setBackground(cardBg);

        // --- Top Row inside Card: Avatar + "@Username • 18 hours ago" + Pin Icon ---
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        topParams.bottomMargin = dp(context, 10);
        topRow.setLayoutParams(topParams);

        ImageView avatar = new ImageView(context);
        LinearLayout.LayoutParams avParams = new LinearLayout.LayoutParams(dp(context, 30), dp(context, 30));
        avParams.rightMargin = dp(context, 10);
        avatar.setLayoutParams(avParams);
        avatar.setImageBitmap(getCircularAvatar(item.authorAvatar, item.authorHandle, dp(context, 30)));
        topRow.addView(avatar);

        LinearLayout authorMetaRow = new LinearLayout(context);
        authorMetaRow.setOrientation(LinearLayout.HORIZONTAL);
        authorMetaRow.setGravity(Gravity.CENTER_VERTICAL);
        authorMetaRow.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));

        TextView authorTv = new TextView(context);
        authorTv.setText(item.authorHandle);
        authorTv.setTextColor(COLOR_TEXT_PRIMARY);
        authorTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        authorTv.setTypeface(null, Typeface.BOLD);
        authorTv.setSingleLine(true);
        authorTv.setEllipsize(TextUtils.TruncateAt.END);
        authorTv.setMaxWidth(dp(context, 170));

        TextView timeTv = new TextView(context);
        timeTv.setText(" • " + item.publishedTime);
        timeTv.setTextColor(COLOR_TEXT_SECONDARY);
        timeTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        timeTv.setSingleLine(true);

        authorMetaRow.addView(authorTv);
        authorMetaRow.addView(timeTv);
        topRow.addView(authorMetaRow);

        if (item.isPinned) {
            ImageView pinIcon = new ImageView(context);
            pinIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 16), dp(context, 16)));
            pinIcon.setImageDrawable(new VectorIconDrawable(VectorIconDrawable.ICON_PIN, COLOR_TEXT_PRIMARY));
            topRow.addView(pinIcon);
        }

        card.addView(topRow);

        // --- Middle Row: Comment Text ---
        TextView bodyTv = new TextView(context);
        bodyTv.setText(item.commentText);
        bodyTv.setTextColor(COLOR_TEXT_PRIMARY);
        bodyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        bodyTv.setLineSpacing(dp(context, 2), 1.05f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bodyParams.bottomMargin = dp(context, 12);
        bodyTv.setLayoutParams(bodyParams);
        card.addView(bodyTv);

        // --- Bottom Row: Outline Thumb Up + Count & Reply Icon + Count ---
        LinearLayout bottomRow = new LinearLayout(context);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);
        bottomRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        ImageView likeIcon = new ImageView(context);
        LinearLayout.LayoutParams likeIconParams = new LinearLayout.LayoutParams(dp(context, 15), dp(context, 15));
        likeIconParams.rightMargin = dp(context, 6);
        likeIcon.setLayoutParams(likeIconParams);
        likeIcon.setImageDrawable(new VectorIconDrawable(VectorIconDrawable.ICON_THUMB_UP, COLOR_TEXT_SECONDARY));
        bottomRow.addView(likeIcon);

        TextView likeCountTv = new TextView(context);
        likeCountTv.setText(TextUtils.isEmpty(item.likeCount) ? "0" : item.likeCount);
        likeCountTv.setTextColor(COLOR_TEXT_SECONDARY);
        likeCountTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams likeTxtParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        likeTxtParams.rightMargin = dp(context, 18);
        likeCountTv.setLayoutParams(likeTxtParams);
        bottomRow.addView(likeCountTv);

        ImageView replyIcon = new ImageView(context);
        LinearLayout.LayoutParams replyIconParams = new LinearLayout.LayoutParams(dp(context, 15), dp(context, 15));
        replyIconParams.rightMargin = dp(context, 6);
        replyIcon.setLayoutParams(replyIconParams);
        replyIcon.setImageDrawable(new VectorIconDrawable(VectorIconDrawable.ICON_COMMENT_LINES, COLOR_TEXT_SECONDARY));
        bottomRow.addView(replyIcon);

        TextView replyCountTv = new TextView(context);
        replyCountTv.setText(TextUtils.isEmpty(item.replyCount) ? "0" : item.replyCount);
        replyCountTv.setTextColor(COLOR_TEXT_SECONDARY);
        replyCountTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        bottomRow.addView(replyCountTv);

        card.addView(bottomRow);

        card.setOnFocusChangeListener((v, hasFocus) -> {
            cardBg.setColor(hasFocus ? COLOR_COMMENT_FOCUSED_BG : COLOR_CARD_BG);
        });
        card.setOnClickListener(v -> onClick.run());

        return card;
    }

    // =========================================================================
    // 3. ZERO-XML CUSTOM VECTOR DRAWABLE & CIRCULAR AVATAR GENERATOR
    // =========================================================================

    private static Bitmap getCircularAvatar(@Nullable Bitmap src, String fallbackName, int sizePx) {
        Bitmap output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float radius = sizePx / 2f;

        if (src != null && !src.isRecycled()) {
            canvas.drawCircle(radius, radius, radius, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            Rect srcRect = new Rect(0, 0, src.getWidth(), src.getHeight());
            RectF dstRect = new RectF(0, 0, sizePx, sizePx);
            canvas.drawBitmap(src, srcRect, dstRect, paint);
        } else {
            paint.setColor(Color.parseColor("#444444"));
            canvas.drawCircle(radius, radius, radius, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(sizePx * 0.45f);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            String letter = (!TextUtils.isEmpty(fallbackName))
                    ? fallbackName.replace("@", "").substring(0, 1).toUpperCase()
                    : "U";
            Paint.FontMetrics fm = paint.getFontMetrics();
            float textY = radius - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(letter, radius, textY, paint);
        }
        return output;
    }

    /**
     * Kono XML file charai Heart, Comment, Remix, 3-Dots, Pin, Thumb-up icon aakar class
     */
    private static class VectorIconDrawable extends Drawable {
        static final int ICON_HEART = 1;
        static final int ICON_COMMENT_LINES = 2;
        static final int ICON_REMIX = 3;
        static final int ICON_MORE_VERT = 4;
        static final int ICON_PLAY = 5;
        static final int ICON_PLUS = 6;
        static final int ICON_MUSIC_NOTE = 7;
        static final int ICON_PIN = 8;
        static final int ICON_THUMB_UP = 9;

        private final int mType;
        private final Paint mPaint;

        VectorIconDrawable(int type, int color) {
            mType = type;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(color);
            mPaint.setStrokeWidth(3.2f);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
        }

        void setColor(int color) {
            mPaint.setColor(color);
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            float cx = b.exactCenterX();
            float cy = b.exactCenterY();

            switch (mType) {
                case ICON_PLAY: {
                    mPaint.setStyle(Paint.Style.FILL);
                    Path path = new Path();
                    path.moveTo(w * 0.25f, h * 0.18f);
                    path.lineTo(w * 0.82f, cy);
                    path.lineTo(w * 0.25f, h * 0.82f);
                    path.close();
                    canvas.drawPath(path, mPaint);
                    break;
                }
                case ICON_PLUS: {
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(cx, h * 0.2f, cx, h * 0.8f, mPaint);
                    canvas.drawLine(w * 0.2f, cy, w * 0.8f, cy, mPaint);
                    break;
                }
                case ICON_MORE_VERT: {
                    mPaint.setStyle(Paint.Style.FILL);
                    float r = w * 0.08f;
                    canvas.drawCircle(cx, h * 0.24f, r, mPaint);
                    canvas.drawCircle(cx, cy, r, mPaint);
                    canvas.drawCircle(cx, h * 0.76f, r, mPaint);
                    break;
                }
                case ICON_HEART: {
                    mPaint.setStyle(Paint.Style.STROKE);
                    Path path = new Path();
                    path.moveTo(cx, h * 0.82f);
                    path.cubicTo(w * 0.1f, h * 0.55f, w * 0.08f, h * 0.22f, w * 0.3f, h * 0.2f);
                    path.cubicTo(w * 0.42f, h * 0.2f, cx, h * 0.3f, cx, h * 0.34f);
                    path.cubicTo(cx, h * 0.3f, w * 0.58f, h * 0.2f, w * 0.7f, h * 0.2f);
                    path.cubicTo(w * 0.92f, h * 0.22f, w * 0.9f, h * 0.55f, cx, h * 0.82f);
                    canvas.drawPath(path, mPaint);
                    break;
                }
                case ICON_COMMENT_LINES: {
                    mPaint.setStyle(Paint.Style.STROKE);
                    RectF rect = new RectF(w * 0.15f, h * 0.16f, w * 0.85f, h * 0.74f);
                    canvas.drawRoundRect(rect, w * 0.12f, w * 0.12f, mPaint);
                    canvas.drawLine(w * 0.30f, h * 0.36f, w * 0.70f, h * 0.36f, mPaint);
                    canvas.drawLine(w * 0.30f, h * 0.54f, w * 0.56f, h * 0.54f, mPaint);
                    break;
                }
                case ICON_REMIX: {
                    mPaint.setStyle(Paint.Style.STROKE);
                    RectF arcRect = new RectF(w * 0.16f, h * 0.16f, w * 0.84f, h * 0.84f);
                    canvas.drawArc(arcRect, 200, 140, false, mPaint);
                    canvas.drawArc(arcRect, 20, 140, false, mPaint);
                    canvas.drawLine(cx, h * 0.36f, cx, h * 0.64f, mPaint);
                    canvas.drawLine(w * 0.36f, cy, w * 0.64f, cy, mPaint);
                    break;
                }
                case ICON_MUSIC_NOTE: {
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(w * 0.35f, h * 0.72f, w * 0.14f, mPaint);
                    canvas.drawLine(w * 0.49f, h * 0.72f, w * 0.49f, h * 0.2f, mPaint);
                    canvas.drawLine(w * 0.49f, h * 0.2f, w * 0.75f, h * 0.28f, mPaint);
                    break;
                }
                case ICON_PIN: {
                    mPaint.setStyle(Paint.Style.FILL);
                    canvas.save();
                    canvas.rotate(30f, cx, cy);
                    canvas.drawRoundRect(new RectF(w * 0.34f, h * 0.18f, w * 0.66f, h * 0.54f), 4f, 4f, mPaint);
                    canvas.drawRect(w * 0.46f, h * 0.54f, w * 0.54f, h * 0.85f, mPaint);
                    canvas.restore();
                    break;
                }
                case ICON_THUMB_UP: {
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(2.4f);
                    Path p = new Path();
                    p.moveTo(w * 0.32f, h * 0.82f);
                    p.lineTo(w * 0.74f, h * 0.82f);
                    p.lineTo(w * 0.84f, h * 0.45f);
                    p.lineTo(w * 0.56f, h * 0.45f);
                    p.lineTo(w * 0.62f, h * 0.18f);
                    p.lineTo(w * 0.48f, h * 0.18f);
                    p.lineTo(w * 0.32f, h * 0.45f);
                    p.close();
                    canvas.drawPath(p, mPaint);
                    canvas.drawRect(w * 0.16f, h * 0.45f, w * 0.32f, h * 0.82f, mPaint);
                    break;
                }
            }
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }
                                                              }

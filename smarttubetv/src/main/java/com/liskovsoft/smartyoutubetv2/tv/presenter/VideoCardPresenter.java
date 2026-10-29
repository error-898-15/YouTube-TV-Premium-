package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview.ComplexImageCardView;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class VideoCardPresenter extends LongClickPresenter {
    private static final String TAG = VideoCardPresenter.class.getSimpleName();
    private int mDefaultBackgroundColor = -1;
    private int mDefaultTextColor = -1;
    private int mSelectedBackgroundColor = -1;
    private int mSelectedTextColor = -1;
    private int mCardPreviewType;
    private int mThumbQuality;
    private int mWidth;
    private int mHeight;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        // Clean YouTube TV style transparent unfocused background & white focused card state
        mDefaultBackgroundColor = Color.TRANSPARENT;
        mDefaultTextColor = Color.parseColor("#F1F1F1");
        mSelectedBackgroundColor = Color.parseColor("#FFFFFF");
        mSelectedTextColor = Color.parseColor("#0F0F0F");

        mCardPreviewType = getCardPreviewType(context);
        mThumbQuality = getThumbQuality(context);

        boolean isCardMultilineTitleEnabled = isCardMultilineTitleEnabled(context);
        boolean isCardMultilineSubtitleEnabled = isCardMultilineSubtitleEnabled(context);
        boolean isCardTextAutoScrollEnabled = isCardTextAutoScrollEnabled(context);
        float cardTextScrollSpeed = getCardTextScrollSpeed(context);

        updateDimensions(context);

        ComplexImageCardView cardView = new ComplexImageCardView(context) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setTitleLinesNum(isCardMultilineTitleEnabled ? 2 : 1);
        cardView.setContentLinesNum(isCardMultilineSubtitleEnabled ? 1 : 1);
        cardView.enableTextAutoScroll(isCardTextAutoScrollEnabled);
        cardView.setTextScrollSpeed(cardTextScrollSpeed);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.enableBadge(isBadgeEnabled());
        cardView.enableTitle(isTitleEnabled());
        cardView.enableContent(isContentEnabled());
        cardView.setBackgroundColor(Color.TRANSPARENT);

        if (VERSION.SDK_INT >= 21) {
            final float cornerRadius = context.getResources().getDimension(R.dimen.lb_rounded_rect_corner_radius);
            cardView.setClipToOutline(true);
            cardView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
                }
            });

            View imageWrapper = cardView.findViewById(R.id.main_image_wrapper);
            if (imageWrapper != null) {
                imageWrapper.setClipToOutline(true);
                imageWrapper.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
                    }
                });
                if (VERSION.SDK_INT >= 23) {
                    imageWrapper.setForeground(ContextCompat.getDrawable(context, R.drawable.lb_card_outline));
                }
            }
        }

        TextView titleText = cardView.findViewById(R.id.title_text);
        if (titleText != null) {
            titleText.setEllipsize(TextUtils.TruncateAt.END);
            titleText.setTextSize(15f);
            titleText.setIncludeFontPadding(false);
        }

        TextView contentText = cardView.findViewById(R.id.content_text);
        if (contentText != null) {
            contentText.setEllipsize(TextUtils.TruncateAt.END);
            contentText.setTextSize(12f);
            contentText.setIncludeFontPadding(false);
        }

        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ComplexImageCardView view, boolean selected) {
        int backgroundColor = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;
        int titleColor = selected ? mSelectedTextColor : mDefaultTextColor;
        int contentColor = selected ? Color.parseColor("#555555") : Color.parseColor("#AAAAAA");

        View infoField = view.findViewById(R.id.info_field);
        if (infoField != null) {
            infoField.setBackgroundColor(backgroundColor);
            infoField.setPadding(10, 8, 10, 8);
        }

        TextView titleText = view.findViewById(R.id.title_text);
        if (titleText != null) {
            titleText.setTextColor(titleColor);
        }
        TextView contentText = view.findViewById(R.id.content_text);
        if (contentText != null) {
            contentText.setTextColor(contentColor);
        }
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        Video video = (Video) item;

        ComplexImageCardView cardView = (ComplexImageCardView) viewHolder.view;
        Context context = cardView.getContext();

        cardView.setTitleText(video.getTitle());
        cardView.setContentText(video.getSecondTitle());
        // Count progress that very close to zero. E.g. when user closed video immediately.
        cardView.setProgress(video.percentWatched > 0 && video.percentWatched < 1 ? 1 : Math.round(video.percentWatched));
        cardView.setBadgeText(
                video.hasNewContent ? context.getString(R.string.badge_new_content) :
                video.isLive ? context.getString(R.string.badge_live) :
                video.isShorts ? context.getString(R.string.header_shorts).toUpperCase() :
                video.badge
        );
        cardView.setBadgeColor(video.hasNewContent || video.isLive || video.isUpcoming ?
                ContextCompat.getColor(context, R.color.dark_red) : ContextCompat.getColor(context, R.color.black));

        if (mCardPreviewType != MainUIData.CARD_PREVIEW_DISABLED) {
            cardView.setPreview(video);
            cardView.setMute(mCardPreviewType == MainUIData.CARD_PREVIEW_MUTED);
        }

        cardView.setMainImageDimensions(mWidth, mHeight);

        if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            // Glide.with(context): IllegalArgumentException: You cannot start a load for a destroyed activity
            return;
        }

        Glide.with(context)
                .load(ClickbaitRemover.updateThumbnail(video, mThumbQuality))
                .apply(ViewUtil.glideOptions())
                .override(mWidth, mHeight)
                .diskCacheStrategy(VERSION.SDK_INT > 21 ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .listener(mErrorListener)
                .error(
                    Glide.with(context)
                        .load(video.cardImageUrl)
                        .apply(ViewUtil.glideOptions())
                        .listener(mErrorListener)
                        .error(R.drawable.card_placeholder)
                )
                .into(cardView.getMainImageView());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        super.onUnbindViewHolder(viewHolder);

        ComplexImageCardView cardView = (ComplexImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);

        Glide.with(cardView.getContext().getApplicationContext()).clear(cardView.getMainImageView());
    }

    private void updateDimensions(Context context) {
        Pair<Integer, Integer> dimens = getCardDimensPx(context);

        mWidth = dimens.first;
        mHeight = dimens.second;
    }

    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(context, R.dimen.card_width, R.dimen.card_height, MainUIData.instance(context).getVideoGridScale());
    }

    protected boolean isCardTextAutoScrollEnabled(Context context) {
        return MainUIData.instance(context).isCardTextAutoScrollEnabled();
    }

    protected int getCardPreviewType(Context context) {
        return MainUIData.instance(context).getCardPreviewType();
    }

    protected boolean isCardMultilineTitleEnabled(Context context) {
        return MainUIData.instance(context).isCardMultilineTitleEnabled();
    }

    protected boolean isCardMultilineSubtitleEnabled(Context context) {
        return MainUIData.instance(context).isCardMultilineSubtitleEnabled();
    }

    protected float getCardTextScrollSpeed(Context context) {
        return MainUIData.instance(context).getCardTextScrollSpeed();
    }

    protected int getThumbQuality(Context context) {
        return MainUIData.instance(context).getThumbQuality();
    }

    protected boolean isContentEnabled() {
        return true;
    }

    protected boolean isTitleEnabled() {
        return true;
    }

    protected boolean isBadgeEnabled() {
        return true;
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };

    private final RequestListener<Bitmap> mErrorListener2 = new RequestListener<Bitmap>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
            }

package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * YouTube TV style sidebar item: icon (or round avatar) + label inside a rounded pill.
 * Optional divider/gap is drawn above the item when a new sidebar group starts.
 */
public class IconHeaderItemPresenter extends RowHeaderPresenter {
    private static final String TAG = IconHeaderItemPresenter.class.getSimpleName();

    public static final int DIVIDER_NONE = 0;
    public static final int DIVIDER_LINE = 1;
    public static final int DIVIDER_GAP = 2;

    public interface DividerResolver {
        /**
         * @return one of {@link #DIVIDER_NONE}, {@link #DIVIDER_LINE}, {@link #DIVIDER_GAP}
         */
        int getDividerMode(Object item);
    }

    /**
     * Holds the state of the collapsed icon rail: labels are hidden and
     * only the current section stays fully bright (like YouTube TV).
     */
    public static class HeaderViewHolder extends RowHeaderPresenter.ViewHolder {
        private boolean mRail;
        private boolean mCurrent;

        public HeaderViewHolder(View view) {
            super(view);
        }

        public void setRailMode(boolean rail, boolean current) {
            mRail = rail;
            mCurrent = current;

            TextView label = view.findViewById(R.id.header_label);
            if (label != null) {
                label.setAlpha(rail ? 0f : 1f);
            }

            view.setAlpha(getRestAlpha());
        }

        float getRestAlpha() {
            return mRail && !mCurrent ? 0.55f : 1f;
        }
    }

    private final int mResId;
    private final String mIconUrl;
    private final DividerResolver mDividerResolver;
    private Drawable mDefaultIcon;

    public IconHeaderItemPresenter(int resId, String iconUrl) {
        this(resId, iconUrl, null);
    }

    public IconHeaderItemPresenter(int resId, String iconUrl, DividerResolver dividerResolver) {
        mResId = resId;
        mIconUrl = iconUrl;
        mDividerResolver = dividerResolver;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        mDefaultIcon = new ColorDrawable(ContextCompat.getColor(viewGroup.getContext(), R.color.lb_grey));

        // Inflate with parent so match_parent width is respected
        View view = inflater.inflate(R.layout.icon_header_item, viewGroup, false);
        view.setAlpha(1f); // YouTube TV: unselected items are fully visible

        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        HeaderItem headerItem;

        if (item instanceof PageRow) {
            headerItem = ((PageRow) item).getHeaderItem();
        } else {
            headerItem = ((ListRow) item).getHeaderItem();
        }

        View rootView = viewHolder.view;
        rootView.setFocusable(true);

        bindDivider(rootView, item);

        ImageView iconView = rootView.findViewById(R.id.header_icon);
        if (iconView != null) {
            if (mIconUrl != null) {
                // Channel avatar -> round like on YouTube TV
                ImageViewCompat.setImageTintList(iconView, null);

                Glide.with(rootView.getContext())
                        .load(mIconUrl)
                        .apply(ViewUtil.glideOptions().circleCrop().error(mDefaultIcon))
                        .listener(mErrorListener)
                        .into(iconView);
            } else {
                Drawable icon = mResId > 0 ? ContextCompat.getDrawable(rootView.getContext(), mResId) : mDefaultIcon;
                iconView.setImageDrawable(icon);
                ImageViewCompat.setImageTintList(iconView,
                        mResId > 0 ? ContextCompat.getColorStateList(rootView.getContext(), R.color.header_content_color) : null);
            }
        }

        TextView label = rootView.findViewById(R.id.header_label);
        if (label != null) {
            label.setText(headerItem.getName());
        }
    }

    private void bindDivider(View rootView, Object item) {
        View divider = rootView.findViewById(R.id.header_divider);

        if (divider == null) {
            return;
        }

        int mode = mDividerResolver != null ? mDividerResolver.getDividerMode(item) : DIVIDER_NONE;

        switch (mode) {
            case DIVIDER_LINE:
                divider.setVisibility(View.VISIBLE);
                break;
            case DIVIDER_GAP:
                divider.setVisibility(View.INVISIBLE); // keeps the space but draws nothing
                break;
            default:
                divider.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // NOP
    }

    // Keep items fully opaque (default leanback dims unselected headers to 50%)
    @Override
    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        holder.view.setAlpha(holder instanceof HeaderViewHolder ? ((HeaderViewHolder) holder).getRestAlpha() : 1f);
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
            }

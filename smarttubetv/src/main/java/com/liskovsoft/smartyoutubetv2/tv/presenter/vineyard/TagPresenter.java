// File Path: smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/presenter/vineyard/TagPresenter.java
package com.liskovsoft.smartyoutubetv2.tv.presenter.vineyard;

import android.graphics.Color;
import android.view.ViewGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.User;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.vineyard.TagCardView;

public class TagPresenter extends LongClickPresenter {
    private static int sDefaultTextColor;
    private static int sSelectedTextColor;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        sDefaultTextColor = Color.parseColor("#F1F1F1");
        sSelectedTextColor = Color.parseColor("#0F0F0F");

        TagCardView cardView = new TagCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardTextColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setBackgroundColor(Color.TRANSPARENT);
        updateCardTextColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private static void updateCardTextColor(TagCardView view, boolean selected) {
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setTextColor(selected ? sSelectedTextColor : sDefaultTextColor);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        if (item instanceof Tag) {
            Tag post = (Tag) item;
            TagCardView cardView = (TagCardView) viewHolder.view;

            if (post.tag != null) {
                cardView.setCardText(post.tag);
                cardView.setCardIcon(R.drawable.icon_trending);
            }
        } else if (item instanceof User) {
            User post = (User) item;
            TagCardView cardView = (TagCardView) viewHolder.view;

            if (post.username != null) {
                cardView.setCardText(post.username);
                cardView.setCardIcon(R.drawable.ic_user);
            }
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
    }

// File Path: common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/controllers/ShortsFeed.java
package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.mediaserviceinterfaces.ContentService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.BlockedChannelData;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Endless feed of recommended Shorts (the same feed as the "Shorts" section of the app).<br/>
 * <br/>
 * ROOT CAUSE of the "Shorts always come from the same channel" bug: the next Short was taken from
 * <em>Video.nextMediaItem</em>, which is the next item of the playlist/channel the Short was opened from.
 * Now the next Short is taken from this feed, so the user gets a mixed (random) Shorts stream like in
 * the official app. The feed is also loaded ahead of time, so the next Short is known before the user
 * presses DOWN (this is needed for the format info preload that makes the Shorts start faster).
 */
public class ShortsFeed {
    private static final String TAG = ShortsFeed.class.getSimpleName();
    /** Load more items in the background when the queue becomes smaller than this */
    private static final int MIN_QUEUE_SIZE = 6;
    /** How many already shown Shorts we remember (to not show them twice) */
    private static final int MAX_SEEN_SIZE = 400;
    private final ContentService mContentService;
    private final List<Video> mQueue = new ArrayList<>();
    private final Set<String> mSeen = new LinkedHashSet<>();
    private final List<Runnable> mCallbacks = new ArrayList<>();
    private MediaGroup mLastGroup;
    private Disposable mAction;
    private boolean mLoadFailed;

    public ShortsFeed(ContentService contentService) {
        mContentService = contentService;
    }

    /**
     * Next Short of the feed. Doesn't remove the item from the queue (see {@link #markSeen(Video)}).
     */
    public Video peekNext() {
        return mQueue.isEmpty() ? null : mQueue.get(0);
    }

    /**
     * @return true if the last attempt to load the feed gave nothing (offline, feed unavailable etc)
     */
    public boolean isLoadFailed() {
        return mLoadFailed;
    }

    /**
     * Call this for every Short that is going to be played. It removes the Short from the queue and
     * guarantees that the Short won't be offered again.
     */
    public void markSeen(Video video) {
        if (video == null || video.videoId == null) {
            return;
        }

        for (Iterator<Video> it = mQueue.iterator(); it.hasNext(); ) {
            if (video.videoId.equals(it.next().videoId)) {
                it.remove();
            }
        }

        mSeen.remove(video.videoId); // move to the end
        mSeen.add(video.videoId);

        if (mSeen.size() > MAX_SEEN_SIZE) {
            Iterator<String> it = mSeen.iterator();
            it.next();
            it.remove();
        }
    }

    /**
     * Makes sure the queue has something to play.<br/>
     * If the queue isn't empty the callback is called immediately (more items are loaded in the background).
     * Otherwise the callback is called after the load is finished (successfully or not).
     */
    public void ensureLoaded(Runnable onReady) {
        if (!mQueue.isEmpty()) {
            if (onReady != null) {
                onReady.run();
            }
            if (mQueue.size() < MIN_QUEUE_SIZE) {
                loadMore();
            }
            return;
        }

        if (onReady != null) {
            mCallbacks.add(onReady);
        }

        loadMore();
    }

    /**
     * Forget everything except the list of already seen Shorts.
     */
    public void reset() {
        RxHelper.disposeActions(mAction);
        mAction = null;
        mQueue.clear();
        mCallbacks.clear();
        mLastGroup = null;
        mLoadFailed = false;
    }

    private void loadMore() {
        if (mContentService == null || RxHelper.isAnyActionRunning(mAction)) {
            return;
        }

        Log.d(TAG, "Loading more Shorts. Continuation: " + (mLastGroup != null));

        Observable<MediaGroup> observable = mLastGroup == null ?
                mContentService.getShortsObserve() : mContentService.continueGroupObserve(mLastGroup);

        mAction = observable.subscribe(
                this::onGroup,
                error -> {
                    Log.e(TAG, "Shorts feed error: %s", error.getMessage());
                    mLastGroup = null; // start from scratch next time
                    onLoadFinished();
                },
                this::onLoadFinished
        );
    }

    private void onGroup(MediaGroup group) {
        if (group == null) {
            mLastGroup = null;
            return;
        }

        mLastGroup = group;

        List<MediaItem> items = group.getMediaItems();

        if (items == null) {
            mLastGroup = null;
            return;
        }

        BlockedChannelData blocked = BlockedChannelData.instance(GlobalPreferences.context());
        int added = 0;

        for (MediaItem item : items) {
            if (item == null || item.getVideoId() == null) {
                continue;
            }

            Video video = Video.from(item);

            if (video == null || video.videoId == null || mSeen.contains(video.videoId) || queueContains(video.videoId)) {
                continue;
            }

            if (blocked != null && !blocked.isEmpty() && blocked.containsChannel(video.channelId, video.getAuthor())) {
                continue;
            }

            video.isShorts = true;
            mQueue.add(video);
            added++;
        }

        if (added == 0) {
            // Nothing new (end of the feed or only duplicates). Start from scratch next time.
            mLastGroup = null;
            if (mSeen.size() >= MAX_SEEN_SIZE / 2) {
                mSeen.clear(); // let the user see the old Shorts again instead of showing nothing
            }
        }

        if (!mQueue.isEmpty()) {
            mLoadFailed = false;
            flushCallbacks();
        }
    }

    private void onLoadFinished() {
        mLoadFailed = mQueue.isEmpty();
        flushCallbacks();
    }

    private boolean queueContains(String videoId) {
        for (Video video : mQueue) {
            if (videoId.equals(video.videoId)) {
                return true;
            }
        }

        return false;
    }

    private void flushCallbacks() {
        if (mCallbacks.isEmpty()) {
            return;
        }

        List<Runnable> copy = new ArrayList<>(mCallbacks);
        mCallbacks.clear();

        for (Runnable callback : copy) {
            try {
                callback.run();
            } catch (Exception e) {
                Log.e(TAG, "Shorts feed callback error: %s", e.getMessage());
            }
        }
    }
}

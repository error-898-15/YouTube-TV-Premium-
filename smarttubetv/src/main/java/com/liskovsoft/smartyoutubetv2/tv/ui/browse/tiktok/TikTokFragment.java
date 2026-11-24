package com.liskovsoft.smartyoutubetv2.tv.ui.browse.tiktok;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Embedded TikTok TV experience (https://www.tiktok.com/) for watching uploaded TikTok videos,
 * browsing creator profiles, and opening comments right from the Browse sidebar.
 */
public class TikTokFragment extends Fragment {
    public static final String TIKTOK_HOME_URL = "https://www.tiktok.com/";
    private static final String TIKTOK_FOR_YOU_URL = "https://www.tiktok.com/foryou";
    private static final String TIKTOK_EXPLORE_URL = "https://www.tiktok.com/explore";
    private static final String DESKTOP_CHROME_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    public interface OnExitToSidebarListener {
        void onExitToSidebar();
    }

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private FrameLayout mFullscreenContainer;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    private View mCursorView;
    private boolean mCursorMode = false;
    private float mCursorX = -1f;
    private float mCursorY = -1f;

    private TextView mBtnForYou;
    private TextView mBtnExplore;
    private TextView mBtnPrev;
    private TextView mBtnNext;
    private TextView mBtnPlayPause;
    private TextView mBtnComments;
    private TextView mBtnProfile;
    private TextView mBtnCursor;
    private TextView mBtnBack;
    private TextView mBtnReload;

    private boolean mIsActive = true;
    private boolean mInitialLoadDone = false;
    private OnExitToSidebarListener mExitToSidebarListener;

    public void setOnExitToSidebarListener(OnExitToSidebarListener listener) {
        mExitToSidebarListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tiktok, container, false);
        initViews(root);
        configureWebView();
        if (!mInitialLoadDone && mWebView != null) {
            mInitialLoadDone = true;
            mWebView.loadUrl(TIKTOK_HOME_URL);
        }
        return root;
    }

    private void initViews(View root) {
        mWebView = root.findViewById(R.id.tiktok_webview);
        mProgressBar = root.findViewById(R.id.tiktok_progress);
        mFullscreenContainer = root.findViewById(R.id.tiktok_fullscreen_container);
        mCursorView = root.findViewById(R.id.tiktok_cursor_view);

        mBtnForYou = root.findViewById(R.id.btn_tiktok_foryou);
        mBtnExplore = root.findViewById(R.id.btn_tiktok_explore);
        mBtnPrev = root.findViewById(R.id.btn_tiktok_prev);
        mBtnNext = root.findViewById(R.id.btn_tiktok_next);
        mBtnPlayPause = root.findViewById(R.id.btn_tiktok_play_pause);
        mBtnComments = root.findViewById(R.id.btn_tiktok_comments);
        mBtnProfile = root.findViewById(R.id.btn_tiktok_profile);
        mBtnCursor = root.findViewById(R.id.btn_tiktok_cursor);
        mBtnBack = root.findViewById(R.id.btn_tiktok_back);
        mBtnReload = root.findViewById(R.id.btn_tiktok_reload);

        setupCursorDrawable();
        setupToolbarButtons();
    }

    private void setupCursorDrawable() {
        if (mCursorView == null) return;
        GradientDrawable pointer = new GradientDrawable();
        pointer.setShape(GradientDrawable.OVAL);
        pointer.setColor(Color.parseColor("#E6FFFFFF"));
        pointer.setStroke(4, Color.parseColor("#FE2C55"));
        mCursorView.setBackground(pointer);
        mCursorView.setElevation(12f);
    }

    private void setupToolbarButtons() {
        TextView[] buttons = {
                mBtnForYou, mBtnExplore, mBtnPrev, mBtnNext,
                mBtnPlayPause, mBtnComments, mBtnProfile, mBtnCursor, mBtnBack, mBtnReload
        };
        for (int i = 0; i < buttons.length; i++) {
            TextView btn = buttons[i];
            if (btn == null) continue;
            final boolean isFirst = (i == 0);
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(hasFocus ? Color.parseColor("#0F0F0F") : Color.parseColor("#F1F1F1"));
                }
            });
            btn.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (isFirst && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (mExitToSidebarListener != null) {
                        mExitToSidebarListener.onExitToSidebar();
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && mWebView != null) {
                    mWebView.requestFocus();
                    return true;
                }
                return false;
            });
        }

        if (mBtnForYou != null) {
            mBtnForYou.setOnClickListener(v -> loadUrl(TIKTOK_FOR_YOU_URL));
        }
        if (mBtnExplore != null) {
            mBtnExplore.setOnClickListener(v -> loadUrl(TIKTOK_EXPLORE_URL));
        }
        if (mBtnPrev != null) {
            mBtnPrev.setOnClickListener(v -> navigateVideo(-1));
        }
        if (mBtnNext != null) {
            mBtnNext.setOnClickListener(v -> navigateVideo(1));
        }
        if (mBtnPlayPause != null) {
            mBtnPlayPause.setOnClickListener(v -> togglePlayPause());
        }
        if (mBtnComments != null) {
            mBtnComments.setOnClickListener(v -> openActiveComments());
        }
        if (mBtnProfile != null) {
            mBtnProfile.setOnClickListener(v -> openActiveProfile());
        }
        if (mBtnCursor != null) {
            mBtnCursor.setOnClickListener(v -> {
                setCursorMode(!mCursorMode);
                if (mCursorMode && mWebView != null) {
                    mWebView.requestFocus();
                }
            });
        }
        if (mBtnBack != null) {
            mBtnBack.setOnClickListener(v -> {
                if (mWebView != null && mWebView.canGoBack()) {
                    mWebView.goBack();
                } else if (mExitToSidebarListener != null) {
                    mExitToSidebarListener.onExitToSidebar();
                }
            });
        }
        if (mBtnReload != null) {
            mBtnReload.setOnClickListener(v -> {
                if (mWebView != null) {
                    mWebView.reload();
                }
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        if (mWebView == null) return;

        mWebView.setBackgroundColor(Color.parseColor("#0F0F0F"));

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(mWebView, true);
        }

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUserAgentString(DESKTOP_CHROME_UA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri == null) return false;
                String scheme = uri.getScheme();
                if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                    // Block intent:// or snssdk:// deep links that fail inside WebView
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                return !url.startsWith("http://") && !url.startsWith("https://");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectTvFriendlyTweaks();
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (mProgressBar == null) return;
                if (newProgress >= 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mFullscreenContainer == null) return;
                if (mCustomView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                mCustomView = view;
                mCustomViewCallback = callback;
                mFullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                mFullscreenContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });

        mWebView.setOnKeyListener((v, keyCode, event) -> handleWebViewKey(keyCode, event));
    }

    private boolean handleWebViewKey(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        if (mCursorMode) {
            return handleCursorKey(keyCode);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mExitToSidebarListener != null) {
                    mExitToSidebarListener.onExitToSidebar();
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mBtnForYou != null) {
                    mBtnForYou.requestFocus();
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                navigateVideo(1);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_UP:
                navigateVideo(-1);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                navigateVideo(1);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                togglePlayPause();
                return true;
            default:
                return false;
        }
    }

    private boolean handleCursorKey(int keyCode) {
        if (mWebView == null || mCursorView == null) return false;

        int w = mWebView.getWidth();
        int h = mWebView.getHeight();
        if (w <= 0 || h <= 0) return false;

        if (mCursorX < 0 || mCursorY < 0) {
            mCursorX = w / 2f;
            mCursorY = h / 2f;
        }

        final float step = 36f;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (mCursorX <= 8f && mExitToSidebarListener != null) {
                    mExitToSidebarListener.onExitToSidebar();
                    return true;
                }
                mCursorX = Math.max(0f, mCursorX - step);
                updateCursorPosition();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mCursorX = Math.min(w - 24f, mCursorX + step);
                updateCursorPosition();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mCursorY <= 8f && mBtnCursor != null) {
                    mBtnCursor.requestFocus();
                    return true;
                }
                mCursorY = Math.max(0f, mCursorY - step);
                if (mCursorY < h * 0.12f) {
                    mWebView.scrollBy(0, -180);
                }
                updateCursorPosition();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mCursorY = Math.min(h - 24f, mCursorY + step);
                if (mCursorY > h * 0.88f) {
                    mWebView.scrollBy(0, 180);
                }
                updateCursorPosition();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                dispatchWebViewTap(mCursorX + 11f, mCursorY + 11f);
                return true;
            default:
                return false;
        }
    }

    private void setCursorMode(boolean enabled) {
        mCursorMode = enabled;
        if (mBtnCursor != null) {
            mBtnCursor.setText(enabled ? "Cursor: ON" : "Cursor: OFF");
            mBtnCursor.setSelected(enabled);
        }
        if (mCursorView != null) {
            if (enabled) {
                if (mWebView != null && (mCursorX < 0 || mCursorY < 0)) {
                    mCursorX = Math.max(40f, mWebView.getWidth() / 2f);
                    mCursorY = Math.max(40f, mWebView.getHeight() / 2f);
                }
                updateCursorPosition();
                mCursorView.setVisibility(View.VISIBLE);
            } else {
                mCursorView.setVisibility(View.GONE);
            }
        }
    }

    private void updateCursorPosition() {
        if (mCursorView != null) {
            mCursorView.setTranslationX(mCursorX);
            mCursorView.setTranslationY(mCursorY);
        }
    }

    private void dispatchWebViewTap(float x, float y) {
        if (mWebView == null) return;
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(downTime, downTime + 40, MotionEvent.ACTION_UP, x, y, 0);
        mWebView.dispatchTouchEvent(down);
        mWebView.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
    }

    public void loadUrl(String url) {
        if (mWebView != null && url != null) {
            mWebView.loadUrl(url);
        }
    }

    public void focusDefaultControl() {
        if (mWebView != null) {
            mWebView.requestFocus();
        } else if (mBtnForYou != null) {
            mBtnForYou.requestFocus();
        }
    }

    public void setActive(boolean active) {
        mIsActive = active;
        if (mWebView == null) return;
        if (active) {
            mWebView.onResume();
            mWebView.resumeTimers();
        } else {
            pauseWebMedia();
            mWebView.onPause();
        }
    }

    private void hideCustomView() {
        if (mFullscreenContainer == null || mCustomView == null) return;
        mFullscreenContainer.removeView(mCustomView);
        mFullscreenContainer.setVisibility(View.GONE);
        mCustomView = null;
        if (mCustomViewCallback != null) {
            mCustomViewCallback.onCustomViewHidden();
            mCustomViewCallback = null;
        }
    }

    public boolean handleBackPress() {
        if (mCustomView != null && mFullscreenContainer != null) {
            hideCustomView();
            return true;
        }
        if (mCursorMode) {
            setCursorMode(false);
            return true;
        }
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }

    private void navigateVideo(int direction) {
        if (mWebView == null) return;
        String js = "(function(){"
                + "var videos = Array.from(document.querySelectorAll('video'));"
                + "var vh = window.innerHeight || 720;"
                + "if(videos.length){"
                + "  var curIdx = 0, bestDist = 1e9;"
                + "  videos.forEach(function(v, i){"
                + "    var r = v.getBoundingClientRect();"
                + "    var d = Math.abs((r.top + r.height/2) - vh/2);"
                + "    if(d < bestDist){ bestDist = d; curIdx = i; }"
                + "  });"
                + "  var target = videos[Math.max(0, Math.min(videos.length - 1, curIdx + (" + direction + ")))];"
                + "  if(target && target !== videos[curIdx]){"
                + "    target.scrollIntoView({behavior:'smooth', block:'center'});"
                + "    setTimeout(function(){ try{ target.play(); }catch(e){} }, 250);"
                + "    return;"
                + "  }"
                + "}"
                + "window.scrollBy({top: (" + direction + ") * Math.round(vh * 0.85), behavior:'smooth'});"
                + "})();";
        mWebView.evaluateJavascript(js, null);
    }

    private void togglePlayPause() {
        if (mWebView == null) return;
        String js = "(function(){"
                + "var videos = Array.from(document.querySelectorAll('video'));"
                + "var vh = window.innerHeight || 720;"
                + "var active = null, bestDist = 1e9;"
                + "videos.forEach(function(v){"
                + "  var r = v.getBoundingClientRect();"
                + "  if(r.height > 80){"
                + "    var d = Math.abs((r.top + r.height/2) - vh/2);"
                + "    if(d < bestDist){ bestDist = d; active = v; }"
                + "  }"
                + "});"
                + "if(active){"
                + "  if(active.paused){ active.muted = false; active.play(); } else { active.pause(); }"
                + "}"
                + "})();";
        mWebView.evaluateJavascript(js, null);
    }

    private void openActiveComments() {
        if (mWebView == null) return;
        String js = "(function(){"
                + "var vh = window.innerHeight || 720;"
                + "var btns = Array.from(document.querySelectorAll('[data-e2e=\"comment-icon\"], [data-e2e=\"browse-comment-icon\"], button[aria-label*=\"comment\" i], button[aria-label*=\"Comment\" i]'));"
                + "var best = null, bestDist = 1e9;"
                + "btns.forEach(function(b){"
                + "  var r = b.getBoundingClientRect();"
                + "  if(r.width > 0 && r.height > 0){"
                + "    var d = Math.abs((r.top + r.height/2) - vh/2);"
                + "    if(d < bestDist){ bestDist = d; best = b; }"
                + "  }"
                + "});"
                + "if(best){ best.click(); return; }"
                + "var el = document.querySelector('[data-e2e=\"comment-level-1\"], [class*=\"DivCommentListContainer\"]');"
                + "if(el){ el.scrollIntoView({behavior:'smooth', block:'start'}); }"
                + "})();";
        mWebView.evaluateJavascript(js, null);
    }

    private void openActiveProfile() {
        if (mWebView == null) return;
        String js = "(function(){"
                + "var vh = window.innerHeight || 720;"
                + "var links = Array.from(document.querySelectorAll('[data-e2e=\"video-author-avatar\"], [data-e2e=\"video-author-uniqueid\"], a[href^=\"/@\"]'));"
                + "var best = null, bestDist = 1e9;"
                + "links.forEach(function(a){"
                + "  var r = a.getBoundingClientRect();"
                + "  if(r.width > 0 && r.height > 0 && r.top >= -100 && r.top <= vh + 100){"
                + "    var d = Math.abs((r.top + r.height/2) - vh/2);"
                + "    if(d < bestDist){ bestDist = d; best = a; }"
                + "  }"
                + "});"
                + "if(best){ best.click(); }"
                + "})();";
        mWebView.evaluateJavascript(js, null);
    }

    private void pauseWebMedia() {
        if (mWebView == null) return;
        mWebView.evaluateJavascript(
                "(function(){document.querySelectorAll('video,audio').forEach(function(m){try{m.pause();}catch(e){}});})();",
                null
        );
    }

    private void injectTvFriendlyTweaks() {
        if (mWebView == null) return;
        String js = "(function(){"
                + "if(document.getElementById('st_tiktok_tv_css')) return;"
                + "var s = document.createElement('style');"
                + "s.id = 'st_tiktok_tv_css';"
                + "s.textContent = '[data-e2e=\"bottom-banner\"], [class*=\"DivBottomBanner\"], [class*=\"DivGetAppButton\"]{display:none !important;} body{background:#0f0f0f !important;}';"
                + "document.head && document.head.appendChild(s);"
                + "})();";
        mWebView.evaluateJavascript(js, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWebView != null && mIsActive) {
            mWebView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWebView != null) {
            pauseWebMedia();
            mWebView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        if (mWebView != null) {
            pauseWebMedia();
            mWebView.stopLoading();
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroyView();
    }
            }

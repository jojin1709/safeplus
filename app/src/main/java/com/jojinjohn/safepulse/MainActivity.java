package com.jojinjohn.safepulse;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final int VPN_START_REQUEST_CODE = 100;
    private static final int VPN_SETUP_REQUEST_CODE = 101;
    private static final int NOTIFICATION_REQUEST_CODE = 102;
    private static final int BG = 0xFFF5F7FB;
    private static final int PRIMARY = 0xFF4F46E5;
    private static final int PRIMARY_DARK = 0xFF3730A3;
    private static final int PRIMARY_SOFT = 0xFFEEF2FF;
    private static final int INK = 0xFF0F172A;
    private static final int MUTED = 0xFF64748B;
    private static final int CARD_BG = 0xFFFFFFFF;
    private static final int BORDER = 0xFFE2E8F0;
    private static final int SUCCESS = 0xFF22C55E;
    private static final int ERROR = 0xFFEF4444;
    private static final int SOFT_SUCCESS = 0xFFDCFCE7;
    private static final int SOFT_ERROR = 0xFFFEE2E2;
    private static final int SOFT_BLUE = 0xFFEFF6FF;
    private static final int DARK_BG = 0xFF0B1020;
    private static final int DARK_CARD = 0xFF111827;
    private static final int DARK_SURFACE = 0xFF172033;
    private static final int DARK_BORDER = 0xFF273449;
    private static final int DARK_INK = 0xFFF8FAFC;
    private static final int DARK_MUTED = 0xFF94A3B8;
    private static final int DARK_PRIMARY = 0xFFA5B4FC;
    private static final int DARK_PRIMARY_SOFT = 0xFF25224A;
    private static final int DARK_SOFT_SUCCESS = 0xFF052E16;
    private static final int DARK_SOFT_ERROR = 0xFF450A0A;
    private static final int DARK_SOFT_BLUE = 0xFF172554;
    private static final int SURFACE_LIGHT = 0xFFF8FAFC;
    private static final int TABLET_WIDTH_DP = 600;
    private static final int MAX_CONTENT_WIDTH_DP = 900;
    private static final int MAX_NAV_WIDTH_DP = 660;
    private static final int TAB_DASHBOARD = 0;
    private static final int TAB_LOGS = 1;
    private static final int TAB_PROTECTION = 2;
    private static final int TAB_DIAGNOSTICS = 3;
    private static final int TAB_ABOUT = 4;
    private static final int TAB_COUNT = 5;
    private static final String APP_VERSION_NAME = "1.5.0";
    private static final long APP_VERSION_CODE = 8L;
    private static final String RELEASES_API_URL = "https://api.github.com/repos/jojin1709/safeplus/releases/latest";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private boolean lastVpnRunning = false;
    private long lastBlockedCount = -1;
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            boolean vpnRunning = AdBlockVpnService.isRunning();
            StatsStore.Snapshot snap = StatsStore.snapshot(MainActivity.this);
            if (vpnRunning != lastVpnRunning || snap.blocked != lastBlockedCount) {
                renderState();
                lastVpnRunning = vpnRunning;
                lastBlockedCount = snap.blocked;
            }
            handler.postDelayed(this, 2000L);
        }
    };

    private Button toggleButton;
    private TextView statusPill;
    private TextView statusText;
    private TextView blockedCount;
    private TextView checkedCount;
    private TextView allowedCount;
    private TextView blockRateText;
    private TextView blocklistCountText;
    private TextView uptimeText;
    private TextView recentBlocksText;
    private TextView logsText;
    private EditText logFilterInput;
    private TextView blocklistMetaText;
    private TextView allowlistText;
    private TextView batteryStatusText;
    private TextView privateDnsStatusText;
    private TextView vpnSetupStatusText;
    private TextView batterySetupStatusText;
    private TextView notificationSetupStatusText;
    private TextView privateDnsSetupStatusText;
    private TextView diagnosticsText;
    private TextView testResultText;
    private Button vpnSetupButton;
    private Button batterySetupButton;
    private Button notificationSetupButton;
    private Button privateDnsSetupButton;
    private EditText allowDomainInput;
    private EditText blocklistUrlInput;
    private LinearLayout contentRoot;
    private LinearLayout recentBlocksList;
    private LinearLayout[] navItems;
    private ImageView[] navIcons;
    private TextView[] navLabels;
    private TextView updateStatusText;
    private Button updateNowButton;
    private ReleaseInfo latestRelease;
    private int currentTab = TAB_DASHBOARD;
    private boolean stoppedFromButton;
    private boolean showAdvancedSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSettings.migrateDefaults(this);
        styleSystemBars();
        buildDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderSettingsState();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private void buildDashboard() {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackgroundColor(color(BG));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color(BG));
        scrollView.setClipToPadding(true);
        scrollView.setPadding(0, 0, 0, bottomScrollPadding());

        FrameLayout scrollContent = new FrameLayout(this);
        contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = contentHorizontalPadding();
        contentRoot.setPadding(horizontalPadding, dp(isTvLayout() ? 28 : 18), horizontalPadding, dp(isTvLayout() ? 34 : 22));
        contentRoot.setFocusableInTouchMode(true);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(contentWidth(), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        scrollContent.addView(contentRoot, contentParams);
        scrollView.addView(scrollContent, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scrollView, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(navWidth(), dp(isTvLayout() ? 94 : isTabletLayout() ? 82 : 76), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        navParams.bottomMargin = dp(isTvLayout() ? 22 : 14);
        shell.addView(buildBottomNav(), navParams);

        setContentView(shell);
        showTab(currentTab);
        if (isTvLayout() && toggleButton != null) {
            toggleButton.requestFocus();
        } else {
            contentRoot.requestFocus();
        }
        renderState();
        renderSettingsState();
    }

    private void showTab(int tab) {
        currentTab = tab;
        resetBoundViews();
        contentRoot.removeAllViews();

        if (tab == TAB_DASHBOARD) {
            buildHeader(contentRoot, "Home");
            if (needsSetupAttention()) {
                buildSetupChecklist(contentRoot);
            }
            buildHero(contentRoot);
            buildStatsGrid(contentRoot);
            buildLiveDetails(contentRoot);
            buildRecentBlocks(contentRoot);
        } else if (tab == TAB_LOGS) {
            buildHeader(contentRoot, "Activity");
            buildLogs(contentRoot);
        } else if (tab == TAB_PROTECTION) {
            buildHeader(contentRoot, "Settings");
            buildSettings(contentRoot);
        } else if (tab == TAB_DIAGNOSTICS) {
            buildHeader(contentRoot, "Quick check");
            buildDiagnostics(contentRoot);
        } else {
            buildHeader(contentRoot, "Help");
            buildAbout(contentRoot);
        }

        refreshBottomNav();
        renderState();
        renderSettingsState();
    }

    private void resetBoundViews() {
        statusPill = null;
        statusText = null;
        blockedCount = null;
        checkedCount = null;
        allowedCount = null;
        blockRateText = null;
        blocklistCountText = null;
        uptimeText = null;
        recentBlocksText = null;
        logsText = null;
        logFilterInput = null;
        blocklistMetaText = null;
        allowlistText = null;
        batteryStatusText = null;
        privateDnsStatusText = null;
        vpnSetupStatusText = null;
        batterySetupStatusText = null;
        notificationSetupStatusText = null;
        privateDnsSetupStatusText = null;
        diagnosticsText = null;
        testResultText = null;
        vpnSetupButton = null;
        batterySetupButton = null;
        notificationSetupButton = null;
        privateDnsSetupButton = null;
        allowDomainInput = null;
        blocklistUrlInput = null;
        recentBlocksList = null;
        toggleButton = null;
        updateStatusText = null;
        updateNowButton = null;
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(isTvLayout() ? 12 : 8), dp(isTvLayout() ? 10 : 7), dp(isTvLayout() ? 12 : 8), dp(isTvLayout() ? 10 : 7));
        nav.setBackground(roundStroke(CARD_BG, BORDER, 28));
        nav.setElevation(dp(isTvLayout() ? 12 : 8));

        navItems = new LinearLayout[]{
                navItem("Home", TAB_DASHBOARD, R.drawable.ic_nav_home),
                navItem("Activity", TAB_LOGS, R.drawable.ic_nav_activity),
                navItem("Settings", TAB_PROTECTION, R.drawable.ic_nav_shield),
                navItem("Check", TAB_DIAGNOSTICS, R.drawable.ic_stat_check),
                navItem("Help", TAB_ABOUT, R.drawable.ic_nav_info)
        };
        for (LinearLayout item : navItems) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.leftMargin = dp(3);
            params.rightMargin = dp(3);
            nav.addView(item, params);
        }
        return nav;
    }

    private LinearLayout navItem(String label, int tab, int iconRes) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), dp(5), dp(4), dp(4));
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription(label);
        item.setOnClickListener(v -> showTab(tab));
        applyTvFocus(item);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        item.addView(icon, new LinearLayout.LayoutParams(dp(isTvLayout() ? 24 : 20), dp(isTvLayout() ? 24 : 20)));

        TextView title = text(label, 10, MUTED, false);
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(false);
        addTopMargin(item, title, 3, new LinearLayout.LayoutParams(-1, -2));

        if (navIcons == null) {
            navIcons = new ImageView[TAB_COUNT];
            navLabels = new TextView[TAB_COUNT];
        }
        navIcons[tab] = icon;
        navLabels[tab] = title;
        return item;
    }

    private void refreshBottomNav() {
        if (navItems == null) return;
        for (int i = 0; i < navItems.length; i++) {
            boolean selected = i == currentTab;
            navItems[i].setBackground(selected ? roundRect(PRIMARY_SOFT, 22) : roundRect(Color.TRANSPARENT, 22));
            navItems[i].setElevation(selected ? dp(1) : 0);
            if (navIcons != null && navIcons[i] != null) {
                navIcons[i].setColorFilter(color(selected ? PRIMARY : MUTED));
            }
            if (navLabels != null && navLabels[i] != null) {
                navLabels[i].setTextColor(color(selected ? PRIMARY : MUTED));
                navLabels[i].setTypeface(typeface(selected));
            }
        }
    }

    private void buildHeader(LinearLayout root, String subtitle) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(2), 0, dp(2));

        FrameLayout logoShell = new FrameLayout(this);
        logoShell.setBackground(roundStroke(CARD_BG, BORDER, 16));
        logoShell.setPadding(dp(4), dp(4), dp(4), dp(4));
        logoShell.setElevation(dp(2));
        logoShell.setContentDescription("SafePulse logo");

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.safepulse_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int logoInner = isTvLayout() ? 56 : 46;
        int logoOuter = isTvLayout() ? 66 : 54;
        logoShell.addView(logo, new FrameLayout.LayoutParams(dp(logoInner), dp(logoInner), Gravity.CENTER));
        header.addView(logoShell, new LinearLayout.LayoutParams(dp(logoOuter), dp(logoOuter)));

        LinearLayout titleColumn = new LinearLayout(this);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        titleColumn.setPadding(dp(12), 0, dp(8), 0);
        TextView title = text("SafePulse", 23, INK, true);
        title.setIncludeFontPadding(false);
        titleColumn.addView(title);
        TextView subTitleView = text(subtitle, 12, MUTED, false);
        subTitleView.setIncludeFontPadding(false);
        addTopMargin(titleColumn, subTitleView, 5);
        header.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout themeToggle = iconButton(
                isDarkMode() ? R.drawable.ic_theme_sun : R.drawable.ic_theme_moon,
                PRIMARY,
                PRIMARY_SOFT
        );
        themeToggle.setContentDescription(isDarkMode() ? "Switch to light mode" : "Switch to dark mode");
        themeToggle.setOnClickListener(v -> toggleTheme());
        actions.addView(themeToggle, new LinearLayout.LayoutParams(dp(isTvLayout() ? 50 : 42), dp(isTvLayout() ? 50 : 42)));

        statusPill = text("ACTIVE", 11, SUCCESS, true);
        statusPill.setGravity(Gravity.CENTER);
        statusPill.setPadding(dp(10), dp(6), dp(10), dp(6));
        statusPill.setBackground(roundRect(SOFT_SUCCESS, 999));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.leftMargin = dp(8);
        actions.addView(statusPill, statusParams);

        header.addView(actions, new LinearLayout.LayoutParams(-2, -2));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildSetupChecklist(LinearLayout root) {
        LinearLayout setup = card(20, CARD_BG);
        setup.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, setup, 16);
        setup.addView(sectionTitle("Finish setup", "One-time permissions"));
        TextView intro = text("Allow these so SafePulse can protect your device and keep running reliably.", 13, MUTED, false);
        intro.setLineSpacing(dp(2), 1.0f);
        addTopMargin(setup, intro, 10);

        vpnSetupStatusText = statusBadge("Required", ERROR, SOFT_ERROR);
        vpnSetupButton = smallButton("Allow");
        vpnSetupButton.setOnClickListener(v -> requestVpnSetupPermission());
        addTopMargin(setup, setupRow(
                "VPN permission",
                "Needed so SafePulse can block unwanted domains.",
                vpnSetupStatusText,
                vpnSetupButton
        ), 14);

        batterySetupStatusText = statusBadge("Recommended", PRIMARY, PRIMARY_SOFT);
        batterySetupButton = smallButton("Allow");
        batterySetupButton.setOnClickListener(v -> openBatterySettings());
        addTopMargin(setup, setupRow(
                "Battery optimization",
                "Helps SafePulse stay on in the background.",
                batterySetupStatusText,
                batterySetupButton
        ), 8);

        notificationSetupStatusText = statusBadge("Recommended", PRIMARY, PRIMARY_SOFT);
        notificationSetupButton = smallButton("Allow");
        notificationSetupButton.setOnClickListener(v -> requestNotificationPermission());
        addTopMargin(setup, setupRow(
                "Notifications",
                "Shows protection status and quick stop.",
                notificationSetupStatusText,
                notificationSetupButton
        ), 8);

        privateDnsSetupStatusText = statusBadge("Check", PRIMARY, PRIMARY_SOFT);
        privateDnsSetupButton = smallButton("Open");
        privateDnsSetupButton.setOnClickListener(v -> openPrivateDnsSettings());
        addTopMargin(setup, setupRow(
                "Private DNS",
                "Turn this off if apps bypass blocking.",
                privateDnsSetupStatusText,
                privateDnsSetupButton
        ), 8);
    }

    private boolean needsSetupAttention() {
        return VpnService.prepare(this) != null
                || !isBatteryOptimizationIgnored()
                || !hasNotificationPermission()
                || !isPrivateDnsOk();
    }

    private LinearLayout setupRow(String title, String detail, TextView status, Button action) {
        LinearLayout item = card(16, 0xFFF8FAFC);
        item.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView titleView = text(title, 14, INK, true);
        titleView.setIncludeFontPadding(false);
        item.addView(titleView);

        TextView detailView = text(detail, 12, MUTED, false);
        detailView.setLineSpacing(dp(2), 1.0f);
        addTopMargin(item, detailView, 5);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.addView(status, new LinearLayout.LayoutParams(-2, -2));
        controls.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(isTvLayout() ? 128 : 96), dp(isTvLayout() ? 48 : 40));
        buttonParams.leftMargin = dp(10);
        controls.addView(action, buttonParams);
        addTopMargin(item, controls, 10);
        return item;
    }

    private TextView statusBadge(String value, int color, int bgColor) {
        TextView badge = text(value, 11, color, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(6), dp(10), dp(6));
        badge.setBackground(roundRect(bgColor, 999));
        return badge;
    }

    private void setSetupStatus(TextView status, String value, int color, int bgColor) {
        if (status == null) return;
        status.setText(value);
        status.setTextColor(color(color));
        status.setBackground(roundRect(bgColor, 999));
    }

    private void buildHero(LinearLayout root) {
        LinearLayout hero = card(24, PRIMARY);
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        hero.setBackground(gradient(PRIMARY, 0xFF7C3AED, 24));
        hero.setElevation(dp(5));
        addTopMargin(root, hero, 20);

        LinearLayout heroTop = new LinearLayout(this);
        heroTop.setOrientation(LinearLayout.HORIZONTAL);
        heroTop.setGravity(Gravity.CENTER_VERTICAL);
        heroTop.addView(iconBadge(R.drawable.ic_nav_shield, 0x33FFFFFF, Color.WHITE, 40, 20));
        TextView label = text("PROTECTION", 11, 0xFFE0E7FF, true);
        label.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(10);
        heroTop.addView(label, labelParams);
        hero.addView(heroTop);

        statusText = text("Ready to start", 29, Color.WHITE, true);
        statusText.setIncludeFontPadding(false);
        addTopMargin(hero, statusText, 18);

        TextView heroCopy = text("Tap Start to block ads, trackers, popups, and risky redirects across supported apps and browsers.", 13, 0xFFEDE9FE, false);
        heroCopy.setLineSpacing(dp(2), 1.0f);
        addTopMargin(hero, heroCopy, 8);

        LinearLayout blockedRow = new LinearLayout(this);
        blockedRow.setOrientation(LinearLayout.HORIZONTAL);
        blockedRow.setGravity(Gravity.CENTER_VERTICAL);
        blockedRow.setPadding(dp(14), dp(10), dp(14), dp(10));
        blockedRow.setBackground(roundRect(0x22FFFFFF, 18));
        blockedCount = text("0", 34, Color.WHITE, true);
        blockedCount.setIncludeFontPadding(false);
        blockedRow.addView(blockedCount);
        TextView blockedLabel = text("blocked requests", 13, 0xFFEDE9FE, false);
        LinearLayout.LayoutParams blockedLabelParams = new LinearLayout.LayoutParams(-2, -2);
        blockedLabelParams.leftMargin = dp(8);
        blockedRow.addView(blockedLabel, blockedLabelParams);
        addTopMargin(hero, blockedRow, 18);

        toggleButton = new Button(this);
        toggleButton.setAllCaps(false);
        toggleButton.setTextSize(15);
        toggleButton.setTextColor(color(PRIMARY));
        toggleButton.setTypeface(typeface(true));
        toggleButton.setPadding(dp(12), 0, dp(12), 0);
        toggleButton.setOnClickListener(this::toggleVpn);
        applyTvFocus(toggleButton);
        addTopMargin(hero, toggleButton, 16, new LinearLayout.LayoutParams(-1, dp(isTvLayout() ? 64 : 54)));

        LinearLayout pauseRow = new LinearLayout(this);
        pauseRow.setOrientation(LinearLayout.HORIZONTAL);
        pauseRow.setGravity(Gravity.CENTER);
        String[] pauseLabels = {"5m", "15m", "30m", "1h"};
        long[] pauseMinutes = {5, 15, 30, 60};
        for (int i = 0; i < pauseLabels.length; i++) {
            Button pauseBtn = new Button(this);
            pauseBtn.setText(pauseLabels[i]);
            pauseBtn.setAllCaps(false);
            pauseBtn.setTextSize(12);
            pauseBtn.setTextColor(color(PRIMARY_DARK));
            pauseBtn.setTypeface(typeface(false));
            pauseBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
            pauseBtn.setBackground(roundRect(0x22FFFFFF, 12));
            long mins = pauseMinutes[i];
            pauseBtn.setOnClickListener(v -> {
                AppSettings.pauseFor(this, mins);
                renderState();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(36), 1);
            params.rightMargin = dp(6);
            pauseRow.addView(pauseBtn, params);
        }
        addTopMargin(hero, pauseRow, 10);
    }

    private void buildStatsGrid(LinearLayout root) {
        LinearLayout statsGrid = new LinearLayout(this);
        statsGrid.setOrientation(LinearLayout.VERTICAL);
        addTopMargin(root, statsGrid, 16);

        if (isTabletLayout()) {
            LinearLayout row = row();
            checkedCount = addStat(row, "Checked", "0", R.drawable.ic_stat_search, SOFT_BLUE, PRIMARY);
            allowedCount = addStat(row, "Allowed", "0", R.drawable.ic_stat_check, SOFT_SUCCESS, SUCCESS);
            blockRateText = addStat(row, "Block rate", "0%", R.drawable.ic_stat_trend, PRIMARY_SOFT, PRIMARY);
            blocklistCountText = addStat(row, "Rules", "0", R.drawable.ic_stat_rules, 0xFFFFF7ED, 0xFFF97316);
            statsGrid.addView(row);
            return;
        }

        LinearLayout rowOne = row();
        checkedCount = addStat(rowOne, "Checked", "0", R.drawable.ic_stat_search, SOFT_BLUE, PRIMARY);
        allowedCount = addStat(rowOne, "Allowed", "0", R.drawable.ic_stat_check, SOFT_SUCCESS, SUCCESS);
        statsGrid.addView(rowOne);

        LinearLayout rowTwo = row();
        blockRateText = addStat(rowTwo, "Block rate", "0%", R.drawable.ic_stat_trend, PRIMARY_SOFT, PRIMARY);
        blocklistCountText = addStat(rowTwo, "Rules", "0", R.drawable.ic_stat_rules, 0xFFFFF7ED, 0xFFF97316);
        addTopMargin(statsGrid, rowTwo, 12);
    }

    private void buildLiveDetails(LinearLayout root) {
        LinearLayout details = card(20, CARD_BG);
        details.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, details, 16);

        details.addView(sectionTitle("Live status", "What SafePulse is doing"));
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.addView(dot(SUCCESS));
        uptimeText = text("Uptime: not running", 14, MUTED, false);
        LinearLayout.LayoutParams uptimeParams = new LinearLayout.LayoutParams(0, -2, 1);
        uptimeParams.leftMargin = dp(10);
        statusRow.addView(uptimeText, uptimeParams);
        addTopMargin(details, statusRow, 14);
        TextView copy = text("Counts update only from real requests. Some YouTube and streaming ads may still appear when ads use the same servers as videos.", 13, MUTED, false);
        copy.setLineSpacing(dp(2), 1.0f);
        addTopMargin(details, copy, 8);
    }

    private void buildRecentBlocks(LinearLayout root) {
        LinearLayout recent = card(20, CARD_BG);
        recent.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, recent, 16);
        recent.addView(sectionTitle("Recent blocks", "Latest blocked items"));
        recentBlocksList = new LinearLayout(this);
        recentBlocksList.setOrientation(LinearLayout.VERTICAL);
        addTopMargin(recent, recentBlocksList, 12);
    }

    private void buildLogs(LinearLayout root) {
        LinearLayout logs = card(20, CARD_BG);
        logs.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, logs, 16);
        logs.addView(sectionTitle("Activity", "Search blocked items"));

        logFilterInput = new EditText(this);
        logFilterInput.setSingleLine(true);
        logFilterInput.setHint("Filter blocked domains");
        logFilterInput.setTextColor(color(INK));
        logFilterInput.setTextSize(14);
        logFilterInput.setHintTextColor(color(MUTED));
        logFilterInput.setBackground(roundStroke(0xFFF8FAFC, BORDER, 16));
        logFilterInput.setPadding(dp(14), 0, dp(14), 0);
        applyTvFocus(logFilterInput);
        logFilterInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        addTopMargin(logs, logFilterInput, 14, new LinearLayout.LayoutParams(-1, dp(50)));

        logsText = text("No activity yet.", 13, MUTED, false);
        logsText.setLineSpacing(dp(5), 1.0f);
        logsText.setBackground(roundRect(0xFFF8FAFC, 16));
        logsText.setPadding(dp(14), dp(12), dp(14), dp(12));
        addTopMargin(logs, logsText, 14);

        Button exportLogs = smallButton("Export logs");
        exportLogs.setOnClickListener(v -> {
            StatsStore.Snapshot snap = StatsStore.snapshot(this);
            StringBuilder sb = new StringBuilder();
            sb.append("SafePulse Activity Log\n");
            sb.append("Blocked: ").append(snap.blocked).append(" | Allowed: ").append(snap.allowed).append("\n\n");
            for (StatsStore.LogEvent event : snap.events) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.US);
                sb.append(sdf.format(new java.util.Date(event.timestamp)))
                  .append(" | ").append(event.action)
                  .append(" | ").append(event.host)
                  .append(" | ").append(event.category).append("\n");
            }
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                android.content.ClipData clip = android.content.ClipData.newPlainText("SafePulse Logs", sb.toString());
                clipboard.setPrimaryClip(clip);
            }
        });
        addTopMargin(logs, exportLogs, 10, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void buildSettings(LinearLayout root) {
        LinearLayout settings = card(20, CARD_BG);
        settings.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, settings, 16);
        settings.addView(sectionTitle("Settings", "Simple controls"));
        TextView guidance = text("Recommended protection is already turned on. Change these only if something stops working.", 13, MUTED, false);
        guidance.setLineSpacing(dp(2), 1.0f);
        addTopMargin(settings, guidance, 12);

        CheckBox aggressiveBlocking = checkBox("Block ads");
        aggressiveBlocking.setChecked(AppSettings.isAggressiveBlockingEnabled(this));
        aggressiveBlocking.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setAggressiveBlockingEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(settings, aggressiveBlocking, 12);

        CheckBox dohGuard = checkBox("Stop apps bypassing SafePulse");
        dohGuard.setChecked(AppSettings.isDohGuardEnabled(this));
        dohGuard.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setDohGuardEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(settings, dohGuard, 8);

        CheckBox antiTracking = checkBox("Block trackers");
        antiTracking.setChecked(AppSettings.isAntiTrackingEnabled(this));
        antiTracking.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setAntiTrackingEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(settings, antiTracking, 12);

        CheckBox neverConsent = checkBox("Block cookie popups");
        neverConsent.setChecked(AppSettings.isNeverConsentEnabled(this));
        neverConsent.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setNeverConsentEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(settings, neverConsent, 8);

        CheckBox redirectProtection = checkBox("Block risky redirects");
        redirectProtection.setChecked(AppSettings.isRedirectProtectionEnabled(this));
        redirectProtection.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setRedirectProtectionEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(settings, redirectProtection, 8);

        CheckBox adultBlock = checkBox("Block adult content (18+)");
        adultBlock.setChecked(AppSettings.isAdultBlockEnabled(this));
        adultBlock.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setAdultBlockEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(settings, adultBlock, 8);

        CheckBox autoUpdate = checkBox("Keep block rules updated");
        autoUpdate.setChecked(AppSettings.isAutoUpdateEnabled(this));
        autoUpdate.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setAutoUpdateEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(settings, autoUpdate, 8);

        CheckBox bootStart = checkBox("Start after reboot");
        bootStart.setChecked(AppSettings.isBootStartEnabled(this));
        bootStart.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setBootStartEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(settings, bootStart, 8);

        Button advancedToggle = smallButton(showAdvancedSettings ? "Hide advanced options" : "Show advanced options");
        advancedToggle.setOnClickListener(v -> {
            showAdvancedSettings = !showAdvancedSettings;
            showTab(TAB_PROTECTION);
        });
        addTopMargin(settings, advancedToggle, 16, new LinearLayout.LayoutParams(-1, dp(50)));

        LinearLayout advanced = card(18, 0xFFF8FAFC);
        advanced.setPadding(dp(14), dp(14), dp(14), dp(14));
        advanced.setVisibility(showAdvancedSettings ? View.VISIBLE : View.GONE);
        addTopMargin(settings, advanced, 12);
        advanced.addView(sectionTitle("Advanced options", "Only change these if you know why"));

        CheckBox strictYoutube = checkBox("Strict YouTube blocking");
        strictYoutube.setChecked(AppSettings.isStrictYoutubeEnabled(this));
        strictYoutube.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setStrictYoutubeEnabled(this, checked);
            renderSettingsState();
        });
        addTopMargin(advanced, strictYoutube, 12);

        TextView strictYoutubeNote = text("May block more YouTube ads, but can also make videos or thumbnails stop loading. Turn it off to return to normal mode.", 12, ERROR, false);
        strictYoutubeNote.setLineSpacing(dp(4), 1.0f);
        addTopMargin(advanced, strictYoutubeNote, 8);

        TextView dnsLabel = text("DNS provider", 14, INK, true);
        addTopMargin(advanced, dnsLabel, 16);
        Spinner dnsSpinner = new Spinner(this);
        ArrayAdapter<String> dnsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, AppSettings.dnsProviders()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(color(INK));
                view.setTextSize(14);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(color(INK));
                view.setTextSize(14);
                view.setBackgroundColor(fillColor(CARD_BG));
                return view;
            }
        };
        dnsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dnsSpinner.setAdapter(dnsAdapter);
        int selectedDns = AppSettings.dnsProviders().indexOf(AppSettings.getDnsProvider(this));
        dnsSpinner.setSelection(Math.max(0, selectedDns));
        dnsSpinner.setBackground(roundStroke(0xFFF8FAFC, BORDER, 16));
        dnsSpinner.setPadding(dp(12), 0, dp(12), 0);
        applyTvFocus(dnsSpinner);
        dnsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppSettings.setDnsProvider(MainActivity.this, AppSettings.dnsProviders().get(position));
                renderSettingsState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        addTopMargin(advanced, dnsSpinner, 8, new LinearLayout.LayoutParams(-1, dp(50)));

        EditText customDnsInput = new EditText(this);
        customDnsInput.setSingleLine(true);
        customDnsInput.setHint("Enter custom DNS IP (e.g., 1.1.1.1)");
        customDnsInput.setTextSize(14);
        customDnsInput.setTextColor(color(INK));
        customDnsInput.setHintTextColor(color(MUTED));
        customDnsInput.setBackground(roundStroke(0xFFF8FAFC, BORDER, 16));
        customDnsInput.setPadding(dp(14), 0, dp(14), 0);
        customDnsInput.setText(AppSettings.getCustomDns(this));
        customDnsInput.setVisibility(AppSettings.DNS_CUSTOM.equals(AppSettings.getDnsProvider(this)) ? View.VISIBLE : View.GONE);
        customDnsInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                AppSettings.setCustomDns(this, customDnsInput.getText().toString());
            }
        });
        customDnsInput.setTag("custom_dns_input");
        addTopMargin(advanced, customDnsInput, 8, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView presetLabel = text("Blocklist preset", 14, INK, true);
        addTopMargin(advanced, presetLabel, 16);
        Spinner presetSpinner = new Spinner(this);
        String[] presets = AppSettings.blocklistPresets();
        ArrayAdapter<String> presetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, presets) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(color(INK));
                view.setTextSize(14);
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(color(INK));
                view.setTextSize(14);
                view.setBackgroundColor(fillColor(CARD_BG));
                return view;
            }
        };
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(presetAdapter);
        presetSpinner.setBackground(roundStroke(0xFFF8FAFC, BORDER, 16));
        presetSpinner.setPadding(dp(12), 0, dp(12), 0);
        applyTvFocus(presetSpinner);
        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String urls = AppSettings.getPresetUrls(presets[position]);
                AppSettings.setBlocklistUrl(MainActivity.this, urls);
                if (blocklistUrlInput != null) blocklistUrlInput.setText(urls);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        addTopMargin(advanced, presetSpinner, 8, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView scheduleLabel = text("Schedule blocking", 14, INK, true);
        addTopMargin(advanced, scheduleLabel, 16);
        CheckBox scheduleToggle = checkBox("Enable schedule");
        scheduleToggle.setChecked(AppSettings.isScheduleEnabled(this));
        scheduleToggle.setOnCheckedChangeListener((b, checked) -> AppSettings.setScheduleEnabled(this, checked));
        addTopMargin(advanced, scheduleToggle, 8);

        LinearLayout scheduleRow = row();
        TextView startLabel = text("Pause at", 12, MUTED, false);
        scheduleRow.addView(startLabel);
        Button startPicker = smallButton(String.format(Locale.US, "%02d:%02d", AppSettings.getScheduleStartHour(this), AppSettings.getScheduleStartMin(this)));
        startPicker.setOnClickListener(v -> showTimePicker("Pause at", AppSettings.getScheduleStartHour(this), AppSettings.getScheduleStartMin(this), (h, m) -> {
            AppSettings.setScheduleStartHour(this, h);
            AppSettings.setScheduleStartMin(this, m);
            startPicker.setText(String.format(Locale.US, "%02d:%02d", h, m));
        }));
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        startParams.leftMargin = dp(10);
        scheduleRow.addView(startPicker, startParams);

        TextView endLabel = text("Resume at", 12, MUTED, false);
        addTopMargin(advanced, endLabel, 8);
        LinearLayout endRow = row();
        endRow.addView(endLabel);
        Button endPicker = smallButton(String.format(Locale.US, "%02d:%02d", AppSettings.getScheduleEndHour(this), AppSettings.getScheduleEndMin(this)));
        endPicker.setOnClickListener(v -> showTimePicker("Resume at", AppSettings.getScheduleEndHour(this), AppSettings.getScheduleEndMin(this), (h, m) -> {
            AppSettings.setScheduleEndHour(this, h);
            AppSettings.setScheduleEndMin(this, m);
            endPicker.setText(String.format(Locale.US, "%02d:%02d", h, m));
        }));
        LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        endParams.leftMargin = dp(10);
        endRow.addView(endPicker, endParams);
        addTopMargin(advanced, endRow, 8);

        TextView bypassTitle = text("App bypass", 14, INK, true);
        addTopMargin(advanced, bypassTitle, 16);
        TextView bypassNote = text("Checked apps will skip SafePulse after protection is restarted.", 12, MUTED, false);
        addTopMargin(advanced, bypassNote, 5);
        addBypassCheck(advanced, "Do not protect YouTube", "com.google.android.youtube");
        addBypassCheck(advanced, "Do not protect Chrome", "com.android.chrome");
        addBypassCheck(advanced, "Do not protect Instagram", "com.instagram.android");
        addBypassCheck(advanced, "Do not protect Facebook", "com.facebook.katana");

        TextView allAppsLabel = text("All installed apps", 14, INK, true);
        addTopMargin(advanced, allAppsLabel, 16);
        TextView allAppsNote = text("Toggle bypass for any app on your device.", 12, MUTED, false);
        addTopMargin(advanced, allAppsNote, 5);
        LinearLayout allAppsListInner = new LinearLayout(this);
        allAppsListInner.setOrientation(LinearLayout.VERTICAL);
        java.util.List<AppSettings.InstalledApp> apps = AppSettings.getInstalledApps(this);
        int shown = 0;
        for (AppSettings.InstalledApp app : apps) {
            if (shown >= 30) break;
            if (app.packageName.equals(getPackageName())) continue;
            CheckBox appCheck = checkBox(app.name + " (" + app.packageName + ")");
            appCheck.setChecked(app.bypassed);
            appCheck.setTextSize(12);
            appCheck.setOnCheckedChangeListener((b, checked) -> AppSettings.setPackageBypassed(this, app.packageName, checked));
            allAppsListInner.addView(appCheck);
            shown++;
        }
        ScrollView appsScroll = new ScrollView(this);
        appsScroll.addView(allAppsListInner);
        addTopMargin(advanced, appsScroll, 8);

        TextView allowTitle = text("Allowed websites", 14, INK, true);
        addTopMargin(advanced, allowTitle, 16);
        allowDomainInput = new EditText(this);
        allowDomainInput.setSingleLine(true);
        allowDomainInput.setHint("example.com");
        allowDomainInput.setTextSize(14);
        allowDomainInput.setTextColor(color(INK));
        allowDomainInput.setHintTextColor(color(MUTED));
        allowDomainInput.setBackground(roundStroke(0xFFF8FAFC, BORDER, 16));
        allowDomainInput.setPadding(dp(14), 0, dp(14), 0);
        applyTvFocus(allowDomainInput);
        addTopMargin(advanced, allowDomainInput, 8, new LinearLayout.LayoutParams(-1, dp(50)));

        LinearLayout allowButtons = row();
        Button addAllowed = smallButton("Allow domain");
        addAllowed.setOnClickListener(v -> {
            if (allowDomainInput != null && AppSettings.addAllowedDomain(this, allowDomainInput.getText().toString())) {
                allowDomainInput.setText("");
            }
            renderSettingsState();
        });
        allowButtons.addView(addAllowed, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button clearAllowed = smallButton("Clear allowlist");
        clearAllowed.setOnClickListener(v -> {
            AppSettings.clearAllowlist(this);
            renderSettingsState();
        });
        LinearLayout.LayoutParams clearAllowedParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        clearAllowedParams.leftMargin = dp(10);
        allowButtons.addView(clearAllowed, clearAllowedParams);
        addTopMargin(advanced, allowButtons, 10);

        allowlistText = text("", 12, MUTED, false);
        allowlistText.setLineSpacing(dp(4), 1.0f);
        allowlistText.setBackground(roundRect(0xFFF8FAFC, 16));
        allowlistText.setPadding(dp(14), dp(12), dp(14), dp(12));
        addTopMargin(advanced, allowlistText, 10);

        TextView blocklistTitle = text("Block rule sources", 14, INK, true);
        addTopMargin(advanced, blocklistTitle, 16);
        blocklistUrlInput = new EditText(this);
        blocklistUrlInput.setSingleLine(false);
        blocklistUrlInput.setMinLines(3);
        blocklistUrlInput.setMaxLines(5);
        blocklistUrlInput.setGravity(Gravity.TOP | Gravity.START);
        blocklistUrlInput.setText(AppSettings.getBlocklistUrl(this));
        blocklistUrlInput.setTextSize(12);
        blocklistUrlInput.setTextColor(color(INK));
        blocklistUrlInput.setHintTextColor(color(MUTED));
        blocklistUrlInput.setBackground(roundStroke(0xFFF8FAFC, BORDER, 16));
        blocklistUrlInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        applyTvFocus(blocklistUrlInput);
        addTopMargin(advanced, blocklistUrlInput, 8, new LinearLayout.LayoutParams(-1, dp(116)));

        Button updateBlocklists = smallButton("Update rules now");
        updateBlocklists.setOnClickListener(v -> runBlocklistUpdate(updateBlocklists));
        addTopMargin(advanced, updateBlocklists, 10, new LinearLayout.LayoutParams(-1, dp(48)));

        blocklistMetaText = text("", 13, MUTED, false);
        blocklistMetaText.setLineSpacing(dp(5), 1.0f);
        blocklistMetaText.setBackground(roundRect(0xFFF8FAFC, 16));
        blocklistMetaText.setPadding(dp(14), dp(12), dp(14), dp(12));
        addTopMargin(advanced, blocklistMetaText, 14);

        TextView systemTitle = text("System help", 14, INK, true);
        addTopMargin(settings, systemTitle, 16);
        batteryStatusText = text("", 13, MUTED, false);
        addTopMargin(settings, batteryStatusText, 8);
        privateDnsStatusText = text("", 13, MUTED, false);
        addTopMargin(settings, privateDnsStatusText, 4);
        LinearLayout systemButtons = row();
        Button batteryButton = smallButton("Fix battery");
        batteryButton.setOnClickListener(v -> openBatterySettings());
        systemButtons.addView(batteryButton, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button privateDns = smallButton("Fix DNS");
        privateDns.setOnClickListener(v -> openPrivateDnsSettings());
        LinearLayout.LayoutParams dnsParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        dnsParams.leftMargin = dp(10);
        systemButtons.addView(privateDns, dnsParams);
        addTopMargin(settings, systemButtons, 10);

        Button clearStats = smallButton("Reset activity");
        clearStats.setOnClickListener(view -> {
            StatsStore.clear(this);
            renderState();
        });
        addTopMargin(settings, clearStats, 16, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView dataLabel = text("Data", 14, INK, true);
        addTopMargin(settings, dataLabel, 16);
        LinearLayout exportImportRow = row();
        Button exportBtn = smallButton("Export settings");
        exportBtn.setOnClickListener(v -> {
            String json = AppSettings.exportSettings(this);
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                android.content.ClipData clip = android.content.ClipData.newPlainText("SafePulse Settings", json);
                clipboard.setPrimaryClip(clip);
            }
        });
        exportImportRow.addView(exportBtn, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button importBtn = smallButton("Import settings");
        importBtn.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null && AppSettings.importSettings(this, text.toString())) {
                    showTab(TAB_PROTECTION);
                }
            }
        });
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        importParams.leftMargin = dp(10);
        exportImportRow.addView(importBtn, importParams);
        addTopMargin(settings, exportImportRow, 8);
    }

    private void buildDiagnostics(LinearLayout root) {
        LinearLayout diagnostics = card(20, CARD_BG);
        diagnostics.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, diagnostics, 16);
        diagnostics.addView(sectionTitle("App check", "Make sure SafePulse is ready"));

        diagnosticsText = text("", 13, MUTED, false);
        diagnosticsText.setLineSpacing(dp(5), 1.0f);
        diagnosticsText.setBackground(roundRect(0xFFF8FAFC, 16));
        diagnosticsText.setPadding(dp(14), dp(12), dp(14), dp(12));
        addTopMargin(diagnostics, diagnosticsText, 14);

        Button refresh = smallButton("Refresh check");
        refresh.setOnClickListener(v -> renderDiagnosticsState());
        addTopMargin(diagnostics, refresh, 12, new LinearLayout.LayoutParams(-1, dp(50)));

        LinearLayout test = card(20, CARD_BG);
        test.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, test, 16);
        test.addView(sectionTitle("Test blocking", "Quick rule check"));

        TextView testNote = text("Tap once to check if common ad and tracker domains are blocked while normal websites stay allowed.", 13, MUTED, false);
        testNote.setLineSpacing(dp(4), 1.0f);
        addTopMargin(test, testNote, 10);

        Button runTest = smallButton("Run test");
        runTest.setOnClickListener(v -> runBlockingTest(runTest));
        addTopMargin(test, runTest, 12, new LinearLayout.LayoutParams(-1, dp(50)));

        testResultText = text("No test run yet.", 13, MUTED, false);
        testResultText.setLineSpacing(dp(5), 1.0f);
        testResultText.setBackground(roundRect(0xFFF8FAFC, 16));
        testResultText.setPadding(dp(14), dp(12), dp(14), dp(12));
        addTopMargin(test, testResultText, 12);

        LinearLayout searchCard = card(20, CARD_BG);
        searchCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, searchCard, 16);
        searchCard.addView(sectionTitle("Blocklist search", "Check if a domain is blocked"));
        EditText searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Enter domain (e.g., ads.example.com)");
        searchInput.setTextSize(14);
        searchInput.setTextColor(color(INK));
        searchInput.setHintTextColor(color(MUTED));
        searchInput.setBackground(roundStroke(0xFFF8FAFC, BORDER, 16));
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        applyTvFocus(searchInput);
        addTopMargin(searchCard, searchInput, 12, new LinearLayout.LayoutParams(-1, dp(50)));
        TextView searchResult = text("", 13, MUTED, false);
        searchResult.setLineSpacing(dp(4), 1.0f);
        searchResult.setBackground(roundRect(0xFFF8FAFC, 16));
        searchResult.setPadding(dp(14), dp(12), dp(14), dp(12));
        Button searchBtn = smallButton("Search");
        searchBtn.setOnClickListener(v -> {
            String domain = searchInput.getText().toString().trim();
            if (domain.isEmpty()) return;
            boolean blocked = BlocklistManager.isDomainBlocked(this, domain);
            searchResult.setText(blocked ? "BLOCKED: " + domain + " is in the blocklist" : "ALLOWED: " + domain + " is NOT in the blocklist");
            searchResult.setTextColor(color(blocked ? ERROR : SUCCESS));
            searchResult.setVisibility(View.VISIBLE);
        });
        addTopMargin(searchCard, searchBtn, 10, new LinearLayout.LayoutParams(-1, dp(48)));
        searchResult.setVisibility(View.GONE);
        addTopMargin(searchCard, searchResult, 10);

        LinearLayout tv = card(20, CARD_BG);
        tv.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, tv, 16);
        tv.addView(sectionTitle("Fire TV", "Simple setup"));
        TextView tvText = text("Install the universal APK, allow the VPN permission, then press the remote center button to start or stop protection. If blocking is not working, turn off Private DNS where available.", 13, MUTED, false);
        tvText.setLineSpacing(dp(4), 1.0f);
        addTopMargin(tv, tvText, 10);

        renderDiagnosticsState();
    }

    private void buildAbout(LinearLayout root) {
        LinearLayout about = card(20, CARD_BG);
        about.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, about, 16);
        about.addView(sectionTitle("SafePulse", "Help and app details"));
        addTopMargin(about, text("Developed by JOJIN JOHN", 15, INK, true), 12);
        TextView description = text("SafePulse starts only when you tap Start. It blocks many ads, trackers, popups, and risky redirects across supported apps and browsers. Some streaming ads can still appear when they use the same servers as videos.", 13, MUTED, false);
        description.setLineSpacing(dp(4), 1.0f);
        addTopMargin(about, description, 8);
        Button linkedin = smallButton("LinkedIn profile");
        linkedin.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/jojin-john/"))));
        addTopMargin(about, linkedin, 12, new LinearLayout.LayoutParams(-1, dp(48)));

        Button privacy = smallButton("Privacy policy");
        privacy.setOnClickListener(v -> openUrl("https://github.com/jojin1709/safeplus/blob/main/PRIVACY.md"));
        addTopMargin(about, privacy, 10, new LinearLayout.LayoutParams(-1, dp(48)));

        Button changelog = smallButton("Changelog");
        changelog.setOnClickListener(v -> openUrl("https://github.com/jojin1709/safeplus/blob/main/CHANGELOG.md"));
        addTopMargin(about, changelog, 10, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout updates = card(20, CARD_BG);
        updates.setPadding(dp(18), dp(18), dp(18), dp(18));
        addTopMargin(root, updates, 16);
        updates.addView(sectionTitle("Updates", "Keep SafePulse current"));

        updateStatusText = text("Current version: " + currentVersionLabel(), 13, MUTED, false);
        updateStatusText.setLineSpacing(dp(4), 1.0f);
        addTopMargin(updates, updateStatusText, 12);

        Button checkUpdates = smallButton("Check for updates");
        checkUpdates.setOnClickListener(v -> checkForUpdates(checkUpdates, true));
        addTopMargin(updates, checkUpdates, 12, new LinearLayout.LayoutParams(-1, dp(48)));

        updateNowButton = smallButton("Update now");
        updateNowButton.setVisibility(View.GONE);
        updateNowButton.setOnClickListener(v -> openLatestRelease());
        addTopMargin(updates, updateNowButton, 10, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView updateNote = text("SafePulse chooses the correct update for this device.", 12, MUTED, false);
        updateNote.setLineSpacing(dp(4), 1.0f);
        addTopMargin(updates, updateNote, 12);

        checkForUpdates(null, false);
    }

    private void checkForUpdates(Button button, boolean userRequested) {
        if (updateStatusText != null) {
            updateStatusText.setText("Checking GitHub Releases...\nInstalled: " + currentVersionLabel());
        }
        if (button != null) {
            button.setEnabled(false);
            button.setText("Checking...");
        }

        new Thread(() -> {
            ReleaseInfo release = null;
            String error = null;
            try {
                release = fetchLatestRelease();
            } catch (Exception exception) {
                error = exception.getMessage();
            }

            ReleaseInfo result = release;
            String resultError = error;
            handler.post(() -> applyUpdateResult(result, resultError, button, userRequested));
        }).start();
    }

    private ReleaseInfo fetchLatestRelease() throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(RELEASES_API_URL).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "SafePulse/" + currentVersionName());

            int status = connection.getResponseCode();
            if (status == 401 || status == 403 || status == 404) {
                throw new IOException("GitHub Release is private or unavailable");
            }
            if (status < 200 || status >= 300) {
                throw new IOException("GitHub returned " + status);
            }

            JSONObject json = new JSONObject(readResponse(connection));
            String tag = json.optString("tag_name", "");
            String version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
            String releaseUrl = json.optString("html_url", "https://github.com/jojin1709/safeplus/releases");
            ReleaseAsset bestAsset = null;

            JSONArray assets = json.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset == null) continue;
                    ReleaseAsset candidate = releaseAsset(asset);
                    if (candidate == null || candidate.score < 0) continue;
                    if (bestAsset == null || candidate.score > bestAsset.score) bestAsset = candidate;
                }
            }

            return new ReleaseInfo(
                    tag,
                    version,
                    releaseUrl,
                    bestAsset == null ? "" : bestAsset.url,
                    bestAsset == null ? "" : bestAsset.name,
                    bestAsset != null,
                    currentDeviceLabel()
            );
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Could not read release metadata", exception);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void applyUpdateResult(ReleaseInfo release, String error, Button button, boolean userRequested) {
        if (button != null) {
            button.setEnabled(true);
            button.setText("Check for updates");
        }
        if (updateStatusText == null) return;

        if (error != null && !error.isEmpty()) {
            latestRelease = null;
            updateStatusText.setText("Could not check updates.\n" + error + ".\nInstalled: " + currentVersionLabel());
            if (updateNowButton != null) updateNowButton.setVisibility(View.GONE);
            return;
        }

        latestRelease = release;
        boolean hasNewerVersion = release != null && compareVersions(release.version, currentVersionName()) > 0;
        if (hasNewerVersion) {
            String assetLine = release.compatibleAsset
                    ? "\nAPK: " + release.assetName
                    : "\nNo compatible APK asset found for " + release.deviceLabel + ".";
            updateStatusText.setText("Update available: " + release.tag + "\nInstalled: " + currentVersionLabel() + assetLine);
            if (updateNowButton != null) {
                updateNowButton.setText(release.compatibleAsset ? "Update now" : "View release");
                updateNowButton.setVisibility(View.VISIBLE);
            }
        } else {
            String latest = release == null || release.tag.isEmpty() ? "unknown" : release.tag;
            String assetLine = release != null && release.compatibleAsset ? "\nAPK channel: " + release.assetName : "";
            updateStatusText.setText("SafePulse is up to date.\nInstalled: " + currentVersionLabel() + "\nLatest release: " + latest + assetLine);
            if (updateNowButton != null) updateNowButton.setVisibility(View.GONE);
        }
    }

    private ReleaseAsset releaseAsset(JSONObject asset) {
        String originalName = asset.optString("name", "");
        String name = originalName.toLowerCase(Locale.US);
        String url = asset.optString("browser_download_url", "");
        if (!name.endsWith(".apk") || url.isEmpty()) return null;
        return new ReleaseAsset(originalName, url, updateAssetScore(name));
    }

    private int updateAssetScore(String name) {
        boolean tvName = name.contains("tv") || name.contains("fire") || name.contains("leanback");
        boolean mobileName = name.contains("phone") || name.contains("mobile") || name.contains("tablet");
        boolean universalName = name.contains("universal") || name.contains("all") || name.matches(".*safepulse-v?\\d.*\\.apk");

        if (isTvLayout()) {
            if (tvName) return 100;
            if (universalName) return 70;
            if (mobileName) return -1;
            return 40;
        }

        if (mobileName) return 100;
        if (universalName) return 70;
        if (tvName) return -1;
        return 40;
    }

    private String currentDeviceLabel() {
        return isTvLayout() ? "Android TV / Fire TV" : "Android phone / tablet";
    }

    private void openLatestRelease() {
        String target = "https://github.com/jojin1709/safeplus/releases";
        if (latestRelease != null) {
            target = latestRelease.apkUrl.isEmpty() ? latestRelease.releaseUrl : latestRelease.apkUrl;
        }
        openUrl(target);
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private String currentVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null || info.versionName.isEmpty() ? APP_VERSION_NAME : info.versionName;
        } catch (PackageManager.NameNotFoundException exception) {
            return APP_VERSION_NAME;
        }
    }

    private long currentVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return info.getLongVersionCode();
            }
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException exception) {
            return APP_VERSION_CODE;
        }
    }

    private String currentVersionLabel() {
        return currentVersionName() + " (" + currentVersionCode() + ")";
    }

    private int compareVersions(String left, String right) {
        int[] leftParts = versionParts(left);
        int[] rightParts = versionParts(right);
        int count = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < count; i++) {
            int leftValue = i < leftParts.length ? leftParts[i] : 0;
            int rightValue = i < rightParts.length ? rightParts[i] : 0;
            if (leftValue != rightValue) return leftValue > rightValue ? 1 : -1;
        }
        return 0;
    }

    private int[] versionParts(String version) {
        if (version == null) return new int[]{0, 0, 0};
        String cleaned = version.replaceFirst("^[vV]", "");
        String[] rawParts = cleaned.split("[^0-9]+");
        int[] parts = new int[Math.max(3, rawParts.length)];
        int index = 0;
        for (String rawPart : rawParts) {
            if (rawPart == null || rawPart.isEmpty()) continue;
            try {
                parts[index] = Integer.parseInt(rawPart);
                index++;
            } catch (NumberFormatException ignored) {
                parts[index] = 0;
                index++;
            }
            if (index >= parts.length) break;
        }
        return parts;
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getInputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private void toggleVpn(View view) {
        if (AdBlockVpnService.isRunning()) {
            stoppedFromButton = true;
            StatsStore.clearStopped(this);
            startService(new Intent(this, AdBlockVpnService.class).setAction(AdBlockVpnService.ACTION_STOP));
            renderState();
            return;
        }

        stoppedFromButton = false;
        ensureProtectionStarted();
    }

    private void ensureProtectionStarted() {
        if (AdBlockVpnService.isRunning() || stoppedFromButton) return;
        Intent permissionIntent = VpnService.prepare(this);
        if (permissionIntent != null) {
            startActivityForResult(permissionIntent, VPN_START_REQUEST_CODE);
        } else {
            startBlocker();
        }
    }

    private void requestVpnSetupPermission() {
        Intent permissionIntent = VpnService.prepare(this);
        if (permissionIntent != null) {
            startActivityForResult(permissionIntent, VPN_SETUP_REQUEST_CODE);
        } else {
            renderState();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_START_REQUEST_CODE && resultCode == RESULT_OK) {
            startBlocker();
        } else if (requestCode == VPN_SETUP_REQUEST_CODE) {
            renderState();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_REQUEST_CODE) {
            renderState();
        }
    }

    private void startBlocker() {
        Intent intent = new Intent(this, AdBlockVpnService.class).setAction(AdBlockVpnService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        renderState();
    }

    private void renderState() {
        boolean running = AdBlockVpnService.isRunning();
        boolean needsVpnApproval = !running && VpnService.prepare(this) != null;
        StatsStore.Snapshot stats = StatsStore.snapshot(this);

        long blocked = running ? stats.blocked : 0L;
        long checked = running ? stats.checked : 0L;
        long allowed = running ? stats.allowed : 0L;

        if (blockedCount != null) blockedCount.setText(formatNumber(blocked));
        if (checkedCount != null) checkedCount.setText(formatNumber(checked));
        if (allowedCount != null) allowedCount.setText(formatNumber(allowed));
        if (blockRateText != null) blockRateText.setText(running ? stats.blockRate() + "%" : "0%");
        if (blocklistCountText != null) {
            blocklistCountText.setText(running ? formatNumber(BlocklistManager.bundledCount(this) + BlocklistManager.remoteCount(this)) : "0");
        }

        if (statusPill != null) {
            statusPill.setVisibility(running ? View.VISIBLE : View.GONE);
            statusPill.setText("ACTIVE");
            statusPill.setTextColor(color(SUCCESS));
            statusPill.setBackground(roundRect(SOFT_SUCCESS, 999));
        }
        if (statusText != null) {
            statusText.setText(running ? "Protection active" : needsVpnApproval ? "VPN approval needed" : "Ready to start");
        }
        if (toggleButton != null) {
            toggleButton.setText(running ? "Stop protection" : "Start protection");
            toggleButton.setTextColor(color(running ? Color.WHITE : PRIMARY));
            toggleButton.setBackground(running ? roundStroke(0x22FFFFFF, 0x55FFFFFF, 16) : roundRect(Color.WHITE, 16));
        }

        if (uptimeText != null) {
            uptimeText.setText(running ? "Uptime: " + formatDuration(System.currentTimeMillis() - stats.startedAt) : "Uptime: not running");
        }
        if (recentBlocksText != null) recentBlocksText.setText(running ? formatRecentBlocks(stats) : "No blocked domains yet.");
        if (recentBlocksList != null) renderRecentBlocks(running, stats);
        if (logsText != null) logsText.setText(running ? formatLogs(stats) : "No activity yet.");
        renderSetupState();
        renderSettingsState();
        renderDiagnosticsState();
    }

    private void renderSetupState() {
        boolean vpnReady = VpnService.prepare(this) == null;
        setSetupStatus(vpnSetupStatusText, vpnReady ? "Allowed" : "Required", vpnReady ? SUCCESS : ERROR, vpnReady ? SOFT_SUCCESS : SOFT_ERROR);
        if (vpnSetupButton != null) {
            vpnSetupButton.setEnabled(!vpnReady);
            vpnSetupButton.setText(vpnReady ? "Done" : "Allow");
        }

        boolean batteryReady = isBatteryOptimizationIgnored();
        setSetupStatus(batterySetupStatusText, batteryReady ? "Allowed" : "Recommended", batteryReady ? SUCCESS : PRIMARY, batteryReady ? SOFT_SUCCESS : PRIMARY_SOFT);
        if (batterySetupButton != null) {
            batterySetupButton.setEnabled(!batteryReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
            batterySetupButton.setText(batteryReady ? "Done" : "Allow");
        }

        boolean notificationsReady = hasNotificationPermission();
        setSetupStatus(notificationSetupStatusText, notificationsReady ? (Build.VERSION.SDK_INT >= 33 ? "Allowed" : "Not required") : "Recommended", notificationsReady ? SUCCESS : PRIMARY, notificationsReady ? SOFT_SUCCESS : PRIMARY_SOFT);
        if (notificationSetupButton != null) {
            notificationSetupButton.setEnabled(!notificationsReady && Build.VERSION.SDK_INT >= 33);
            notificationSetupButton.setText(notificationsReady ? "Done" : "Allow");
        }

        boolean privateDnsOk = isPrivateDnsOk();
        setSetupStatus(privateDnsSetupStatusText, privateDnsOk ? privateDnsStatus() : "May bypass", privateDnsOk ? SUCCESS : ERROR, privateDnsOk ? SOFT_SUCCESS : SOFT_ERROR);
        if (privateDnsSetupButton != null) {
            privateDnsSetupButton.setText("Open");
        }
    }

    private void renderSettingsState() {
        if (blocklistMetaText == null) return;
        long lastUpdate = BlocklistManager.lastUpdate(this);
        String updateText = lastUpdate == 0L ? "Remote blocklist: not downloaded yet" : "Remote blocklist: " + formatNumber(BlocklistManager.remoteCount(this)) + " rules, updated " + timeFormat.format(new Date(lastUpdate));
        String error = BlocklistManager.lastError(this);
        if (error != null && !error.isEmpty()) updateText += "\nLast update error: " + error;
        Set<String> allowlist = AppSettings.getAllowlist(this);
        Set<String> bypassPackages = AppSettings.getBypassPackages(this);
        blocklistMetaText.setText(
                "System-DNS apps: protected\n"
                        + "YouTube official app: best-effort, video-safe\n"
                        + "Private DNS / hardcoded encrypted DNS: may bypass\n"
                        + "Cosmetic empty ad boxes: not removable by DNS\n"
                        + "Bypass list: " + (bypassPackages.isEmpty() ? "off" : bypassPackages.size() + " app(s)") + "\n"
                        + "Allowlist: " + (allowlist.isEmpty() ? "off" : allowlist.size() + " domain(s)") + "\n"
                        + "DNS: " + AppSettings.getDnsProvider(this) + "\n"
                        + "Aggressive mode: " + onOff(AppSettings.isAggressiveBlockingEnabled(this)) + "\n"
                        + "Strict YouTube: " + onOff(AppSettings.isStrictYoutubeEnabled(this)) + "\n"
                        + "Encrypted DNS guard: " + onOff(AppSettings.isDohGuardEnabled(this)) + "\n"
                        + "Anti-tracking: " + onOff(AppSettings.isAntiTrackingEnabled(this)) + "\n"
                        + "Never-consent: " + onOff(AppSettings.isNeverConsentEnabled(this)) + "\n"
                        + "Redirect protection: " + onOff(AppSettings.isRedirectProtectionEnabled(this)) + "\n"
                        + "Adult content block: " + onOff(AppSettings.isAdultBlockEnabled(this)) + "\n"
                        + "Auto-start after reboot: " + onOff(AppSettings.isBootStartEnabled(this)) + "\n"
                        + "Auto-update: " + onOff(AppSettings.isAutoUpdateEnabled(this)) + "\n"
                        + updateText
        );

        if (allowlistText != null) {
            allowlistText.setText(allowlist.isEmpty() ? "No allowed domains." : TextUtils.join("\n", allowlist));
        }
        if (batteryStatusText != null) {
            batteryStatusText.setText(isBatteryOptimizationIgnored() ? "Battery: ready" : "Battery: may stop background protection");
        }
        if (privateDnsStatusText != null) {
            privateDnsStatusText.setText(isPrivateDnsOk() ? "Private DNS: ready" : "Private DNS: may bypass blocking");
        }
    }

    private void renderDiagnosticsState() {
        if (diagnosticsText == null) return;
        boolean running = AdBlockVpnService.isRunning();
        boolean vpnReady = VpnService.prepare(this) == null;
        boolean batteryReady = isBatteryOptimizationIgnored();
        boolean notificationsReady = hasNotificationPermission();
        boolean privateDnsOk = isPrivateDnsOk();
        long lastUpdate = BlocklistManager.lastUpdate(this);
        int bundled = BlocklistManager.bundledCount(this);
        int remote = BlocklistManager.remoteCount(this);
        String lastUpdateText = lastUpdate == 0L ? "not downloaded yet" : timeFormat.format(new Date(lastUpdate));
        String blocklistError = BlocklistManager.lastError(this);

        StringBuilder builder = new StringBuilder();
        appendDiagnostic(builder, "Protection", running ? "on" : "off", running);
        appendDiagnostic(builder, "VPN permission", vpnReady ? "allowed" : "needs approval", vpnReady);
        appendDiagnostic(builder, "DNS", AppSettings.getDnsProvider(this), true);
        appendDiagnostic(builder, "Block rules", formatNumber(bundled + remote) + " loaded", bundled + remote > 0);
        appendDiagnostic(builder, "Rule update", lastUpdateText, blocklistError == null || blocklistError.isEmpty());
        appendDiagnostic(builder, "Private DNS", privateDnsStatus(), privateDnsOk);
        appendDiagnostic(builder, "Battery", batteryReady ? "optimization ignored" : "may restrict background service", batteryReady);
        appendDiagnostic(builder, "Notifications", notificationsReady ? "allowed or not required" : "permission recommended", notificationsReady);
        appendDiagnostic(builder, "Device mode", currentDeviceLabel(), true);
        builder.append("\nYouTube: best effort. SafePulse avoids blocking video servers that can break playback.");
        diagnosticsText.setText(builder.toString());
    }

    private void appendDiagnostic(StringBuilder builder, String label, String value, boolean healthy) {
        if (builder.length() > 0) builder.append('\n');
        builder.append(healthy ? "Good - " : "Check - ")
                .append(label)
                .append(": ")
                .append(value);
    }

    private void runBlockingTest(Button button) {
        if (testResultText != null) {
            testResultText.setText("Running rule test...");
        }
        button.setEnabled(false);
        button.setText("Testing...");

        new Thread(() -> {
            Set<String> effectiveBlocklist = BlocklistManager.loadEffectiveBlocklist(this);
            StringBuilder result = new StringBuilder();
            int passed = 0;
            int total = 0;
            total += 1; passed += appendRuleTest(result, effectiveBlocklist, "googleads.g.doubleclick.net", true);
            total += 1; passed += appendRuleTest(result, effectiveBlocklist, "imasdk.googleapis.com", true);
            total += 1; passed += appendRuleTest(result, effectiveBlocklist, "api.segment.io", true);
            total += 1; passed += appendRuleTest(result, effectiveBlocklist, "clickserve.dartsearch.net", true);
            total += 1; passed += appendRuleTest(result, effectiveBlocklist, "example.com", false);
            total += 1; passed += appendRuleTest(result, effectiveBlocklist, "youtube.com", false);
            total += 1; passed += appendRuleTest(result, effectiveBlocklist, "r1---sn-a5mekn6z.googlevideo.com", AppSettings.isStrictYoutubeEnabled(this));

            result.insert(0, "Result: " + passed + " of " + total + " checks passed.\n");
            if (!AdBlockVpnService.isRunning()) {
                result.append("\nProtection is off. Start protection to use these rules on your device.");
            } else {
                result.append("\nProtection is on. Matching requests should be blocked live.");
            }

            handler.post(() -> {
                if (testResultText != null) testResultText.setText(result.toString());
                button.setEnabled(true);
                button.setText("Run test");
            });
        }).start();
    }

    private int appendRuleTest(StringBuilder builder, Set<String> effectiveBlocklist, String host, boolean expectedBlocked) {
        String reason = AdBlockVpnService.blockReasonForHost(this, effectiveBlocklist, host);
        boolean blocked = reason != null;
        boolean passed = blocked == expectedBlocked;
        if (builder.length() > 0) builder.append('\n');
        builder.append(passed ? "Good - " : "Check - ")
                .append(host)
                .append(" -> ")
                .append(blocked ? "blocked" : "allowed");
        return passed ? 1 : 0;
    }

    private String formatRecentBlocks(StatsStore.Snapshot stats) {
        if (stats.recentBlocks.isEmpty()) return "No blocked domains yet.";

        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (StatsStore.RecentBlock block : stats.recentBlocks) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(index)
                    .append(". ")
                    .append(block.host)
                    .append("  -  ")
                    .append(block.count)
                    .append(block.count == 1L ? " block" : " blocks");
            index++;
        }
        return builder.toString();
    }

    private String formatLogs(StatsStore.Snapshot stats) {
        if (stats.events.isEmpty()) return "No log entries yet.";
        String filter = logFilterInput == null ? "" : logFilterInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        int shown = 0;
        for (StatsStore.LogEvent event : stats.events) {
            if (!filter.isEmpty() && !event.host.toLowerCase(Locale.ROOT).contains(filter)) continue;
            if (builder.length() > 0) builder.append('\n');
            builder.append(timeFormat.format(new Date(event.timestamp)))
                    .append("  ")
                    .append(event.action)
                    .append("  ")
                    .append(event.host)
                    .append("  (")
                    .append(event.category)
                    .append(")");
            shown++;
            if (shown >= 25) break;
        }
        return shown == 0 ? "No matching log entries." : builder.toString();
    }

    private void renderRecentBlocks(boolean running, StatsStore.Snapshot stats) {
        recentBlocksList.removeAllViews();
        if (!running || stats.recentBlocks.isEmpty()) {
            LinearLayout empty = activityRow("No blocked domains yet", "Activity appears here after protection starts.", "Ready", SUCCESS);
            recentBlocksList.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        int shown = 0;
        for (StatsStore.RecentBlock block : stats.recentBlocks) {
            String count = block.count + (block.count == 1L ? " block" : " blocks");
            String meta = "Last seen " + timeFormat.format(new Date(block.lastSeen));
            LinearLayout row = activityRow(block.host, meta, count, ERROR);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            if (shown > 0) params.topMargin = dp(8);
            recentBlocksList.addView(row, params);
            shown++;
            if (shown >= 5) break;
        }
    }

    private LinearLayout activityRow(String title, String subtitle, String badge, int statusColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(roundRect(0xFFF8FAFC, 16));

        row.addView(dot(statusColor));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 13, INK, true);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        textColumn.addView(titleView);
        TextView subtitleView = text(subtitle, 11, MUTED, false);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        addTopMargin(textColumn, subtitleView, 4);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.leftMargin = dp(10);
        row.addView(textColumn, textParams);

        TextView badgeView = text(badge, 11, statusColor, true);
        badgeView.setGravity(Gravity.CENTER);
        badgeView.setPadding(dp(8), dp(5), dp(8), dp(5));
        badgeView.setBackground(roundRect(statusColor == ERROR ? SOFT_ERROR : SOFT_SUCCESS, 999));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
        badgeParams.leftMargin = dp(8);
        row.addView(badgeView, badgeParams);
        return row;
    }

    private void runBlocklistUpdate(Button button) {
        button.setEnabled(false);
        button.setText("Updating...");
        if (blocklistUrlInput == null) {
            button.setEnabled(true);
            button.setText("Update blocklists now");
            return;
        }
        AppSettings.setBlocklistUrl(this, blocklistUrlInput.getText().toString());
        new Thread(() -> {
            BlocklistManager.UpdateResult result = BlocklistManager.updateFromSource(this);
            handler.post(() -> {
                button.setEnabled(true);
                button.setText(result.success ? "Updated" : "Retry update");
                if (result.success && AdBlockVpnService.isRunning()) {
                    startService(new Intent(this, AdBlockVpnService.class).setAction(AdBlockVpnService.ACTION_RELOAD_BLOCKLIST));
                }
                renderState();
            });
        }, "manual-blocklist-update").start();
    }

    private void addBypassCheck(LinearLayout parent, String label, String packageName) {
        CheckBox box = checkBox(label);
        box.setChecked(AppSettings.isPackageBypassed(this, packageName));
        box.setEnabled(isPackageInstalled(packageName));
        if (!box.isEnabled()) {
            box.setText(label + " (not installed)");
        }
        box.setOnCheckedChangeListener((buttonView, checked) -> {
            AppSettings.setPackageBypassed(this, packageName, checked);
            renderSettingsState();
        });
        addTopMargin(parent, box, 8);
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private interface TimePickerCallback {
        void onTimeSelected(int hour, int minute);
    }

    private void showTimePicker(String title, int currentHour, int currentMinute, TimePickerCallback callback) {
        new android.app.AlertDialog.Builder(this, isDarkMode() ? android.R.style.Theme_Material_Dialog_Alert : android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle(title)
                .setPositiveButton("OK", (d, w) -> {
                    android.widget.NumberPicker hourPicker = new android.widget.NumberPicker(this);
                    hourPicker.setMinValue(0);
                    hourPicker.setMaxValue(23);
                    hourPicker.setValue(currentHour);
                    android.widget.NumberPicker minPicker = new android.widget.NumberPicker(this);
                    minPicker.setMinValue(0);
                    minPicker.setMaxValue(59);
                    minPicker.setValue(currentMinute);
                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setGravity(Gravity.CENTER);
                    layout.addView(hourPicker, new LinearLayout.LayoutParams(-2, -2));
                    TextView colon = text(":", 20, INK, true);
                    layout.addView(colon);
                    layout.addView(minPicker, new LinearLayout.LayoutParams(-2, -2));
                    new android.app.AlertDialog.Builder(this, isDarkMode() ? android.R.style.Theme_Material_Dialog_Alert : android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                            .setTitle(title)
                            .setView(layout)
                            .setPositiveButton("OK", (dd, ww) -> callback.onTimeSelected(hourPicker.getValue(), minPicker.getValue()))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openBatterySettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void openPrivateDnsSettings() {
        try {
            startActivity(new Intent("android.settings.PRIVATE_DNS_SETTINGS"));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        }
    }

    private String privateDnsStatus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return "not supported";
        String mode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
        String specifier = Settings.Global.getString(getContentResolver(), "private_dns_specifier");
        if (mode == null || mode.isEmpty() || "off".equals(mode)) return "off";
        if ("hostname".equals(mode) && specifier != null && !specifier.isEmpty()) {
            return "on (" + specifier + ") - may bypass blocking";
        }
        return mode + " - may bypass blocking";
    }

    private boolean isPrivateDnsOk() {
        String status = privateDnsStatus();
        return status.startsWith("off") || status.startsWith("not supported");
    }

    private boolean isBatteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private TextView addStat(LinearLayout row, String label, String value, int iconRes, int iconBg, int iconTint) {
        LinearLayout stat = card(18, CARD_BG);
        stat.setPadding(dp(14), dp(13), dp(14), dp(12));
        stat.setMinimumHeight(dp(isTabletLayout() ? 112 : 104));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(iconBadge(iconRes, iconBg, iconTint, 34, 18));

        TextView labelView = text(label, 12, MUTED, false);
        labelView.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(8);
        top.addView(labelView, labelParams);
        stat.addView(top);

        TextView valueView = text(value, 24, INK, true);
        valueView.setIncludeFontPadding(false);
        addTopMargin(stat, valueView, 11);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        if (row.getChildCount() > 0) params.leftMargin = dp(12);
        row.addView(stat, params);
        return valueView;
    }

    private CheckBox checkBox(String label) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextColor(color(INK));
        box.setTextSize(textSizeForDevice(14));
        box.setTypeface(typeface(false));
        box.setMinHeight(dp(isTvLayout() ? 58 : 50));
        box.setPadding(dp(12), 0, dp(12), 0);
        box.setBackground(roundStroke(0xFFF8FAFC, BORDER, 16));
        box.setFocusable(true);
        applyTvFocus(box);
        if (Build.VERSION.SDK_INT >= 21) {
            box.setButtonTintList(ColorStateList.valueOf(color(PRIMARY)));
        }
        return box;
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(textSizeForDevice(14));
        button.setTextColor(color(INK));
        button.setTypeface(typeface(true));
        button.setBackground(ripple(roundStroke(CARD_BG, BORDER, 16)));
        button.setFocusable(true);
        button.setMinHeight(dp(isTvLayout() ? 54 : 46));
        applyTvFocus(button);
        return button;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout card(int radiusDp, int color) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(color == CARD_BG ? roundStroke(CARD_BG, BORDER, radiusDp) : roundRect(color, radiusDp));
        layout.setElevation(color == CARD_BG ? dp(2) : dp(3));
        return layout;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(textSizeForDevice(sp));
        textView.setTextColor(color(color));
        textView.setTypeface(typeface(bold));
        return textView;
    }

    private LinearLayout sectionTitle(String title, String subtitle) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 18, INK, true);
        titleView.setIncludeFontPadding(false);
        section.addView(titleView);
        TextView subtitleView = text(subtitle, 12, MUTED, false);
        subtitleView.setIncludeFontPadding(false);
        addTopMargin(section, subtitleView, 5);
        return section;
    }

    private FrameLayout iconButton(int iconRes, int iconTint, int bgColor) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(ripple(roundRect(bgColor, 14)));
        frame.setClickable(true);
        frame.setFocusable(true);
        applyTvFocus(frame);
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(color(iconTint));
        frame.addView(icon, new FrameLayout.LayoutParams(dp(isTvLayout() ? 22 : 18), dp(isTvLayout() ? 22 : 18), Gravity.CENTER));
        return frame;
    }

    private FrameLayout iconBadge(int iconRes, int bgColor, int iconTint, int sizeDp, int iconSizeDp) {
        FrameLayout badge = new FrameLayout(this);
        badge.setMinimumWidth(dp(sizeDp));
        badge.setMinimumHeight(dp(sizeDp));
        badge.setBackground(roundRect(bgColor, 999));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(color(iconTint));
        badge.addView(icon, new FrameLayout.LayoutParams(dp(iconSizeDp), dp(iconSizeDp), Gravity.CENTER));
        badge.setElevation(dp(1));
        return badge;
    }

    private View dot(int color) {
        View dot = new View(this);
        dot.setBackground(circle(color));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(9), dp(9)));
        return dot;
    }

    private RippleDrawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x1A4F46E5), content, null);
    }

    private Typeface typeface(boolean bold) {
        return Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL);
    }

    private int textSizeForDevice(int sp) {
        if (!isTvLayout()) return sp;
        if (sp >= 24) return sp + 3;
        if (sp >= 14) return sp + 2;
        return sp + 1;
    }

    private void applyTvFocus(View view) {
        if (!isTvLayout()) return;
        view.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.04f : 1.0f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(120L).start();
            v.setTranslationZ(hasFocus ? dp(8) : 0);
        });
    }

    private boolean isDarkMode() {
        return AppSettings.isDarkModeEnabled(this);
    }

    private void toggleTheme() {
        AppSettings.setDarkModeEnabled(this, !isDarkMode());
        styleSystemBars();
        buildDashboard();
    }

    private int color(int value) {
        if (!isDarkMode()) return value;
        if (value == BG) return DARK_BG;
        if (value == BORDER) return DARK_BORDER;
        if (value == INK) return DARK_INK;
        if (value == MUTED) return DARK_MUTED;
        if (value == PRIMARY) return DARK_PRIMARY;
        if (value == PRIMARY_SOFT) return DARK_PRIMARY_SOFT;
        if (value == SOFT_SUCCESS) return DARK_SOFT_SUCCESS;
        if (value == SOFT_ERROR) return DARK_SOFT_ERROR;
        if (value == SOFT_BLUE) return DARK_SOFT_BLUE;
        if (value == SURFACE_LIGHT || value == 0xFFF8FAFC) return DARK_SURFACE;
        if (value == 0xFFFFF7ED) return 0xFF431407;
        if (value == 0x33FFFFFF) return 0x33000000;
        if (value == 0x22FFFFFF) return 0x22000000;
        if (value == 0x55FFFFFF) return 0x55000000;
        return value;
    }

    private int fillColor(int value) {
        if (isDarkMode() && value == CARD_BG) return DARK_CARD;
        return color(value);
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor(color));
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable roundStroke(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = roundRect(color, radiusDp);
        drawable.setStroke(dp(1), color(strokeColor));
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private void addTopMargin(LinearLayout parent, View child, int topDp) {
        addTopMargin(parent, child, topDp, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addTopMargin(LinearLayout parent, View child, int topDp, LinearLayout.LayoutParams params) {
        params.topMargin = dp(topDp);
        parent.addView(child, params);
    }

    private String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private String formatNumber(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private String formatDuration(long millis) {
        if (millis < 0L) millis = 0L;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private float cachedDensity = 0;

    private int dp(int value) {
        if (cachedDensity == 0) {
            cachedDensity = getResources().getDisplayMetrics().density;
        }
        return Math.round(value * cachedDensity);
    }

    private boolean isTabletLayout() {
        return getResources().getConfiguration().screenWidthDp >= TABLET_WIDTH_DP;
    }

    private boolean isTvLayout() {
        if (BuildConfig.IS_TV) return true;
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    private int contentWidth() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int sideGutter = isTvLayout() ? dp(160) : isTabletLayout() ? dp(48) : 0;
        int maxWidth = isTvLayout() ? dp(1120) : dp(MAX_CONTENT_WIDTH_DP);
        int availableWidth = Math.max(dp(280), screenWidth - sideGutter);
        return Math.min(screenWidth, Math.min(availableWidth, maxWidth));
    }

    private int navWidth() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int availableWidth = Math.max(dp(220), screenWidth - dp(isTvLayout() ? 180 : 32));
        int maxWidth = isTvLayout() ? dp(820) : dp(MAX_NAV_WIDTH_DP);
        return Math.min(screenWidth, Math.min(availableWidth, maxWidth));
    }

    private int contentHorizontalPadding() {
        return dp(isTvLayout() ? 30 : isTabletLayout() ? 24 : 20);
    }

    private int bottomScrollPadding() {
        return dp(isTvLayout() ? 138 : isTabletLayout() ? 114 : 104);
    }

    private void styleSystemBars() {
        Window window = getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        window.setStatusBarColor(color(BG));
        window.setNavigationBarColor(color(BG));
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = isDarkMode() ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (!isDarkMode() && Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST_CODE);
        } else {
            renderState();
        }
    }

    private static final class ReleaseInfo {
        private final String tag;
        private final String version;
        private final String releaseUrl;
        private final String apkUrl;
        private final String assetName;
        private final boolean compatibleAsset;
        private final String deviceLabel;

        private ReleaseInfo(String tag, String version, String releaseUrl, String apkUrl, String assetName, boolean compatibleAsset, String deviceLabel) {
            this.tag = tag == null ? "" : tag;
            this.version = version == null ? "" : version;
            this.releaseUrl = releaseUrl == null || releaseUrl.isEmpty() ? "https://github.com/jojin1709/safeplus/releases" : releaseUrl;
            this.apkUrl = apkUrl == null ? "" : apkUrl;
            this.assetName = assetName == null || assetName.isEmpty() ? "release page" : assetName;
            this.compatibleAsset = compatibleAsset;
            this.deviceLabel = deviceLabel == null ? "this device" : deviceLabel;
        }
    }

    private static final class ReleaseAsset {
        private final String name;
        private final String url;
        private final int score;

        private ReleaseAsset(String name, String url, int score) {
            this.name = name;
            this.url = url;
            this.score = score;
        }
    }
}

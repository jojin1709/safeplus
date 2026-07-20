package com.jojinjohn.safepulse;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public final class StatsStore {
    private static final String PREFS = "blocker_stats";
    private static final String KEY_BLOCKED = "blocked";
    private static final String KEY_ALLOWED = "allowed";
    private static final String KEY_STARTED_AT = "started_at";
    private static final String KEY_RECENT_BLOCKS = "recent_blocks";
    private static final String KEY_EVENTS = "events";
    private static final int MAX_RECENT = 12;
    private static final int MAX_EVENTS = 120;
    private static final long FLUSH_INTERVAL_MS = 5000;

    private static volatile long memBlocked = 0;
    private static volatile long memAllowed = 0;
    private static volatile long memStartedAt = 0;
    private static volatile List<RecentBlock> memRecent = new ArrayList<>();
    private static volatile List<LogEvent> memEvents = new ArrayList<>();
    private static final AtomicLong lastFlush = new AtomicLong(0);

    private StatsStore() {
    }

    public static synchronized void markStarted(Context context) {
        memStartedAt = System.currentTimeMillis();
        prefs(context).edit().putLong(KEY_STARTED_AT, memStartedAt).apply();
    }

    public static synchronized void markStopped(Context context) {
        memStartedAt = 0;
        prefs(context).edit().putLong(KEY_STARTED_AT, 0L).apply();
    }

    public static synchronized void recordAllowed(Context context) {
        memAllowed++;
        maybeFlush(context);
    }

    public static synchronized void recordBlocked(Context context, String host) {
        recordBlocked(context, host, null);
    }

    public static synchronized void recordBlocked(Context context, String host, String categoryOverride) {
        String normalizedHost = host == null ? "unknown" : host.toLowerCase(Locale.ROOT);
        String category = categoryOverride == null || categoryOverride.isEmpty() ? categoryFor(normalizedHost) : categoryOverride;

        memBlocked++;

        LinkedHashMap<String, RecentBlock> merged = new LinkedHashMap<>();
        RecentBlock current = null;
        for (RecentBlock block : memRecent) {
            if (block.host.equals(normalizedHost)) {
                current = block;
            } else {
                merged.put(block.host, block);
            }
        }

        if (current == null) {
            current = new RecentBlock(normalizedHost, 0L, 0L);
        }
        current.count += 1L;
        current.lastSeen = System.currentTimeMillis();

        LinkedHashMap<String, RecentBlock> ordered = new LinkedHashMap<>();
        ordered.put(current.host, current);
        for (RecentBlock block : merged.values()) {
            if (ordered.size() >= MAX_RECENT) break;
            ordered.put(block.host, block);
        }
        memRecent = new ArrayList<>(ordered.values());

        List<LogEvent> newEvents = new ArrayList<>();
        newEvents.add(new LogEvent(System.currentTimeMillis(), "BLOCKED", normalizedHost, category));
        for (LogEvent existing : memEvents) {
            if (newEvents.size() >= MAX_EVENTS) break;
            newEvents.add(existing);
        }
        memEvents = newEvents;

        maybeFlush(context);
    }

    public static synchronized void loadFromDisk(Context context) {
        SharedPreferences preferences = prefs(context);
        memBlocked = preferences.getLong(KEY_BLOCKED, 0L);
        memAllowed = preferences.getLong(KEY_ALLOWED, 0L);
        memStartedAt = preferences.getLong(KEY_STARTED_AT, 0L);
        memRecent = parseRecent(preferences.getString(KEY_RECENT_BLOCKS, ""));
        memEvents = parseEvents(preferences.getString(KEY_EVENTS, ""));
    }

    public static synchronized Snapshot snapshot(Context context) {
        return new Snapshot(
                memBlocked,
                memAllowed,
                memBlocked + memAllowed,
                memStartedAt,
                new ArrayList<>(memRecent),
                new ArrayList<>(memEvents)
        );
    }

    public static synchronized void clear(Context context) {
        long startedAt = memStartedAt;
        memBlocked = 0;
        memAllowed = 0;
        memRecent = new ArrayList<>();
        memEvents = new ArrayList<>();
        prefs(context).edit()
                .clear()
                .putLong(KEY_STARTED_AT, startedAt)
                .apply();
    }

    public static synchronized void clearStopped(Context context) {
        memBlocked = 0;
        memAllowed = 0;
        memRecent = new ArrayList<>();
        memEvents = new ArrayList<>();
        memStartedAt = 0;
        prefs(context).edit().clear().apply();
    }

    private static void maybeFlush(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastFlush.get() >= FLUSH_INTERVAL_MS) {
            lastFlush.set(now);
            flushToDisk(context);
        }
    }

    private static synchronized void flushToDisk(Context context) {
        prefs(context).edit()
                .putLong(KEY_BLOCKED, memBlocked)
                .putLong(KEY_ALLOWED, memAllowed)
                .putLong(KEY_STARTED_AT, memStartedAt)
                .putString(KEY_RECENT_BLOCKS, serializeRecent(memRecent))
                .putString(KEY_EVENTS, serializeEvents(memEvents))
                .apply();
    }

    public static void forceFlush(Context context) {
        flushToDisk(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static List<RecentBlock> parseRecent(String raw) {
        List<RecentBlock> recent = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return recent;

        String[] rows = raw.split("\\n");
        for (String row : rows) {
            String[] parts = row.split("\\|");
            if (parts.length != 3) continue;
            try {
                recent.add(new RecentBlock(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2])));
            } catch (NumberFormatException ignored) {
            }
        }
        return recent;
    }

    private static String serializeRecent(List<RecentBlock> recent) {
        StringBuilder builder = new StringBuilder();
        for (RecentBlock block : recent) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(block.host).append('|').append(block.count).append('|').append(block.lastSeen);
        }
        return builder.toString();
    }

    private static List<LogEvent> parseEvents(String raw) {
        List<LogEvent> events = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return events;

        String[] rows = raw.split("\\n");
        for (String row : rows) {
            String[] parts = row.split("\\|");
            if (parts.length != 4) continue;
            try {
                events.add(new LogEvent(Long.parseLong(parts[0]), parts[1], parts[2], parts[3]));
            } catch (NumberFormatException ignored) {
            }
        }
        return events;
    }

    private static String serializeEvents(List<LogEvent> events) {
        StringBuilder builder = new StringBuilder();
        for (LogEvent event : events) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(event.timestamp)
                    .append('|')
                    .append(event.action)
                    .append('|')
                    .append(event.host)
                    .append('|')
                    .append(event.category);
        }
        return builder.toString();
    }

    private static String categoryFor(String host) {
        if (host.contains("dns.google") || host.contains("cloudflare-dns") || host.contains("quad9") || host.contains("opendns")) {
            return "Encrypted DNS";
        }
        if (host.contains("analytics") || host.contains("tagmanager") || host.contains("scorecard")) {
            return "Anti-tracking";
        }
        if (host.contains("consent") || host.contains("cookie") || host.contains("onetrust") || host.contains("trustarc")) {
            return "Never-consent";
        }
        if (host.contains("redirect") || host.contains("click") || host.contains("linksynergy") || host.contains("awin")) {
            return "Redirect protection";
        }
        if (host.contains("facebook") || host.contains("connect")) {
            return "Anti-tracking";
        }
        if (host.contains("doubleclick") || host.contains("googlesyndication") || host.contains("googleadservices") || host.contains("adservice")) {
            return "Google ads";
        }
        return "Ads / tracker";
    }

    public static final class Snapshot {
        public final long blocked;
        public final long allowed;
        public final long checked;
        public final long startedAt;
        public final List<RecentBlock> recentBlocks;
        public final List<LogEvent> events;

        Snapshot(long blocked, long allowed, long checked, long startedAt, List<RecentBlock> recentBlocks, List<LogEvent> events) {
            this.blocked = blocked;
            this.allowed = allowed;
            this.checked = checked;
            this.startedAt = startedAt;
            this.recentBlocks = recentBlocks;
            this.events = events;
        }

        public int blockRate() {
            if (checked == 0L) return 0;
            return (int) Math.round((blocked * 100.0d) / checked);
        }
    }

    public static final class RecentBlock {
        public final String host;
        public long count;
        public long lastSeen;

        RecentBlock(String host, long count, long lastSeen) {
            this.host = host;
            this.count = count;
            this.lastSeen = lastSeen;
        }
    }

    public static final class LogEvent {
        public final long timestamp;
        public final String action;
        public final String host;
        public final String category;

        LogEvent(long timestamp, String action, String host, String category) {
            this.timestamp = timestamp;
            this.action = action;
            this.host = host;
            this.category = category;
        }
    }
}

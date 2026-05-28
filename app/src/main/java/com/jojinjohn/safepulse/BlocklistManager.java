package com.jojinjohn.safepulse;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class BlocklistManager {
    private static final String PREFS = "blocklist_meta";
    private static final String KEY_REMOTE_COUNT = "remote_count";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_LAST_ERROR = "last_error";
    private static final String KEY_LAST_SOURCE = "last_source";
    private static final String REMOTE_FILE = "remote-blocklist.txt";

    private BlocklistManager() {
    }

    public static Set<String> loadEffectiveBlocklist(Context context) {
        Set<String> domains = new HashSet<>();
        loadBundled(context, domains);
        loadRemote(context, domains);
        return domains;
    }

    public static boolean shouldAutoUpdate(Context context) {
        String lastSource = prefs(context).getString(KEY_LAST_SOURCE, "");
        if (!AppSettings.getBlocklistUrl(context).equals(lastSource)) return true;
        return System.currentTimeMillis() - lastUpdate(context) > TimeUnit.HOURS.toMillis(24);
    }

    public static UpdateResult updateFromSource(Context context) {
        List<String> sourceUrls = AppSettings.getBlocklistUrls(context);
        File temp = new File(context.getFilesDir(), REMOTE_FILE + ".tmp");
        File target = new File(context.getFilesDir(), REMOTE_FILE);
        Set<String> downloadedDomains = new HashSet<>();
        List<String> errors = new ArrayList<>();

        try {
            if (sourceUrls.isEmpty()) {
                throw new IOException("No blocklist sources configured");
            }

            for (String sourceUrl : sourceUrls) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(sourceUrl).openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(45000);
                    connection.setRequestProperty("User-Agent", "SafePulse/1.0");

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String domain = parseDomain(line);
                            if (!domain.isEmpty()) {
                                downloadedDomains.add(domain);
                            }
                        }
                    }
                } catch (IOException e) {
                    String message = e.getMessage() == null ? "failed" : e.getMessage();
                    errors.add(shortSourceName(sourceUrl) + ": " + message);
                }
            }

            if (downloadedDomains.isEmpty()) {
                throw new IOException("Downloaded blocklist had no domains");
            }

            List<String> sortedDomains = new ArrayList<>(downloadedDomains);
            Collections.sort(sortedDomains);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp, false))) {
                for (String domain : sortedDomains) {
                    writer.write(domain);
                    writer.newLine();
                }
            }

            if (target.exists() && !target.delete()) {
                throw new IOException("Could not replace old blocklist");
            }
            if (!temp.renameTo(target)) {
                throw new IOException("Could not save blocklist");
            }

            String errorText = errors.isEmpty() ? "" : "Some sources failed: " + String.join("; ", errors);
            prefs(context).edit()
                    .putInt(KEY_REMOTE_COUNT, downloadedDomains.size())
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .putString(KEY_LAST_ERROR, errorText)
                    .putString(KEY_LAST_SOURCE, AppSettings.getBlocklistUrl(context))
                    .apply();
            return new UpdateResult(true, downloadedDomains.size(), errorText);
        } catch (IOException e) {
            if (temp.exists()) temp.delete();
            String message = e.getMessage() == null ? "Update failed" : e.getMessage();
            prefs(context).edit().putString(KEY_LAST_ERROR, message).apply();
            return new UpdateResult(false, remoteCount(context), message);
        }
    }

    public static int bundledCount(Context context) {
        Set<String> domains = new HashSet<>();
        loadBundled(context, domains);
        return domains.size();
    }

    public static int remoteCount(Context context) {
        return prefs(context).getInt(KEY_REMOTE_COUNT, 0);
    }

    public static long lastUpdate(Context context) {
        return prefs(context).getLong(KEY_LAST_UPDATE, 0L);
    }

    public static String lastError(Context context) {
        return prefs(context).getString(KEY_LAST_ERROR, "");
    }

    private static void loadBundled(Context context, Set<String> domains) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("blocklist.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String domain = parseDomain(line);
                if (!domain.isEmpty()) domains.add(domain);
            }
        } catch (IOException ignored) {
        }
    }

    private static void loadRemote(Context context, Set<String> domains) {
        File file = new File(context.getFilesDir(), REMOTE_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String domain = parseDomain(line);
                if (!domain.isEmpty()) domains.add(domain);
            }
        } catch (IOException ignored) {
        }
    }

    private static String parseDomain(String line) {
        if (line == null) return "";
        String trimmed = line.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()
                || trimmed.startsWith("#")
                || trimmed.startsWith("!")
                || trimmed.startsWith("@@")
                || trimmed.startsWith("[")
                || trimmed.startsWith("/")) return "";

        int comment = trimmed.indexOf('#');
        if (comment >= 0) trimmed = trimmed.substring(0, comment).trim();
        int optionStart = trimmed.indexOf('$');
        if (optionStart >= 0) trimmed = trimmed.substring(0, optionStart).trim();

        if (trimmed.startsWith("||")) {
            trimmed = trimmed.substring(2);
            int end = trimmed.indexOf('^');
            if (end >= 0) trimmed = trimmed.substring(0, end);
        } else if (trimmed.startsWith("|http://") || trimmed.startsWith("|https://")) {
            trimmed = trimmed.substring(1);
            int scheme = trimmed.indexOf("://");
            if (scheme >= 0) trimmed = trimmed.substring(scheme + 3);
            int slash = trimmed.indexOf('/');
            if (slash >= 0) trimmed = trimmed.substring(0, slash);
        } else if (trimmed.startsWith("0.0.0.0 ") || trimmed.startsWith("127.0.0.1 ")) {
            String[] parts = trimmed.split("\\s+");
            trimmed = parts.length > 1 ? parts[1] : "";
        } else if (trimmed.contains(" ")) {
            return "";
        }

        if (trimmed.equals("localhost") || trimmed.contains(":")) return "";
        if (trimmed.startsWith("*.")) trimmed = trimmed.substring(2);
        while (trimmed.startsWith(".")) trimmed = trimmed.substring(1);
        while (trimmed.endsWith(".")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.contains("*") || trimmed.contains("/") || trimmed.contains("^") || trimmed.contains("|")) return "";
        if (!trimmed.contains(".")) return "";
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-')) return "";
        }
        return trimmed;
    }

    private static String shortSourceName(String sourceUrl) {
        try {
            return new URL(sourceUrl).getHost();
        } catch (Exception ignored) {
            return sourceUrl;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class UpdateResult {
        public final boolean success;
        public final int count;
        public final String message;

        UpdateResult(boolean success, int count, String message) {
            this.success = success;
            this.count = count;
            this.message = message;
        }
    }
}

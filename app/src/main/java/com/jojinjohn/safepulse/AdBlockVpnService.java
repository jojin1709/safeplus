package com.jojinjohn.safepulse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdBlockVpnService extends VpnService implements Runnable {
    public static final String ACTION_START = "com.jojinjohn.safepulse.START";
    public static final String ACTION_STOP = "com.jojinjohn.safepulse.STOP";
    public static final String ACTION_RELOAD_BLOCKLIST = "com.jojinjohn.safepulse.RELOAD_BLOCKLIST";

    private static final String CHANNEL_ID = "blocking";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final String[] DOH_DOMAINS = {
            "cloudflare-dns.com",
            "dns.google",
            "dns.quad9.net",
            "doh.opendns.com",
            "mozilla.cloudflare-dns.com",
            "dns.nextdns.io",
            "dns.adguard-dns.com",
            "doh.cleanbrowsing.org",
            "security.cloudflare-dns.com"
    };
    private static final String[] AGGRESSIVE_DOMAINS = {
            "ads.youtube.com",
            "s.youtube.com",
            "ad.doubleclick.net",
            "adservice.google.com",
            "adservice.google.co.in",
            "googleads.g.doubleclick.net",
            "googleads4.g.doubleclick.net",
            "pubads.g.doubleclick.net",
            "securepubads.g.doubleclick.net",
            "stats.g.doubleclick.net",
            "fls.doubleclick.net",
            "partnerad.l.doubleclick.net",
            "pagead-googlehosted.l.google.com",
            "pagead2.googlesyndication.com",
            "pagead2.googleadservices.com",
            "pagead2.google.com",
            "tpc.googlesyndication.com",
            "googlesyndication.com",
            "googletagservices.com",
            "googletagservices.l.google.com",
            "ade.googlesyndication.com",
            "imasdk.googleapis.com",
            "static.doubleclick.net",
            "video-stats.l.google.com",
            "fundingchoices.google.com",
            "fundingchoicesmessages.google.com",
            "mobileads.google.com",
            "googleadapis.l.google.com",
            "amazon-adsystem.com",
            "media.net",
            "adnxs.com",
            "adsrvr.org",
            "criteo.com",
            "criteo.net",
            "taboola.com",
            "outbrain.com",
            "rubiconproject.com",
            "openx.net",
            "pubmatic.com",
            "yieldmo.com",
            "lijit.com",
            "sharethrough.com",
            "indexww.com",
            "casalemedia.com",
            "doubleverify.com",
            "mgid.com",
            "adcolony.com",
            "unityads.unity3d.com",
            "vungle.com",
            "applovin.com",
            "ironsrc.com",
            "admob.com",
            "adsafeprotected.com",
            "moatads.com",
            "innovid.com",
            "serving-sys.com"
    };
    private static final String[] AGGRESSIVE_LABELS = {
            "ad",
            "ads",
            "adservice",
            "adserver",
            "adservers",
            "adsystem",
            "adtech",
            "advert",
            "advertising",
            "banner",
            "banners",
            "sponsor",
            "sponsors"
    };
    private static final String[] STRICT_YOUTUBE_DOMAINS = {
            "googlevideo.com",
            "youtubei.googleapis.com",
            "youtube.googleapis.com",
            "youtube-nocookie.com",
            "ytimg.com",
            "yt3.ggpht.com"
    };
    private static final String[] TRACKER_DOMAINS = {
            "google-analytics.com",
            "analytics.google.com",
            "googletagmanager.com",
            "app-measurement.com",
            "firebase-settings.crashlytics.com",
            "crashlytics.com",
            "scorecardresearch.com",
            "quantserve.com",
            "hotjar.com",
            "segment.io",
            "mixpanel.com",
            "amplitude.com",
            "braze.com",
            "kochava.com",
            "datadoghq-browser-agent.com",
            "browser-intake-datadoghq.com",
            "sentry.io",
            "bugsnag.com",
            "newrelic.com",
            "facebook.net",
            "connect.facebook.net"
    };
    private static final String[] TRACKER_LABELS = {
            "analytics",
            "telemetry",
            "tracking",
            "tracker",
            "metrics",
            "measurement",
            "collect",
            "pixel",
            "beacon",
            "datadog",
            "sentry"
    };
    private static final String[] CONSENT_DOMAINS = {
            "onetrust.com",
            "cookielaw.org",
            "cookiebot.com",
            "consentmanager.net",
            "consensu.org",
            "cmp.inmobi.com",
            "privacy-mgmt.com",
            "trustarc.com",
            "truste.com",
            "usercentrics.eu",
            "didomi.io",
            "iubenda.com",
            "sourcepointcmp.com",
            "privacy-center.org",
            "consentframework.com"
    };
    private static final String[] CONSENT_LABELS = {
            "consent",
            "cmp",
            "cookieconsent",
            "cookielaw",
            "cookiebot",
            "privacy"
    };
    private static final String[] REDIRECT_DOMAINS = {
            "clickserve.dartsearch.net",
            "googleadservices.com",
            "linksynergy.com",
            "awin1.com",
            "anrdoezrs.net",
            "tkqlhce.com",
            "jdoqocy.com",
            "dpbolvw.net",
            "emjcd.com",
            "qksrv.net",
            "skimresources.com",
            "redirectingat.com",
            "app.adjust.com",
            "appsflyer.com",
            "branch.io",
            "onelink.me"
    };
    private static final String[] REDIRECT_LABELS = {
            "click",
            "clk",
            "clicktrack",
            "redirect",
            "redir",
            "outbound"
    };

    private ParcelFileDescriptor vpnInterface;
    private Thread worker;
    private volatile Set<String> blocklist = new HashSet<>();
    private int notificationTick = 0;

    public static boolean isRunning() {
        return RUNNING.get();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;
        if (ACTION_STOP.equals(action)) {
            stopBlocking();
            return START_NOT_STICKY;
        }
        if (ACTION_RELOAD_BLOCKLIST.equals(action)) {
            if (RUNNING.get()) {
                blocklist = BlocklistManager.loadEffectiveBlocklist(this);
                updateNotification();
                return START_STICKY;
            }
            return START_NOT_STICKY;
        }

        if (RUNNING.compareAndSet(false, true)) {
            AppSettings.migrateDefaults(this);
            createNotificationChannel();
            startForeground(1, buildNotification());
            blocklist = BlocklistManager.loadEffectiveBlocklist(this);
            maybeUpdateBlocklist();
            StatsStore.markStarted(this);
            startVpn();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopBlocking();
        super.onDestroy();
    }

    private void startVpn() {
        try {
            Builder builder = new Builder()
                    .setSession("SafePulse")
                    .addAddress("10.111.0.2", 32)
                    .addDnsServer("10.111.0.1")
                    .addRoute("10.111.0.1", 32)
                    .allowFamily(android.system.OsConstants.AF_INET);

            try {
                builder.addAddress("fd00:111:0:0:0:0:0:2", 128)
                        .addDnsServer("fd00:111:0:0:0:0:0:1")
                        .addRoute("fd00:111:0:0:0:0:0:1", 128)
                        .allowFamily(android.system.OsConstants.AF_INET6);
            } catch (Exception ignored) {
                // Some Android builds reject local IPv6 VPN DNS addresses. IPv4 DNS remains active.
            }

            applyBypassPackages(builder);
            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                throw new IOException("VPN interface was not created");
            }

            worker = new Thread(this, "safepulse-dns-vpn");
            worker.start();
        } catch (Exception e) {
            stopBlocking();
        }
    }

    private void applyBypassPackages(Builder builder) {
        for (String packageName : AppSettings.getBypassPackages(this)) {
            try {
                builder.addDisallowedApplication(packageName);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
    }

    @Override
    public void run() {
        if (vpnInterface == null) return;

        byte[] buffer = new byte[32767];
        try (
                FileInputStream input = new FileInputStream(vpnInterface.getFileDescriptor());
                java.io.FileOutputStream output = new java.io.FileOutputStream(vpnInterface.getFileDescriptor())
        ) {
            while (RUNNING.get()) {
                int length = input.read(buffer);
                if (length > 0) {
                    byte[] response = handlePacket(buffer, length);
                    if (response != null) {
                        output.write(response);
                    }
                }
            }
        } catch (IOException ignored) {
            stopBlocking();
        }
    }

    private byte[] handlePacket(byte[] packet, int length) {
        if (length < 28) return null;
        int version = (packet[0] >> 4) & 0x0F;
        if (version == 4) return handleIpv4Packet(packet, length);
        if (version == 6) return handleIpv6Packet(packet, length);
        return null;
    }

    private byte[] handleIpv4Packet(byte[] packet, int length) {
        int ipHeaderLength = (packet[0] & 0x0F) * 4;
        if (length < ipHeaderLength + 8 || packet[9] != 17) return null;

        int udpOffset = ipHeaderLength;
        int srcPort = readShort(packet, udpOffset);
        int dstPort = readShort(packet, udpOffset + 2);
        if (dstPort != 53) return null;

        int dnsOffset = udpOffset + 8;
        int dnsLength = length - dnsOffset;
        if (dnsLength < 12) return null;

        String host = parseQuestionHost(packet, dnsOffset, dnsLength);
        String blockReason = blockReason(host);
        boolean blocked = blockReason != null;
        byte[] dnsResponse;
        if (blocked) {
            StatsStore.recordBlocked(this, host, blockReason);
            updateNotificationSoon(true);
            dnsResponse = buildBlockedDnsResponse(packet, dnsOffset, dnsLength);
        } else {
            if (host != null && !host.isEmpty()) {
                StatsStore.recordAllowed(this);
                updateNotificationSoon(false);
            }
            dnsResponse = forwardDns(packet, dnsOffset, dnsLength);
        }

        if (dnsResponse == null) return null;
        return wrapUdpResponse(packet, ipHeaderLength, srcPort, dnsResponse);
    }

    private byte[] handleIpv6Packet(byte[] packet, int length) {
        int ipHeaderLength = 40;
        if (length < ipHeaderLength + 8 || packet[6] != 17) return null;

        int udpOffset = ipHeaderLength;
        int srcPort = readShort(packet, udpOffset);
        int dstPort = readShort(packet, udpOffset + 2);
        if (dstPort != 53) return null;

        int dnsOffset = udpOffset + 8;
        int dnsLength = length - dnsOffset;
        if (dnsLength < 12) return null;

        String host = parseQuestionHost(packet, dnsOffset, dnsLength);
        String blockReason = blockReason(host);
        boolean blocked = blockReason != null;
        byte[] dnsResponse;
        if (blocked) {
            StatsStore.recordBlocked(this, host, blockReason);
            updateNotificationSoon(true);
            dnsResponse = buildBlockedDnsResponse(packet, dnsOffset, dnsLength);
        } else {
            if (host != null && !host.isEmpty()) {
                StatsStore.recordAllowed(this);
                updateNotificationSoon(false);
            }
            dnsResponse = forwardDns(packet, dnsOffset, dnsLength);
        }

        if (dnsResponse == null) return null;
        return wrapUdp6Response(packet, srcPort, dnsResponse);
    }

    private String blockReason(String host) {
        return blockReasonForHost(this, blocklist, host);
    }

    public static String blockReasonForHost(android.content.Context context, Set<String> blocklist, String host) {
        if (host == null || host.isEmpty()) return null;
        String name = host.toLowerCase(Locale.ROOT);
        if (AppSettings.isAllowed(context, name)) return null;
        if (AppSettings.isDohGuardEnabled(context) && isKnownDohHost(name)) return "Encrypted DNS";
        if (AppSettings.isNeverConsentEnabled(context) && isNeverConsentHost(name)) return "Never-consent";
        if (AppSettings.isRedirectProtectionEnabled(context) && isRedirectHost(name)) return "Redirect protection";
        if (AppSettings.isAntiTrackingEnabled(context) && isTrackingHost(name)) return "Anti-tracking";
        if (AppSettings.isStrictYoutubeEnabled(context) && isStrictYoutubeHost(name)) return "Strict YouTube";
        if (AppSettings.isAggressiveBlockingEnabled(context) && isAggressiveAdHost(name)) return "Ads / tracker";
        while (!name.isEmpty()) {
            if (blocklist != null && blocklist.contains(name)) return "Blocklist";
            int dot = name.indexOf('.');
            if (dot < 0) break;
            name = name.substring(dot + 1);
        }
        return null;
    }

    private static boolean isAggressiveAdHost(String host) {
        return matchesModuleHost(host, AGGRESSIVE_DOMAINS, AGGRESSIVE_LABELS);
    }

    private static boolean isStrictYoutubeHost(String host) {
        return matchesModuleHost(host, STRICT_YOUTUBE_DOMAINS, new String[0]);
    }

    private static boolean isTrackingHost(String host) {
        return matchesModuleHost(host, TRACKER_DOMAINS, TRACKER_LABELS);
    }

    private static boolean isNeverConsentHost(String host) {
        return matchesModuleHost(host, CONSENT_DOMAINS, CONSENT_LABELS);
    }

    private static boolean isRedirectHost(String host) {
        return matchesModuleHost(host, REDIRECT_DOMAINS, REDIRECT_LABELS);
    }

    private static boolean matchesModuleHost(String host, String[] domains, String[] labelsToMatch) {
        String name = host.toLowerCase(Locale.ROOT);
        while (!name.isEmpty()) {
            for (String domain : domains) {
                if (name.equals(domain)) return true;
            }
            int dot = name.indexOf('.');
            if (dot < 0) break;
            name = name.substring(dot + 1);
        }

        String[] labels = host.split("\\.");
        for (String label : labels) {
            for (String labelToMatch : labelsToMatch) {
                if (label.equals(labelToMatch)
                        || label.startsWith(labelToMatch + "-")
                        || label.startsWith(labelToMatch + "_")
                        || label.endsWith("-" + labelToMatch)
                        || label.endsWith("_" + labelToMatch)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isKnownDohHost(String host) {
        String name = host.toLowerCase(Locale.ROOT);
        while (!name.isEmpty()) {
            for (String dohDomain : DOH_DOMAINS) {
                if (name.equals(dohDomain)) return true;
            }
            int dot = name.indexOf('.');
            if (dot < 0) break;
            name = name.substring(dot + 1);
        }
        return false;
    }

    private byte[] forwardDns(byte[] packet, int dnsOffset, int dnsLength) {
        byte[] query = new byte[dnsLength];
        System.arraycopy(packet, dnsOffset, query, 0, dnsLength);

        for (String dnsServer : AppSettings.getDnsServers(this)) {
            try (DatagramSocket socket = new DatagramSocket()) {
                protect(socket);
                socket.setSoTimeout(3000);
                InetAddress upstream = InetAddress.getByName(dnsServer);
                socket.send(new DatagramPacket(query, query.length, upstream, 53));

                byte[] response = new byte[8192];
                DatagramPacket responsePacket = new DatagramPacket(response, response.length);
                socket.receive(responsePacket);
                byte[] exact = new byte[responsePacket.getLength()];
                System.arraycopy(response, 0, exact, 0, exact.length);
                if (isTruncatedDnsResponse(exact)) {
                    byte[] tcpResponse = forwardDnsTcp(query, upstream);
                    if (tcpResponse != null) return tcpResponse;
                }
                return exact;
            } catch (IOException ignored) {
            }
        }

        return buildServerFailure(packet, dnsOffset, dnsLength);
    }

    private byte[] forwardDnsTcp(byte[] query, InetAddress upstream) {
        try (Socket socket = new Socket()) {
            protect(socket);
            socket.connect(new InetSocketAddress(upstream, 53), 3000);
            socket.setSoTimeout(5000);

            OutputStream output = socket.getOutputStream();
            output.write((query.length >> 8) & 0xFF);
            output.write(query.length & 0xFF);
            output.write(query);
            output.flush();

            InputStream input = socket.getInputStream();
            int high = input.read();
            int low = input.read();
            if (high < 0 || low < 0) return null;
            int responseLength = (high << 8) | low;
            if (responseLength <= 0) return null;

            byte[] response = new byte[responseLength];
            int offset = 0;
            while (offset < responseLength) {
                int read = input.read(response, offset, responseLength - offset);
                if (read < 0) return null;
                offset += read;
            }
            return response;
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean isTruncatedDnsResponse(byte[] response) {
        return response != null && response.length >= 3 && (response[2] & 0x02) != 0;
    }

    private byte[] buildBlockedDnsResponse(byte[] packet, int dnsOffset, int dnsLength) {
        byte[] response = new byte[dnsLength];
        System.arraycopy(packet, dnsOffset, response, 0, dnsLength);
        response[2] = (byte) 0x81;
        response[3] = (byte) 0x83; // NXDOMAIN
        response[6] = 0;
        response[7] = 0;
        return response;
    }

    private byte[] buildServerFailure(byte[] packet, int dnsOffset, int dnsLength) {
        byte[] response = new byte[dnsLength];
        System.arraycopy(packet, dnsOffset, response, 0, dnsLength);
        response[2] = (byte) 0x81;
        response[3] = (byte) 0x82;
        return response;
    }

    private byte[] wrapUdpResponse(byte[] request, int ipHeaderLength, int originalSrcPort, byte[] dnsResponse) {
        int totalLength = ipHeaderLength + 8 + dnsResponse.length;
        byte[] response = new byte[totalLength];

        System.arraycopy(request, 0, response, 0, ipHeaderLength);
        response[2] = (byte) (totalLength >> 8);
        response[3] = (byte) totalLength;
        response[8] = 64;
        response[10] = 0;
        response[11] = 0;

        for (int i = 0; i < 4; i++) {
            response[12 + i] = request[16 + i];
            response[16 + i] = request[12 + i];
        }

        int udpOffset = ipHeaderLength;
        writeShort(response, udpOffset, 53);
        writeShort(response, udpOffset + 2, originalSrcPort);
        writeShort(response, udpOffset + 4, 8 + dnsResponse.length);
        writeShort(response, udpOffset + 6, 0);
        System.arraycopy(dnsResponse, 0, response, udpOffset + 8, dnsResponse.length);

        writeShort(response, 10, ipChecksum(response, 0, ipHeaderLength));
        return response;
    }

    private byte[] wrapUdp6Response(byte[] request, int originalSrcPort, byte[] dnsResponse) {
        int ipHeaderLength = 40;
        int udpLength = 8 + dnsResponse.length;
        int totalLength = ipHeaderLength + udpLength;
        byte[] response = new byte[totalLength];

        System.arraycopy(request, 0, response, 0, ipHeaderLength);
        response[4] = (byte) (udpLength >> 8);
        response[5] = (byte) udpLength;
        response[6] = 17;
        response[7] = 64;

        System.arraycopy(request, 24, response, 8, 16);
        System.arraycopy(request, 8, response, 24, 16);

        int udpOffset = ipHeaderLength;
        writeShort(response, udpOffset, 53);
        writeShort(response, udpOffset + 2, originalSrcPort);
        writeShort(response, udpOffset + 4, udpLength);
        writeShort(response, udpOffset + 6, 0);
        System.arraycopy(dnsResponse, 0, response, udpOffset + 8, dnsResponse.length);
        writeShort(response, udpOffset + 6, udpChecksumIpv6(response, udpOffset, udpLength));
        return response;
    }

    private String parseQuestionHost(byte[] data, int dnsOffset, int dnsLength) {
        StringBuilder host = new StringBuilder();
        int cursor = dnsOffset + 12;
        int end = dnsOffset + dnsLength;

        while (cursor < end) {
            int partLength = data[cursor++] & 0xFF;
            if (partLength == 0) break;
            if ((partLength & 0xC0) != 0 || cursor + partLength > end) return null;
            if (host.length() > 0) host.append('.');
            host.append(new String(data, cursor, partLength, StandardCharsets.US_ASCII));
            cursor += partLength;
        }
        return host.toString();
    }

    private void maybeUpdateBlocklist() {
        if (!AppSettings.isAutoUpdateEnabled(this) || !BlocklistManager.shouldAutoUpdate(this)) return;
        new Thread(() -> {
            BlocklistManager.UpdateResult result = BlocklistManager.updateFromSource(this);
            if (result.success) {
                blocklist = BlocklistManager.loadEffectiveBlocklist(this);
                updateNotification();
            }
        }, "safepulse-blocklist-update").start();
    }

    private void stopBlocking() {
        RUNNING.set(false);
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException ignored) {
            }
            vpnInterface = null;
        }
        stopForeground(true);
        StatsStore.clearStopped(this);
        stopSelf();
    }

    private Notification buildNotification() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        Intent stopIntent = new Intent(this, AdBlockVpnService.class).setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        StatsStore.Snapshot stats = StatsStore.snapshot(this);
        return builder
                .setContentTitle("SafePulse")
                .setContentText("Blocked " + stats.blocked + " of " + stats.checked + " DNS requests")
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.safepulse_logo))
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotificationSoon(boolean force) {
        notificationTick++;
        if (force || notificationTick % 25 == 0) {
            updateNotification();
        }
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null && RUNNING.get()) {
            manager.notify(1, buildNotification());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Blocking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private static int readShort(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 2).getShort() & 0xFFFF;
    }

    private static void writeShort(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 8);
        data[offset + 1] = (byte) value;
    }

    private static int ipChecksum(byte[] data, int offset, int length) {
        long sum = 0;
        for (int i = offset; i < offset + length; i += 2) {
            int word = (data[i] & 0xFF) << 8;
            if (i + 1 < offset + length) word |= data[i + 1] & 0xFF;
            sum += word;
            while ((sum >> 16) != 0) {
                sum = (sum & 0xFFFF) + (sum >> 16);
            }
        }
        return (int) (~sum) & 0xFFFF;
    }

    private static int udpChecksumIpv6(byte[] packet, int udpOffset, int udpLength) {
        long sum = 0;
        sum = addWords(sum, packet, 8, 16);
        sum = addWords(sum, packet, 24, 16);
        sum += (udpLength >> 16) & 0xFFFF;
        sum += udpLength & 0xFFFF;
        sum += 17;
        sum = addWords(sum, packet, udpOffset, udpLength);
        while ((sum >> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        int checksum = (int) (~sum) & 0xFFFF;
        return checksum == 0 ? 0xFFFF : checksum;
    }

    private static long addWords(long sum, byte[] data, int offset, int length) {
        for (int i = offset; i < offset + length; i += 2) {
            int word = (data[i] & 0xFF) << 8;
            if (i + 1 < offset + length) word |= data[i + 1] & 0xFF;
            sum += word;
            while ((sum >> 16) != 0) {
                sum = (sum & 0xFFFF) + (sum >> 16);
            }
        }
        return sum;
    }
}

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
            "security.cloudflare-dns.com",
            "dns64.quad9.net",
            "doh.dns.samsung.com",
            "dns-family.adguard.com",
            "dns-unfiltered.adguard.com",
            "dns.alidns.com",
            "doh.pub",
            "dot.pub",
            "doh.iqilin.com",
            "dns.ipv6dns.com",
            "skydns.ru",
            "dns-family.starlink.com",
            "odvr.nic.cz",
            "dns.piaservices.net",
            "dns.dnswarden.com"
    };
    private static final String[] AGGRESSIVE_DOMAINS = {
            // Google Ads & YouTube
            "ads.youtube.com",
            "s.youtube.com",
            "ad.doubleclick.net",
            "googleads.g.doubleclick.net",
            "googleads4.g.doubleclick.net",
            "pubads.g.doubleclick.net",
            "securepubads.g.doubleclick.net",
            "stats.g.doubleclick.net",
            "fls.doubleclick.net",
            "partnerad.l.doubleclick.net",
            "static.doubleclick.net",
            "adservice.google.com",
            "adservice.google.co.in",
            "adservice.google.com.tw",
            "adservice.google.com.hk",
            "adservice.google.com.sg",
            "adservice.google.com.my",
            "adservice.google.com.ph",
            "adservice.google.com.vn",
            "adservice.google.com.pk",
            "adservice.google.com.bd",
            "adservice.google.com.ua",
            "adservice.google.com.pl",
            "adservice.google.com.br",
            "adservice.google.com.mx",
            "adservice.google.com.ar",
            "adservice.google.com.tr",
            "adservice.google.com.sa",
            "adservice.google.com.ae",
            "adservice.google.com.ng",
            "adservice.google.com.ke",
            "adservice.google.com.eg",
            "adservice.google.com.za",
            "pagead-googlehosted.l.google.com",
            "pagead2.googlesyndication.com",
            "pagead2.googleadservices.com",
            "pagead2.google.com",
            "pagead.l.google.com",
            "tpc.googlesyndication.com",
            "googlesyndication.com",
            "ade.googlesyndication.com",
            "googletagservices.com",
            "googletagservices.l.google.com",
            "www-googletagmanager.l.google.com",
            "imasdk.googleapis.com",
            "video-stats.l.google.com",
            "fundingchoices.google.com",
            "fundingchoicesmessages.google.com",
            "mobileads.google.com",
            "googleadapis.l.google.com",
            "encrypted-tbn0.gstatic.com",
            "jnn-pa.googleapis.com",
            "play-lh.googleusercontent.com",
            "yt3.l.google.com",
            "yt4.l.google.com",
            "youtube-ui.l.google.com",
            "wide-youtube.l.google.com",
            "s2.youtube.com",
            "jnn-pa.googleapis.com",
            "play-lh.googleusercontent.com",
            "yt3.l.google.com",
            "yt4.l.google.com",
            "encrypted-tbn0.gstatic.com",
            "video-stats.l.google.com",
            "youtube-ui.l.google.com",
            "yt3.ggpht.com",
            "yt3.googleusercontent.com",
            "i.ytimg.com",
            "i9.ytimg.com",
            "www.youtube.com/s/player",
            "youtube.com/videoplayback",
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com",
            "pagead2.googleadservices.com",
            "tpc.googlesyndication.com",
            "googlesyndication.com",
            "ade.googlesyndication.com",
            "imasdk.googleapis.com",
            "fundingchoices.google.com",
            "fundingchoicesmessages.google.com",
            "youtube.com/api/stats/ads",
            "youtube.com/api/stats/qoe",
            "youtube.com/get_video_info",
            "youtube.com/ptracking",
            "youtube.com/youtubei/v1/log_event",
            "youtube.com/s/player",
            "play.google.com/log",
            "play.google.com/gen204",
            // Facebook / Meta
            "facebook.net",
            "connect.facebook.net",
            "fbcdn.net",
            "graph.facebook.com",
            // Major Ad Networks
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
            "popads.net",
            "propellerads.com",
            "exoclick.com",
            "bidswitch.net",
            "sonobi.com",
            "teads.com",
            "adition.com",
            "smartadserver.com",
            "mathtag.com",
            "turn.com",
            "adform.com",
            "bluekai.com",
            "exelator.com",
            "rlcdn.com",
            "demdex.net",
            "everesttech.net",
            "adsymptotic.com",
            "tapad.com",
            "mookie1.com",
            "bidgear.com",
            "ad-maven.com",
            "fyber.com",
            "startapp.com",
            "inmobi.com",
            "smaato.net",
            "adskeeper.com",
            "revcontent.com",
            "nativo.com",
            "spotxchange.com",
            "jivox.com",
            "brightcom.com",
            "simpli.fi",
            "matomy.com",
            "traffective.com",
            "adnium.com",
            "adcash.com",
            "hilltopads.com",
            "onclickads.net",
            "adreactor.com",
            "adroll.com",
            "adstream.com",
            "adtechus.com",
            "advertising.com",
            "tribalfusion.com",
            "valueclick.com",
            "adsafeprotected.com",
            "moatads.com",
            "innovid.com",
            "serving-sys.com",
            // Mobile Ad SDKs
            "applovin.com",
            "unityads.unity3d.com",
            "vungle.com",
            "ironsrc.com",
            "supersonicads.com",
            "chartboost.com",
            "admob.com",
            // Push / notification ad platforms
            "pushwoosh.com",
            "onesignal.com",
            "notifpush.com",
            "pushengage.com",
            "pushnami.com",
            "aimtell.com",
            "pushowl.com",
            "subscribers.com"
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
            "sponsors",
            "promo",
            "promos",
            "promotion",
            "campaign",
            "sponsorship",
            "adslot",
            "adunit",
            "adzone",
            "adwall",
            "adpush",
            "admixer",
            "adzerk",
            "adglare",
            "adform",
            "admarketplace",
            "adroll",
            "adsterra",
            "admob",
            "adsense",
            "adwords",
            "pagead",
            "pubads",
            "pubad",
            "videoad",
            "preroll",
            "midroll",
            "postroll",
            "skippable",
            "unskippable",
            "companion",
            "overlay",
            "masthead",
            "bumper",
            "retarget",
            "remarketing",
            "impression",
            "clicktrack",
            "affiliate"
    };
    private static final String[] STRICT_YOUTUBE_DOMAINS = {
            "googlevideo.com",
            "youtubei.googleapis.com",
            "youtube.googleapis.com",
            "youtube-nocookie.com",
            "ytimg.com",
            "yt3.ggpht.com",
            "jnn-pa.googleapis.com",
            "play-lh.googleusercontent.com",
            "i.ytimg.com",
            "i9.ytimg.com",
            "s.youtube.com",
            "s2.youtube.com",
            "video-stats.l.google.com",
            "youtube-ui.l.google.com",
            "wide-youtube.l.google.com",
            "yt3.l.google.com",
            "yt4.l.google.com",
            "rr5---sn-a5msenl6.googlevideo.com",
            "rr1---sn-a5msenl6.googlevideo.com",
            "rr3---sn-a5msenl6.googlevideo.com"
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
            "connect.facebook.net",
            "fullstory.com",
            "crazyegg.com",
            "mouseflow.com",
            "luckyorange.com",
            "heap.io",
            "pendo.io",
            "statsig.com",
            "posthog.com",
            "matomo.org",
            "optimizely.com",
            "convert.com",
            "vwo.com",
            "abtasty.com",
            "inspectlet.com",
            "usabilla.com",
            "clarity.ms",
            "smartlook.com",
            "ptengine.com",
            "contentsquare.com",
            "quantummetric.com",
            "blueconic.net",
            "tealiumiq.com",
            "launchdarkly.com",
            "rudderstack.com",
            "mparticle.com",
            "lytics.io",
            "treasuredata.com",
            "zeotap.com",
            "liftoff.io",
            "liveramp.com",
            "branch.io",
            "appsflyer.com",
            "adjust.com",
            "singular.net",
            "flurry.com",
            "chartboost.com",
            "leanplum.com",
            "airship.com",
            "batch.com",
            "urbanairship.com",
            "netmera.com"
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
            "sentry",
            "stats",
            "insights",
            "monitor",
            "logger",
            "report",
            "log",
            "events",
            "event",
            "hit",
            "impression",
            "view",
            "engage",
            "tag",
            "tags",
            "gtm",
            "ga",
            "mixpanel",
            "amplitude",
            "segment",
            "heap",
            "pendo",
            "hotjar",
            "clarity",
            "fullstory"
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
            "consentframework.com",
            "termly.io",
            "cookieyes.com",
            "cookiepro.com",
            "quantcast.com",
            "quantcast.mgr.consensu.org",
            "gdpr.eu",
            "cookie-information.com",
            "complianz.io",
            "borlabs.io",
            "european-privacy.eu"
    };
    private static final String[] CONSENT_LABELS = {
            "consent",
            "cmp",
            "cookieconsent",
            "cookielaw",
            "cookiebot",
            "privacy",
            "gdpr",
            "ccpa",
            "privacycenter",
            "privacypolicy",
            "cookienotice",
            "cookiemanager",
            "consentmanager",
            "consentbanner"
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
            "onelink.me",
            "go2cloud.org",
            "clickmeter.com",
            "trk.rs",
            "bit.ly",
            "tinyurl.com",
            "t.co",
            "bitly.com",
            "rebrand.ly",
            "shorte.st",
            "bc.vc",
            "adf.ly",
            "sh.st",
            "ouo.io",
            "bluenik.com",
            "partage.mobi",
            "dl-protect.com"
    };
    private static final String[] REDIRECT_LABELS = {
            "click",
            "clk",
            "clicktrack",
            "redirect",
            "redir",
            "outbound",
            "go",
            "link",
            "ref",
            "affiliate",
            "track",
            "trk",
            "jump",
            "goto"
    };
    private static final String[] ADULT_DOMAINS = {
            "pornhub.com",
            "xvideos.com",
            "xnxx.com",
            "xhamster.com",
            "redtube.com",
            "youporn.com",
            "tube8.com",
            "spankbang.com",
            "beeg.com",
            "brazzers.com",
            "bangbros.com",
            "realitykings.com",
            "mofos.com",
            "naughtyamerica.com",
            "digitalplayground.com",
            "teamskeet.com",
            "blacked.com",
            "vixen.com",
            "tushy.com",
            "deeper.com",
            "onlyfans.com",
            "fansly.com",
            "chaturbate.com",
            "livejasmin.com",
            "stripchat.com",
            "bongacams.com",
            "myfreecams.com",
            "cam4.com",
            "flirt4free.com",
            "camsoda.com",
            "xhamsterlive.com",
            "spankchat.com",
            "adultfriendfinder.com",
            "ashleymadison.com",
            "fetlife.com",
            "sex.com",
            "porntube.com",
            "drtuber.com",
            "eporner.com",
            "txxx.com",
            "hclips.com",
            "hdzog.com",
            "vjav.com",
            "sxyprn.com",
            "pornzog.com",
            "txxx.com",
            "playvids.com",
            "porndig.com",
            "fuq.com",
            "thumbzilla.com",
            "tnaflix.com",
            "porntrex.com",
            "xcafe.com",
            "anysex.com",
            "4tube.com",
            "daftsex.com",
            "hotmovs.com",
            "seexxx.com",
            "okporn.com",
            "porn0sex.net",
            "pornone.com",
            "pornhat.com",
            "porn.com",
            "xgroovy.com",
            "xxxymovies.com",
            "ashemaletube.com",
            "trannyshemale.com",
            "shemalez.com",
            " tranny tube",
            " Shemalez",
            "gotporn.com",
            "youjizz.com",
            "youporn.to",
            "motherless.com",
            "heavy-r.com",
            "efukt.com",
            "bestgore.com",
            "theync.com",
            "zoo.com",
            "porndoe.com",
            "porndig.com",
            "porngo.com",
            "tubegalore.com",
            "mrdeepfakes.com",
            "fapello.com",
            "thothub.tv",
            "simpcity.su",
            "leakedbb.com",
            "coomer.kiwi",
            "kemono.su",
            "nudostar.com",
            "aznude.com",
            "celebjihad.com",
            "celebsecrets.com",
            "mrdeepfakes.com",
            "pornpen.ai",
            "aiporn.net",
            "deepnude.com"
    };

    private ParcelFileDescriptor vpnInterface;
    private Thread worker;
    private volatile Set<String> blocklist = new HashSet<>();
    private int notificationTick = 0;
    private volatile boolean blocklistUpdating = false;
    private android.graphics.Bitmap cachedLargeIcon;

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
            StatsStore.loadFromDisk(this);
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

        byte[] buffer = new byte[65535];
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
        if (ipHeaderLength < 20 || length < ipHeaderLength + 8 || packet[9] != 17) return null;

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
        if (AppSettings.isPaused(this) || AppSettings.isInScheduledPause(this)) return null;
        return blockReasonForHost(this, blocklist, host);
    }

    public static String blockReasonForHost(android.content.Context context, Set<String> blocklist, String host) {
        if (host == null || host.isEmpty()) return null;
        String name = host.toLowerCase(Locale.ROOT);
        if (AppSettings.isAllowed(context, name)) return null;
        if (AppSettings.isDohGuardEnabled(context) && isKnownDohHost(name)) return "Encrypted DNS";
        if (AppSettings.isNeverConsentEnabled(context) && isNeverConsentHost(name)) return "Never-consent";
        if (AppSettings.isRedirectProtectionEnabled(context) && isRedirectHost(name)) return "Redirect protection";
        if (AppSettings.isAdultBlockEnabled(context) && isAdultBlockHost(name)) return "Adult content";
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

    private static boolean isAdultBlockHost(String host) {
        return matchesModuleHost(host, ADULT_DOMAINS, new String[0]);
    }

    private static boolean matchesModuleHost(String host, String[] domains, String[] labelsToMatch) {
        String name = host.toLowerCase(Locale.ROOT);
        while (!name.isEmpty()) {
            for (String domain : domains) {
                if (domain.contains("*")) {
                    if (matchesWildcard(name, domain)) return true;
                } else if (name.equals(domain)) {
                    return true;
                }
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

    private static boolean matchesWildcard(String host, String pattern) {
        int starIndex = pattern.indexOf('*');
        if (starIndex < 0) return host.equals(pattern);
        String prefix = pattern.substring(0, starIndex);
        String suffix = pattern.substring(starIndex + 1);
        if (!prefix.isEmpty() && !host.startsWith(prefix)) return false;
        if (!suffix.isEmpty() && !host.endsWith(suffix)) return false;
        return true;
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
        if (blocklistUpdating) return;
        blocklistUpdating = true;
        new Thread(() -> {
            try {
                BlocklistManager.UpdateResult result = BlocklistManager.updateFromSource(this);
                if (result.success) {
                    blocklist = BlocklistManager.loadEffectiveBlocklist(this);
                    updateNotification();
                }
            } finally {
                blocklistUpdating = false;
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
        StatsStore.forceFlush(this);
        StatsStore.markStopped(this);
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    private Notification buildNotification() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE
        );
        Intent stopIntent = new Intent(this, AdBlockVpnService.class).setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        StatsStore.Snapshot stats = StatsStore.snapshot(this);
        if (cachedLargeIcon == null) {
            cachedLargeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.safepulse_logo);
        }
        return builder
                .setContentTitle("SafePulse")
                .setContentText("Blocked " + stats.blocked + " of " + stats.checked + " DNS requests")
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(cachedLargeIcon)
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

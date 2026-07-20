# SafePulse ProGuard Rules

# Keep VPN service
-keep class com.jojinjohn.safepulse.AdBlockVpnService { *; }
-keep class com.jojinjohn.safepulse.BootReceiver { *; }

# Keep MainActivity for intents
-keep class com.jojinjohn.safepulse.MainActivity { *; }

# Keep data classes used in serialization
-keep class com.jojinjohn.safepulse.StatsStore$* { *; }
-keep class com.jojinjohn.safepulse.BlocklistManager$* { *; }
-keep class com.jojinjohn.safepulse.AppSettings { *; }

# Suppress warnings for unused code
-dontwarn javax.annotation.**

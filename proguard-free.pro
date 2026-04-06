# ──────────────────────────────────────────────────────────────────
# ProGuard config for SovereignTraders Free (SpigotMC)
# Basic name obfuscation only — compliant with SpigotMC free rules.
# No string encryption · No control-flow obfuscation · No repackaging
# ──────────────────────────────────────────────────────────────────

# ── Core directives ──────────────────────────────────────────────
-dontshrink                # Keep all classes (no dead-code removal)
-dontoptimize              # No bytecode optimization
# -dontpreverify is NOT used: Java 7+ requires valid StackMapTable
# frames. Omitting preverification causes VerifyError at runtime
# when combined with control-flow obfuscation (Allatori).
-dontwarn **               # Suppress warnings (server runtime classes)
-ignorewarnings

# ── Keep package structure visible (SpigotMC requirement) ────────
# Free resources must retain a recognisable package root.
-keeppackagenames net.sovereign.**

# ── Paper / Bukkit plugin bootstrap ──────────────────────────────
-keep class net.sovereign.core.SovereignFreePlugin {
    public *;
}
-keep class net.sovereign.core.SovereignCore {
    public *;
}

# ── Bukkit event handlers (resolved reflectively at runtime) ─────
-keep class * implements org.bukkit.event.Listener {
    @org.bukkit.event.EventHandler <methods>;
}

# ── Bukkit command executors ─────────────────────────────────────
-keep class * implements org.bukkit.command.CommandExecutor {
    public *;
}
-keep class * implements org.bukkit.command.TabCompleter {
    public *;
}

# ── Enum classes (Kotlin enums use valueOf/entries via reflection)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** getEntries();
    <fields>;
}

# ── Shaded / relocated libraries — do NOT touch ──────────────────
-keep class net.sovereign.libs.** { *; }
-dontwarn net.sovereign.libs.**

# ── Kotlin internals ─────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.Metadata *;
}

# ── Annotations and attributes needed at runtime ─────────────────
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# ── Source file names for stack traces ────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

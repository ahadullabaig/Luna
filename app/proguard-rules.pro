# R8 rules for the release build.
#
# Most of what Luna depends on ships its own consumer rules inside the AAR — Compose,
# Room, Hilt/Dagger and the AndroidX libraries all do — so this file deliberately holds
# only the two things R8 cannot work out from this project's bytecode. Resist adding
# blanket `-keep class com.luna.app.**` rules: that would switch off shrinking for the
# whole app and make `isMinifyEnabled` a lie.

# --- Enum constants that are persisted by name -------------------------------------
#
# data/Converters.kt writes FlowLevel and Energy into SQLite with `.name` and reads
# them back with `valueOf`. The constant names are therefore part of the on-disk
# format, not an implementation detail. If R8 renamed HEAVY to `a`, every row written
# by an earlier build would throw IllegalArgumentException on read — a crash that only
# shows up in release, only on a device with existing data, which is the worst possible
# combination to debug. Pin the names.
-keepclassmembers enum com.luna.app.domain.model.FlowLevel { *; }
-keepclassmembers enum com.luna.app.domain.model.Energy { *; }

# --- kotlinx.serialization ----------------------------------------------------------
#
# ui/nav/Routes.kt declares the navigation routes as @Serializable objects, and
# Navigation Compose resolves them through their generated serializers. Those are only
# ever reached reflectively, so R8 sees them as unused and strips them, and the failure
# is a SerializationException at the first navigate() rather than at build time.
#
# These are the upstream-recommended rules, narrowed to the object case Luna actually
# uses: keep INSTANCE and serializer() on any @Serializable object.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Companion-based serializers, for when a route stops being an object and starts
# carrying arguments.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

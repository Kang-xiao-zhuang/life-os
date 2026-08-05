# LifeOS keeps all data locally, so there is nothing here beyond the defaults —
# Room and Compose ship their own consumer rules.

# ...except enum constant names, which are not just identifiers here: they are written into
# DataStore (theme, language) and into alarm Intent extras (ReminderKind), then read back and
# matched by string. R8 keeps them in practice, but "in practice" is not good enough for values
# that have to survive an app update: if the names ever shifted between two builds, the settings a
# user had chosen would silently reset to the defaults after upgrading.
-keepclassmembers enum com.zk.lifeos.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

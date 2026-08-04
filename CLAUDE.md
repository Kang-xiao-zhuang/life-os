# CLAUDE.md

## Project

LifeOS — a local-first Android personal workbench. Product spec: `docs/PRD.md` (authoritative;
read it before adding anything).

Not a todo app, not a note app, not a calendar. It manages 工作 / 学习 / 项目 / 习惯 / 成长.

Principles that actually constrain the code: **Local First · Offline First · No Login ·
Privacy First · Fast · Minimal · Export Friendly**. No cloud sync, no collaboration, no AI,
no accounts. The app declares **zero permissions** — keep it that way.

---

## Tech Stack

Native Android, Kotlin + Jetpack Compose. (The PRD's original React Native plan was dropped:
Android-only + local-only removes RN's cross-platform advantage, and Room/DataStore/zip are
cleaner natively.)

- Kotlin 2.3.21, Compose (BOM), Navigation Compose
- **State**: ViewModel + StateFlow (the Zustand equivalent)
- **Data**: Room → SQLite; DataStore Preferences for settings (replaces MMKV — no extra dep)
- **DI**: hand-rolled `AppContainer`, no Hilt/Koin (开发原则「不引入复杂依赖」)
- compileSdk / targetSdk 36, minSdk 26, JVM target 17
- Gradle 8.14.3, AGP 8.13.2

### ⚠️ Versions are pinned deliberately — do not "update to latest"

`gradle/libs.versions.toml` holds versions **older than the newest releases on purpose**.
Anything newer requires **AGP 9.1+ and compileSdk 37**, and this machine only has Android
platforms **28 / 36 / 36.1** installed. Bumping a library without installing platform 37 and
moving to AGP 9 breaks the build with "requires Android Gradle plugin 9.1.0 or higher".

To actually upgrade later: install platform 37 via SDK Manager, accept licenses, move to
Gradle 9.1.0 (already cached locally) + AGP 9.x, then raise compileSdk to 37.

---

## Architecture

The PRD mandates this direction, and it is enforced by package boundaries:

```
UI (Compose)  →  Service  →  Repository  →  Room / DataStore
```

**业务层不得直接操作数据库.** Only `data/repository/*` may touch a DAO or DataStore. Screens talk
to a ViewModel; ViewModels talk to a Service.

```
app/src/main/java/com/zk/lifeos/
├── LifeOsApplication.kt     # builds AppContainer
├── AppContainer.kt          # the whole object graph, lazy
├── MainActivity.kt          # single activity; themes the tree from the persisted setting
├── model/                   # shared models (ThemeMode, OverviewCounts) — no layer owns these
├── data/
│   ├── db/                  # LifeOsDatabase + entity/ + dao/
│   ├── prefs/               # AppPreferences (DataStore)
│   └── repository/          # the only place DAOs are used
├── service/                 # business layer the UI is allowed to call
└── ui/
    ├── theme/               # Color / Type / Theme — dark-first M3, no dynamic colour
    ├── navigation/          # TopLevelDestination (5 tabs) + Routes
    ├── components/          # shared composables
    ├── ViewModelFactories.kt
    └── screen/<feature>/    # Screen + ViewModel per feature
```

Bottom nav is exactly five: Dashboard / Projects / Capture / Habits / Journal, with **Capture in
the middle** (thumb reach). **Settings is not a tab** — it opens from the Dashboard top bar, and
the bottom bar hides on it.

---

## Status

**Phase 1 (项目初始化) done** — builds, installs, runs, verified on an emulator.

- Full V1 schema declared up front (6 tables: projects / tasks / habits / habit_checks /
  journal_entries / captures), so Phase 3 does not churn through Room migrations.
- Theme switching is the one fully working feature, on purpose: it exercises the entire
  UI → Service → Repository → DataStore chain, verified by killing the app and relaunching.
- Every screen except Dashboard/Settings is an explicit `PlaceholderScreen` that says what will
  live there. Dashboard shows a temporary "Phase 1 自检" card reading counts from SQLite.

Next: **Phase 2 (基础页面)** — real layouts for Dashboard / Projects / Tasks / Habits / Journal /
Capture. Then Phase 3 (核心功能), Phase 4 (数据管理: 导出/导入 + settings).

### 🔴 Must fix before the app holds real data

`LifeOsDatabase.build()` uses `fallbackToDestructiveMigration(dropAllTables = true)`. That is a
development convenience — **a schema change silently wipes everything**. Replace with real
migrations as part of Phase 4.

---

## Local Dev

```powershell
$env:JAVA_HOME="D:\Develop\android\jbr"          # Android Studio's bundled JDK 21
cd D:\Develop\idea_project\lifeos
.\gradlew.bat :app:assembleDebug
```

Emulator + install (SDK at `C:\Users\TT603064\AppData\Local\Android\sdk`):

```powershell
$sdk="C:\Users\TT603064\AppData\Local\Android\sdk"
& "$sdk\emulator\emulator.exe" -avd Resizable_Experimental -no-boot-anim   # android-37.1 x86_64
& "$sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
& "$sdk\platform-tools\adb.exe" shell am start -n com.zk.lifeos.debug/com.zk.lifeos.MainActivity
```

Debug builds install as **`com.zk.lifeos.debug`** (`applicationIdSuffix`), so debug and a future
release build can coexist on one device.

### Gotchas

- **Screenshots**: `adb exec-out screencap -p > file.png` **corrupts the file** in PowerShell —
  the binary stream gets text-encoded (BOM added). Use
  `adb shell screencap -p /sdcard/s.png` then `adb pull`.
- Editing files with a `Get-Content | Set-Content` pipeline destroys Chinese text (UTF-8 read as
  ANSI). Use proper file-editing tools.
- Compose `by` delegation on a `State` needs `import androidx.compose.runtime.getValue` — the
  error ("has no method getValue") does not name the missing import.
- The `-Xannotation-default-target=param-property` compiler arg in `app/build.gradle.kts` silences
  the Kotlin warning about annotations on constructor `val`s; keep it rather than annotating
  every site.

---

## Coding Rules

From the PRD's 开发原则 + Claude Code 开发要求:

1. 保持界面简单 — if a feature does not answer 「它是否值得每天使用?」, do not add it.
2. 优先保证流畅体验.
3. 不增加无意义配置.
4. 不引入复杂依赖.
5. 保持代码可维护; 一个模块只负责一件事.
6. 优先完成 MVP, 再持续迭代.
7. **不提前开发未来 Phase 的功能.** Placeholders should say so plainly rather than fake a layout.
8. **每完成一个模块, 先确保能正常运行再继续** — build AND launch it, don't stop at compiling.
9. 统一目录结构与命名; 必要注释.
10. When several designs are possible, pick the simple, stable, maintainable one — long-term
    maintenance and single-user experience come first.

UI keywords: **Calm · Minimal · Modern · Clean** — 深色模式优先 (dark is the default, not just an
option), 卡片式布局, 留白充足, 柔和圆角, 动画简洁自然, 不加无意义视觉效果. No dynamic colour:
the palette is part of the product identity.

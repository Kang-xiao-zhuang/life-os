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

**V1 complete — Phases 1–4 all done.** Builds, installs, runs; every screen verified on the
emulator, 18 write-path checks pass end to end, and the backup round trip is covered by
instrumented tests.

- Full V1 schema declared up front (6 tables: projects / tasks / habits / habit_checks /
  journal_entries / captures), so Phase 3 does not churn through Room migrations.
- Theme switching is the one fully working *write* path, on purpose: it exercises the entire
  UI → Service → Repository → DataStore chain, verified by killing the app and relaunching.
- Six screens laid out against real queries: Dashboard (今日最重要 / 今日任务 / 今日习惯 /
  快速记录 / 今日复盘), Projects (progress + 未归类), Project detail = the task list,
  Habits (streak + week dots), Journal (today's four prompts + history), Capture (inbox).
- Streaks and the week grid are **computed from check-ins, never stored**, so they cannot drift.
- Phase 3 made it all live: create/edit/delete tasks (title, notes, due date, MIT, project),
  create/rename projects (archive, never delete), create/rename/delete habits, tap-to-check-in,
  capture → task, and the journal editor. The `PhaseNote` placeholders are gone.

**Behaviours worth not breaking**

- A ticked task **stays on Dashboard for the rest of the day** (struck through, sorted last).
  It used to vanish instantly, which made a mis-tap impossible to undo where it happened.
  The queries are `done = 0 OR completedAt >= todayMidnight`.
- 今日任务 **excludes MIT tasks** — they are already featured above, and listing them twice on a
  minimal screen looked like a bug. Filtered in `DashboardService`, so the queries stay generic.
- Saving an **emptied** journal entry deletes the row rather than storing a blank one, so
  「未写」and「写了又清空」don't look different.
- Reopening a task clears `completedAt`, so "finished today" stays truthful.
- Habit tap **toggles** today's check-in; tapping again removes it. Streaks tolerate an unchecked
  today (measured from yesterday) because the day isn't over.
- `JournalViewModel` owns the edit draft. A `remember`-ed local copy would be seeded from the
  empty placeholder before the DB emits and then never refresh — blank fields over saved text.

- Phase 4 added 导出 / 导入 (`LifeOS_Backup.zip`), finished the settings page, and **closed the
  destructive-migration risk** (see Database above — there is no `fallbackToDestructiveMigration`
  any more).

**Backup, and why it is built this way**

- Export writes `database.db` + `config.json` (+ `attachments/` when any exist) through the system
  file picker (SAF). That is what keeps the app at **zero permissions**: the user picks the file
  and grants access to that one file.
- **`database.checkpoint()` before copying the file.** With WAL enabled the newest writes live in
  `lifeos.db-wal`, so copying the main file alone yields a backup missing whatever the user just
  did. There is a test for exactly this.
- Import copies rows into the live database **inside one transaction** rather than swapping the
  file under an open Room instance — a half-applied restore is worse than a failed one. Primary
  keys are preserved so tasks keep their project and check-ins keep their habit. Delete children
  before parents, insert parents before children, or the foreign keys refuse.
- `config.json` carries `schemaVersion`; importing an archive from a different schema version is
  refused with a message rather than half-read.
- Zip entries under `attachments/` are path-checked before writing, so a crafted archive cannot
  escape the attachments directory.

**Post-V1 additions (2026-08-04)**

- **桌面快捷记录** — a home-screen widget (`widget/CaptureWidgetProvider`, plain `RemoteViews`)
  and a launcher shortcut, both firing `LifeOsIntents.ACTION_QUICK_CAPTURE`. MainActivity is
  `singleTop` and counts the requests, so a repeat tap re-opens the field via `onNewIntent`.
  Landing focuses the input: from the home screen you arrive on a cursor, not on a screen you
  still have to tap. Settings has an 添加到桌面 button (`WidgetPinning`) because nobody discovers
  widgets by browsing the picker.
  - The shortcut is registered **at runtime**, not in `res/xml/shortcuts.xml`: a static shortcut
    must hard-code `targetPackage`, and resource files get no `${applicationId}` substitution, so
    it would point at the release id and silently do nothing in the `.debug` build.
  - The widget shows no data on purpose — nothing to update, nothing to go stale, no wakeups.
    It carries its own colours in `values/colors.xml` since widgets don't inherit the app theme.
- **复盘可回看** — history rows open a read-only sheet with the full four prompts. Blank prompts
  are skipped. History limit raised to 90 entries.

Considered and dropped: **weather on the Dashboard**. It needs `INTERNET`, which would break the
「不联网 · 零权限」claim that the About card makes. (A keyless source — open-meteo — was verified
reachable, so this stays possible if the trade is ever worth making.)

**Second round of PM fixes (2026-08-04)**

- **所有待办** (`ui/screen/tasks/`, route `tasks/all`, entry at the top of Projects) — every open
  task in one flat list, grouped 已经逾期 / 今天到期 / 以后 / 没有日期. Exists because Dashboard
  only shows what's due today or flagged MIT: a task with neither was reachable only by opening its
  project, so「我现在能做什么」cost one tap per project. Each row shows which project it's in
  (`TaskRow(projectLabel = …)`), because outside its project a bare title is ambiguous.
- **习惯月历热力图** (`ui/screen/habits/HabitHeatmap.kt`) — a month of check-ins, Monday first,
  shaded by *completion* (checked ÷ active habits) rather than a raw count, so a day means the same
  thing whether you track two habits or eight. Pages back through months; forward stops at the
  current one. Built from plain `Row`s — a lazy grid inside the scrolling column would fight the
  parent for gestures. Card title is neutral (`月度打卡`) since it can show any month.
- **MIT 软上限** (`TaskService.MIT_SOFT_LIMIT = 2`) — the editor warns when a third task is flagged
  今日最重要, and never blocks. The spec says 一天挑一到两件就够 but nothing else in the app would
  ever have mentioned it.
- **逾期批量处理** — 「把 N 项逾期挪到今天」on Dashboard and in 所有待办. Overdue items otherwise
  accumulate as red text until the card becomes noise the user stops reading.

Still open, only if it earns its place: Markdown *rendering* for journal entries (text is already
stored verbatim), an attachments feature (the backup format already carries them), and reminders
(would need `POST_NOTIFICATIONS`, same zero-permission trade-off as weather).

### Verifying on the emulator

The app never seeds sample data. To eyeball a screen with content, insert rows straight into the
app's database and delete them afterwards:

```powershell
adb shell "run-as com.zk.lifeos.debug sqlite3 /data/data/com.zk.lifeos.debug/databases/lifeos.db \"<sql>\""
```

`run-as` reaches the private data dir without root, and `sqlite3` is on the emulator image.
Dates are epoch days, timestamps epoch millis.

**Driving the UI from the CLI** — worth reusing, and worth not re-learning the hard way:

- Locate widgets with `adb shell uiautomator dump` and tap the centre of the matching node.
  Blind pixel taps drift as soon as a layout differs (the FAB sits lower on screens with no
  bottom bar).
- **Never** send `keyevent 111` (ESCAPE) or `4` (BACK) to dismiss the keyboard — both close the
  open dialog too, which silently invalidates the whole run. The emulator has a hardware
  keyboard, so the soft IME never covers the layout anyway.
- Screen titles and bottom-nav labels use the same words (项目 / 习惯 / 记录 / 复盘). Match nav
  items by position near the bottom edge, or the title wins and nothing happens.
- The dump can be **stale** right after a navigation — poll until the expected text appears
  rather than sleeping a fixed time, and assert the expected screen before continuing.
- `adb shell input text` is ASCII-only; use Latin text for automated entry.

### Tests

```powershell
$env:JAVA_HOME="D:\Develop\android\jbr"
.\gradlew.bat :app:connectedDebugAndroidTest     # needs a running emulator/device
```

`BackupRoundTripTest` runs against the **real** `lifeos.db` (an in-memory Room instance would not
exercise the WAL checkpoint) and wipes the database around each test — so don't run it on a device
holding data you care about.

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

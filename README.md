# LifeOS 🌱

> Personal Operating System —— 一个本地优先的 Android 个人工作台。

LifeOS 不是 Todo 软件，不是笔记软件，也不是日历软件。

它帮我管理**工作、学习、项目、习惯和成长**——每天打开一次，用完就走。

完整产品定义见 [`docs/PRD.md`](docs/PRD.md)。

---

## 🔒 它的立场

这几条不是宣传语，是写进代码里的约束：

- **Local First** —— 所有数据只存在这台设备的私有目录里
- **Offline First** —— 没有网络也是完整可用的
- **No Login** —— 没有账号，没有注册
- **Privacy First** —— **App 声明零权限**，连 `INTERNET` 都没有；系统云备份和设备迁移也主动关闭了
- **Fast / Minimal** —— 打开即用，界面不堆东西

目前不做：云同步、多人协作、AI 功能、在线账号。

> 数据只在本机，也意味着**卸载 App 就是全没**。请用设置里的导出留一份备份。

---

## 📱 功能

底部五个 tab：**首页 · 项目 · 记录 · 习惯 · 复盘**。设置不占 tab，从首页右上角进入。

> **任务刻意不做 tab** —— 它只在所属项目或「今天」的语境里才有意义。记录放在正中间，
> 因为快速记录是打开 App 后第二高频的动作，拇指要够得到。

### 🏠 首页

打开就回答一个问题：今天要做什么。

- **今日最重要（MIT）** —— 一天挑一到两件。标到第三个时会提醒你「都最重要就等于没有重点」，但不阻止你
- **今日任务** —— 今天到期的和逾期的；逾期标红，一键「把 N 项逾期挪到今天」，**可撤销**
- **今日习惯** —— 连续天数 + 本周七个点，点一下直接打卡
- **快速记录 / 今日复盘** 入口
- 勾掉的任务**当天不会消失**（划线沉底）—— 误点了能在原地撤销

### 📁 项目 & ✅ 任务

- 项目 = 长期在做的事（工作 / 学习 / 阅读 / 健身 / 自媒体），带进度条和待做数
- 项目**先归档**，归档后进「已归档」，随时能恢复；彻底删除只在归档页里，且它的任务会变成「未归类」不跟着消失
- **所有待办** —— 不分项目的一整张平铺清单，按 已经逾期 / 今天到期 / 以后 / 没有日期 分段，每行显示所属项目
- 任务：新建 / 编辑 / 删除 / 完成 / 截止日期（含今天、明天快捷）/ 今日最重要 / 改所属项目

### ➕ 记录（Capture）

一个输入框、一个按钮，记完即清空。**刻意不问项目和截止日期** —— 一问就不快了。

- **桌面小组件 + 长按图标快捷方式**：从桌面一步落到光标上
- 待整理的记录可以「→ 任务」（变成未归类任务，原记录留档）或直接删掉

### 🔥 习惯

- 点一下打卡，再点一下取消；整行可点，不用瞄准
- 连续天数（streak）和本周网格**由打卡记录实时算出、不存库**，所以不会算错或过期
- 一天没打卡**不算断**：今天还没结束，streak 从昨天量
- **月度打卡热力图** —— 按完成度（当天打卡数 ÷ 习惯数）着色，可以往前翻月份
- 不想做了就**归档**，打卡记录全留着，以后随时恢复；彻底删除只在「已归档」里，并且会告诉你要销毁多少条打卡

### 📖 复盘

每天一篇，四个固定问题：今天完成了什么 / 最大的收获 / 遇到的问题 / 明天最重要的一件事。

- 就地编辑 + 保存；清空后保存会删掉整行，不留空记录
- **历史可点开回看**完整内容

### ⚙️ 设置

- 外观：跟随系统 / 浅色 / **深色（默认）**
- **导出 / 导入备份** —— 一个 `LifeOS_Backup.zip`，存到哪由你选
- 添加桌面小组件
- 关于

---

## 💾 备份

```
LifeOS_Backup.zip
├── database.db      SQLite 全量数据
├── attachments/     附件（V1 还没有附件功能，所以通常不存在）
└── config.json      设置 + 元信息（含 schemaVersion）
```

走系统文件选择器（SAF），所以**不需要任何权限**：你选文件，系统只授权那一个文件。

几个不显眼但要紧的地方：

- 拷数据库之前先做 **WAL checkpoint** —— 否则最新的写入还在 `-wal` 文件里，备份会缺掉你刚做的操作
- 导入是把行**在一个事务里**灌进当前库，不是把文件塞到运行中的 Room 底下 —— 恢复一半比恢复失败更糟
- **主键原样保留**，所以任务还挂在原项目上、打卡还挂在原习惯上
- `config.json` 带 `schemaVersion`，版本不一致直接拒绝并说明原因
- 压缩包里 `attachments/` 的路径会校验，构造出来的包跑不出附件目录

⚠️ **导入会全量替换**现有数据。App 里会先确认一次。

---

## 🧱 技术栈

原生 Android，无跨平台层。

- **Kotlin** + **Jetpack Compose**（Material 3）
- **状态**：ViewModel + StateFlow
- **数据**：Room → SQLite；DataStore Preferences 存设置
- **依赖注入**：手写 `AppContainer`，没有 Hilt/Koin
- **导航**：Navigation Compose
- compileSdk / targetSdk 36 · minSdk 26 · JVM target 17
- Gradle 8.14.3 · AGP 8.13.2 · Kotlin 2.3.21

设计上刻意不做的：不用动态取色（Material You）—— 这套配色是产品气质的一部分，应该在每台手机上都一样；不引入图表库、Markdown 渲染库、Glance。整个项目**没有一个二进制资源**，图标和小组件背景都是 vector / XML。

### 分层

```
UI (Compose) → Service → Repository → Room / DataStore
```

**业务层不得直接操作数据库** —— 只有 `data/repository/*` 能碰 DAO。页面找 ViewModel，ViewModel 找 Service。

---

## 📁 目录结构

```
lifeos/
├── app/src/main/java/com/zk/lifeos/
│   ├── LifeOsApplication.kt   # 构建对象图 + 注册桌面快捷方式
│   ├── AppContainer.kt        # 全部 lazy 的依赖容器
│   ├── MainActivity.kt        # 单 Activity；先定主题再渲染
│   ├── model/                 # 共享模型，不归任何一层所有
│   ├── data/
│   │   ├── db/                # LifeOsDatabase + entity/ + dao/
│   │   ├── prefs/             # DataStore
│   │   ├── repository/        # 唯一能碰 DAO 的一层
│   │   └── backup/            # zip 读写 + 数据库搬运
│   ├── service/               # UI 唯一允许调用的业务层
│   ├── widget/                # 桌面小组件
│   └── ui/
│       ├── theme/             # 深色优先 M3
│       ├── navigation/        # 五个 tab + 详情路由
│       ├── components/        # 共享组件
│       └── screen/<feature>/  # 每个功能一个 Screen + ViewModel
├── app/src/androidTest/        # 备份往返仪器化测试
├── app/schemas/               # Room 导出的 schema（入库,便于 review 迁移）
├── docs/PRD.md                # 产品定义
└── CLAUDE.md                  # 技术约定与踩过的坑
```

---

## 🚀 本地运行

需要 **JDK 17+**（用 Android Studio 自带的 JBR 最省事）和 Android SDK。

```powershell
$env:JAVA_HOME="<Android Studio>\jbr"
.\gradlew.bat :app:assembleDebug
```

Debug 包的 applicationId 是 `com.zk.lifeos.debug`，可以和 release 版共存。

### 测试

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest   # 需要连着模拟器/真机
```

备份往返测试跑在**真实的** `lifeos.db` 上（内存库测不到 WAL checkpoint），并且会在每个测试前后清库 —— **别在存了真实数据的设备上跑**。跑完 Gradle 还会卸载 App。

### ⚠️ 依赖版本是刻意钉住的，不要顺手升级

`gradle/libs.versions.toml` 里的版本**故意不是最新的**。更新的 AndroidX 要求 **AGP 9.1+ 和 compileSdk 37**；在没装 platform 37 的机器上升级会直接构建失败。

要升级的正确顺序：装 SDK platform 37 → 上 Gradle 9.x + AGP 9.x → compileSdk 提到 37 → 再动库版本。

### 改数据库结构

`ddl-auto` 式的破坏性迁移**已经移除**：没有迁移就在启动时直接报错，而不是悄悄清库。

改结构时：`LifeOsDatabase.SCHEMA_VERSION` +1 → 写 `Migration` → 加进 `MIGRATIONS` → 把 `app/schemas/` 下新生成的 JSON 一起提交。

---

## 🗺️ Roadmap

**已完成（V1 + 三轮打磨）**
项目 / 任务 / 习惯打卡 / 快速记录 / 每日复盘 · 备份导出导入 · 桌面小组件与快捷方式 ·
所有待办平铺视图 · 习惯月历热力图 · MIT 软上限 · 逾期批量挪期（可撤销）· 复盘可回看 ·
项目与习惯的归档/恢复 · 深色优先主题

> 有一条贯穿整个 App 的规则：**任何一步操作都不会直接销毁你记录过的东西。**
> 项目和习惯都要先归档，只有归档页里才能彻底删除，而且会先告诉你代价。

**评估后没做**
- **天气** —— 需要 `INTERNET`，会打破「不联网 · 零权限」。（无 key 的数据源实测可达，以后想加随时能加）
- **自动本地备份** —— 有价值，暂时搁置
- **搜索 / 标签 / 子任务 / 重复任务 / 拖拽排序 / 统计仪表盘** —— 守住简单比补功能更难

**还开着的**
- **提醒通知** —— 需要 `POST_NOTIFICATIONS`，和天气是同一个取舍。一个不提醒的习惯 App，大概率两周后就不打开了
- 复盘的 Markdown **渲染**（内容已经按原文存着）
- 附件功能（备份格式已经预留）

---

## 最后

每加一个功能，先回答一个问题：

> **它是否值得每天使用？**

答案是否，就不要加。

_Made by 康小庄_

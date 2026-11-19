# LifeOS 🌱

> Personal Operating System —— 本地优先的个人工作台。

LifeOS 不是 Todo 软件，不是笔记软件，也不是日历软件。

它帮我管理**工作、学习、项目、习惯和成长**——每天打开一次，用完就走。

---

## 🔒 它的立场

- **Local First** —— 所有数据只存在这台设备的浏览器 localStorage 里
- **Offline First** —— 没有网络也是完整可用的
- **No Login** —— 没有账号，没有注册，没有后端
- **Fast / Minimal** —— 打开即用，界面不堆东西

目前不做：云同步、多人协作、AI 功能、在线账号、通知提醒、桌面小组件。

> 数据只在本机，也意味着**卸载 App 或清除浏览器数据就是全没**。请用设置里的导出留一份备份。

---

## 📱 功能

底部五个 tab：**首页 · 项目 · 记录 · 习惯 · 复盘**。设置不占 tab，从首页右上角进入。
任务刻意不做 tab——它只在所属项目或「今天」的语境里才有意义。

### 🏠 首页
- **今日最重要（MIT）** —— 一天挑一到两件，独占视觉强调（不装卡片、大字 + 绿色标签）；标第三个时会提示「都最重要就等于没有重点」，但不阻止
- **今日任务** —— 今天到期 + 逾期；逾期标红，可一键「把 N 项挪到今天」，带撤销
- **今日习惯** —— 连续天数 + 本周七个点，点一下直接打卡
- **快速记录 / 今日复盘** 入口
- **每日一句** —— 按日期选，当天不变、隔天换
- MIT 建议从昨日复盘的「明天最重要」带出

### 📁 项目 & ✅ 任务
- 项目 = 长期在做的事，带进度条和待做数；先归档、归档页可恢复或彻底删除（删除后任务变「未归类」）
- **所有待办** —— 按 已逾期 / 今天到期 / 以后 / 没有日期 分段
- 任务：新建 / 编辑 / 完成 / 截止日期 / 今日最重要 / 改所属项目 / 备注
- **重复任务**（每天 / 每周 / 每月 / 每年）：勾选后按截止日锚定生成下一条（晚一天勾也不漂）；取消勾选会收回刚生成的那条；下一条不继承 MIT

### ➕ 记录（Capture）
一个输入框、一个按钮，记完即清空，刻意不问项目和截止日期。待整理记录可「转为任务」或删除。

### 🔥 习惯
- 点一下打卡，再点取消；连续天数与本周网格由打卡记录**实时算出**
- 今天没打卡不算断（streak 从昨天量）
- **月度热力图** —— 按完成度（当天打卡数 ÷ 活跃习惯数）着色，可翻月
- 归档保留打卡记录，彻底删除只在归档页且提示要销毁多少条

### 📖 复盘
每天一篇，四个固定问题：完成了什么 / 最大收获 / 遇到的问题 / 明天最重要的一件事。
支持任意一天补写和编辑、换天前自动保存、未来日期禁止、带出当天已完成项、轻量 Markdown 渲染与历史列表。

### ⚙️ 设置
外观（跟随系统 / 浅 / 深）、语言（跟随系统 / 中文 / English）、JSON 备份导出导入（换机覆盖恢复）、上次备份时间（超 14 天变色提醒）。

---

## 🧱 技术栈

与 Track100 完全一致的 Web 技术栈：

- **Next.js 15**（静态导出 `output: export`）· **React 19** · **TypeScript**
- **TailwindCSS** —— 米白克制风设计令牌（CSS 变量驱动深浅色）
- **Zustand** —— 单一 `app.store`，localStorage 持久化（延迟写 + 写入失败告警）
- **React Hook Form + Zod** —— 依赖就位（当前表单用受控组件）
- **Vitest**（单测）· **Playwright**（端到端截图验证）
- **Capacitor 6** —— 打包 Android APK；保留 `share` / `filesystem` / `haptics` / `app` / `status-bar` 等插件

数据层只放纯数据，streak / 热力图 / 进度 / Dashboard 全部由 `utils/business.ts` 实时推导，不存冗余。

### 源码结构

```
src/
  app/          路由页面(首页 / projects / capture / habits / journal / settings / tasks/all)
  components/   layout / common(卡片·任务行·习惯行·弹层·热力图·Markdown)
  store/        Zustand app.store(所有实体 CRUD + 持久化)
  lib/          storage / backup / i18n / date 主题
  utils/        date / business(重复规则·streak·热力图·Dashboard)
  types/        Project / Task / Capture / Habit / HabitCheck / JournalEntry / Settings
legacy/         原 Kotlin/Compose 原生项目（完整归档，保留 git 历史与签名配置）
```

原原生实现完整归档在 `legacy/`，包括 Kotlin 源码、Gradle 配置与签名配置。

---

## 🚀 本地运行

```bash
npm install
npm run dev       # http://localhost:3000
npm run build     # 静态导出到 out/
npm test          # 单元测试
```

预览静态产物：`PORT=4174 node scripts/serve-out.mjs`

### 打包 Android APK

```bash
npm run android:apk    # build + cap sync + assembleDebug
cd android && ./gradlew.bat assembleRelease   # release 包
```

> 首次打包需先 `npx cap add android` 生成原生工程（签名配置可从 `legacy/` 取回）。
> 产物在 `android/app/build/outputs/apk/`。

---

## 🗺️ 关于路线

v1.x 聚焦「一个人能用得久」：视觉手感、数据安心、长期正反馈。云同步 / 登录、通知提醒、桌面小组件均评估后不做——守住简单比补功能更难。当前**完全本地、无后端、无账号**。

每加一个功能，先回答：

> **它是否值得每天使用？**

答案是否，就不要加。

_Made by 康小庄_

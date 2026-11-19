// 零依赖静态服务器:给 Playwright 提供 next export 生成的 out/ 目录。
// 关键是把 /create 这类无扩展名路由映射到 /create.html(Capacitor 里由客户端路由处理,
// 但普通静态服务器不会)。仅用于本地测试与 CI,不参与 App 运行时。
import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import { extname, join, resolve, sep } from "node:path";

const ROOT = resolve(process.cwd(), "out");
const PORT = Number(process.env.PORT ?? 4173);
const MIME = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
  ".webmanifest": "application/manifest+json",
  ".txt": "text/plain; charset=utf-8",
  ".woff2": "font/woff2",
};

const server = createServer(async (req, res) => {
  let pathname;
  try {
    pathname = decodeURIComponent(new URL(req.url ?? "/", "http://localhost").pathname);
  } catch {
    res.writeHead(400).end("Bad request");
    return;
  }
  if (pathname === "/") pathname = "/index.html";

  let file = resolve(ROOT, "." + pathname);
  if (file !== ROOT && !file.startsWith(ROOT + sep)) {
    res.writeHead(403).end("Forbidden");
    return;
  }

  // 无扩展名的路由优先映射到同名 .html（Next 导出会同时生成 projects.html 和 projects/ 子目录）。
  if (!extname(file)) {
    try {
      await stat(file + ".html");
      file += ".html";
    } catch {
      try {
        const s = await stat(file);
        if (s.isDirectory()) file = join(file, "index.html");
      } catch { /* 交给下面的 readFile 报 404 */ }
    }
  } else {
    try {
      const s = await stat(file);
      if (s.isDirectory()) file = join(file, "index.html");
    } catch { /* 404 */ }
  }

  try {
    const body = await readFile(file);
    res.writeHead(200, { "Content-Type": MIME[extname(file)] ?? "application/octet-stream" });
    res.end(body);
  } catch {
    res.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" }).end("Not found");
  }
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`serving out/ on http://127.0.0.1:${PORT}`);
});

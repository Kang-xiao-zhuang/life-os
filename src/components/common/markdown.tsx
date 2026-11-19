import React from "react";

/** 轻量 Markdown 渲染：标题/粗斜体/行内代码/引用/列表。输入来自用户自己的复盘，无需防 XSS（React 转义）。 */
export function Markdown({ text }: { text: string }) {
  if (!text?.trim()) return null;
  const blocks = text.split(/\n\s*\n/);
  return (
    <div className="md-body text-sm text-text-primary">
      {blocks.map((block, bi) => {
        const lines = block.split("\n");
        const first = lines[0]?.trim() ?? "";
        const isUL = /^\s*[-*]\s+/.test(first);
        const isOL = /^\s*\d+\.\s+/.test(first);
        const isQuote = /^\s*>\s?/.test(first);
        const hm = first.match(/^(#{1,3})\s+(.*)$/);
        if (hm) {
          const Tag = (hm[1].length === 1 ? "h1" : hm[1].length === 2 ? "h2" : "h3") as "h1" | "h2" | "h3";
          return <Tag key={bi}>{inline(hm[2])}</Tag>;
        }
        if (isUL || isOL) {
          const Tag = isUL ? "ul" : "ol";
          return (
            <Tag key={bi}>
              {lines.map((l, i) => {
                const m = l.match(/^\s*(?:[-*]|\d+\.)\s+(.*)$/);
                return <li key={i}>{inline(m?.[1] ?? l)}</li>;
              })}
            </Tag>
          );
        }
        if (isQuote) {
          return (
            <blockquote key={bi}>
              {lines.map((l, i) => <p key={i}>{inline(l.replace(/^\s*>\s?/, ""))}</p>)}
            </blockquote>
          );
        }
        return <p key={bi}>{lines.map((l, i) => <React.Fragment key={i}>{i > 0 && <br />}{inline(l)}</React.Fragment>)}</p>;
      })}
    </div>
  );
}

/** 行内：**粗体** *斜体* `代码`。 */
function inline(s: string): React.ReactNode {
  const parts: React.ReactNode[] = [];
  const re = /(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`)/g;
  let last = 0;
  let m: RegExpExecArray | null;
  let key = 0;
  while ((m = re.exec(s))) {
    if (m.index > last) parts.push(s.slice(last, m.index));
    const tok = m[0];
    if (tok.startsWith("**")) parts.push(<strong key={key++}>{tok.slice(2, -2)}</strong>);
    else if (tok.startsWith("`")) parts.push(<code key={key++}>{tok.slice(1, -1)}</code>);
    else parts.push(<em key={key++}>{tok.slice(1, -1)}</em>);
    last = m.index + tok.length;
  }
  if (last < s.length) parts.push(s.slice(last));
  return parts;
}

/** 去掉 Markdown 标记，取纯文本（复盘历史摘要用）。 */
export function toPlainText(s: string): string {
  return s.split("\n").map((l) => l.replace(/^\s*(#{1,3}\s*|[-*]\s*|\d+\.\s*|>\s?)/, "").replace(/\*\*|\*|`/g, "").trim()).filter(Boolean).join(" ");
}

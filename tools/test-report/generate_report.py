#!/usr/bin/env python3
"""Build a static HTML test report from Maven Surefire and Failsafe XML results.

Reads the real TEST-*.xml files written by `mvnw verify` and renders a single
self-contained page (no external fonts, scripts or CDNs).

Usage (from the repository root):
    python tools/test-report/generate_report.py
    python tools/test-report/generate_report.py --surefire backend/target/surefire-reports \
        --failsafe backend/target/failsafe-reports --out docs/test-report/index.html
"""

from __future__ import annotations

import argparse
import glob
import html
import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from datetime import datetime, timezone

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
REPO_URL = "https://github.com/JovanySk/qaflow-api"


@dataclass
class Case:
    name: str
    classname: str
    time: float
    status: str  # passed | failed | error | skipped
    message: str = ""


@dataclass
class Suite:
    name: str
    kind: str  # unit | integration
    time: float
    timestamp: str
    cases: list[Case] = field(default_factory=list)

    @property
    def simple_name(self) -> str:
        return self.name.rsplit(".", 1)[-1]

    @property
    def package(self) -> str:
        return self.name.rsplit(".", 1)[0] if "." in self.name else ""

    def count(self, status: str) -> int:
        return sum(1 for c in self.cases if c.status == status)

    @property
    def status(self) -> str:
        if self.count("failed") or self.count("error"):
            return "failed"
        if self.cases and self.count("skipped") == len(self.cases):
            return "skipped"
        return "passed"


def parse_float(value: str | None) -> float:
    try:
        return float((value or "0").replace(",", ""))
    except ValueError:
        return 0.0


def load_suites(directory: str, kind: str) -> list[Suite]:
    suites: list[Suite] = []
    for path in sorted(glob.glob(os.path.join(directory, "TEST-*.xml"))):
        root = ET.parse(path).getroot()
        nodes = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for node in nodes:
            suite = Suite(
                name=node.get("name", os.path.basename(path)),
                kind=kind,
                time=parse_float(node.get("time")),
                # Surefire 3.x omits the timestamp attribute; fall back to the file's write time.
                timestamp=node.get("timestamp")
                or datetime.fromtimestamp(os.path.getmtime(path), timezone.utc).strftime("%Y-%m-%dT%H:%M"),
            )
            for tc in node.findall("testcase"):
                status, message = "passed", ""
                for tag, label in (("failure", "failed"), ("error", "error"), ("skipped", "skipped")):
                    child = tc.find(tag)
                    if child is not None:
                        status = label
                        message = (child.get("message") or child.text or "").strip()
                        break
                suite.cases.append(
                    Case(
                        name=tc.get("name", "?"),
                        classname=tc.get("classname", suite.name),
                        time=parse_float(tc.get("time")),
                        status=status,
                        message=message,
                    )
                )
            suites.append(suite)
    return suites


def fmt_time(seconds: float) -> str:
    if seconds >= 60:
        minutes, rest = divmod(seconds, 60)
        return f"{int(minutes)}m {rest:04.1f}s"
    if seconds >= 1:
        return f"{seconds:.2f}s"
    return f"{seconds * 1000:.0f}ms"


def git_commit() -> str:
    sha = os.environ.get("GITHUB_SHA", "")
    if sha:
        return sha[:7]
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=REPO_ROOT, capture_output=True, text=True, timeout=10,
        )
        return out.stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return ""


def esc(value: object) -> str:
    return html.escape(str(value), quote=True)


STATUS_LABEL = {"passed": "Passed", "failed": "Failed", "error": "Error", "skipped": "Skipped"}


def render_case_rows(suite: Suite) -> str:
    rows = []
    max_time = max((c.time for c in suite.cases), default=0) or 1
    for case in suite.cases:
        width = max(2.0, case.time / max_time * 100)
        msg = f'<div class="msg">{esc(case.message[:400])}</div>' if case.message else ""
        rows.append(
            f'<tr><td class="tname">{esc(case.name)}{msg}</td>'
            f'<td class="num"><span class="bar"><i style="width:{width:.1f}%"></i></span>{esc(fmt_time(case.time))}</td>'
            f'<td class="st"><span class="pill {case.status}">{STATUS_LABEL[case.status]}</span></td></tr>'
        )
    return "\n".join(rows)


def render_suite(suite: Suite) -> str:
    anchor = suite.name.replace(".", "-")
    return f"""
<details class="suite" id="{esc(anchor)}">
  <summary>
    <span class="chev" aria-hidden="true"></span>
    <span class="sname">{esc(suite.simple_name)}<small>{esc(suite.package)}</small></span>
    <span class="scount">{len(suite.cases)} tests</span>
    <span class="stime">{esc(fmt_time(suite.time))}</span>
    <span class="pill {suite.status}">{STATUS_LABEL[suite.status]}</span>
  </summary>
  <table>
    <thead><tr><th>Test</th><th class="num">Time</th><th class="st">Status</th></tr></thead>
    <tbody>
{render_case_rows(suite)}
    </tbody>
  </table>
</details>"""


def render_group(title: str, subtitle: str, suites: list[Suite], source: str) -> str:
    if not suites:
        return ""
    cases = sum(len(s.cases) for s in suites)
    passed = sum(s.count("passed") for s in suites)
    duration = sum(s.time for s in suites)
    body = "".join(render_suite(s) for s in sorted(suites, key=lambda s: s.simple_name.lower()))
    return f"""
<section class="group">
  <header class="ghead">
    <div>
      <h2>{esc(title)}</h2>
      <p>{esc(subtitle)}</p>
    </div>
    <dl class="gstats">
      <div><dt>Suites</dt><dd>{len(suites)}</dd></div>
      <div><dt>Tests</dt><dd>{cases}</dd></div>
      <div><dt>Passed</dt><dd>{passed}</dd></div>
      <div><dt>Time</dt><dd>{esc(fmt_time(duration))}</dd></div>
      <div><dt>Runner</dt><dd class="mono">{esc(source)}</dd></div>
    </dl>
  </header>
  {body}
</section>"""


def render_slowest(suites: list[Suite], limit: int = 10) -> str:
    cases = [(c, s) for s in suites for c in s.cases]
    cases.sort(key=lambda pair: pair[0].time, reverse=True)
    top = cases[:limit]
    if not top:
        return ""
    max_time = top[0][0].time or 1
    rows = []
    for idx, (case, suite) in enumerate(top, 1):
        width = max(1.5, case.time / max_time * 100)
        kind = "IT" if suite.kind == "integration" else "Unit"
        rows.append(
            f'<li><span class="rank">{idx:02d}</span>'
            f'<span class="sl-name">{esc(case.name)}<small>{esc(suite.simple_name)} <em>{kind}</em></small></span>'
            f'<span class="sl-bar"><i style="width:{width:.1f}%"></i></span>'
            f'<span class="sl-time">{esc(fmt_time(case.time))}</span></li>'
        )
    return '<ol class="slowest">' + "".join(rows) + "</ol>"


CSS = """
:root{
  --bg:#0b0d10;--surface:#12151a;--surface-2:#171b21;--line:#232830;--line-2:#2c323b;
  --text:#e6e9ee;--muted:#8b94a3;--faint:#5d6573;
  --accent:#4fd1a5;--accent-soft:rgba(79,209,165,.12);--accent-line:rgba(79,209,165,.35);
  --danger:#f2727f;--danger-soft:rgba(242,114,127,.12);--warn:#d8b45a;--warn-soft:rgba(216,180,90,.12);
  --mono:ui-monospace,SFMono-Regular,"Cascadia Code","JetBrains Mono",Menlo,Consolas,monospace;
  color-scheme:dark;
  --sans:system-ui,-apple-system,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif;
}
*{box-sizing:border-box}
html{-webkit-text-size-adjust:100%}
body{margin:0;background:var(--bg);color:var(--text);font:15px/1.55 var(--sans);-webkit-font-smoothing:antialiased}
.wrap{max-width:1120px;margin:0 auto;padding:48px 24px 72px}
a{color:var(--accent);text-decoration:none}a:hover{text-decoration:underline}
.mono{font-family:var(--mono)}
.top{display:flex;flex-wrap:wrap;gap:24px;align-items:flex-end;justify-content:space-between;padding-bottom:28px;border-bottom:1px solid var(--line)}
.eyebrow{font:600 12px/1 var(--mono);letter-spacing:.14em;text-transform:uppercase;color:var(--accent);margin:0 0 14px}
h1{font-size:34px;line-height:1.15;letter-spacing:-.02em;margin:0 0 10px;font-weight:650}
.lede{color:var(--muted);margin:0;max-width:620px}
.meta{display:grid;grid-template-columns:auto auto;gap:6px 18px;font:13px/1.4 var(--mono);color:var(--muted);margin:0}
.meta dt{color:var(--faint)}.meta dd{margin:0;color:var(--text)}
.verdict{display:inline-flex;align-items:center;gap:10px;margin-top:22px;padding:8px 14px;border:1px solid var(--accent-line);background:var(--accent-soft);border-radius:999px;font:600 13px/1 var(--mono);color:var(--accent)}
.verdict.bad{border-color:var(--danger);background:var(--danger-soft);color:var(--danger)}
.dot{width:8px;height:8px;border-radius:50%;background:currentColor;box-shadow:0 0 0 4px var(--accent-soft)}
.tiles{display:grid;grid-template-columns:repeat(5,1fr);gap:1px;background:var(--line);border:1px solid var(--line);border-radius:14px;overflow:hidden;margin:32px 0 18px}
.tile{background:var(--surface);padding:20px 22px}
.tile dt{font:600 11px/1 var(--mono);letter-spacing:.12em;text-transform:uppercase;color:var(--faint);margin-bottom:12px}
.tile dd{margin:0;font:600 32px/1 var(--mono);letter-spacing:-.02em;font-variant-numeric:tabular-nums}
.tile dd small{font-size:14px;color:var(--muted);font-weight:500;margin-left:4px}
.tile.acc dd{color:var(--accent)}
.split{display:flex;height:8px;border-radius:6px;overflow:hidden;background:var(--surface-2);margin:0 0 10px}
.split i{display:block;height:100%}
.split .u{background:var(--accent)}.split .it{background:var(--accent);opacity:.45}
.legend{display:flex;flex-wrap:wrap;gap:20px;font:13px/1.4 var(--mono);color:var(--muted);margin:0 0 44px}
.legend span::before{content:"";display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:8px;vertical-align:-1px;background:var(--accent)}
.legend .it::before{opacity:.45}
h2{font-size:20px;letter-spacing:-.01em;margin:0 0 4px;font-weight:620}
.section-title{display:flex;align-items:baseline;justify-content:space-between;gap:16px;margin:0 0 16px}
.section-title p,.ghead p{margin:0;color:var(--muted);font-size:14px}
.group{margin:0 0 44px}
.ghead{display:flex;flex-wrap:wrap;gap:18px;align-items:flex-end;justify-content:space-between;margin-bottom:14px}
.gstats{display:flex;gap:22px;margin:0;font-family:var(--mono)}
.gstats dt{font-size:11px;letter-spacing:.1em;text-transform:uppercase;color:var(--faint)}
.gstats dd{margin:2px 0 0;font-size:15px;font-weight:600;font-variant-numeric:tabular-nums}
.suite{background:var(--surface);border:1px solid var(--line);border-radius:12px;margin-bottom:8px;overflow:hidden;transition:border-color .15s}
.suite[open]{border-color:var(--line-2)}
.suite summary{list-style:none;display:grid;grid-template-columns:16px 1fr auto 84px 84px;gap:16px;align-items:center;padding:14px 18px;cursor:pointer;user-select:none}
.suite summary::-webkit-details-marker{display:none}
.suite summary:hover{background:var(--surface-2)}
.suite summary:focus-visible{outline:2px solid var(--accent);outline-offset:-2px}
.chev{width:8px;height:8px;border-right:1.5px solid var(--muted);border-bottom:1.5px solid var(--muted);transform:rotate(-45deg);transition:transform .15s}
.suite[open] .chev{transform:rotate(45deg)}
.sname{font-weight:600;font-family:var(--mono);font-size:14px;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.sname small{display:block;font-weight:400;color:var(--faint);font-size:12px;margin-top:2px}
.scount,.stime{font:13px/1 var(--mono);color:var(--muted);text-align:right;font-variant-numeric:tabular-nums}
.pill{justify-self:end;display:inline-block;padding:4px 9px;border-radius:999px;font:600 11px/1 var(--mono);letter-spacing:.06em;text-transform:uppercase;border:1px solid var(--accent-line);color:var(--accent);background:var(--accent-soft)}
.pill.failed,.pill.error{border-color:var(--danger);color:var(--danger);background:var(--danger-soft)}
.pill.skipped{border-color:var(--warn);color:var(--warn);background:var(--warn-soft)}
table{width:100%;border-collapse:collapse;border-top:1px solid var(--line)}
th{font:600 11px/1 var(--mono);letter-spacing:.1em;text-transform:uppercase;color:var(--faint);text-align:left;padding:12px 18px;background:var(--surface-2)}
td{padding:10px 18px;border-top:1px solid var(--line);vertical-align:top;font-size:14px}
tbody tr:hover td{background:var(--surface-2)}
.tname{font-family:var(--mono);font-size:13px;word-break:break-word}
.msg{color:var(--danger);font-size:12px;margin-top:4px}
th.num,td.num{text-align:right;white-space:nowrap;font-family:var(--mono);font-size:13px;color:var(--muted);width:190px;font-variant-numeric:tabular-nums}
th.num{font-size:11px;color:var(--faint)}
th.st,td.st{text-align:right;width:110px}
.bar{display:inline-block;width:70px;height:4px;border-radius:2px;background:var(--surface-2);margin-right:12px;vertical-align:middle;overflow:hidden}
.bar i{display:block;height:100%;background:var(--accent);opacity:.7}
.slowest{list-style:none;margin:0 0 48px;padding:0;background:var(--surface);border:1px solid var(--line);border-radius:12px;overflow:hidden}
.slowest li{display:grid;grid-template-columns:34px minmax(0,1.4fr) minmax(0,1fr) 80px;gap:16px;align-items:center;padding:12px 18px;border-top:1px solid var(--line)}
.slowest li:first-child{border-top:0}
.rank{font:12px/1 var(--mono);color:var(--faint)}
.sl-name{font:13px/1.35 var(--mono);min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.sl-name small{display:block;color:var(--faint);font-size:12px;margin-top:2px}
.sl-name em{font-style:normal;color:var(--accent);margin-left:6px}
.sl-bar{height:6px;border-radius:3px;background:var(--surface-2);overflow:hidden}
.sl-bar i{display:block;height:100%;background:var(--accent)}
.sl-time{font:600 13px/1 var(--mono);text-align:right;font-variant-numeric:tabular-nums}
.ci{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:0 0 20px;padding:0;list-style:none;counter-reset:step}
.ci li{background:var(--surface);border:1px solid var(--line);border-radius:12px;padding:18px;counter-increment:step;font-size:14px;color:var(--muted)}
.ci li::before{content:counter(step,decimal-leading-zero);display:block;font:600 12px/1 var(--mono);color:var(--accent);margin-bottom:12px}
.ci b{display:block;color:var(--text);font-weight:600;margin-bottom:4px}
.ci code,.note code{font-family:var(--mono);font-size:12.5px;color:var(--text);background:var(--surface-2);padding:1px 6px;border-radius:5px}
.note{color:var(--muted);font-size:14px;margin:0 0 8px}
.tools{display:flex;gap:8px}
.btn{font:600 12px/1 var(--mono);color:var(--muted);background:transparent;border:1px solid var(--line-2);border-radius:8px;padding:8px 12px;cursor:pointer}
.btn:hover{color:var(--text);border-color:var(--muted)}
footer{margin-top:56px;padding-top:20px;border-top:1px solid var(--line);font:12px/1.6 var(--mono);color:var(--faint);display:flex;flex-wrap:wrap;justify-content:space-between;gap:12px}
@media (max-width:860px){
  .tiles{grid-template-columns:repeat(2,1fr)}
  .ci{grid-template-columns:1fr 1fr}
  .suite summary{grid-template-columns:16px 1fr auto;gap:12px}
  .suite summary .scount,.suite summary .stime{display:none}
  .slowest li{grid-template-columns:28px 1fr 70px}.sl-bar{display:none}
  th.num,td.num{width:auto}.bar{display:none}
  .gstats{gap:14px;flex-wrap:wrap}
}
@media (max-width:520px){.wrap{padding:32px 16px 56px}h1{font-size:27px}.ci{grid-template-columns:1fr}}
"""

JS = """
document.querySelectorAll('[data-toggle]').forEach(function(btn){
  btn.addEventListener('click',function(){
    var open = btn.getAttribute('data-toggle') === 'open';
    document.querySelectorAll('details.suite').forEach(function(d){ d.open = open; });
  });
});
if (location.hash) { var t = document.getElementById(location.hash.slice(1)); if (t && t.tagName === 'DETAILS') { t.open = true; } }
"""


def render(suites: list[Suite], source_label: str) -> str:
    unit = [s for s in suites if s.kind == "unit"]
    integ = [s for s in suites if s.kind == "integration"]
    all_cases = [c for s in suites for c in s.cases]
    total = len(all_cases)
    failed = sum(1 for c in all_cases if c.status in ("failed", "error"))
    skipped = sum(1 for c in all_cases if c.status == "skipped")
    passed = total - failed - skipped
    duration = sum(s.time for s in suites)
    unit_n = sum(len(s.cases) for s in unit)
    integ_n = sum(len(s.cases) for s in integ)
    unit_pct = unit_n / total * 100 if total else 0
    run_ts = max((s.timestamp for s in suites if s.timestamp), default="")
    run_date = run_ts.replace("T", " ")[:16] + " UTC" if run_ts else "n/a"
    generated = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    commit = git_commit()
    commit_html = (
        f'<a href="{REPO_URL}/commit/{esc(commit)}">{esc(commit)}</a>' if commit else "n/a"
    )
    ok = failed == 0 and total > 0
    verdict = (
        f'<span class="verdict"><span class="dot"></span>All {total} tests passed</span>'
        if ok
        else f'<span class="verdict bad"><span class="dot"></span>{failed} of {total} tests failing</span>'
    )

    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>QAFlow Test Report</title>
<meta name="description" content="Unit and integration test results for the QAFlow manufacturing API, generated from Maven Surefire and Failsafe XML reports.">
<style>{CSS}</style>
</head>
<body>
<main class="wrap">
  <header class="top">
    <div>
      <p class="eyebrow">QAFlow API / Test report</p>
      <h1>Unit and integration test results</h1>
      <p class="lede">Generated from the Maven Surefire (unit/web) and Failsafe (integration, real MySQL 8 via Testcontainers) XML reports of a full <code class="mono">mvnw verify</code> run.</p>
      {verdict}
    </div>
    <dl class="meta">
      <dt>Run</dt><dd>{esc(run_date)}</dd>
      <dt>Commit</dt><dd>{commit_html}</dd>
      <dt>Source</dt><dd>{esc(source_label)}</dd>
      <dt>Generated</dt><dd>{esc(generated)}</dd>
    </dl>
  </header>

  <dl class="tiles">
    <div class="tile"><dt>Total</dt><dd>{total}</dd></div>
    <div class="tile acc"><dt>Passed</dt><dd>{passed}</dd></div>
    <div class="tile"><dt>Failed</dt><dd>{failed}</dd></div>
    <div class="tile"><dt>Skipped</dt><dd>{skipped}</dd></div>
    <div class="tile"><dt>Duration</dt><dd>{esc(fmt_time(duration))}</dd></div>
  </dl>
  <div class="split" role="img" aria-label="{unit_n} unit tests, {integ_n} integration tests"><i class="u" style="width:{unit_pct:.1f}%"></i><i class="it" style="width:{100 - unit_pct:.1f}%"></i></div>
  <div class="legend"><span class="u">Unit / web: {unit_n} tests in {len(unit)} suites</span><span class="it">Integration: {integ_n} tests in {len(integ)} suites</span></div>

  <div class="section-title">
    <div><h2>Suites</h2><p>Click a suite to see every test case with its execution time.</p></div>
    <div class="tools"><button class="btn" data-toggle="open" type="button">Expand all</button><button class="btn" data-toggle="close" type="button">Collapse all</button></div>
  </div>
  {render_group("Integration tests", "Real HTTP calls against the running app and a disposable MySQL 8 container (Testcontainers). Run by maven-failsafe-plugin.", integ, "failsafe")}
  {render_group("Unit and web tests", "Services, domain rules, JWT validation and error handling with JUnit 5, Mockito and MockMvc. Run by maven-surefire-plugin.", unit, "surefire")}

  <div class="section-title"><div><h2>Slowest tests</h2><p>Top 10 test cases by execution time across both phases.</p></div></div>
  {render_slowest(suites)}

  <div class="section-title"><div><h2>How it runs in CI</h2><p>Workflow <a href="{REPO_URL}/actions/workflows/ci.yml">.github/workflows/ci.yml</a>, on every push to main and every pull request.</p></div></div>
  <ol class="ci">
    <li><b>Checkout and JDK 17</b>Temurin 17 on ubuntu-latest with the Maven dependency cache.</li>
    <li><b>Unit tests</b><code>./mvnw -B clean verify</code> runs Surefire first for <code>*Test</code> classes.</li>
    <li><b>Integration tests</b>Failsafe runs <code>*IT</code> classes; Testcontainers starts MySQL 8 on the runner's Docker.</li>
    <li><b>Reports</b>Raw XML and this HTML page are uploaded as build artifacts, even when tests fail.</li>
  </ol>
  <p class="note">Regenerate locally with <code>python tools/test-report/generate_report.py</code> after <code>mvnw verify</code>.</p>

  <footer><span>QAFlow API</span><span><a href="{REPO_URL}">{REPO_URL.replace("https://", "")}</a></span></footer>
</main>
<script>{JS}</script>
</body>
</html>
"""


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--surefire", default=os.path.join(REPO_ROOT, "backend", "target", "surefire-reports"))
    parser.add_argument("--failsafe", default=os.path.join(REPO_ROOT, "backend", "target", "failsafe-reports"))
    parser.add_argument("--out", default=os.path.join(REPO_ROOT, "docs", "test-report", "index.html"))
    parser.add_argument("--source", default="GitHub Actions" if os.environ.get("GITHUB_ACTIONS") else "Local run")
    args = parser.parse_args(argv)

    suites = load_suites(args.surefire, "unit") + load_suites(args.failsafe, "integration")
    if not suites:
        print(f"No TEST-*.xml files found in {args.surefire} or {args.failsafe}", file=sys.stderr)
        return 1

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(render(suites, args.source))

    total = sum(len(s.cases) for s in suites)
    bad = sum(1 for s in suites for c in s.cases if c.status in ("failed", "error"))
    print(f"Wrote {args.out}: {len(suites)} suites, {total} tests, {bad} failed/errors")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

/**
 * A zero-dependency browser console for the FireMind API.
 *
 * Served by server.js at GET / so the backend can be watched live: it shows the
 * same health and recommendation endpoints the Android TV app calls, including
 * which engine answered (Amazon Bedrock AI, or the deterministic fallback).
 *
 * No build step, no framework, no external requests - one page, same origin as
 * the API, so there is no CORS layer to configure.
 */
export function consoleHtml() {
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>FireMind backend console</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; }
  body {
    margin: 0; padding: 32px 28px 64px;
    background: #0B0E14; color: #E7ECF3;
    font: 15px/1.5 ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  }
  main { max-width: 940px; margin: 0 auto; }
  h1 { font-size: 26px; margin: 0; letter-spacing: .01em; }
  h1 span { color: #FF6B35; }
  .sub { color: #8A94A6; margin: 6px 0 24px; }
  .card {
    background: #141A24; border: 1px solid #1E2733; border-radius: 14px;
    padding: 18px 20px; margin-bottom: 18px;
  }
  .pills { display: flex; flex-wrap: wrap; gap: 10px; }
  .pill {
    border: 1px solid #1E2733; background: #0F141D; border-radius: 999px;
    padding: 6px 14px; font-size: 13px; color: #B9C2D0;
  }
  .pill b { color: #E7ECF3; font-weight: 600; }
  .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 7px; }
  .ok { background: #37D67A; } .warn { background: #F0B429; } .off { background: #6B7280; }
  label { display: block; font-size: 13px; color: #8A94A6; margin-bottom: 6px; }
  textarea {
    width: 100%; min-height: 68px; resize: vertical; padding: 12px 14px;
    background: #0F141D; color: #E7ECF3; border: 1px solid #26303D; border-radius: 10px;
    font: inherit;
  }
  textarea:focus { outline: none; border-color: #FF6B35; }
  .row { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-top: 14px; }
  button {
    font: inherit; cursor: pointer; border-radius: 10px; padding: 10px 18px;
    border: 1px solid #26303D; background: #1B2330; color: #E7ECF3;
  }
  button.primary { background: #FF6B35; border-color: #FF6B35; color: #0B0E14; font-weight: 700; }
  button:disabled { opacity: .55; cursor: progress; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
  .chip {
    font-size: 13px; padding: 6px 12px; border-radius: 999px;
    border: 1px solid #26303D; background: #0F141D; color: #B9C2D0; cursor: pointer;
  }
  .chip:hover { border-color: #FF6B35; color: #E7ECF3; }
  .badge {
    display: inline-block; font-size: 12px; font-weight: 700; letter-spacing: .04em;
    padding: 4px 10px; border-radius: 999px; text-transform: uppercase;
  }
  .badge.ai { background: #FF6B3522; color: #FF6B35; border: 1px solid #FF6B35; }
  .badge.local { background: #1E2733; color: #B9C2D0; border: 1px solid #26303D; }
  .result { border-top: 1px solid #1E2733; padding: 16px 0 4px; }
  .result:first-of-type { border-top: 0; }
  .result h3 { margin: 0 0 2px; font-size: 17px; }
  .meta { color: #8A94A6; font-size: 13px; margin-bottom: 8px; }
  .reason { margin: 0 0 6px; }
  .summary { margin: 0; color: #9AA5B5; font-size: 14px; }
  code { background: #0F141D; border: 1px solid #1E2733; border-radius: 6px; padding: 1px 6px; font-size: 13px; }
  .status { margin-top: 14px; font-size: 14px; color: #8A94A6; }
  .error { color: #FF8266; }
  footer { color: #6B7280; font-size: 13px; margin-top: 26px; }
  footer code { font-size: 12px; }
</style>
</head>
<body>
<main>
  <h1>Fire<span>Mind</span> backend console</h1>
  <p class="sub">The same API the Android TV app calls - watch which engine answers, live.</p>

  <section class="card">
    <div class="pills" id="health">
      <span class="pill"><span class="dot off"></span>checking health…</span>
    </div>
  </section>

  <section class="card">
    <label for="q">Ask for something to watch</label>
    <textarea id="q" placeholder="something funny and short for the family">something funny and short for the family</textarea>
    <div class="chips" id="chips"></div>
    <div class="row">
      <button class="primary" id="go">Ask FireMind</button>
      <label style="margin:0; display:flex; gap:8px; align-items:center;">
        <input type="checkbox" id="family" /> family-friendly only
      </label>
      <label style="margin:0; display:flex; gap:8px; align-items:center;">
        <input type="checkbox" id="short" /> under 100 minutes
      </label>
      <button id="refresh">Re-check health</button>
    </div>
    <div class="status" id="status"></div>
    <div id="out"></div>
  </section>

  <footer>
    Endpoints: <code>GET /api/health</code> <code>POST /api/recommend</code>
    <code>POST /api/summarize</code> <code>POST /api/similar</code><br />
    When Amazon Bedrock answers, the badge reads <b>AI</b>; if the model is
    unreachable or throttled, the same request is served locally and the badge
    reads <b>Local</b> - the app never shows an error either way.
  </footer>
</main>
<script>
  var EXAMPLES = [
    "something funny and short for the family",
    "a mind-bending sci-fi thriller",
    "slow-burn mystery for a rainy night",
    "cozy comfort watch, nothing heavy",
    "heist movie with a great twist"
  ];
  var state = { busy: false };

  function el(id) { return document.getElementById(id); }

  function pill(kind, text) {
    return '<span class="pill"><span class="dot ' + kind + '"></span>' + text + '</span>';
  }

  function esc(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  async function health() {
    el("health").innerHTML = pill("off", "checking health…");
    try {
      var r = await fetch("/api/health");
      var h = await r.json();
      var ai = h.aiConfigured;
      el("health").innerHTML =
        pill("ok", "backend <b>" + esc(h.status) + "</b>") +
        (ai
          ? pill("ok", "AI <b>Bedrock</b> · " + esc(h.model))
          : pill("warn", "AI <b>off</b> · deterministic fallback active")) +
        pill("off", "catalog <b>" + esc(h.catalogSize) + "</b> titles");
    } catch (err) {
      el("health").innerHTML = pill("warn", "health check failed: " + esc(err.message));
    }
  }

  async function ask() {
    if (state.busy) return;
    var query = el("q").value.trim();
    if (!query) { el("q").focus(); return; }
    state.busy = true;
    el("go").disabled = true;
    el("out").innerHTML = "";
    el("status").textContent = "asking the backend…";
    var t0 = Date.now();
    try {
      var r = await fetch("/api/recommend", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          query: query,
          filters: { familyOnly: el("family").checked, runtimeMax: el("short").checked ? 100 : null }
        })
      });
      var data = await r.json();
      var ms = Date.now() - t0;
      if (!r.ok) throw new Error(data.error || ("HTTP " + r.status));
      var source = data.source === "ai" ? "ai" : "local";
      var label = source === "ai" ? "AI · Amazon Bedrock" : "Local · deterministic fallback";
      el("status").innerHTML =
        '<span class="badge ' + source + '">' + esc(label) + "</span> " +
        "<span style='margin-left:8px'>" + data.recommendations.length +
        " recommendations in " + ms + " ms</span>";
      el("out").innerHTML = data.recommendations.map(function (m) {
        var meta = [m.year, (m.genres || []).join(" / "), m.runtime + " min", "rated " + m.rating]
          .filter(Boolean).join(" · ");
        return '<div class="result"><h3>' + esc(m.title) + "</h3>" +
          '<div class="meta">' + esc(meta) + "</div>" +
          '<p class="reason">' + esc(m.reason) + "</p>" +
          '<p class="summary">' + esc(m.summary) + "</p></div>";
      }).join("");
    } catch (err) {
      el("status").innerHTML = '<span class="error">request failed: ' + esc(err.message) + "</span>";
    } finally {
      state.busy = false;
      el("go").disabled = false;
    }
  }

  el("chips").innerHTML = EXAMPLES.map(function (e) {
    return '<span class="chip" data-q="' + esc(e) + '">' + esc(e) + "</span>";
  }).join("");
  el("chips").addEventListener("click", function (ev) {
    var t = ev.target.closest(".chip");
    if (!t) return;
    el("q").value = t.getAttribute("data-q");
    ask();
  });
  el("go").addEventListener("click", ask);
  el("refresh").addEventListener("click", health);

  health();
  ask();
</script>
</body>
</html>`;
}

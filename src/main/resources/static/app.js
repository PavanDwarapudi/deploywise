const SAMPLES = {
  compile: `[INFO] Compiling 12 source files
[ERROR] COMPILATION ERROR :
[ERROR] /app/src/main/java/com/shop/OrderService.java:[42,18] cannot find symbol
[ERROR]   symbol:   method getTotl()
[ERROR]   location: variable order of type com.shop.Order
[INFO] BUILD FAILURE`,
  dependency: `[INFO] Resolving dependencies
[ERROR] Failed to execute goal on project shop: Could not resolve dependencies for project com.shop:shop:jar:1.0
[ERROR] Could not find artifact org.example:missing-lib:jar:9.9.9 in central
[INFO] BUILD FAILURE`,
  test: `[INFO] Running com.shop.OrderServiceTest
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
[ERROR] testTotal  Time elapsed: 0.02 s  <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <100> but was: <90>
[ERROR] There are test failures.
[INFO] BUILD FAILURE`
};

const $ = (id) => document.getElementById(id);

function label(cat) {
  return cat.toLowerCase().replace(/_/g, ' ').replace(/^./, (c) => c.toUpperCase());
}

function cell(text, tag) {
  const td = document.createElement('td');
  if (tag) {
    const el = document.createElement(tag);
    el.textContent = text;
    td.appendChild(el);
  } else {
    td.textContent = text;
  }
  return td;
}

function row(...cells) {
  const tr = document.createElement('tr');
  cells.forEach((c) => tr.appendChild(c));
  return tr;
}

function showResult(r) {
  $('resultCard').hidden = false;
  const badge = $('badge');
  badge.textContent = label(r.primaryCategory);
  badge.className = 'badge ' + r.primaryCategory;
  $('summary').textContent = r.summary;
  const body = $('findings');
  body.replaceChildren();
  r.findings.forEach((f) => {
    body.appendChild(row(cell(f.lineNumber), cell(label(f.category)), cell(f.line, 'code'), cell(f.cause), cell(f.suggestion)));
  });
  if (r.findings.length === 0) {
    body.appendChild(row(cell('-'), cell('-'), cell('No matching lines'), cell('-'), cell('-')));
  }
}

async function loadStats() {
  const res = await fetch('/api/stats');
  const s = await res.json();
  $('total').textContent = '(' + s.totalAnalyses + ' analyses)';
  const body = $('stats');
  body.replaceChildren();
  Object.entries(s.byCategory).forEach(([cat, n]) => body.appendChild(row(cell(label(cat)), cell(n))));
}

async function loadHistory() {
  const res = await fetch('/api/analyses');
  const list = await res.json();
  const body = $('history');
  body.replaceChildren();
  list.slice(0, 10).forEach((a) => {
    body.appendChild(row(cell(a.id), cell(new Date(a.analyzedAt).toLocaleString()), cell(label(a.primaryCategory)), cell(a.findings.length)));
  });
}

async function analyze() {
  const log = $('log').value;
  $('status').textContent = 'Analyzing...';
  $('analyze').disabled = true;
  try {
    const res = await fetch('/api/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ log })
    });
    const data = await res.json();
    if (!res.ok) {
      $('status').textContent = data.error || 'Something went wrong';
      return;
    }
    $('status').textContent = '';
    showResult(data);
    await Promise.all([loadStats(), loadHistory()]);
  } catch (e) {
    $('status').textContent = 'Could not reach the server';
  } finally {
    $('analyze').disabled = false;
  }
}

$('analyze').addEventListener('click', analyze);
$('sample').addEventListener('change', (e) => {
  if (e.target.value) {
    $('log').value = SAMPLES[e.target.value];
  }
});
loadStats();
loadHistory();

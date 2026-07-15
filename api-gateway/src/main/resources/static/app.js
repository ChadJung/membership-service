/* Membership console — drives the gateway API and visualizes which service
   (and Kafka hop) handles each call. All responses use snake_case. */

const $ = (id) => document.getElementById(id);
const KRW = (n) => '₩' + Number(n).toLocaleString('ko-KR');

function userId() {
  return Number($('userId').value);
}

/* ---------- log & architecture pulse ---------- */

function log(service, message, isError = false) {
  const li = document.createElement('li');
  const time = new Date().toLocaleTimeString('ko-KR', { hour12: false });
  li.innerHTML = `<span class="t">${time}</span> ` +
      `<span class="svc-${service}">[${service}]</span> ` +
      (isError ? `<span class="err">${message}</span>` : message);
  $('log').prepend(li);
}

function pulse(service) {
  ['gateway', service].forEach((name) => {
    const node = $('node-' + name);
    if (!node) return;
    node.classList.add('pulse');
    setTimeout(() => node.classList.remove('pulse'), 700);
  });
}

/* ---------- API helper ---------- */

async function call(service, method, path, body) {
  pulse(service);
  log(service, `${method} ${path}`);
  const res = await fetch(path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = json?.error?.message || `HTTP ${res.status}`;
    log(service, `${res.status} — ${msg}`, true);
    throw new Error(msg);
  }
  return json?.data;
}

/* ---------- renderers ---------- */

function renderMembership(m) {
  const card = $('membershipCard');
  if (!m) {
    card.className = 'membership-card empty';
    card.innerHTML = '<p class="empty-note">활성 멤버십이 없습니다. 등급을 선택해 가입하세요.</p>';
    $('btnCancel').disabled = true;
    return;
  }
  const cancelled = m.status === 'CANCELLED';
  card.className = 'membership-card';
  card.innerHTML = `
    <div class="grade">${m.grade_display_name}
      <span class="badge badge-${m.grade}">${m.grade}</span>
      ${cancelled ? '<span class="badge badge-CANCELLED">CANCELLED</span>' : ''}
    </div>
    <div class="meta">사용자 <b>${m.user_id}</b> · 상태 <b>${m.status}</b></div>
    <div class="meta">구독일 <b>${(m.subscribed_at || '').slice(0, 10)}</b> · 만료일 <b>${(m.expired_at || '').slice(0, 16).replace('T', ' ')}</b></div>`;
  $('btnCancel').disabled = cancelled;
}

function renderBenefits(list) {
  const ul = $('benefitList');
  ul.innerHTML = '';
  if (!list || list.length === 0) {
    ul.innerHTML = '<li class="empty-note">표시할 혜택이 없습니다.</li>';
    return;
  }
  for (const b of list) {
    const li = document.createElement('li');
    li.className = 'benefit';
    li.innerHTML = `<span class="name">${b.name}</span>` +
        `<span class="desc">${b.description ?? ''}</span>` +
        `<span class="value">${b.type}${b.discount_value ? ' · ' + KRW(b.discount_value) : ''}</span>`;
    ul.appendChild(li);
  }
}

function renderHistory(list) {
  const tbody = $('historyTable').querySelector('tbody');
  tbody.innerHTML = '';
  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" class="empty-note">결제 이력이 없습니다.</td></tr>';
    return;
  }
  for (const p of list) {
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${(p.payment_date || '').slice(0, 16).replace('T', ' ')}</td>` +
        `<td>${KRW(p.amount)}</td><td>${p.payment_method}</td>` +
        `<td class="st-${p.status}">${p.status}</td>`;
    tbody.appendChild(tr);
  }
}

/* ---------- actions ---------- */

async function lookup() {
  try {
    renderMembership(await call('member', 'GET', `/api/v1/memberships/${userId()}`));
  } catch { renderMembership(null); }
}

$('btnLookup').addEventListener('click', lookup);

$('btnSubscribe').addEventListener('click', async () => {
  try {
    const m = await call('member', 'POST', '/api/v1/memberships',
        { user_id: userId(), grade: $('grade').value });
    pulse('kafka');
    log('kafka', 'membership-events ← SUBSCRIBED');
    renderMembership(m);
  } catch { /* logged */ }
});

$('btnCancel').addEventListener('click', async () => {
  try {
    const m = await call('member', 'DELETE', `/api/v1/memberships/${userId()}`);
    pulse('kafka');
    log('kafka', 'membership-events ← CANCELLED (benefit-service가 캐시를 무효화합니다)');
    renderMembership(m);
  } catch { /* logged */ }
});

$('btnBenefits').addEventListener('click', async () => {
  try {
    renderBenefits(await call('benefit', 'GET', `/api/v1/benefits/${userId()}`));
  } catch { renderBenefits(null); }
});

$('btnHistory').addEventListener('click', async () => {
  try {
    renderHistory(await call('payment', 'GET', `/api/v1/payments/${userId()}`));
  } catch { renderHistory(null); }
});

$('btnPay').addEventListener('click', async () => {
  try {
    const p = await call('payment', 'POST', '/api/v1/payments',
        { user_id: userId(), payment_method: 'CARD' });
    pulse('kafka');
    log('kafka', `payment-events ← COMPLETED ${KRW(p.amount)} (member-service가 만료일을 갱신합니다)`);
    // Renewal is eventually consistent: re-read the membership after the
    // event has had time to travel payment → kafka → member.
    setTimeout(async () => {
      log('member', '이벤트 반영 확인을 위해 멤버십을 다시 조회합니다');
      await lookup();
    }, 1500);
  } catch { /* logged */ }
});

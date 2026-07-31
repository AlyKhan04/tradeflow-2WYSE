// ============================================================================
// trades.js — TICKET-I093 + TICKET-I094 + TICKET-I098
// ============================================================================
// WHAT:    Fetches /api/v1/trades and renders the table.
// HOW:     fetch() + DOM querySelectors. NO frameworks — that's the point of Day 7.
// WHY:     Day 7 is the "feel the pain" sprint. Day 8 you replace it with React.
// OBSERVE: After this script runs, the <tbody> contains real trade rows.
// ============================================================================
// HINT: ONLY for this local dashboard, hard-code the viewer creds.
//       Credentials in source is a deliberate temporary hack — in the React
//       app (Day 9+) auth is centralised in apiService.js and the creds come
//       from a login form.
// ============================================================================

const API_BASE = "http://localhost:8080/api/v1";
// Must match SecurityConfig's in-memory user: viewer / viewer-pw.
const AUTH_HEADER = "Basic " + btoa("viewer:viewer-pw");
// TICKET-I068 caps page size at 100 server-side; anything larger returns 400.
const PAGE_SIZE = 20;

// Module-level state — Day 8 is exactly what makes this approach painful.
let allTrades = [];
let sortKey = "tradeDate";
let sortDir = "desc";

document.addEventListener("DOMContentLoaded", () => {
    bindSortHandlers();
    loadTrades();
});

/**
 * TICKET-I093 — fetch the trades page and render it.
 * Loading shows in the prologue and hides in `finally`, so it clears on both
 * the success and the error path.
 */
async function loadTrades() {
    const loading = document.getElementById("trades-loading");
    const errorDiv = document.getElementById("trades-error");
    loading.classList.remove("hidden");
    errorDiv.classList.add("hidden");

    try {
        const res = await fetch(`${API_BASE}/trades?page=0&size=${PAGE_SIZE}`, {
            headers: { "Authorization": AUTH_HEADER }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const page = await res.json();
        allTrades = page.content || page; // Spring Data Page or plain array
        render();
    } catch (e) {
        errorDiv.textContent = "Could not load trades: " + e.message;
        errorDiv.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

/**
 * TODO(TICKET-I094): build the click handler for each <th>.
 *  - same key clicked twice → toggle direction
 *  - render arrow ▲/▼
 */
function bindSortHandlers() {
    document.querySelectorAll("th[data-key]").forEach(th => {
        th.addEventListener("click", () => {
            const key = th.dataset.key;
            if (key === sortKey) sortDir = sortDir === "asc" ? "desc" : "asc";
            else { sortKey = key; sortDir = "asc"; }
            render();
        });
    });
}

function render() {
    const tbody = document.getElementById("trades-tbody");
    const sorted = [...allTrades].sort(compareBy(sortKey, sortDir));
    tbody.innerHTML = sorted.map(rowHtml).join("");
}

function rowHtml(t) {
    const status = t.status ?? "PENDING";
    const badgeClass = "badge-" + status.toLowerCase();
    return `
        <tr>
            <td>${escapeHtml(t.tradeRef)}</td>
            <td>${escapeHtml(t.instrumentId)}</td>
            <td>${escapeHtml(t.counterpartyId)}</td>
            <td>${formatNumber(t.quantity)}</td>
            <td>${formatNumber(t.price)}</td>
            <td>${escapeHtml(t.tradeDate)}</td>
            <td><span class="badge ${badgeClass}">${escapeHtml(status)}</span></td>
        </tr>
    `;
}

/** Numbers arrive as JSON numbers or strings (BigDecimal); render both readably. */
function formatNumber(n) {
    if (n == null) return "";
    const num = Number(n);
    return Number.isFinite(num)
        ? num.toLocaleString("en-GB", { maximumFractionDigits: 4 })
        : escapeHtml(n);
}

/** Everything interpolated into innerHTML goes through here — habit, not paranoia. */
function escapeHtml(s) {
    return String(s ?? "").replace(/[&<>"']/g, c => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"
    }[c]));
}

function compareBy(key, dir) {
    const mul = dir === "asc" ? 1 : -1;
    return (a, b) => {
        if (a[key] < b[key]) return -1 * mul;
        if (a[key] > b[key]) return  1 * mul;
        return 0;
    };
}

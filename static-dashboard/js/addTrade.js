// ============================================================================
// addTrade.js — TICKET-I095 + TICKET-I096
// ============================================================================
// WHAT:    Handles the Add-Trade form: client validation + POST.
// WHY:     Showcases why ad-hoc DOM manipulation gets ugly fast — every
//          field has its own error <span> we must show/hide manually.
//          React Hook Form (Day 9) replaces all of this.
// ============================================================================

const API_BASE = "http://localhost:8080/api/v1";
// Must match SecurityConfig's in-memory user: trader / trader-pw.
// POST /api/v1/** requires ROLE_TRADER, so viewer creds would 403 here.
const AUTH_HEADER = "Basic " + btoa("trader:trader-pw");

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("trade-form").addEventListener("submit", onSubmit);
});

/**
 * TICKET-I095 (validate) + TICKET-I096 (POST).
 * Validation gates the network call entirely — an invalid form produces zero
 * requests. On a server-side rejection the GlobalExceptionHandler envelope's
 * `details` map is fanned back out into the per-field error spans, which is the
 * same contract React Hook Form's setError() honours in I106.
 */
async function onSubmit(evt) {
    evt.preventDefault();
    const form = evt.target;
    const data = Object.fromEntries(new FormData(form).entries());

    if (!validate(data)) return;

    const submitBtn = form.querySelector("button[type=submit]");
    if (submitBtn) submitBtn.disabled = true;

    try {
        const res = await fetch(`${API_BASE}/trades`, {
            method: "POST",
            headers: {
                "Authorization": AUTH_HEADER,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                tradeRef:        data.tradeRef.trim(),
                instrumentId:    Number(data.instrumentId),
                counterpartyId:  Number(data.counterpartyId),
                quantity:        data.quantity,
                price:           data.price,
                tradeDate:       data.tradeDate
            })
        });

        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            // Field-level errors from the Day-6 GlobalExceptionHandler envelope:
            // { code, message, details: { quantity: "must be > 0", ... } }
            if (body.details) {
                Object.entries(body.details).forEach(([field, msg]) => setError(field, msg));
            }
            throw new Error(body.message || `HTTP ${res.status}`);
        }

        showToast("Trade created — redirecting…");
        setTimeout(() => location.href = "trades.html", 800);
    } catch (e) {
        showToast("Error: " + e.message, true);
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

/** TICKET-I095 — returns true only if every rule passes. */
function validate(data) {
    clearErrors();
    let ok = true;

    if (!data.tradeRef || !data.tradeRef.trim()) {
        setError("tradeRef", "required"); ok = false;
    }
    if (!data.instrumentId)   { setError("instrumentId",   "required"); ok = false; }
    if (!data.counterpartyId) { setError("counterpartyId", "required"); ok = false; }

    if (!data.quantity || Number(data.quantity) <= 0) {
        setError("quantity", "must be > 0"); ok = false;
    }
    if (!data.price || Number(data.price) <= 0) {
        setError("price", "must be > 0"); ok = false;
    }

    if (!data.tradeDate) {
        setError("tradeDate", "required"); ok = false;
    } else if (new Date(data.tradeDate) > new Date()) {
        setError("tradeDate", "must not be in the future"); ok = false;
    }

    return ok;
}

function clearErrors() {
    document.querySelectorAll(".field-error").forEach(s => s.textContent = "");
}
function setError(field, msg) {
    const el = document.querySelector(`.field-error[data-for="${field}"]`);
    if (el) el.textContent = msg;
}
function showToast(msg, isError = false) {
    const t = document.getElementById("form-feedback");
    t.textContent = msg;
    t.style.borderLeftColor = isError ? "#c62828" : "#003366";
    t.classList.remove("hidden");
}

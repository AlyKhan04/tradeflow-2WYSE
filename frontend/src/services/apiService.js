const BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

// Hard-coded TRADER credentials for Day 8. TRADER role is required for POST/PUT/DELETE.
const AUTH = 'Basic ' + btoa('trader:trader-pw');

export class ApiError extends Error {
    constructor(status, body) {
        super(body?.message || `HTTP ${status}`);
        this.status = status;
        this.body = body;
    }
}

async function request(path, options = {}) {
    const res = await fetch(BASE + path, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': AUTH,
            ...(options.headers || {})
        }
    });

    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new ApiError(res.status, body);
    }

    if (res.status === 204) return null;
    return res.json();
}

// ----- Trades --------------------------------------------------------------
export const getTrades       = (params = {}) =>
    request('/trades?' + new URLSearchParams(params).toString());

export const createTrade     = (body) =>
    request('/trades', { method: 'POST', body: JSON.stringify(body) });

// ----- Recon ---------------------------------------------------------------
export const getReconResults = (params = {}) =>
    request('/recon/results?' + new URLSearchParams(params).toString());

export const resolveBreak    = (id) =>
    request(`/recon/${id}/resolve`, { method: 'PUT' });

// static-dashboard/js/cards.js
// TICKET-I092A — skeleton; TICKET-I091/I098 fill this in on Day 8.

document.addEventListener("DOMContentLoaded", () => {
  const totalEl     = document.querySelector("#card-total-trades");
  const matchedEl   = document.querySelector("#card-matched-pct");
  const unmatchedEl = document.querySelector("#card-unmatched");
  const lagEl       = document.querySelector("#card-avg-lag");

  console.log("cards.js: ready");
  // TICKET-I091 — TODO: fetch /api/v1/trades and /api/v1/recon/run
  // TICKET-I098 — TODO: loading + error UX on the 4 cards
});
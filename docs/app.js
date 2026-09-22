/* =============================================================================
   Ri-magwinya — web build
   =============================================================================

   The same app as the Android build, against the same Supabase project: the
   same accounts, the same menu, the same orders, the same rules. Placing an
   order here writes the same rows the phone does, and the phone sees them.

   Deliberately one plain file with no framework and no build step, so it can
   be dropped on GitHub Pages and opened on any phone.

   Two things are the server's job, not this file's:
     • Prices. The client shows a total; place_order recomputes every line in
       the database and charges that. This file mirrors the formula so the
       number on the button is right, and that is all it is for.
     • Who may do what. Row Level Security and the SQL functions decide.
       Hiding the staff tabs from a student is a convenience, not a lock.

   Reference: Supabase. 2026. REST API. [Online].
   Available at: <https://supabase.com/docs/guides/api> [Accessed 22 September 2026].
   Reference: Supabase. 2026. Auth with REST. [Online].
   Available at: <https://supabase.com/docs/reference/javascript/auth-signinwithpassword>
   [Accessed 22 September 2026].
   AI assistance: Anthropic. 2026. Claude [Large language model]. Available at:
   https://claude.ai [Accessed 22 September 2026]. Used to: port the Android
   app to this web build.
   ========================================================================== */
(function () {
  "use strict";

  /* ==========================================================================
     CONFIG
     ========================================================================== */

  // The publishable key is meant to be public — it is in the Android APK too.
  // It grants nothing on its own: every table has Row Level Security, so this
  // key can read the menu and nothing else until somebody signs in.
  var SUPABASE_URL = "https://jykswltsegssjmvnqqbi.supabase.co";
  var PUBLISHABLE_KEY = "sb_publishable_119qk3mA53nQaFK_BI3-XA_6BmPUMpl";

  var ZONE = "Africa/Johannesburg";
  var SLOT_CUTOFF_MIN = 5;      // orders close 5 minutes before a break ends
  var LOYALTY_TARGET = 10;
  var POLL_MS = 6000;           // stands in for the app's realtime socket
  var MENU_SELECT = "*,option_groups(*,options(*))";
  var ORDER_SELECT =
    "*,order_items(*),collection_slots(name,starts_at,ends_at),profiles(full_name,student_number)";

  /* ==========================================================================
     STATE
     ========================================================================== */

  var S = {
    screen: "welcome",
    session: null,      // { access_token, refresh_token, expires_at }
    profile: null,      // the row from `profiles`, which carries the role
    menu: [],
    slots: [],
    orders: [],
    weather: null,
    cart: [],           // [{ key, item, sel }]
    slotId: null,
    q: "",
    cat: "All",
    staffFilter: "New",
    sales: null,
    openOrderId: null,
    busy: false,
    online: navigator.onLine,
    dark: (function () { try { return localStorage.getItem("rm.dark") === "1"; } catch (e) { return false; } })(),
    hasToppedUp: null,
    stamps: 0,
    error: null,
  };

  var app = document.getElementById("app");
  var screenEl = document.getElementById("screen");
  var sheetEl = document.getElementById("sheet");
  var scrimEl = document.getElementById("scrim");
  var toastEl = document.getElementById("toast");
  var toastMsg = document.getElementById("toastMsg");

  /* ==========================================================================
     SMALL HELPERS
     ========================================================================== */

  function esc(s) {
    return String(s == null ? "" : s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  /** Cents to "R12.50". Money is cents everywhere, never a float. */
  function R(cents) {
    var sign = cents < 0 ? "-" : "";
    var abs = Math.abs(Math.round(cents));
    return sign + "R" + Math.floor(abs / 100) + "." + String(abs % 100).padStart(2, "0");
  }

  /** The server sends numeric(10,2) as a JSON number. This is the one place it becomes cents. */
  function toCents(value) { return Math.round(Number(value || 0) * 100); }

  function ico(id, sz) {
    return '<svg width="' + (sz || 20) + '" height="' + (sz || 20) +
      '" viewBox="0 0 24 24" aria-hidden="true"><use href="#' + id + '"/></svg>';
  }
  function food(key, sz) {
    var known = ["wors", "vetkoek", "chips", "packet", "triangle", "sweet", "can", "bottle", "energy", "cup"];
    var id = known.indexOf(key) > -1 ? "f-" + key : "f-packet";
    return '<svg width="' + (sz || 30) + '" height="' + (sz || 30) +
      '" viewBox="0 0 40 40" aria-hidden="true"><use href="#' + id + '"/></svg>';
  }
  function mark(sz) {
    return '<svg width="' + sz + '" height="' + sz + '" viewBox="0 0 100 100" aria-hidden="true"><use href="#brand"/></svg>';
  }

  /** "09:41" in Bloemfontein, whatever the phone's own time zone is. */
  function hhmm(value) {
    var d = value instanceof Date ? value : new Date(value);
    return d.toLocaleTimeString("en-ZA", { hour: "2-digit", minute: "2-digit", hour12: false, timeZone: ZONE });
  }
  function todayISO() {
    return new Date().toLocaleDateString("en-CA", { timeZone: ZONE }); // YYYY-MM-DD
  }
  function minutesNow() {
    var t = new Date().toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit", hour12: false, timeZone: ZONE });
    return Number(t.slice(0, 2)) * 60 + Number(t.slice(3, 5));
  }
  function minutesOf(hms) { return Number(hms.slice(0, 2)) * 60 + Number(hms.slice(3, 5)); }

  var toastT;
  function toast(msg, icon) {
    toastMsg.textContent = msg;
    toastEl.querySelector(".tk").innerHTML = ico(icon || "i-check", 15);
    toastEl.classList.add("on");
    clearTimeout(toastT);
    toastT = setTimeout(function () { toastEl.classList.remove("on"); }, 3200);
  }

  function openSheet(html) {
    sheetEl.innerHTML = '<div class="grab"></div>' + html;
    scrimEl.classList.add("on");
    scrimEl.style.pointerEvents = "auto";
    requestAnimationFrame(function () { sheetEl.classList.add("on"); });
  }
  function closeSheet() {
    sheetEl.classList.remove("on");
    scrimEl.classList.remove("on");
    scrimEl.style.pointerEvents = "none";
  }

  function go(screen) {
    closeSheet();
    S.screen = screen;
    S.error = null;
    render();
    var sc = app.querySelector(".scroll");
    if (sc) sc.scrollTop = 0;
  }

  /* ==========================================================================
     NETWORK
     --------------------------------------------------------------------------
     One wrapper. Every call sends the publishable key, and the user's token
     when there is one. Failures come back as { code, detail } the way the
     Edge Functions send them, so a screen can say something useful.
     ========================================================================== */

  function ApiError(code, detail, status) {
    this.code = code || "UNKNOWN";
    this.detail = detail || null;
    this.status = status || 0;
  }

  async function api(path, options) {
    options = options || {};
    var headers = { apikey: PUBLISHABLE_KEY, "Content-Type": "application/json" };
    if (options.headers) Object.assign(headers, options.headers);
    if (S.session && S.session.access_token && !options.anon) {
      headers.Authorization = "Bearer " + S.session.access_token;
    }

    var res;
    try {
      res = await fetch(SUPABASE_URL + path, {
        method: options.method || "GET",
        headers: headers,
        body: options.body ? JSON.stringify(options.body) : undefined,
      });
    } catch (e) {
      throw new ApiError("OFFLINE", null, 0);
    }

    // The token lasts an hour. One silent refresh, then the call again.
    if (res.status === 401 && S.session && S.session.refresh_token && !options.retried) {
      var refreshed = await refreshSession();
      if (refreshed) return api(path, Object.assign({}, options, { retried: true }));
    }

    if (res.status === 204) return null;
    var text = await res.text();
    var data = null;
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }

    if (!res.ok) {
      var code = (data && (data.code || data.error_code || data.error)) || String(res.status);
      var detail = (data && (data.detail || data.msg || data.message)) || null;
      console.error("[api] " + path + " failed", res.status, code, detail);
      throw new ApiError(code, detail, res.status);
    }
    return data;
  }

  /* ==========================================================================
     AUTH
     ========================================================================== */

  function saveSession(session) {
    S.session = session;
    // Safari in private browsing throws on storage. Staying signed in is a
    // convenience; losing it must not stop the app working.
    try {
      if (session) localStorage.setItem("rm.session", JSON.stringify(session));
      else localStorage.removeItem("rm.session");
    } catch (e) { /* no storage, no memory of the session */ }
  }

  function loadSession() {
    try {
      var raw = localStorage.getItem("rm.session");
      if (raw) S.session = JSON.parse(raw);
    } catch (e) { /* a corrupt session is no session */ }
  }

  async function refreshSession() {
    try {
      var data = await api("/auth/v1/token?grant_type=refresh_token", {
        method: "POST",
        anon: true,
        body: { refresh_token: S.session.refresh_token },
      });
      saveSession(data);
      console.debug("[auth] session refreshed");
      return true;
    } catch (e) {
      console.error("[auth] refresh failed, signing out");
      saveSession(null);
      S.profile = null;
      return false;
    }
  }

  async function signIn(email, password) {
    console.debug("[auth] sign in started");
    var data = await api("/auth/v1/token?grant_type=password", {
      method: "POST",
      anon: true,
      body: { email: email.trim().toLowerCase(), password: password },
    });
    saveSession(data);
    await loadProfile();
    console.debug("[auth] signed in as", S.profile && S.profile.role);
  }

  async function signUp(fullName, email, studentNumber, password) {
    console.debug("[auth] register started");
    var data = await api("/auth/v1/signup", {
      method: "POST",
      anon: true,
      body: {
        email: email.trim().toLowerCase(),
        password: password,
        data: { full_name: fullName.trim(), student_number: studentNumber.trim().toUpperCase() },
      },
    });
    // No access_token means Supabase is holding the account until the email
    // is confirmed. Say so, rather than looking broken.
    if (!data || !data.access_token) return false;
    saveSession(data);
    await loadProfile();
    return true;
  }

  async function signOut() {
    try { await api("/auth/v1/logout", { method: "POST" }); } catch (e) { /* the token is going anyway */ }
    saveSession(null);
    S.profile = null;
    S.cart = [];
    S.orders = [];
    go("welcome");
  }

  async function loadProfile() {
    var rows = await api("/rest/v1/profiles?select=*");
    S.profile = (rows && rows[0]) || null;
    return S.profile;
  }

  /* ==========================================================================
     PRICING
     --------------------------------------------------------------------------
     A mirror of PriceCalculator.kt and of place_order() in SQL. All three
     run the same formula; the database's answer is the one that counts.
     ========================================================================== */

  function groupsOf(item) {
    return (item.option_groups || []).slice().sort(function (a, b) { return a.sort_order - b.sort_order; });
  }
  function optionsOf(group) {
    return (group.options || []).slice().sort(function (a, b) { return a.sort_order - b.sort_order; });
  }
  function hasBase(item) { return !!item.base_step_label; }
  function hasQtyGroup(item) {
    return groupsOf(item).some(function (g) { return g.type === "qty"; });
  }
  /** A build item's steppers are its quantity, so it has no outer count. */
  function isBuild(item) { return hasBase(item) || hasQtyGroup(item); }

  /** A fresh selection: the base at its start, every single group on its default. */
  function initialSelection(item) {
    var sel = { baseQty: hasBase(item) ? Math.max(item.base_step_min || 0, 1) : 0, singles: {}, counts: {}, qty: 1 };
    groupsOf(item).forEach(function (g) {
      if (g.type !== "single") return;
      var opts = optionsOf(g);
      var def = opts.filter(function (o) { return o.is_default; })[0] || (g.replaces_price ? opts[0] : null);
      if (def) sel.singles[g.id] = def.id;
    });
    return sel;
  }

  function unitPriceCents(item, sel) {
    var cents;
    if (hasBase(item)) {
      cents = toCents(item.price) * (sel.baseQty || 0);
    } else {
      cents = toCents(item.price);
    }
    groupsOf(item).forEach(function (g) {
      if (g.type === "qty") {
        optionsOf(g).forEach(function (o) {
          var n = sel.counts[o.id] || 0;
          if (n > 0) cents += toCents(o.price) * n;
        });
      } else {
        var chosen = optionsOf(g).filter(function (o) { return sel.singles[g.id] === o.id; })[0];
        if (!chosen) return;
        if (g.replaces_price && !hasBase(item)) {
          // Large chips is R40, not R28 + R40.
          cents = cents - toCents(item.price) + toCents(chosen.price);
        } else {
          cents += toCents(chosen.price);
        }
      }
    });
    return cents;
  }

  function quantityOf(item, sel) { return isBuild(item) ? 1 : (sel.qty || 1); }
  function lineTotal(item, sel) { return unitPriceCents(item, sel) * quantityOf(item, sel); }
  function unitsConsumed(item, sel) { return hasBase(item) ? (sel.baseQty || 0) : quantityOf(item, sel); }
  function canAdd(item, sel) { return unitPriceCents(item, sel) > 0; }

  /** "2 vetkoeks, 2 Polony, Cheese slice" — what staff read at the counter. */
  function labelFor(item, sel) {
    var parts = [];
    if (hasBase(item)) {
      var n = sel.baseQty || 0;
      parts.push(n === 0 ? "No " + item.base_step_singular
        : n === 1 ? "1 " + item.base_step_singular
          : n + " " + String(item.base_step_label).toLowerCase());
    }
    groupsOf(item).forEach(function (g) {
      if (g.type === "qty") {
        optionsOf(g).forEach(function (o) {
          var n = sel.counts[o.id] || 0;
          if (n === 1) parts.push(o.name);
          else if (n > 1) parts.push(n + " " + o.name);
        });
      } else {
        var chosen = optionsOf(g).filter(function (o) { return sel.singles[g.id] === o.id; })[0];
        if (chosen) parts.push(chosen.name);
      }
    });
    return parts.join(", ");
  }

  /** The cheapest this item can be, for the "from R…" label. */
  function fromPriceCents(item) {
    var sel = initialSelection(item);
    if (hasBase(item)) sel.baseQty = Math.max(item.base_step_min || 0, 1);
    return unitPriceCents(item, sel);
  }
  function variablePrice(item) { return isBuild(item) || groupsOf(item).some(function (g) { return g.replaces_price; }); }
  function priceTag(item) {
    return variablePrice(item) ? "from " + R(fromPriceCents(item)) : R(toCents(item.price));
  }

  /* ==========================================================================
     CART
     ========================================================================== */

  function cartKey(item, sel) { return item.id + "#" + JSON.stringify([sel.baseQty, sel.singles, sel.counts]); }
  function cartCount() { return S.cart.reduce(function (a, l) { return a + quantityOf(l.item, l.sel); }, 0); }
  function cartTotal() { return S.cart.reduce(function (a, l) { return a + lineTotal(l.item, l.sel); }, 0); }

  function addToCart(item, sel) {
    if (!canAdd(item, sel)) return;
    var key = cartKey(item, sel);
    var existing = S.cart.filter(function (l) { return l.key === key; })[0];
    if (existing && !isBuild(item)) {
      existing.sel.qty += sel.qty;
    } else if (!existing) {
      S.cart.push({ key: key, item: item, sel: sel });
    }
    saveCart();
    console.debug("[cart] added", item.slug, R(lineTotal(item, sel)));
  }

  function saveCart() {
    try {
      localStorage.setItem("rm.cart", JSON.stringify(S.cart.map(function (l) {
        return { itemId: l.item.id, sel: l.sel };
      })));
    } catch (e) { /* a full storage quota must not break ordering */ }
  }

  function restoreCart() {
    try {
      var raw = JSON.parse(localStorage.getItem("rm.cart") || "[]");
      S.cart = raw.map(function (row) {
        var item = S.menu.filter(function (m) { return m.id === row.itemId; })[0];
        return item ? { key: cartKey(item, row.sel), item: item, sel: row.sel } : null;
      }).filter(Boolean);
    } catch (e) { S.cart = []; }
  }

  /* ==========================================================================
     DATA
     ========================================================================== */

  async function loadMenu() {
    S.menu = await api("/rest/v1/menu_items?select=" + encodeURIComponent(MENU_SELECT) + "&order=sort_order.asc");
    console.debug("[menu] loaded", S.menu.length, "items");
    if (!S.cart.length) restoreCart();
  }

  async function loadSlots() {
    S.slots = await api("/rest/v1/rpc/ensure_slots", { method: "POST", body: { p_date: todayISO() } });
    S.slots.sort(function (a, b) { return a.starts_at.localeCompare(b.starts_at); });
    var open = openSlots();
    var current = open.filter(function (s) { return s.id === S.slotId; })[0];
    if (!current || current.orders_taken >= current.capacity) {
      var first = open.filter(function (s) { return s.orders_taken < s.capacity; })[0];
      S.slotId = first ? first.id : null;
    }
  }

  /** Breaks still taking orders: hidden once they are within 5 minutes of ending. */
  function openSlots() {
    var now = minutesNow();
    return S.slots.filter(function (s) { return now < minutesOf(s.ends_at) - SLOT_CUTOFF_MIN; });
  }

  async function loadOrders() {
    S.orders = await api("/rest/v1/orders?select=" + encodeURIComponent(ORDER_SELECT) +
      "&order=placed_at.desc&limit=50");
  }

  async function loadWeather() {
    try {
      var res = await fetch("https://api.open-meteo.com/v1/forecast?latitude=-29.12&longitude=26.21" +
        "&current=temperature_2m,weather_code&daily=precipitation_probability_max&timezone=" + ZONE +
        "&forecast_days=2");
      var d = await res.json();
      S.weather = {
        temp: Math.round(d.current.temperature_2m),
        code: d.current.weather_code,
        rain: (d.daily.precipitation_probability_max || [0])[0] || 0,
      };
    } catch (e) { /* the weather never blocks the menu */ }
  }

  function sky(code) {
    if (code === 0 || code === 1) return "Clear";
    if (code === 2 || code === 3) return "Partly cloudy";
    if (code === 45 || code === 48) return "Fog";
    if (code >= 51 && code <= 57) return "Drizzle";
    if (code >= 61 && code <= 67) return "Rain";
    if (code >= 80 && code <= 86) return "Showers";
    if (code >= 95) return "Thunderstorm";
    return "Partly cloudy";
  }
  function weatherMood() {
    if (!S.weather) return "neutral";
    if (S.weather.temp < 15 || S.weather.rain >= 50) return "cold";
    if (S.weather.temp >= 27) return "hot";
    return "neutral";
  }
  /** Cold or wet lifts hot food; hot lifts cold drinks; otherwise menu order. */
  function weatherSorted(list) {
    var mood = weatherMood();
    if (mood === "neutral") return list;
    var want = mood === "cold" ? "hot" : "cold";
    return list.slice().sort(function (a, b) {
      var A = a.temperature_tag === want ? 0 : 1, B = b.temperature_tag === want ? 0 : 1;
      return A - B || a.sort_order - b.sort_order;
    });
  }

  async function loadStudentExtras() {
    try {
      var tops = await api("/rest/v1/wallet_transactions?type=eq.topup&select=id&limit=1");
      S.hasToppedUp = tops.length > 0;
    } catch (e) { S.hasToppedUp = null; }
    try {
      var stamps = await api("/rest/v1/loyalty_stamps?select=id&limit=200");
      S.stamps = stamps.length;
    } catch (e) { /* not fatal */ }
  }

  /* ==========================================================================
     ACTIONS THAT CHANGE THE DATABASE
     ========================================================================== */

  async function placeOrder(method) {
    var body = {
      slot_id: S.slotId,
      payment_method: method,
      // The same reference on a retry returns the first order rather than
      // making a second one.
      client_ref: crypto.randomUUID(),
      lines: S.cart.map(function (l) {
        var line = { item_id: l.item.id, options: [] };
        if (hasBase(l.item)) line.base_qty = l.sel.baseQty || 0;
        if (!isBuild(l.item)) line.quantity = l.sel.qty || 1;
        Object.keys(l.sel.singles).forEach(function (gid) {
          line.options.push({ option_id: l.sel.singles[gid] });
        });
        Object.keys(l.sel.counts).forEach(function (oid) {
          if (l.sel.counts[oid] > 0) line.options.push({ option_id: oid, count: l.sel.counts[oid] });
        });
        return line;
      }),
    };
    console.debug("[order] placing", body.lines.length, "lines", R(cartTotal()));
    var order = await api("/functions/v1/place-order", { method: "POST", body: body });
    console.debug("[order] placed #" + order.order_number, "code", order.collection_code);
    S.cart = [];
    saveCart();
    await Promise.all([loadProfile(), loadOrders(), loadMenu(), loadSlots()]);
    return order;
  }

  async function setStatus(orderId, status) {
    console.debug("[order] status ->", status);
    var order = await api("/functions/v1/order-status", {
      method: "PATCH",
      body: { order_id: orderId, status: status },
    });
    await Promise.all([loadOrders(), loadProfile()]);
    return order;
  }

  async function adjustStock(itemId, delta) {
    console.debug("[stock] adjust", itemId, delta);
    await api("/rest/v1/rpc/adjust_stock", { method: "POST", body: { p_item_id: itemId, p_delta: delta } });
    await loadMenu();
  }

  async function topUp(studentNumber, amountRands) {
    console.debug("[wallet] top up", studentNumber, amountRands);
    return api("/functions/v1/load-wallet", {
      method: "POST",
      body: { student_number: studentNumber, amount: amountRands },
    });
  }

  async function loadSales() {
    S.sales = await api("/rest/v1/rpc/sales_summary", {
      method: "POST",
      body: { p_from: todayISO(), p_to: todayISO() },
    });
  }

  /** One sentence per reason the server can refuse, like the Android app. */
  function friendly(err) {
    if (!(err instanceof ApiError)) return "Something went wrong. Try again.";
    switch (err.code) {
      case "OFFLINE": return "No connection. Check your signal and try again.";
      case "OUT_OF_STOCK": return (err.detail || "Something in your cart") + " just sold out. Remove it and try again.";
      case "SLOT_FULL": return "That collection time just filled up. Pick another one.";
      case "SLOT_NOT_TODAY": return "That collection time has closed. Pick another one.";
      case "INSUFFICIENT_FUNDS": return "Your wallet balance is too low for this order. Top up at the counter.";
      case "COUNTER_BLOCKED":
        return err.detail === "TOO_MANY_NO_SHOWS"
          ? "Pay-at-counter is switched off after two uncollected orders."
          : "Pay-at-counter needs at least one wallet top-up first.";
      case "EMPTY_SELECTION": return "One of the items has nothing chosen. Edit it in your cart.";
      case "STUDENT_NUMBER_REQUIRED": return "Add your student number before ordering.";
      case "INVALID_TRANSITION": return "That order has already moved on. The queue is up to date now.";
      case "STUDENT_NOT_FOUND": return "No student with that number. Check the card and try again.";
      case "AMOUNT_OUT_OF_RANGE": return "Top-ups are between R10 and R1000.";
      case "FORBIDDEN": return "You do not have access to that.";
      case "invalid_credentials":
      case "invalid_grant": return "Email or password is incorrect.";
      case "user_already_exists":
      case "email_exists": return "That email already has an account. Sign in instead.";
      case "over_email_send_rate_limit": return "Too many sign-ups just now. Wait a minute and try again.";
      case "weak_password": return "Use at least 8 characters.";
      default: return err.detail || "Something went wrong. Try again.";
    }
  }

  /* ==========================================================================
     SHARED BITS OF UI
     ========================================================================== */

  function stockPill(m) {
    if (!m.is_available || m.stock_quantity <= 0) return '<span class="pill err"><span class="dot"></span>Sold out</span>';
    if (m.stock_quantity <= m.reorder_level) return '<span class="pill warn"><span class="dot"></span>' + m.stock_quantity + ' left</span>';
    return '<span class="pill ok"><span class="dot"></span>In stock</span>';
  }

  function statusPill(st) {
    if (st === "placed") return '<span class="pill warn"><span class="dot"></span>New</span>';
    if (st === "preparing") return '<span class="pill sky"><span class="dot"></span>Preparing</span>';
    if (st === "ready") return '<span class="pill ok"><span class="dot"></span>Ready</span>';
    if (st === "collected") return '<span class="pill">Collected</span>';
    if (st === "no_show") return '<span class="pill err">Not collected</span>';
    return '<span class="pill">Cancelled</span>';
  }

  function isStaff() { return S.profile && S.profile.role === "staff"; }

  function tabbar(active) {
    var tabs = isStaff()
      ? [["queue", "Queue", "i-clock"], ["stock", "Stock", "i-box"], ["sales", "Sales", "i-chart"], ["profile", "Profile", "i-user"]]
      : [["menu", "Menu", "i-grid"], ["cart", "Cart", "i-cart"], ["orders", "Orders", "i-clock"], ["profile", "Profile", "i-user"]];
    return '<nav class="tabbar">' + tabs.map(function (t) {
      var badge = "";
      if (t[0] === "cart" && cartCount() > 0) badge = '<span class="badge">' + cartCount() + '</span>';
      if (t[0] === "queue") {
        var n = S.orders.filter(function (o) { return o.status === "placed"; }).length;
        if (n) badge = '<span class="badge">' + n + '</span>';
      }
      return '<button class="tab' + (active === t[0] ? " active" : "") + '" data-act="tab" data-arg="' + t[0] + '">' +
        ico(t[2], 21) + t[1] + badge + '</button>';
    }).join("") + '</nav>';
  }

  function banner(tone, text) {
    var icons = { ok: "i-check", warn: "i-warn", err: "i-warn", info: "i-clock" };
    return '<div class="banner ' + (tone === "info" ? "" : tone) + '">' + ico(icons[tone] || "i-clock", 16) +
      '<span>' + text + '</span></div>';
  }

  function loading(rows) {
    var out = '<div class="list">';
    for (var i = 0; i < (rows || 4); i++) {
      out += '<div class="list-row"><span class="thumb muted"></span><span class="grow">' +
        '<span class="sk" style="width:50%"></span><span class="sk" style="width:30%;margin-top:8px"></span></span></div>';
    }
    return out + '</div>';
  }

  function emptyState(icon, title, body) {
    return '<div class="empty"><span class="thumb muted">' + ico(icon, 26) + '</span>' +
      '<p class="h-md">' + esc(title) + '</p><p class="tiny" style="margin-top:6px">' + esc(body) + '</p></div>';
  }

  /* ==========================================================================
     SCREENS
     ========================================================================== */

  var V = {};

  V.welcome = function () {
    return '<div class="view"><div class="scroll" style="padding-top:40px">' +
      '<div style="text-align:center;margin-bottom:28px">' + mark(64) +
      '<h2 class="h-lg" style="font-size:24px;margin-top:14px">Ri-magwinya</h2>' +
      '<p class="sub" style="margin-top:6px">Order ahead. Skip the queue.</p></div>' +
      '<button class="card row between" style="width:100%;text-align:left" data-act="go" data-arg="login">' +
      '<span><span class="h-md" style="display:block">I am a student</span>' +
      '<span class="tiny">Browse the menu, order, and collect with a code.</span></span>' + ico("i-chev", 18) + '</button>' +
      '<div class="sp12"></div>' +
      '<button class="card row between" style="width:100%;text-align:left" data-act="go" data-arg="login">' +
      '<span><span class="h-md" style="display:block">I work at the tuckshop</span>' +
      '<span class="tiny">Work the order queue, manage stock and top up wallets.</span></span>' + ico("i-chev", 18) + '</button>' +
      '<div class="sp24"></div>' +
      '<p class="tiny" style="text-align:center">Which half of the app you get is decided by your account, not by this choice.</p>' +
      '</div></div>';
  };

  V.login = function () {
    return '<div class="view"><div class="appbar">' +
      '<button class="iconbtn ghost" data-act="go" data-arg="welcome" aria-label="Back">' + ico("i-back", 19) + '</button>' +
      '<h3>Sign in</h3></div><div class="scroll">' +
      (S.error ? banner("err", esc(S.error)) + '<div class="sp16"></div>' : "") +
      '<p class="label">Email</p><div class="field">' + ico("i-mail", 17) +
      '<input id="inEmail" type="email" inputmode="email" autocomplete="email" placeholder="you@example.com"></div>' +
      '<div class="sp12"></div>' +
      '<p class="label">Password</p><div class="field">' + ico("i-lock", 17) +
      '<input id="inPass" type="password" autocomplete="current-password" placeholder="At least 8 characters"></div>' +
      '<div class="sp20"></div>' +
      '<button class="btn" data-act="signin"' + (S.busy ? " disabled" : "") + '>' +
      (S.busy ? "Signing in…" : "Sign in") + '</button>' +
      '<div class="sp12"></div>' +
      '<button class="btn secondary" data-act="go" data-arg="register">New here? Create an account</button>' +
      '</div></div>';
  };

  V.register = function () {
    return '<div class="view"><div class="appbar">' +
      '<button class="iconbtn ghost" data-act="go" data-arg="login" aria-label="Back">' + ico("i-back", 19) + '</button>' +
      '<h3>Create your account</h3></div><div class="scroll">' +
      (S.error ? banner("err", esc(S.error)) + '<div class="sp16"></div>' : "") +
      '<p class="label">Full name</p><div class="field">' + ico("i-user", 17) +
      '<input id="rgName" placeholder="Thabo Mokoena" autocomplete="name"></div><div class="sp12"></div>' +
      '<p class="label">Email</p><div class="field">' + ico("i-mail", 17) +
      '<input id="rgMail" type="email" inputmode="email" placeholder="you@example.com" autocomplete="email"></div><div class="sp12"></div>' +
      '<p class="label">Student number</p><div class="field">' + ico("i-sig", 17) +
      '<input id="rgNum" placeholder="ST2024001"></div><div class="sp12"></div>' +
      '<p class="label">Password</p><div class="field">' + ico("i-lock", 17) +
      '<input id="rgPass" type="password" placeholder="At least 8 characters" autocomplete="new-password"></div><div class="sp12"></div>' +
      '<p class="label">Confirm password</p><div class="field">' + ico("i-lock", 17) +
      '<input id="rgPass2" type="password" placeholder="Type it again" autocomplete="new-password"></div>' +
      '<div class="sp16"></div>' +
      banner("info", "We store your name, email, student number and order history so staff can find your order at the counter. Nothing is shared with anyone else.") +
      '<div class="sp16"></div>' +
      '<button class="btn" data-act="register"' + (S.busy ? " disabled" : "") + '>' +
      (S.busy ? "Creating your account…" : "Create account") + '</button>' +
      '</div></div>';
  };

  V.menu = function () {
    var mine = S.orders.filter(function (o) {
      return ["placed", "preparing", "ready"].indexOf(o.status) > -1;
    });
    var active = mine[mine.length - 1];
    var q = S.q.toLowerCase();
    var list = weatherSorted(S.menu).filter(function (m) {
      return (S.cat === "All" || m.category === S.cat) &&
        (!q || m.name.toLowerCase().indexOf(q) > -1 || (m.description || "").toLowerCase().indexOf(q) > -1);
    });

    return '<div class="view"><div class="scroll" style="padding-top:0">' +
      '<div class="hero">' +
      '<div class="row between" style="margin-bottom:18px">' +
      '<span class="row" style="gap:11px">' + mark(38) +
      '<span><span class="tiny" style="display:block">Good day</span>' +
      '<span class="h-lg" style="font-size:20px">' + esc((S.profile.full_name || "").split(" ")[0]) + '</span></span></span>' +
      '<button class="iconbtn" data-act="bell" aria-label="Notifications" style="border-color:rgba(255,255,255,.2);background:rgba(255,255,255,.12);color:#fff">' +
      ico("i-bell", 18) + '</button></div>' +
      '<div class="row between" style="padding:13px 15px;background:rgba(255,255,255,.11);border-radius:13px">' +
      '<span class="row" style="gap:10px">' + ico("i-wallet", 19) + '<span style="font-size:12.5px">Campus wallet</span></span>' +
      '<span style="font-size:16px;font-weight:600;font-variant-numeric:tabular-nums">' + R(toCents(S.profile.wallet_balance)) + '</span></div>' +
      (S.weather ? '<p class="tiny" style="margin-top:10px;color:rgba(255,255,255,.75)">' + S.weather.temp + '°C · ' +
        sky(S.weather.code) + ' · ' + (weatherMood() === "cold" ? "Hot food first today"
          : weatherMood() === "hot" ? "Cold drinks first today" : "Today’s menu as usual") + '</p>' : "") +
      '</div><div class="sp16"></div>' +
      (S.weather && S.weather.rain >= 50
        ? banner("info", "Rain expected. Order ahead so you’re not queueing outside.") + '<div class="sp16"></div>' : "") +
      (active ? '<button class="card row between" style="width:100%;text-align:left;border-color:var(--sky)" data-act="openorder" data-arg="' + active.id + '">' +
        '<span class="row" style="gap:11px"><span class="thumb sm">' + ico("i-clock", 19) + '</span>' +
        '<span><span class="h-md" style="display:block">Order #' + active.order_number + ' · code ' + esc(active.collection_code) + '</span>' +
        '<span class="tiny" style="display:block;margin-top:2px">' +
        (active.status === "ready" ? "Ready for collection" : active.status === "preparing" ? "Being prepared" : "Waiting to be prepared") +
        '</span></span></span>' + ico("i-chev", 17) + '</button><div class="sp16"></div>' : "") +
      '<div class="field">' + ico("i-search", 17) +
      '<input id="q" data-live="1" placeholder="Search the menu" value="' + esc(S.q) + '" aria-label="Search the menu"></div>' +
      '<div class="sp16"></div>' +
      '<div class="chips">' + ["All", "Meals", "Snacks", "Drinks"].map(function (c) {
        return '<button class="chip' + (S.cat === c ? " on" : "") + '" data-act="cat" data-arg="' + c + '">' + c + '</button>';
      }).join("") + '</div><div class="sp16"></div>' +
      '<div class="row between" style="margin-bottom:10px"><span class="h-md">Today’s menu</span>' +
      '<span class="pill ok"><span class="dot"></span>Live stock</span></div>' +
      (!S.menu.length ? loading(6) : !list.length
        ? emptyState("i-search", "Nothing matches that", "Try a different word, or pick another category.")
        : '<div class="list">' + list.map(function (m) {
          var out = !m.is_available || m.stock_quantity <= 0;
          return '<button class="list-row' + (out ? " dim" : "") + '" ' +
            (out ? "disabled" : 'data-act="item" data-arg="' + m.id + '"') + '>' +
            '<span class="thumb' + (out ? " muted" : "") + '">' + food(m.icon_key, 30) + '</span>' +
            '<span class="grow"><span class="h-md" style="display:block">' + esc(m.name) + '</span>' +
            '<span style="display:block;margin-top:5px">' + stockPill(m) + '</span></span>' +
            '<span class="price">' + priceTag(m) + '</span></button>';
        }).join("") + '</div>') +
      '</div>' + tabbar("menu") + '</div>';
  };

  V.cart = function () {
    if (!S.cart.length) {
      return '<div class="view"><div class="appbar"><h3>Your cart</h3></div><div class="scroll">' +
        emptyState("i-cart", "Your cart is empty", "Add something from the menu and it shows up here.") +
        '<button class="btn secondary" style="width:auto;margin:0 auto;padding:0 22px" data-act="tab" data-arg="menu">Browse the menu</button>' +
        '</div>' + tabbar("cart") + '</div>';
    }
    var open = openSlots();
    var chosen = open.filter(function (s) { return s.id === S.slotId; })[0];

    return '<div class="view"><div class="appbar"><h3>Your cart</h3>' +
      '<span class="pill">' + cartCount() + ' item' + (cartCount() === 1 ? "" : "s") + '</span></div><div class="scroll">' +
      '<div class="list">' + S.cart.map(function (l) {
        var label = labelFor(l.item, l.sel);
        return '<div class="list-row"><span class="thumb">' + food(l.item.icon_key, 30) + '</span>' +
          '<span class="grow"><span class="h-md" style="display:block">' + esc(l.item.name) + '</span>' +
          (label ? '<span class="tiny" style="display:block;margin-top:2px">' + esc(label) + '</span>' : "") +
          '<span class="tiny">' + R(unitPriceCents(l.item, l.sel)) + ' each</span></span>' +
          (isBuild(l.item)
            ? '<button class="btn sm secondary" style="width:auto;padding:0 14px" data-act="rm" data-arg="' + l.key + '">Remove</button>'
            : '<span class="stepper"><button data-act="qty" data-arg="' + l.key + ':-1" aria-label="One fewer">' + ico("i-minus", 15) + '</button>' +
            '<span class="n">' + l.sel.qty + '</span>' +
            '<button data-act="qty" data-arg="' + l.key + ':1" aria-label="One more">' + ico("i-plus", 15) + '</button></span>') +
          '</div>';
      }).join("") + '</div>' +
      '<div class="sp24"></div><p class="label">Collection time</p>' +
      (!open.length
        ? banner("info", "No more collection times today. The tuckshop takes orders again tomorrow.")
        : '<div class="chips">' + open.map(function (s) {
          var full = s.orders_taken >= s.capacity;
          return '<button class="chip' + (S.slotId === s.id ? " on" : "") + '"' + (full ? " disabled" : "") +
            ' data-act="slot" data-arg="' + s.id + '">' + esc(s.name) + (full ? " · Full" : "") + '</button>';
        }).join("") + '</div>' +
        (chosen ? '<div class="sp8"></div><p class="tiny">' + chosen.starts_at.slice(0, 5) + '–' + chosen.ends_at.slice(0, 5) +
          ' · ' + chosen.orders_taken + ' of ' + chosen.capacity + ' taken</p>' : "")) +
      '<div class="sp24"></div>' +
      '<div class="card"><div class="row between"><span class="sub">Subtotal</span><span class="price">' + R(cartTotal()) + '</span></div>' +
      '<div class="sp12"></div><div style="height:1px;background:var(--border)"></div><div class="sp12"></div>' +
      '<div class="row between"><span class="h-md">Total</span><span class="price" style="font-size:18px">' + R(cartTotal()) + '</span></div></div>' +
      '<div class="sp20"></div>' +
      '<button class="btn" data-act="go" data-arg="checkout"' + (chosen ? "" : " disabled") + '>' +
      (chosen ? "Continue to checkout" : "Pick a collection time") + '</button>' +
      '</div>' + tabbar("cart") + '</div>';
  };

  V.checkout = function () {
    var slot = S.slots.filter(function (s) { return s.id === S.slotId; })[0] || {};
    var total = cartTotal();
    var balance = toCents(S.profile.wallet_balance);
    var short = total - balance;
    var noShowBlocked = (S.profile.no_show_count || 0) >= 2;
    var counterOk = S.hasToppedUp === true && !noShowBlocked;

    return '<div class="view"><div class="appbar">' +
      '<button class="iconbtn ghost" data-act="go" data-arg="cart" aria-label="Back">' + ico("i-back", 19) + '</button>' +
      '<h3>Checkout</h3></div><div class="scroll">' +
      (S.error ? banner("err", esc(S.error)) + '<div class="sp16"></div>' : "") +
      '<p class="label">Order summary</p><div class="list">' + S.cart.map(function (l) {
        var label = labelFor(l.item, l.sel);
        return '<div class="list-row" style="min-height:50px"><span class="pill sky">' + quantityOf(l.item, l.sel) + '×</span>' +
          '<span class="grow"><span class="h-md trunc" style="display:block">' + esc(l.item.name) + '</span>' +
          (label ? '<span class="tiny">' + esc(label) + '</span>' : "") + '</span>' +
          '<span class="price">' + R(lineTotal(l.item, l.sel)) + '</span></div>';
      }).join("") + '</div>' +
      '<div class="sp24"></div><p class="label">Collection</p>' +
      '<button class="card row between" style="width:100%;text-align:left" data-act="go" data-arg="cart">' +
      '<span><span class="h-md" style="display:block">' + esc(slot.name || "") + '</span>' +
      '<span class="tiny" style="display:block;margin-top:2px">Today · ' +
      (slot.starts_at || "").slice(0, 5) + '–' + (slot.ends_at || "").slice(0, 5) + '</span></span>' + ico("i-edit", 17) + '</button>' +
      '<div class="sp24"></div><p class="label">Payment</p>' +
      '<div class="list"><div class="list-row" style="border:1.5px solid var(--navy);border-radius:var(--r-m)">' +
      '<span class="thumb sm">' + ico("i-wallet", 20) + '</span>' +
      '<span class="grow"><span class="h-md" style="display:block">Campus wallet</span>' +
      '<span class="tiny">Balance ' + R(balance) + '</span></span>' + ico("i-check", 18) + '</div></div>' +
      '<div class="sp8"></div>' +
      (short > 0
        ? banner("err", "Not enough balance. You are short " + R(short) + ". Top up at the tuckshop counter.")
        : '<p class="tiny">' + R(balance - total) + ' left after this order.</p>') +
      '<div class="sp12"></div>' +
      '<div class="list"><div class="list-row"' + (counterOk ? "" : ' style="opacity:.55"') + '>' +
      '<span class="thumb sm">' + ico("i-store", 20) + '</span>' +
      '<span class="grow"><span class="h-md" style="display:block">Pay at the counter</span>' +
      '<span class="tiny">' + (counterOk ? "Pay on the speed point when you collect."
        : noShowBlocked ? "Not available. Two orders were not collected."
          : "Available once you have topped up your wallet at least once.") + '</span></span>' +
      (counterOk ? '<button class="btn sm secondary" style="width:auto;padding:0 14px" data-act="place" data-arg="counter">Use</button>' : "") +
      '</div></div>' +
      '<div class="sp16"></div>' +
      banner("warn", "You can cancel free of charge until the tuckshop starts preparing your order.") +
      '<div class="sp24"></div>' +
      '<div class="row between" style="margin-bottom:12px"><span class="h-md">Total</span>' +
      '<span class="price" style="font-size:19px">' + R(total) + '</span></div>' +
      '<button class="btn" data-act="place" data-arg="wallet"' + (short > 0 || S.busy ? " disabled" : "") + '>' +
      (S.busy ? "Placing your order…" : "Place order · " + R(total)) + '</button>' +
      '</div></div>';
  };

  V.order = function () {
    var o = S.orders.filter(function (x) { return x.id === S.openOrderId; })[0];
    if (!o) return V.orders();
    var slot = o.collection_slots || {};
    var steps = [
      ["Order placed", hhmm(o.placed_at)],
      [o.payment_method === "wallet" ? "Payment confirmed" : "Pay on collection", o.payment_method === "wallet" ? hhmm(o.placed_at) : ""],
      ["Preparing", o.prepared_at ? hhmm(o.prepared_at) : "Pending"],
      ["Ready for collection", o.ready_at ? hhmm(o.ready_at) + " · you were notified" : "Pending"],
      ["Collected", o.completed_at ? hhmm(o.completed_at) : "Pending"],
    ];
    var idx = { placed: 1, preparing: 2, ready: 3, collected: 5 }[o.status];
    var live = ["placed", "preparing", "ready"].indexOf(o.status) > -1;

    return '<div class="view"><div class="appbar">' +
      '<button class="iconbtn ghost" data-act="tab" data-arg="orders" aria-label="Back">' + ico("i-back", 19) + '</button>' +
      '<h3>Order #' + o.order_number + '</h3>' + statusPill(o.status) + '</div><div class="scroll">' +
      (S.error ? banner("err", esc(S.error)) + '<div class="sp16"></div>' : "") +
      (o.status === "ready" ? banner("ok", "<strong>Your order is ready.</strong> Show the code at the counter.") + '<div class="sp16"></div>' : "") +
      (o.status === "cancelled" ? banner("info", "This order was cancelled" +
        (o.payment_method === "wallet" ? " and " + R(toCents(o.total_amount)) + " went back to your wallet." : ".")) + '<div class="sp16"></div>' : "") +
      (o.status === "no_show" ? banner("warn", "This order was not collected before the break ended.") + '<div class="sp16"></div>' : "") +
      (live ? '<div class="card" style="text-align:center;padding:24px 16px">' +
        '<p class="label" style="margin-bottom:16px">Show this code at the counter</p>' +
        '<div class="code">' + String(o.collection_code).trim().split("").map(function (c) { return "<span>" + c + "</span>"; }).join("") + '</div>' +
        '<p class="tiny" style="margin-top:16px">' + esc(slot.name || "") + ' · ' +
        (slot.starts_at || "").slice(0, 5) + '–' + (slot.ends_at || "").slice(0, 5) + '</p></div><div class="sp24"></div>' : "") +
      (o.status === "cancelled" || o.status === "no_show" ? "" :
        '<div class="rail">' + steps.map(function (s, i) {
          return '<div class="rail-step ' + (i < idx ? "done" : i === idx ? "now" : "") + '">' +
            '<p class="rail-t">' + s[0] + '</p><p class="rail-s">' + s[1] + '</p></div>';
        }).join("") + '</div><div class="sp24"></div>') +
      '<p class="label">Items</p><div class="list">' + (o.order_items || []).map(function (i) {
        return '<div class="list-row" style="min-height:50px"><span class="pill sky">' + i.quantity + '×</span>' +
          '<span class="grow"><span class="h-md trunc" style="display:block">' + esc(i.item_name) + '</span>' +
          (i.options_label ? '<span class="tiny">' + esc(i.options_label) + '</span>' : "") + '</span>' +
          '<span class="price">' + R(toCents(i.subtotal)) + '</span></div>';
      }).join("") + '</div><div class="sp12"></div>' +
      '<div class="row between" style="padding:0 4px"><span class="h-md">' +
      (o.payment_method === "counter" && live ? "Due at the counter" : "Total paid") + '</span>' +
      '<span class="price" style="font-size:17px">' + R(toCents(o.total_amount)) + '</span></div>' +
      '<div class="sp24"></div>' +
      (o.status === "placed"
        ? '<button class="btn danger" data-act="cancel" data-arg="' + o.id + '">Cancel order</button>'
        : live ? '<p class="tiny" style="text-align:center">This order is being prepared and can no longer be cancelled.</p>' : "") +
      '</div>' + tabbar("orders") + '</div>';
  };

  V.orders = function () {
    var live = S.orders.filter(function (o) { return ["placed", "preparing", "ready"].indexOf(o.status) > -1; });
    var past = S.orders.filter(function (o) { return ["placed", "preparing", "ready"].indexOf(o.status) === -1; });

    function rows(list) {
      return '<div class="list">' + list.map(function (o) {
        var n = (o.order_items || []).length;
        return '<button class="list-row" data-act="openorder" data-arg="' + o.id + '">' +
          '<span class="thumb' + (live.indexOf(o) === -1 ? " muted" : "") + '">' + ico("i-clock", 22) + '</span>' +
          '<span class="grow"><span class="h-md" style="display:block">#' + o.order_number + ' · ' + n + ' item' + (n === 1 ? "" : "s") + '</span>' +
          '<span class="tiny">' + hhmm(o.placed_at) + ' · ' + esc((o.collection_slots || {}).name || "") + '</span></span>' +
          '<span style="text-align:right"><span class="price" style="display:block">' + R(toCents(o.total_amount)) + '</span>' +
          '<span style="display:block;margin-top:4px">' + statusPill(o.status) + '</span></span></button>';
      }).join("") + '</div>';
    }

    return '<div class="view"><div class="appbar"><h3>Orders</h3></div><div class="scroll">' +
      (!S.orders.length ? emptyState("i-clock", "No orders yet", "Your orders and collection codes appear here.") :
        (live.length ? '<p class="label">In progress</p>' + rows(live) + '<div class="sp24"></div>' : "") +
        (past.length ? '<p class="label">Past orders</p>' + rows(past) : "")) +
      '</div>' + tabbar("orders") + '</div>';
  };

  /* ---------- STAFF ---------- */

  V.queue = function () {
    var map = { New: "placed", Preparing: "preparing", Ready: "ready" };
    var today = todayISO();
    var open = S.orders.filter(function (o) {
      return ["placed", "preparing", "ready"].indexOf(o.status) > -1 && o.placed_at.slice(0, 10) >= today;
    });
    var list = (S.staffFilter === "All" ? open : open.filter(function (o) { return o.status === map[S.staffFilter]; }))
      .sort(function (a, b) { return a.placed_at.localeCompare(b.placed_at); });
    var counts = {
      n: open.filter(function (o) { return o.status === "placed"; }).length,
      p: open.filter(function (o) { return o.status === "preparing"; }).length,
      r: open.filter(function (o) { return o.status === "ready"; }).length,
    };

    return '<div class="view"><div class="appbar"><h3>Order queue</h3>' +
      '<span class="pill sky"><span class="dot"></span>Live</span>' +
      '<button class="btn sm secondary" style="width:auto;padding:0 14px;margin-left:auto" data-act="go" data-arg="topup">Top up</button>' +
      '</div><div class="scroll">' +
      (S.error ? banner("err", esc(S.error)) + '<div class="sp16"></div>' : "") +
      '<div class="row" style="gap:8px">' +
      '<div class="card grow" style="padding:13px"><p class="tiny">New</p><p class="stat-n">' + counts.n + '</p></div>' +
      '<div class="card grow" style="padding:13px"><p class="tiny">Preparing</p><p class="stat-n">' + counts.p + '</p></div>' +
      '<div class="card grow" style="padding:13px"><p class="tiny">Ready</p><p class="stat-n">' + counts.r + '</p></div></div>' +
      '<div class="sp16"></div><div class="chips">' + ["New", "Preparing", "Ready", "All"].map(function (f) {
        return '<button class="chip' + (S.staffFilter === f ? " on" : "") + '" data-act="sfilter" data-arg="' + f + '">' + f + '</button>';
      }).join("") + '</div><div class="sp16"></div>' +
      (!list.length ? emptyState("i-check", "Nothing in “" + S.staffFilter + "”", "The queue is clear for this filter.") :
        list.map(function (o) {
          var next = o.status === "placed" ? ["Start preparing", "preparing"]
            : o.status === "preparing" ? ["Mark ready", "ready"] : ["Mark collected", "collected"];
          var slot = o.collection_slots || {};
          var ended = slot.ends_at && minutesNow() >= minutesOf(slot.ends_at);
          return '<div class="card" style="margin-bottom:12px' + (o.status === "ready" ? ";border-color:var(--ok)" : "") + '">' +
            '<div class="row between" style="margin-bottom:10px">' +
            '<span class="row" style="gap:9px"><span class="pill sky">#' + o.order_number + '</span>' +
            '<span class="tiny">' + hhmm(o.placed_at) + ' · ' + esc(slot.name || "") + '</span></span>' + statusPill(o.status) + '</div>' +
            '<p class="sub" style="color:var(--text)">' + (o.order_items || []).map(function (i) {
              return i.quantity + "× " + esc(i.item_name) + (i.options_label ? " (" + esc(i.options_label) + ")" : "");
            }).join(" · ") + '</p>' +
            (o.payment_method === "counter"
              ? '<p class="tiny" style="color:var(--warn);margin-top:6px">Collect ' + R(toCents(o.total_amount)) + ' at the counter</p>' : "") +
            '<div class="row between" style="margin-top:14px">' +
            '<span class="tiny">Code ' + esc(String(o.collection_code).trim()) + ' · ' + esc((o.profiles || {}).full_name || "") + '</span>' +
            '<span class="row" style="gap:8px">' +
            (o.status === "ready" && ended
              ? '<button class="btn sm danger" style="width:auto;padding:0 12px" data-act="advance" data-arg="' + o.id + ':no_show">No-show</button>' : "") +
            '<button class="btn sm" style="width:auto;padding:0 14px" data-act="advance" data-arg="' + o.id + ':' + next[1] + '">' + next[0] + '</button>' +
            '</span></div></div>';
        }).join("")) +
      '</div>' + tabbar("queue") + '</div>';
  };

  V.stock = function () {
    var low = S.menu.filter(function (m) { return m.stock_quantity <= m.reorder_level; });
    return '<div class="view"><div class="appbar"><h3>Stock</h3></div><div class="scroll">' +
      (S.error ? banner("err", esc(S.error)) + '<div class="sp16"></div>' : "") +
      (low.length
        ? banner("warn", "<strong>" + low.length + " item" + (low.length === 1 ? "" : "s") + " need attention.</strong> " +
          low.map(function (m) { return esc(m.name); }).join(", ") + ".")
        : banner("ok", "Everything is above its reorder level.")) +
      '<div class="sp16"></div>' +
      (!S.menu.length ? loading(6) : '<div class="list">' + S.menu.map(function (m) {
        var out = m.stock_quantity <= 0;
        return '<div class="list-row"><span class="thumb' + (out ? " muted" : "") + '">' + food(m.icon_key, 28) + '</span>' +
          '<span class="grow"><span class="h-md" style="display:block">' + esc(m.name) + '</span>' +
          '<span style="display:block;margin-top:4px">' +
          (out ? '<span class="pill err"><span class="dot"></span>Sold out, hidden from menu</span>'
            : m.stock_quantity <= m.reorder_level ? '<span class="pill warn"><span class="dot"></span>Below reorder level</span>'
              : '<span class="tiny">' + R(toCents(m.price)) + ' · ' + esc(m.category) + '</span>') + '</span></span>' +
          '<span class="stepper"><button data-act="stock" data-arg="' + m.id + ':-1" aria-label="One fewer">' + ico("i-minus", 15) + '</button>' +
          '<span class="n"' + (m.stock_quantity <= m.reorder_level ? ' style="color:var(--warn)"' : "") + '>' + m.stock_quantity + '</span>' +
          '<button data-act="stock" data-arg="' + m.id + ':1" aria-label="One more">' + ico("i-plus", 15) + '</button></span></div>';
      }).join("") + '</div>') +
      '<div class="sp16"></div><p class="tiny" style="text-align:center">Changes reach the student menu immediately.</p>' +
      '</div>' + tabbar("stock") + '</div>';
  };

  V.sales = function () {
    var s = S.sales;
    if (!s) return '<div class="view"><div class="appbar"><h3>Sales</h3></div><div class="scroll">' + loading(3) + '</div>' + tabbar("sales") + '</div>';
    var hours = [7, 8, 9, 10, 11, 12, 13, 14, 15, 16];
    var byHour = {};
    (s.orders_by_hour || []).forEach(function (h) { byHour[h.hour] = h.orders; });
    var values = hours.map(function (h) { return byHour[h] || 0; });
    var max = Math.max.apply(null, values.concat([1]));
    var peak = hours[values.indexOf(Math.max.apply(null, values))];

    return '<div class="view"><div class="appbar"><h3>Sales</h3><span class="pill">Today</span></div><div class="scroll">' +
      '<div class="row" style="gap:10px">' +
      '<div class="card grow"><p class="tiny">Revenue</p><p class="stat-n">' + R(toCents(s.revenue)) + '</p>' +
      '<p class="tiny" style="color:var(--ok);margin-top:3px">' + s.order_count + ' collected order' + (s.order_count === 1 ? "" : "s") + '</p></div>' +
      '<div class="card grow"><p class="tiny">Average order</p><p class="stat-n">' + R(toCents(s.average_order)) + '</p>' +
      '<p class="tiny" style="margin-top:3px">All breaks</p></div></div>' +
      '<div class="sp20"></div>' +
      '<div class="card"><div class="row between" style="margin-bottom:18px"><span class="h-md">Orders by hour</span>' +
      (s.order_count ? '<span class="pill sky">Peak ' + String(peak).padStart(2, "0") + ':00</span>' : "") + '</div>' +
      '<div class="bars">' + values.map(function (v, i) {
        return '<div class="b' + (hours[i] === peak && v > 0 ? " peak" : "") + '" style="height:' + Math.max(3, Math.round(v / max * 100)) + '%"></div>';
      }).join("") + '</div>' +
      '<div class="bar-x">' + hours.map(function (h) { return "<span>" + String(h).padStart(2, "0") + "</span>"; }).join("") + '</div></div>' +
      '<div class="sp20"></div><p class="label">Top sellers today</p>' +
      ((s.top_items || []).length
        ? '<div class="list">' + s.top_items.slice(0, 5).map(function (t, i) {
          return '<div class="list-row" style="min-height:52px"><span class="pill' + (i === 0 ? " gold" : "") + '">' + (i + 1) + '</span>' +
            '<span class="grow h-md trunc">' + esc(t.item_name) + '</span><span class="tiny">' + t.quantity + ' sold</span></div>';
        }).join("") + '</div>'
        : '<p class="tiny">No sales recorded yet today. Sales count once orders are collected.</p>') +
      '<div class="sp16"></div><button class="btn secondary" data-act="export">Export report (CSV)</button>' +
      '</div>' + tabbar("sales") + '</div>';
  };

  V.topup = function () {
    return '<div class="view"><div class="appbar">' +
      '<button class="iconbtn ghost" data-act="tab" data-arg="queue" aria-label="Back">' + ico("i-back", 19) + '</button>' +
      '<h3>Top up a wallet</h3></div><div class="scroll">' +
      banner("info", "The student pays on the tuckshop speed point first. The speed point is a separate system: enter the same amount here to add it to their wallet.") +
      '<div class="sp16"></div>' +
      (S.error ? banner("err", esc(S.error)) + '<div class="sp16"></div>' : "") +
      '<p class="label">Student number</p><div class="field">' + ico("i-sig", 17) +
      '<input id="tuNum" placeholder="ST2024001"></div><div class="sp12"></div>' +
      '<p class="label">Amount (R)</p><div class="field">' + ico("i-wallet", 17) +
      '<input id="tuAmt" inputmode="decimal" placeholder="50"></div><div class="sp12"></div>' +
      '<div class="chips">' + [20, 50, 100, 200].map(function (a) {
        return '<button class="chip" data-act="preset" data-arg="' + a + '">R' + a + '</button>';
      }).join("") + '</div>' +
      '<div class="sp20"></div>' +
      '<button class="btn" data-act="topup"' + (S.busy ? " disabled" : "") + '>' + (S.busy ? "Adding…" : "Add to wallet") + '</button>' +
      '</div>' + tabbar("queue") + '</div>';
  };

  V.profile = function () {
    var staff = isStaff();
    return '<div class="view"><div class="appbar"><h3>Profile</h3></div><div class="scroll">' +
      '<div class="card row" style="gap:14px">' +
      '<span class="thumb lg"' + (staff ? ' style="background:var(--gold-wash);color:var(--gold)"' : "") + '>' +
      ico(staff ? "i-store" : "i-user", 26) + '</span>' +
      '<span class="grow"><span class="h-md" style="display:block;font-size:16px">' + esc(S.profile.full_name) + '</span>' +
      '<span class="tiny" style="display:block;margin-top:2px;word-break:break-all">' + esc(S.profile.email) + '</span>' +
      '<span class="tiny" style="display:block">' + (S.profile.student_number ? "Student number " + esc(S.profile.student_number) : "No student number") +
      (S.profile.phone ? " · " + esc(S.profile.phone) : "") + '</span>' +
      '<span class="pill ' + (staff ? "gold" : "sky") + '" style="margin-top:8px">' + (staff ? "Tuckshop staff" : "Student") + '</span></span></div>' +
      (staff ? "" :
        '<div class="sp12"></div><div class="card row between"><span class="row" style="gap:11px">' + ico("i-wallet", 19) +
        '<span class="h-md">Campus wallet</span></span><span class="price" style="font-size:17px">' + R(toCents(S.profile.wallet_balance)) + '</span></div>' +
        '<div class="sp12"></div><div class="card row between"><span class="row" style="gap:11px"><span style="color:var(--gold)">' + ico("i-star", 19) + '</span>' +
        '<span class="h-md">Loyalty stamps</span></span><span class="pill gold">' + (S.stamps % LOYALTY_TARGET) + ' of ' + LOYALTY_TARGET + '</span></div>') +
      '<div class="sp24"></div><p class="label">Preferences</p><div class="list">' +
      '<button class="list-row" data-act="dark">' + ico("i-moon", 19) + '<span class="grow h-md">Dark mode</span>' +
      '<span class="toggle' + (S.dark ? "" : " off") + '" role="img" aria-label="' + (S.dark ? "On" : "Off") + '"></span></button>' +
      '<button class="list-row" data-act="editprofile">' + ico("i-edit", 19) + '<span class="grow h-md">Edit profile</span>' + ico("i-chev", 16) + '</button>' +
      '</div>' +
      '<div class="sp24"></div><p class="label">Account</p><div class="list">' +
      '<button class="list-row" data-act="privacy">' + ico("i-sig", 19) + '<span class="grow h-md">Privacy and POPIA</span>' + ico("i-chev", 16) + '</button>' +
      '</div><div class="sp16"></div>' +
      '<button class="btn danger" data-act="signout">' + ico("i-out", 17) + 'Sign out</button>' +
      '</div>' + tabbar("profile") + '</div>';
  };

  /* ==========================================================================
     THE ITEM SHEET — the option engine
     ========================================================================== */

  var sheetItem = null, sheetSel = null;

  function openItemSheet(item) {
    sheetItem = item;
    sheetSel = initialSelection(item);
    renderItemSheet();
  }

  function renderItemSheet() {
    var item = sheetItem, sel = sheetSel;
    var unit = unitPriceCents(item, sel);
    var total = lineTotal(item, sel);

    var html = '<div class="row" style="gap:14px;align-items:flex-start">' +
      '<span class="thumb lg">' + food(item.icon_key, 32) + '</span>' +
      '<span class="grow"><span class="h-md" style="display:block;font-size:17px">' + esc(item.name) + '</span>' +
      '<span class="tiny" style="display:block;margin-top:5px">' + esc(item.category) + '</span>' +
      '<span style="display:inline-block;margin-top:6px">' + stockPill(item) + '</span></span></div>' +
      '<div class="sp16"></div><p class="sub">' + esc(item.description || "") + '</p>';

    if (hasBase(item)) {
      html += '<div class="sp24"></div><p class="label">' + esc(item.base_step_label) + '</p>' +
        '<div class="list"><div class="opt"><span class="thumb sm">' + food(item.icon_key, 20) + '</span>' +
        '<span class="grow"><span class="h-md" style="display:block">' + esc(item.base_step_label) + '</span>' +
        '<span class="tiny">' + R(toCents(item.price)) + ' each</span></span>' +
        '<span class="stepper"><button data-act="base" data-arg="-1" aria-label="Fewer">' + ico("i-minus", 15) + '</button>' +
        '<span class="n">' + sel.baseQty + '</span>' +
        '<button data-act="base" data-arg="1" aria-label="More">' + ico("i-plus", 15) + '</button></span></div></div>' +
        (sel.baseQty === 0 ? '<div class="sp8"></div><p class="tiny">Fillings only, no ' + esc(item.base_step_singular) + '.</p>' : "");
    }

    groupsOf(item).forEach(function (g) {
      html += '<div class="sp24"></div><p class="label">' + esc(g.label) + '</p><div class="list">';
      if (g.type === "qty") {
        html += optionsOf(g).map(function (o) {
          var n = sel.counts[o.id] || 0;
          return '<div class="opt"><span class="grow"><span class="h-md" style="display:block">' + esc(o.name) + '</span>' +
            '<span class="tiny">' + R(toCents(o.price)) + ' each</span></span>' +
            '<span class="stepper"><button data-act="count" data-arg="' + o.id + ':-1" aria-label="Fewer">' + ico("i-minus", 15) + '</button>' +
            '<span class="n"' + (n > 0 ? ' style="color:var(--navy)"' : "") + '>' + n + '</span>' +
            '<button data-act="count" data-arg="' + o.id + ':1" aria-label="More">' + ico("i-plus", 15) + '</button></span></div>';
        }).join("");
      } else {
        html += optionsOf(g).map(function (o) {
          var on = sel.singles[g.id] === o.id;
          var price = toCents(o.price);
          return '<button class="opt" data-act="single" data-arg="' + g.id + ':' + o.id + '">' +
            '<span class="radio' + (on ? " on" : "") + '"></span>' +
            '<span class="grow h-md">' + esc(o.name) + '</span>' +
            (price ? '<span class="price">' + (g.replaces_price ? "" : "+") + R(price) + '</span>' : "") + '</button>';
        }).join("");
      }
      html += '</div>';
    });

    if (!isBuild(item)) {
      html += '<div class="sp24"></div><div class="row between"><span class="h-md">How many</span>' +
        '<span class="stepper"><button data-act="qty2" data-arg="-1" aria-label="Decrease">' + ico("i-minus", 15) + '</button>' +
        '<span class="n">' + sel.qty + '</span>' +
        '<button data-act="qty2" data-arg="1" aria-label="Increase">' + ico("i-plus", 15) + '</button></span></div>';
    }

    html += '<div class="sp20"></div><button class="btn" data-act="add"' + (unit > 0 ? "" : " disabled") + '>' +
      (unit > 0 ? "Add to cart · " + R(total) : "Choose something first") + '</button>';

    openSheet(html);
  }

  /* ==========================================================================
     EVENTS
     ========================================================================== */

  document.addEventListener("click", function (e) {
    var el = e.target.closest("[data-act]");
    if (!el) return;
    var act = el.getAttribute("data-act");
    var arg = el.getAttribute("data-arg");
    handle(act, arg, el);
  });

  document.addEventListener("input", function (e) {
    if (e.target.id === "q") {
      S.q = e.target.value;
      var listEl = app.querySelector(".list, .empty");
      render(true);
      var box = document.getElementById("q");
      if (box) { box.focus(); box.setSelectionRange(S.q.length, S.q.length); }
    }
  });

  async function handle(act, arg, el) {
    try {
      switch (act) {
        case "go": return go(arg);
        case "tab": {
          if (arg === "orders" || arg === "queue") await loadOrders();
          if (arg === "sales") await loadSales();
          if (arg === "cart") await loadSlots();
          return go(arg);
        }
        case "closesheet": return closeSheet();
        case "cat": S.cat = arg; return render();
        case "sfilter": S.staffFilter = arg; return render();
        case "slot": S.slotId = arg; return render();
        case "dark": {
          S.dark = !S.dark;
          try { localStorage.setItem("rm.dark", S.dark ? "1" : "0"); } catch (e) { /* not fatal */ }
          screenEl.classList.toggle("dark", S.dark);
          return render();
        }

        case "signin": {
          var email = document.getElementById("inEmail").value;
          var pass = document.getElementById("inPass").value;
          if (!email || !pass) { S.error = "Enter your email and password."; return render(); }
          S.busy = true; S.error = null; render();
          try {
            await signIn(email, pass);
            await afterSignIn();
          } catch (err) {
            S.error = friendly(err);
          } finally { S.busy = false; render(); }
          return;
        }

        case "register": {
          var name = document.getElementById("rgName").value;
          var mail = document.getElementById("rgMail").value;
          var num = document.getElementById("rgNum").value;
          var p1 = document.getElementById("rgPass").value;
          var p2 = document.getElementById("rgPass2").value;
          if (!name.trim()) { S.error = "Please enter your full name."; return render(); }
          if (!/^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$/.test(mail.trim())) { S.error = "That does not look like an email address."; return render(); }
          if (num.trim().length < 3) { S.error = "Use the student number on your card."; return render(); }
          if (p1.length < 8) { S.error = "Use at least 8 characters."; return render(); }
          if (p1 !== p2) { S.error = "The two passwords do not match."; return render(); }
          S.busy = true; S.error = null; render();
          try {
            var signedIn = await signUp(name, mail, num, p1);
            if (!signedIn) {
              S.error = null;
              S.busy = false;
              render();
              toast("Account made. Check your email, then sign in.", "i-mail");
              return go("login");
            }
            await afterSignIn();
          } catch (err) {
            S.error = friendly(err);
          } finally { S.busy = false; render(); }
          return;
        }

        case "signout": return signOut();

        case "item": {
          var item = S.menu.filter(function (m) { return m.id === arg; })[0];
          if (item) openItemSheet(item);
          return;
        }
        case "base": {
          sheetSel.baseQty = Math.max(sheetItem.base_step_min || 0, sheetSel.baseQty + Number(arg));
          return renderItemSheet();
        }
        case "count": {
          var parts = arg.split(":");
          var n = (sheetSel.counts[parts[0]] || 0) + Number(parts[1]);
          if (n <= 0) delete sheetSel.counts[parts[0]]; else sheetSel.counts[parts[0]] = n;
          return renderItemSheet();
        }
        case "single": {
          var p = arg.split(":");
          sheetSel.singles[p[0]] = p[1];
          return renderItemSheet();
        }
        case "qty2": {
          sheetSel.qty = Math.max(1, sheetSel.qty + Number(arg));
          return renderItemSheet();
        }
        case "add": {
          addToCart(sheetItem, sheetSel);
          closeSheet();
          toast("Added to your cart");
          return render();
        }

        case "qty": {
          var bits = arg.split(":");
          var line = S.cart.filter(function (l) { return l.key === bits[0]; })[0];
          if (!line) return;
          line.sel.qty += Number(bits[1]);
          if (line.sel.qty <= 0) S.cart = S.cart.filter(function (l) { return l !== line; });
          saveCart();
          return render();
        }
        case "rm": {
          S.cart = S.cart.filter(function (l) { return l.key !== arg; });
          saveCart();
          return render();
        }

        case "place": {
          if (S.busy) return;
          S.busy = true; S.error = null; render();
          try {
            var order = await placeOrder(arg);
            S.openOrderId = order.id;
            S.busy = false;
            go("order");
            toast("Order placed. Code " + String(order.collection_code).trim());
          } catch (err) {
            S.error = friendly(err);
            S.busy = false;
            // A full or closed slot means going back to pick another.
            if (err.code === "SLOT_FULL" || err.code === "SLOT_NOT_TODAY") { await loadSlots(); }
            render();
          }
          return;
        }

        case "openorder": {
          S.openOrderId = arg;
          await loadOrders();
          return go("order");
        }

        case "cancel": {
          try {
            await setStatus(arg, "cancelled");
            toast("Order cancelled. Money back in your wallet.");
          } catch (err) { S.error = friendly(err); }
          return render();
        }

        case "advance": {
          var a = arg.split(":");
          el.disabled = true;
          try {
            await setStatus(a[0], a[1]);
            toast("Order #" + a[0].slice(0, 4) + " → " + a[1]);
          } catch (err) { S.error = friendly(err); }
          return render();
        }

        case "stock": {
          var s = arg.split(":");
          el.disabled = true;
          try { await adjustStock(s[0], Number(s[1])); }
          catch (err) { S.error = friendly(err); }
          return render();
        }

        case "preset": {
          document.getElementById("tuAmt").value = arg;
          return;
        }
        case "topup": {
          var num2 = document.getElementById("tuNum").value.trim();
          var amt = Number(document.getElementById("tuAmt").value);
          if (!num2) { S.error = "Enter the student number."; return render(); }
          if (!(amt >= 10 && amt <= 1000)) { S.error = "Top-ups are between R10 and R1000."; return render(); }
          S.busy = true; S.error = null; render();
          try {
            var result = await topUp(num2, amt);
            toast(result.full_name + " now has R" + Number(result.wallet_balance).toFixed(2));
            S.error = null;
          } catch (err) { S.error = friendly(err); }
          S.busy = false;
          return render();
        }

        case "export": {
          exportCsv();
          return;
        }

        case "bell": {
          var notes = await api("/rest/v1/notifications?select=*&order=sent_at.desc&limit=20");
          return openSheet('<p class="h-md" style="font-size:17px">Notifications</p><div class="sp16"></div>' + (notes.length
              ? '<div class="list">' + notes.map(function (n) {
                return '<div class="list-row"><span class="grow"><span class="h-md" style="display:block">' + esc(n.title) + '</span>' +
                  '<span class="tiny">' + esc(n.message) + '</span></span><span class="tiny">' + hhmm(n.sent_at) + '</span></div>';
              }).join("") + '</div>'
              : emptyState("i-bell", "Nothing yet", "Order updates and wallet top-ups appear here.")));
        }

        case "editprofile": {
          return openSheet('<p class="h-md" style="font-size:17px">Edit profile</p><div class="sp16"></div>' +
            '<p class="label">Full name</p><div class="field">' + ico("i-user", 17) +
            '<input id="epName" value="' + esc(S.profile.full_name) + '"></div><div class="sp12"></div>' +
            '<p class="label">Phone number</p><div class="field">' + ico("i-sig", 17) +
            '<input id="epPhone" inputmode="tel" placeholder="082 123 4567" value="' + esc(S.profile.phone || "") + '"></div>' +
            '<div class="sp20"></div><button class="btn" data-act="saveprofile">Save changes</button>');
        }
        case "saveprofile": {
          var newName = document.getElementById("epName").value.trim();
          var newPhone = document.getElementById("epPhone").value.trim();
          if (!newName) { toast("Give your name.", "i-warn"); return; }
          try {
            await api("/rest/v1/profiles?id=eq." + S.profile.id, {
              method: "PATCH",
              headers: { Prefer: "return=representation" },
              body: { full_name: newName, phone: newPhone },
            });
            await loadProfile();
            closeSheet();
            toast("Profile updated");
            return render();
          } catch (err) { toast(friendly(err), "i-warn"); }
          return;
        }

        case "privacy": {
          return openSheet('<p class="h-md" style="font-size:17px">Privacy and POPIA</p><div class="sp16"></div>' + '<p class="sub">We keep your name, email address and student number so staff can find your order at the counter, plus your orders and your wallet balance.</p>' +
            '<div class="sp12"></div><p class="sub">Top-ups are paid on the tuckshop speed point, which belongs to the bank and is a separate system. No card number ever reaches this app.</p>' +
            '<div class="sp12"></div><p class="sub">Only you and tuckshop staff see your orders. Nothing is sold or shared. Ask staff to delete your account and we remove it along with your orders.</p>');
        }
      }
    } catch (err) {
      console.error("[action] " + act + " failed", err);
      S.error = friendly(err);
      render();
    }
  }

  /** The day as a CSV, the same shape the Android app exports. */
  function exportCsv() {
    var s = S.sales;
    if (!s) return;
    var rands = function (v) { return Number(v || 0).toFixed(2); };
    var lines = ["Ri-magwinya sales," + todayISO(), "", "Revenue," + rands(s.revenue),
      "Collected orders," + s.order_count, "Average order," + rands(s.average_order), "", "Hour,Orders"];
    (s.orders_by_hour || []).forEach(function (h) { lines.push(String(h.hour).padStart(2, "0") + ":00," + h.orders); });
    lines.push("", "Item,Quantity,Revenue");
    (s.top_items || []).forEach(function (t) {
      var name = /[",]/.test(t.item_name) ? '"' + t.item_name.replace(/"/g, '""') + '"' : t.item_name;
      lines.push(name + "," + t.quantity + "," + rands(t.revenue));
    });
    var blob = new Blob([lines.join("\n")], { type: "text/csv" });
    var a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "rimagwinya-sales-" + todayISO() + ".csv";
    a.click();
    URL.revokeObjectURL(a.href);
    toast("Report downloaded");
  }

  /* ==========================================================================
     RENDER AND START
     ========================================================================== */

  function render(keepScroll) {
    var sc = app.querySelector(".scroll");
    var top = sc ? sc.scrollTop : 0;

    var view = V[S.screen] || V.welcome;
    // Signed out, only the three auth screens exist.
    if (!S.profile && ["welcome", "login", "register"].indexOf(S.screen) === -1) view = V.welcome;
    app.innerHTML = (S.online ? "" : '<div class="offline-bar">You’re offline. Changes go through when you’re back.</div>') +
      view();

    if (keepScroll) {
      var sc2 = app.querySelector(".scroll");
      if (sc2) sc2.scrollTop = top;
    }
  }

  async function afterSignIn() {
    if (isStaff()) {
      await Promise.all([loadMenu(), loadOrders()]);
      go("queue");
    } else {
      await Promise.all([loadMenu(), loadOrders(), loadSlots(), loadStudentExtras()]);
      loadWeather().then(function () { render(true); });
      go("menu");
    }
    startPolling();
  }

  /* Stands in for the app's realtime socket: the same effect, less machinery. */
  var pollTimer = null;
  function startPolling() {
    if (pollTimer) clearInterval(pollTimer);
    pollTimer = setInterval(async function () {
      if (!S.profile || document.hidden || S.busy) return;
      try {
        var before = JSON.stringify(S.orders.map(function (o) { return o.id + o.status; })) +
          JSON.stringify(S.menu.map(function (m) { return m.stock_quantity; }));
        await Promise.all([loadOrders(), loadMenu()]);
        var after = JSON.stringify(S.orders.map(function (o) { return o.id + o.status; })) +
          JSON.stringify(S.menu.map(function (m) { return m.stock_quantity; }));
        if (before !== after) render(true);
      } catch (e) { /* a missed poll is not worth a message */ }
    }, POLL_MS);
  }

  window.addEventListener("online", function () { S.online = true; render(true); });
  window.addEventListener("offline", function () { S.online = false; render(true); });

  async function start() {
    screenEl.classList.toggle("dark", S.dark);
    loadSession();
    render();

    if (S.session) {
      try {
        await loadProfile();
        if (S.profile) { await afterSignIn(); return; }
      } catch (e) {
        console.error("[start] stored session no longer works");
        saveSession(null);
      }
    }
    go("welcome");
  }

  /**
   * A handle on the internals, for two reasons: a test harness can check the
   * pricing against the real menu without a browser, and during a demo the
   * console can show that the numbers on screen came from the same formula
   * the server uses.
   */
  window.RM = {
    state: S,
    api: api,
    unitPriceCents: unitPriceCents,
    lineTotal: lineTotal,
    labelFor: labelFor,
    initialSelection: initialSelection,
    unitsConsumed: unitsConsumed,
    openSlots: openSlots,
    weatherSorted: weatherSorted,
    R: R,
  };

  start();
})();

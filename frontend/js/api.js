// Talks to the Login Service. No page code touches fetch() directly; everything goes through here.

/* ---------- 1. Remembering the login token ---------- */

// The token is the "wristband" the backend gave us at login. We keep it in the browser's
// localStorage so the user stays logged in after a refresh.
const Session = {
  KEY: "zodiac.accessToken",
  get() {
    try { return localStorage.getItem(this.KEY); } catch { return null; }
  },
  set(token) {
    try { localStorage.setItem(this.KEY, token); } catch { /* storage blocked: user must log in again next time */ }
  },
  clear() {
    try { localStorage.removeItem(this.KEY); } catch { /* nothing to do */ }
  },
};

/* ---------- 2. Friendly messages for the backend's error codes ---------- */

// The backend sends a stable "code" (for programs). The frontend decides the words (for people).
// This is also where Arabic could be added later.
const ERROR_MESSAGES = {
  EMAIL_ALREADY_REGISTERED: "An account with this email already exists. Try logging in instead.",
  INVALID_CREDENTIALS: "Email or password is incorrect.",
  ACCOUNT_LOCKED: "Too many failed attempts. Please wait a few minutes and try again.",
  ACCOUNT_DISABLED: "This account has been disabled. Please contact the shop.",
  INVALID_SESSION: "Your session has ended. Please log in again.",
  INCORRECT_CURRENT_PASSWORD: "Your current password is not correct.",
  PASSWORD_TOO_LONG: "That password is too long. Please use a shorter one.",
  VALIDATION_FAILED: "Please check the highlighted fields.",
  MALFORMED_REQUEST: "Something went wrong with the form. Please try again.",
  NETWORK_ERROR: "Cannot reach the server. Check your connection and try again.",
};
const FALLBACK_MESSAGE = "Something went wrong. Please try again.";

// Friendly text for a field that failed the backend's validation.
const FIELD_MESSAGES = {
  email: "Please enter a valid email address.",
  password: "Password must be 8 to 72 characters.",
  newPassword: "Password must be 8 to 72 characters.",
};

class ApiError extends Error {
  constructor(code, status, fieldErrors = {}) {
    super(ERROR_MESSAGES[code] ?? FALLBACK_MESSAGE);
    this.code = code;
    this.status = status;
    this.fieldErrors = fieldErrors; // e.g. { email: "must be a well-formed email address" }
  }
}

/* ---------- 3. The one function that calls the backend ---------- */

async function apiRequest(path, { method = "GET", body, auth = false } = {}) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (auth) {
    const token = Session.get();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(window.ZODIAC_CONFIG.apiBaseUrl + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    // fetch() only throws when the request never got an answer (server down, no internet, blocked).
    throw new ApiError("NETWORK_ERROR", 0);
  }

  if (response.status === 204) return null; // "done, nothing to say"

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(data?.code ?? "UNKNOWN", response.status, data?.fieldErrors ?? {});
  }
  return data;
}

/* ---------- 4. One function per backend endpoint ---------- */

const AuthApi = {
  register: (email, password) =>
    apiRequest("/api/v1/auth/register", { method: "POST", body: { email, password } }),
  login: (email, password) =>
    apiRequest("/api/v1/auth/login", { method: "POST", body: { email, password } }),
  me: () => apiRequest("/api/v1/auth/me", { auth: true }),
  logout: () => apiRequest("/api/v1/auth/logout", { method: "POST", auth: true }),
  changePassword: (currentPassword, newPassword) =>
    apiRequest("/api/v1/auth/password", { method: "PUT", auth: true, body: { currentPassword, newPassword } }),
};

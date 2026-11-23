// Small helpers for showing messages and handling forms, shared by all pages.
//
// Safety rule for this whole frontend: text that came from the server or from a user is shown
// with textContent, NEVER innerHTML. innerHTML would run any <script> hidden inside that text.

/** Show a message in a box. type is "error" or "success". Pass null to hide the box. */
function showMessage(box, text, type = "error") {
  if (!text) {
    box.hidden = true;
    box.textContent = "";
    return;
  }
  box.textContent = text;
  box.className = `message message--${type}`;
  box.hidden = false;
}

/** Remove all red field errors inside a form. */
function clearFieldErrors(form) {
  form.querySelectorAll(".field__error").forEach((el) => { el.textContent = ""; });
  form.querySelectorAll("input").forEach((el) => el.removeAttribute("aria-invalid"));
}

/** Show an error under one input (the input must have name="x" and a sibling <p data-error-for="x">). */
function setFieldError(form, fieldName, text) {
  const input = form.elements[fieldName];
  const slot = form.querySelector(`[data-error-for="${fieldName}"]`);
  if (!input || !slot) return false;
  slot.textContent = text;
  input.setAttribute("aria-invalid", "true");
  return true;
}

/** Turn a failed API call into messages on the form. */
function showApiError(form, messageBox, error) {
  if (!(error instanceof ApiError)) {
    showMessage(messageBox, FALLBACK_MESSAGE);
    return;
  }
  let shownOnField = false;
  for (const field of Object.keys(error.fieldErrors)) {
    shownOnField = setFieldError(form, field, FIELD_MESSAGES[field] ?? error.fieldErrors[field]) || shownOnField;
  }
  // A field-level hint is enough for validation errors; everything else gets the banner.
  showMessage(messageBox, shownOnField ? null : error.message);
}

/**
 * Run an async action when a form is submitted.
 * While it runs the button is disabled (no double-submits) and shows "busyText".
 */
function handleSubmit(form, action) {
  const button = form.querySelector("button[type=submit]");
  const idleText = button.textContent;
  form.addEventListener("submit", async (event) => {
    event.preventDefault(); // stop the browser's default "reload the page" behaviour
    clearFieldErrors(form);
    button.disabled = true;
    button.textContent = button.dataset.busyText ?? "Please wait…";
    try {
      await action();
    } finally {
      button.disabled = false;
      button.textContent = idleText;
    }
  });
}

/** Read the "?reason=..." note a previous page left in the address bar. */
function getNotice() {
  const reason = new URLSearchParams(window.location.search).get("reason");
  if (reason === "expired") return "Your session has ended. Please log in again.";
  if (reason === "loggedout") return "You have been logged out.";
  return null;
}

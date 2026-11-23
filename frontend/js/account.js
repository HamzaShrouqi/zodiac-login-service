const welcome = document.getElementById("welcome");
const details = document.getElementById("details");
const passwordForm = document.getElementById("password-form");
const passwordMessage = document.getElementById("password-message");
const logoutButton = document.getElementById("logout-button");

/** Forget the token and go to the login page. */
function leave(reason) {
  Session.clear();
  window.location.replace(`index.html?reason=${reason}`);
}

// 1. This page is for logged-in users only. Ask the backend who we are.
async function loadAccount() {
  if (!Session.get()) {
    leave("expired");
    return;
  }
  try {
    const user = await AuthApi.me();
    welcome.textContent = `Welcome, ${user.email}`;
    details.replaceChildren(
      detailRow("Account type", user.role.charAt(0) + user.role.slice(1).toLowerCase()),
      detailRow("Member since", new Date(user.createdAt).toLocaleDateString(undefined, { dateStyle: "long" })),
    );
    document.body.classList.remove("is-loading");
  } catch (error) {
    if (error instanceof ApiError && error.code === "INVALID_SESSION") {
      leave("expired");
    } else {
      welcome.textContent = "We couldn't load your account.";
      showMessage(passwordMessage, error.message ?? FALLBACK_MESSAGE);
      document.body.classList.remove("is-loading");
    }
  }
}

/** One "label: value" line, built with textContent (never innerHTML). */
function detailRow(label, value) {
  const row = document.createElement("div");
  row.className = "detail";
  const labelEl = document.createElement("span");
  labelEl.className = "detail__label";
  labelEl.textContent = label;
  const valueEl = document.createElement("span");
  valueEl.textContent = value;
  row.append(labelEl, valueEl);
  return row;
}

// 2. Change password.
handleSubmit(passwordForm, async () => {
  showMessage(passwordMessage, null);
  const { currentPassword, newPassword, confirmPassword } = passwordForm.elements;

  if (newPassword.value !== confirmPassword.value) {
    setFieldError(passwordForm, "confirmPassword", "The two passwords do not match.");
    return;
  }
  try {
    await AuthApi.changePassword(currentPassword.value, newPassword.value);
    passwordForm.reset();
    showMessage(passwordMessage, "Password changed. Your other devices have been logged out.", "success");
  } catch (error) {
    if (error instanceof ApiError && error.code === "INVALID_SESSION") {
      leave("expired");
      return;
    }
    showApiError(passwordForm, passwordMessage, error);
  }
});

// 3. Log out. We forget the token even if the server can't be reached.
logoutButton.addEventListener("click", async () => {
  logoutButton.disabled = true;
  try {
    await AuthApi.logout();
  } catch {
    /* the server will expire the session on its own; leaving is what matters */
  }
  leave("loggedout");
});

loadAccount();

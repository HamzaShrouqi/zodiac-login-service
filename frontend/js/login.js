const form = document.getElementById("login-form");
const messageBox = document.getElementById("message");

// Already logged in? Skip the form. (account.html checks the token and sends us back if it's stale.)
if (Session.get()) {
  window.location.replace("account.html");
}

const notice = getNotice();
if (notice) showMessage(messageBox, notice, "success");

handleSubmit(form, async () => {
  showMessage(messageBox, null);
  try {
    const result = await AuthApi.login(form.elements.email.value, form.elements.password.value);
    Session.set(result.accessToken);
    window.location.assign("account.html");
  } catch (error) {
    showApiError(form, messageBox, error);
    form.elements.password.value = ""; // never leave a wrong password sitting in the box
    form.elements.password.focus();
  }
});

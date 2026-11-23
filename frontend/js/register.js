const form = document.getElementById("register-form");
const messageBox = document.getElementById("message");

if (Session.get()) {
  window.location.replace("account.html");
}

handleSubmit(form, async () => {
  showMessage(messageBox, null);
  const email = form.elements.email.value;
  const password = form.elements.password.value;

  // Checked here only to save a trip to the server. The backend never trusts this check.
  if (password !== form.elements.confirmPassword.value) {
    setFieldError(form, "confirmPassword", "The two passwords do not match.");
    return;
  }

  try {
    await AuthApi.register(email, password);
  } catch (error) {
    showApiError(form, messageBox, error);
    return;
  }

  // Registration doesn't return a token, so log in straight away for the user.
  try {
    const result = await AuthApi.login(email, password);
    Session.set(result.accessToken);
    window.location.assign("account.html");
  } catch {
    window.location.assign("index.html"); // account exists; let them log in manually
  }
});

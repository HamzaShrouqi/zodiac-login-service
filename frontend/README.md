# Zodiac frontend (login pages)

Plain HTML, CSS and JavaScript. No build step, no framework, no npm.

| Page | Purpose |
|---|---|
| `index.html` | Log in |
| `register.html` | Create a customer account (and log in straight away) |
| `account.html` | Logged-in page: who am I, change password, log out |

## Run locally

1. Start the database and the backend (from the `login-service` folder):

   ```bash
   docker compose up -d
   ./gradlew bootRun
   ```

2. Serve this folder on port **5500** (the backend only accepts calls from this address, see `application-local.yml`):

   ```bash
   python3 -m http.server 5500 --directory frontend
   ```

3. Open <http://localhost:5500>.

Pages must be opened through that server, not by double-clicking the file. A `file://` page has no proper origin, so the backend's CORS rules cannot recognise it.

## Pointing at another backend

Change `apiBaseUrl` in `js/config.js`. The backend must also list this site's address in `ZODIAC_CORS_ALLOWED_ORIGINS`.

## Files

```
frontend/
├── index.html, register.html, account.html
├── css/styles.css        all colours and layout
└── js/
    ├── config.js         where the backend lives
    ├── api.js            the ONLY code that calls the backend, plus token storage and error messages
    ├── ui.js             shared helpers: show messages, handle form submits
    └── login.js, register.js, account.js   one script per page
```

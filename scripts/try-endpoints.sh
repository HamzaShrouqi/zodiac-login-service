#!/bin/bash
# Tries every Login Service endpoint, including the error cases, and prints each response.
# Needs the service running (./gradlew bootRun). Run from login-service/:  ./scripts/try-endpoints.sh
# It creates sara@zodiac.test and layla@zodiac.test. To remove them afterwards:
#   docker exec zodiac-login-db psql -U login_user -d login_db -c "DELETE FROM user_account WHERE email LIKE '%@zodiac.test'"
B=http://localhost:8081/api/v1/auth
J='Content-Type: application/json'
call() { # title, then curl args
  t="$1"; shift
  out=$(curl -s -w '\n%{http_code}' "$@"); code=$(echo "$out" | tail -1); body=$(echo "$out" | sed '$d')
  printf '\n### %s\n→ %s %s\n' "$t" "$code" "$body"
}
call "1. Register Sara (mixed-case email)" -X POST $B/register -H "$J" -d '{"email":"  Sara@Zodiac.TEST ","password":"rose-oud-2026"}'
call "2. Register again (same email)"      -X POST $B/register -H "$J" -d '{"email":"sara@zodiac.test","password":"something-else"}'
call "3. Register with bad input"          -X POST $B/register -H "$J" -d '{"email":"not-an-email","password":"short"}'
call "4. Broken JSON"                      -X POST $B/register -H "$J" -d '{oops'
call "5. Login, wrong password"            -X POST $B/login -H "$J" -d '{"email":"sara@zodiac.test","password":"wrong-password"}'
call "6. Login, email that doesn't exist"  -X POST $B/login -H "$J" -d '{"email":"nobody@zodiac.test","password":"whatever-123"}'
out=$(curl -s -X POST $B/login -H "$J" -A "curl-demo" -d '{"email":"SARA@zodiac.test","password":"rose-oud-2026"}')
printf '\n### 7. Login, correct\n→ %s\n' "$out"
T=$(echo "$out" | python3 -c 'import json,sys;print(json.load(sys.stdin)["accessToken"])')
call "8. /me with token"                   $B/me -H "Authorization: Bearer $T"
call "9. /me without token"                $B/me
call "10. /me with made-up token"          $B/me -H "Authorization: Bearer made-up-token"
T2=$(curl -s -X POST $B/login -H "$J" -d '{"email":"sara@zodiac.test","password":"rose-oud-2026"}' | python3 -c 'import json,sys;print(json.load(sys.stdin)["accessToken"])')
echo; echo "(logged in a second time = 'second device')"
call "11. Change password, wrong current"  -X PUT $B/password -H "Authorization: Bearer $T" -H "$J" -d '{"currentPassword":"nope","newPassword":"musk-amber-2026"}'
call "12. Change password, correct"        -X PUT $B/password -H "Authorization: Bearer $T" -H "$J" -d '{"currentPassword":"rose-oud-2026","newPassword":"musk-amber-2026"}'
call "13. Second device after change"      $B/me -H "Authorization: Bearer $T2"
call "14. This device after change"        $B/me -H "Authorization: Bearer $T"
call "15. Login with OLD password"         -X POST $B/login -H "$J" -d '{"email":"sara@zodiac.test","password":"rose-oud-2026"}'
call "16. Logout"                          -X POST $B/logout -H "Authorization: Bearer $T"
call "17. /me after logout"                $B/me -H "Authorization: Bearer $T"
echo; echo "### 18. Lockout: Layla gets her password wrong 5 times"
curl -s -o /dev/null -X POST $B/register -H "$J" -d '{"email":"layla@zodiac.test","password":"jasmine-2026"}'
for i in 1 2 3 4 5; do printf ' attempt %s → %s\n' $i "$(curl -s -o /dev/null -w '%{http_code}' -X POST $B/login -H "$J" -d '{"email":"layla@zodiac.test","password":"wrong"}')"; done
call "19. Layla, now with the RIGHT password" -X POST $B/login -H "$J" -d '{"email":"layla@zodiac.test","password":"jasmine-2026"}'
call "20. Unknown URL"                     $B/does-not-exist
call "21. Wrong HTTP method"               -X DELETE $B/login

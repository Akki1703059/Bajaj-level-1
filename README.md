# Bajaj Finserv Health | Qualifier 1 | JAVA — Starter

This starter finishes the required flow **on app startup** (no controller exposed):
1) Calls *generate webhook*.
2) Logs the question link to open (odd/even based on your regNo).
3) Submits your **final SQL query** to the webhook using the JWT token.

> Edit `src/main/resources/application.yml` with your **name**, **regNo**, **email**, and paste your **finalQuery**.

## Run (Java 17 + Maven)
```bash
# Build runnable JAR
mvn -q -DskipTests package

# Run
java -jar target/bh-java-qualifier-0.0.1.jar
```

## What it does
- On startup, it POSTs to `/hiring/generateWebhook/JAVA` and receives `webhook` + `accessToken` (JWT).
- It extracts the last two digits of your `regNo` to decide odd/even and prints the **Google Drive question link** in the logs.
- It writes your SQL into `final_query.sql` and then POSTs `{ "finalQuery": "<your SQL>" }` to the **returned webhook** with `Authorization: <accessToken>`. If that fails with 401, it retries once with `Authorization: Bearer <accessToken>`.

## GitHub Submission
- Create a new repo and push this folder.
- Upload the built JAR (`target/bh-java-qualifier-0.0.1.jar`) as a Release or to any public file host.
- Share both the repo URL and a **direct downloadable JAR link** in the submission form.

Good luck!

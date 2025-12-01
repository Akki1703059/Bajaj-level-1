Bajaj Finserv Health – JAVA Qualifier 1 (My Starter Notes)

This project is a simple starter template that completes the required Qualifier-1 workflow automatically when the application starts. No controller calls are needed.
 What the application does on startup

Generates a webhook by calling the generateWebhook API for JAVA.

Reads my regNo, checks the last two digits, and prints the correct Google Drive question link (odd/even).

Submits my final SQL query to the generated webhook using the provided JWT token.

 What I have to update

In src/main/resources/application.yml, I need to fill in my:

name

regNo

email

finalQuery (my final SQL answer that will be submitted)

 How to Run (Java 17 + Maven)
# Build the JAR (skips tests)
mvn -q -DskipTests package

# Run the application
java -jar target/bh-java-qualifier-0.0.1.jar

 Internal Flow (Summary)

On startup, the app sends a POST request to
/hiring/generateWebhook/JAVA
and receives:

webhook URL

accessToken (JWT)

It figures out if my register number is odd or even and prints the correct question link.

It writes my SQL query into final_query.sql.

Finally, it posts the SQL to the received webhook with
Authorization: <accessToken>
and if that fails (401), it retries with
Authorization: Bearer <accessToken>.

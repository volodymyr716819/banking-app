#!/bin/bash
# Find H2 jar in Maven repository
H2_JAR=$(find ~/.m2/repository -name "h2-*.jar" | grep -v "sources" | grep -v "javadoc" | head -1)

if [ -z "$H2_JAR" ]; then
  echo "H2 jar not found in Maven repository"
  exit 1
fi

echo "Using H2 jar: $H2_JAR"

# Stop the application if it's running
echo "Make sure the application is stopped before running this script"

# Run the SQL script
java -cp "$H2_JAR" org.h2.tools.RunScript -url jdbc:h2:file:./data/bankdb -user sa -script fix_database.sql

echo "Database fix applied. You can now restart the application."
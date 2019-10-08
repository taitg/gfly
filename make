#!/bin/bash
# Compile the program:
echo "Building..."
rm *.class >/dev/null 2>&1
rm *.jar >/dev/null 2>&1
javac -classpath .:./lib/'*' -d . Gfly.java \
&& echo "Success!" && \
jar cfm Gfly.jar Manifest.txt *.class && \
rm *.class && \
echo "Compiled to Gfly.jar"

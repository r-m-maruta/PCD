#!/bin/bash

GSON_JAR="./src/kahoot/lib/gson-2.9.1.jar"
OUT_DIR="./out"

echo ">> A executar o servidor..."
java -cp "$OUT_DIR:$GSON_JAR" kahoot.net.server.ServerMain

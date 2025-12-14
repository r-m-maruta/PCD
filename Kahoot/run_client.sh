#!/bin/bash

GSON_JAR="./src/kahoot/lib/gson-2.9.1.jar"
OUT_DIR="./out"

if [ $# -ne 5 ]; then
    echo "Uso: ./run_client.sh IP PORT JOGO EQUIPA USERNAME"
    exit 1
fi

java -cp "$OUT_DIR:$GSON_JAR" kahoot.net.client.ClientLauncher "$1" "$2" "$3" "$4" "$5"

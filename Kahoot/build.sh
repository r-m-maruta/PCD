#!/bin/bash

GSON_JAR="./src/kahoot/lib/gson-2.9.1.jar"
SRC_DIR="./src"
OUT_DIR="./out"

echo ">> Limpar diretório out/"
rm -rf $OUT_DIR
mkdir -p $OUT_DIR

echo ">> A compilar projeto..."
find $SRC_DIR -name "*.java" > sources.txt

javac -cp "$GSON_JAR" -d $OUT_DIR @sources.txt

if [ $? -ne 0 ]; then
    echo "❌ Erro na compilação!"
    exit 1
fi

echo "✔ Compilação concluída com sucesso!"

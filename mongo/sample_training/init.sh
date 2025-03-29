#!/bin/bash
# Скрипт для импорта всех JSON-файлов в текущей директории в MongoDB

# Параметры подключения (со значениями по умолчанию)
HOST=${1:-localhost}
PORT=${2:-27017}
USERNAME=${3:-}
PASSWORD=${4:-}

# Определяем имя базы данных как имя текущей директории
DB_NAME=$(basename "$PWD")

# Формируем параметры аутентификации (если указаны)
AUTH_PARAMS=""
if [ -n "$USERNAME" ]; then
    if [ -n "$PASSWORD" ]; then
        echo "Using password authentication!"
        AUTH_PARAMS="--authenticationDatabase admin -u $USERNAME -p $PASSWORD"
    fi
fi

# Обрабатываем все JSON-файлы в текущей директории
for JSON_FILE in *.json; do
    if [ -f "$JSON_FILE" ]; then
        COLLECTION_NAME=$(basename "$JSON_FILE" .json)
        echo "Importing $JSON_FILE into collection $COLLECTION_NAME in database $DB_NAME"

        mongoimport --drop --host "$HOST" --port "$PORT" --db "$DB_NAME" \
                   --collection "$COLLECTION_NAME" --file "$JSON_FILE" $AUTH_PARAMS
    fi
done

echo "Import completed for database $DB_NAME"
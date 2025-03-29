# MongoDB Sharded Cluster Deployment

## Состав кластера
- **3 Config Servers** (порты 20001-20003)
- **3 Shard Replica Sets** (по 3 ноды в каждом)
    - Shard1: 30001-30003
    - Shard2: 40001-40003
    - Shard3: 50001-50003
- **1 Mongos Router** (порт 10001)

## 1. Развертывание кластера

```bash
# Запуск всех сервисов
docker-compose up -d

# Проверка статуса
docker-compose ps
```

## 2. Настройка локального подключения

Добавляем все хосты в /etc/hosts
```bash
echo -e "127.0.0.1 mongo-config-server-1 mongo-config-server-2 mongo-config-server-3\n127.0.0.1 mongo-shard1-rs1 mongo-shard1-rs2 mongo-shard1-rs3\n127.0.0.1 mongo-shard2-rs1 mongo-shard2-rs2 mongo-shard2-rs3\n127.0.0.1 mongo-shard3-rs1 mongo-shard3-rs2 mongo-shard3-rs3\n127.0.0.1 mongos-router" | sudo tee -a /etc/hosts
```

## 3. Подключение к кластеру

Основной способ (через роутер):
```bash
mongosh "mongodb://mongos-router:10001"
```

Для администрирования (прямое подключение):
```bash
# К config серверам
mongosh "mongodb://mongo-config-server-1:20001"

# К конкретному шарду
mongosh "mongodb://mongo-shard1-rs1:30001/?replicaSet=shard1-rs"
```

## 4. Инициализация шардирования

```js
// В mongosh (через роутер)

// Добавление шардов
sh.addShard("shard1-rs/mongo-shard1-rs1:30001,mongo-shard1-rs2:30002,mongo-shard1-rs3:30003")
sh.addShard("shard2-rs/mongo-shard2-rs1:40001,mongo-shard2-rs2:40002,mongo-shard2-rs3:40003")
sh.addShard("shard3-rs/mongo-shard3-rs1:50001,mongo-shard3-rs2:50002,mongo-shard3-rs3:50003")

// Проверка статуса
sh.status()
```

## 5. Загрузка данных

Переходим в директорию с датасетом
```bash
cd sample_training
```

Загружаем датасет через роутер
```bash
sh init.sh mongos-router 10001
```

Подключаемся к роутеру
```bash
mongosh "mongodb://mongos-router:10001"
```

Проверяем наличие данных
```js
use sample_training
db.grades.getShardDistribution()
```

## 6. Шардирование grades

Включаем шардирование для БД
```js
sh.enableSharding("sample_training")
```

Указываем ключ шардирования для коллекции students
```js
db.grades.createIndex({ "student_id": 1 })
sh.shardCollection("sample_training.grades", { "student_id": 1 })
db.grades.getShardDistribution()
```
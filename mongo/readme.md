# MongoDB Sharded Cluster Deployment

## Состав кластера
- **1 Mongos Router** (порт 10001)
- **3 Config Servers** (порты 20001-20003)
- **3 Shard Replica Sets**
    - Shard1: 30001-30003
    - Shard2: 40001-40003
    - Shard3: 50001-50003

## 1. Развертывание кластера

```bash
# Запуск всех сервисов
docker-compose -f docker-compose.yml up -d

# Проверка статуса
docker-compose ps
```

## 2. Настройка локального подключения

Добавляем все хосты в /etc/hosts
```bash
echo -e "127.0.0.1 mongo-config-server-1 mongo-config-server-2 mongo-config-server-3\n127.0.0.1 mongo-shard1-rs1 mongo-shard1-rs2 mongo-shard1-rs3\n127.0.0.1 mongo-shard2-rs1 mongo-shard2-rs2 mongo-shard2-rs3\n127.0.0.1 mongo-shard3-rs1 mongo-shard3-rs2 mongo-shard3-rs3\n127.0.0.1 mongos-router" | sudo tee -a /etc/hosts
```

## 3. Инициализация шардирования

Подключение к роутеру:
```bash
mongosh "mongodb://mongos-router:10001"
```

Добавляем шарды
```js
sh.addShard("shard1-rs/mongo-shard1-rs1:30001,mongo-shard1-rs2:30002,mongo-shard1-rs3:30003")
sh.addShard("shard2-rs/mongo-shard2-rs1:40001,mongo-shard2-rs2:40002,mongo-shard2-rs3:40003")
sh.addShard("shard3-rs/mongo-shard3-rs1:50001,mongo-shard3-rs2:50002,mongo-shard3-rs3:50003")
```

## 4. Загрузка данных

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
db.grades.find().limit(1).pretty();
```

## 5. Шардирование grades

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

Уменьшим размер чанка, чтобы зафорсить шардирование
```js
use config
db.settings.updateOne(
  { _id: "chunksize" },
  { $set: { value: 1 } }, // 1MB вместо 64MB
  { upsert: true }
)
```

Проверим что данные разлетелись по шардам
```js
use sample_training
sh.startBalancer()
db.grades.getShardDistribution()
```

## 6. Создание рутового пользователя

Подключаемся без auth к admin
```bash
mongosh "mongodb://mongos-router:10001/admin"
```

Создаём рутового пользователя
```js
db.createUser({
  user: "root",
  pwd: "password123",
  roles: ["root"]
})
```

Создаём keyfile
```bash
openssl rand -base64 756 > mongodb-keyfile
chmod 400 mongodb-keyfile
```

Перезапускаем кластер уже с auth
```bash
docker-compose down
docker-compose -f docker-compose.auth.yml up -d
```

## 7. Создание пользователя с ограниченными правами

Подключаемся с правами root
```bash
mongosh "mongodb://root:password123@mongos-router:10001/admin"
```

Создаем нового пользователя
```js
use sample_training
db.createUser({
  user: "grades_user",
  pwd: "user123password", 
  roles: [
    { role: "readWrite", db: "sample_training" }
  ]
})
```

Подключаемся как grades_user
```bash
mongosh "mongodb://grades_user:user123password@mongos-router:10001/sample_training"
```

Проверяем права
```js
// Должно работать
use sample_training
db.grades.find().limit(1)

// Должно вызвать ошибку (нет прав на другие БД)
use admin
show collections
```


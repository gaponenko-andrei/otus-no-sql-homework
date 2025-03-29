# Настройка подключения

Чтобы с локальной тачки нормально подключаться к кластеру, можно выполнить
```
echo "127.0.0.1 mongo-rs-1" | sudo tee -a /etc/hosts
echo "127.0.0.1 mongo-rs-2" | sudo tee -a /etc/hosts
echo "127.0.0.1 mongo-rs-3" | sudo tee -a /etc/hosts
```
После этого можно подключаться к кластеру через 
```bash
mongosh mongodb://mongo-rs-1:30001,mongo-rs-2:30002,mongo-rs-3:30003/?replicaSet=my-replica-set
```
или 
```bash
mongosh mongodb://localhost:30001,localhost:30002,localhost:30003/?replicaSet=my-replica-set
```

# Наливка даных в БД

```bash
cd sample_training
mongosh localhost:30001
rs.status() # находим PRIMARY
sh init.sh localhost <primary host>
```
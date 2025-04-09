# Apache Kvrocks cluster
This Readme instructions how to deploy a cluster of Kvrocks using Docker Compose.

## Apache Kvrocks quick notes

### From cluster management perspective: Kvrocks cluster is different from Redis cluster.
  - Redis use the Gossip protocol between nodes to communicate with each other which allows to ack the failure status of the nodes
  and failover the cluster automatically.
  - Meanwhile, Kvrocks choose to not implement the Gossip protocol (which they think is too complex) and use a coordination service to manage the 
   Kvrocks cluster called [Apache Kvrocks Controller](https://github.com/apache/kvrocks-controller).

    Because the Kvrocks nodes do not communicate directly with each other, every information about the cluster for example the ROLE needs to be sent to all the Kvrocks nodes.

    Kvrocks controller would be responsible for the cluster management, including: set up the cluster, failover, scale cluster, migration data slots...

    Kvrocks controller can be deployed in a standalone mode or in a cluster mode (to provide HA and avoid single point of failure).

    In short, it seems Kvrocks cluster management is more complicated than Redis cluster.

### From client usage perspective: Kvrocks cluster claims to be a drop-in replacement for Redis cluster.

  The client can use the same Redis client library to connect to Kvrocks cluster.

## Apache Kvrocks cluster installation
We provide a sample `docker-compose-kvrocks-cluster.yml` file to deploy a cluster of Kvrocks with 3 master nodes and 3 slave nodes.

1. Start the docker compose: `docker compose -f docker-compose-kvrocks-cluster.yml up -d`
2. Use Kvrocks Controller to setup the Kvrocks cluster
 - Create a namespace: `docker exec -it kvrocks-controller ./bin/kvctl create namespace my-namespace`
 - Initialize Kvrocks cluster with only master nodes at first: `docker exec -it kvrocks-controller ./bin/kvctl create cluster my-cluster -n my-namespace --replica 1 --nodes kvrocks-master-1:6666,kvrocks-master-2:6666,kvrocks-master-3:6666`
 - Configure replica nodes:
   -  `docker exec -it kvrocks-controller ./bin/kvctl create node kvrocks-replica-1:6666 -n my-namespace -c my-cluster --shard 0`
   -  `docker exec -it kvrocks-controller ./bin/kvctl create node kvrocks-replica-2:6666 -n my-namespace -c my-cluster --shard 1`
   -  `docker exec -it kvrocks-controller ./bin/kvctl create node kvrocks-replica-3:6666 -n my-namespace -c my-cluster --shard 2`
 - You can verify the cluster status using the Redis `CLUSTER NODES` or `CLUSTER SLOTS` commands.
   It should return something like:
  ```
  ➜  ~ docker exec -it kvrocks-replica-1 redis-cli -p 6666 CLUSTER NODES
  C1mFX09tVCpqHwMJxWibRuRWi3iKF1tlHxvzOBNq kvrocks-replica-3:6666@16666 slave Na9dCWrMPUsi3FbP8xi86QVsKT5aX8vmjLQjwciT 1744190353491 1744190353492 4 connected
  T1T44dMNwb7R8qGrOyVJdLsrIFYGdsg0LN28M4fU kvrocks-replica-2:6666@16666 slave 6SRHuuZMw5GHr3wmxqVuOFcSxGsUpxCf9q047iDk 1744190353491 1744190353492 4 connected
  Na9dCWrMPUsi3FbP8xi86QVsKT5aX8vmjLQjwciT kvrocks-master-3:6666@16666 master - 1744190353491 1744190353492 4 connected 10922-16383
  6SRHuuZMw5GHr3wmxqVuOFcSxGsUpxCf9q047iDk kvrocks-master-2:6666@16666 master - 1744190353491 1744190353492 4 connected 5461-10921
  MTHuEU8LTcFB1ru8fx4cYMklOK1YiiiwuH6o4g7x kvrocks-replica-1:6666@16666 myself,slave chWSX7vlZPlcPdtnHKPwdd9uELos0zbDAcyWKsVu 1744190353491 1744190353492 4 connected
  chWSX7vlZPlcPdtnHKPwdd9uELos0zbDAcyWKsVu kvrocks-master-1:6666@16666 master - 1744190353491 1744190353492 4 connected 0-5460
  
  ➜  ~ docker exec -it kvrocks-replica-1 redis-cli -p 6666 CLUSTER SLOTS
  1) 1) (integer) 0
     2) (integer) 5460
     3) 1) "kvrocks-master-1"
        2) (integer) 6666
        3) "chWSX7vlZPlcPdtnHKPwdd9uELos0zbDAcyWKsVu"
     4) 1) "kvrocks-replica-1"
        2) (integer) 6666
        3) "MTHuEU8LTcFB1ru8fx4cYMklOK1YiiiwuH6o4g7x"
  2) 1) (integer) 5461
     2) (integer) 10921
     3) 1) "kvrocks-master-2"
        2) (integer) 6666
        3) "6SRHuuZMw5GHr3wmxqVuOFcSxGsUpxCf9q047iDk"
     4) 1) "kvrocks-replica-2"
        2) (integer) 6666
        3) "T1T44dMNwb7R8qGrOyVJdLsrIFYGdsg0LN28M4fU"
  3) 1) (integer) 10922
     2) (integer) 16383
     3) 1) "kvrocks-master-3"
        2) (integer) 6666
        3) "Na9dCWrMPUsi3FbP8xi86QVsKT5aX8vmjLQjwciT"
     4) 1) "kvrocks-replica-3"
        2) (integer) 6666
        3) "C1mFX09tVCpqHwMJxWibRuRWi3iKF1tlHxvzOBNq"
  ```
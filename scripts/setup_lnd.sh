#!/usr/bin/env bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
COMPOSE_FILE="$SCRIPTPATH/docker/docker-compose.yml"
export NETWORK="simnet"
init(){
    echo "===============init env================="
    docker-compose -f $COMPOSE_FILE pull btcd lnd
}

startlnd(){
    echo -e "===============start lnd for $1==================="
    docker-compose -f $COMPOSE_FILE run -p$2:10009 -d --name $1 lnd_btc --lnddir=/root/.lnd/lnd_$1
    sleep 6
}



new_address(){
    docker exec -i -t $1 lncli --lnddir=/root/.lnd/lnd_$1 --network=simnet newaddress np2wkh|grep address | awk -F "\"" '{print $4}'
}


start_btcd(){
    address=$(new_address $1)
    echo -e "=======start btcd with mining address: $address======="
    MINING_ADDRESS=$address docker-compose -f $COMPOSE_FILE up -d btcd
    docker-compose -f $COMPOSE_FILE run btcctl generate 400
}

clean(){
    echo "==============clean env=================="
    docker rm alice -f
    docker rm bob -f
    docker-compose -f $COMPOSE_FILE stop
    docker-compose -f $COMPOSE_FILE rm -f
    docker volume rm docker_bitcoin
    docker volume rm docker_shared
    docker volume rm docker_lnd
    rm -rf /tmp/thor
    mkdir -p /tmp/thor/lnd
    
}
init
clean
startlnd alice 10009
start_btcd alice
startlnd bob 20009


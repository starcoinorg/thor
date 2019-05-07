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
    docker-compose -f $COMPOSE_FILE run -p$2:10009 -p$3:80 -d --name $1 lnd_btc --lnddir=/root/.lnd/lnd_$1
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
    address=$(new_address $2)
    echo -e "=======start btcd with mining address: $address======="
    docker-compose  -f $COMPOSE_FILE stop btcd
    MINING_ADDRESS=$address docker-compose -f $COMPOSE_FILE up -d btcd
    docker-compose -f $COMPOSE_FILE run btcctl generate 400
    sleep 6
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

lncl(){
    local user=$1
    shift 1
    docker exec -i -t $user lncli --lnddir=/root/.lnd/lnd_$user  --network=simnet $@
}

create_channel(){
    bob_pubkey=$(lncl bob getinfo|grep identity_pubkey | awk -F "\"" '{print $4}')
    bob_address=$(docker inspect bob | grep IPAddress | awk -F "\"" '{print $4}'|grep -v '^$')
    lncl alice connect $bob_pubkey@$bob_address
    lncl alice --network=simnet listpeers
    lncl bob --network=simnet listpeers
    lncl alice openchannel --node_key=$bob_pubkey --local_amt=10000000 --push_amt=10000000
    docker-compose -f $COMPOSE_FILE run btcctl generate 10
}

main(){
    init
    clean
    startlnd alice 10009 8080
    startlnd bob 20009 8081    
    start_btcd alice bob
    create_channel
}

main

lncl(){
    local user=$1
    shift 1
    docker exec -i -t $user lncli --lnddir=/root/.lnd/lnd_$user  --network=simnet $ 
}

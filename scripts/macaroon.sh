#!/usr/bin/env bash

echo "alice macaroon is :"
cat /tmp/thor/lnd/lnd_alice/data/chain/bitcoin/simnet/admin.macaroon | xxd -ps -c 200 | tr -d '\n'
echo -e '\n'

echo "bob macaroon is :"
cat /tmp/thor/lnd/lnd_bob/data/chain/bitcoin/simnet/admin.macaroon | xxd -ps -c 200 | tr -d '\n'
echo -e '\n'

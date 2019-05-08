#!/usr/bin/env bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
source $SCRIPTPATH/util.sh
usage(){
    echo "Usage $(basename $0) [setup,stop,macaroon,lncli]"
}

if [ $# -lt 1 ]; then
    usage;
fi
case $"$1" in
    setup)
	setup_lnd
	;;
    stop)
	clean
	;;
    macaroon)
	echo -e "alice macaroon is:\n$(get_macaroon alice)\n"
	echo -e "bob macaroon is:\n$(get_macaroon bob)\n"
	;;
    lncli)
	shift 1
	lncl $@
esac

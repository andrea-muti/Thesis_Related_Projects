#!/bin/bash

# uccide brutalmente il processo cassandra in esecuzione sul nodo specificato
# attenzione, se ci sono più processi cassandra, li uccide tutti [attenzione in locale]

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]
  then
    echo ""
    echo " ERROR : not enough arguments"
    echo " usage: ./cassandra_killer.sh <IP-address> <remote-ssh-username> <remote-ssh-password>" 
    echo ""
    exit 1
fi

IP_ADDRESS=$1
REMOTE_USER=$2
REMOTE_PASSWORD=$3

user="$(sshpass -p ${REMOTE_PASSWORD} ssh ${REMOTE_USER}@${IP_ADDRESS} whoami)"

casspid="$(sshpass -p ${REMOTE_PASSWORD} ssh ${REMOTE_USER}@${IP_ADDRESS} pgrep -u $user -f cassandra)"; sshpass -p ${REMOTE_PASSWORD} ssh ${REMOTE_USER}@${IP_ADDRESS} kill -9 ${casspid}

echo "  - killed cassandra process in ${IP_ADDRESS}"

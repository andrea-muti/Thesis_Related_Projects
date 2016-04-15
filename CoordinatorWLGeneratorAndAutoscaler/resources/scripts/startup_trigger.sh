#!/bin/bash

# triggers the startup of the cassandra process on the node specified

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ] 
  then
    echo ""
    echo " ERROR : not enough arguments"
    echo " usage: ./startup_trigger.sh <IP-address> <remote-ssh-username> <remote-ssh-password> <cassandra-path>" 
    echo ""
    exit 1
fi

IP_ADDRESS=$1
REMOTE_USER=$2
REMOTE_PASSWORD=$3
REMOTE_CASSANDRA_PATH=$4

echo ""
echo "  *******************************************"
echo "  *         Startup Executor Trigger        *"
echo "  *******************************************"
echo ""
echo ""
echo "  - ssh into the node  ${IP_ADDRESS} "
echo "  - executing the startup ... "

sshpass -p ${REMOTE_PASSWORD} ssh ${REMOTE_USER}@${IP_ADDRESS} "${REMOTE_CASSANDRA_PATH}/bin/cassandra"

echo "  - startup completed"

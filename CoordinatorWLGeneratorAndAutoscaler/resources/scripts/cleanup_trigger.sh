#!/bin/bash

# triggers the execution of a cleanup on the node specified

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ] 
  then
    echo ""
    echo " ERROR : not enough arguments"
    echo " usage: ./cleanup_trigger.sh <IP-address> <remote-ssh-username> <remote-ssh-password> <cassandra-path>" 
    echo ""
    exit 1
fi

IP_ADDRESS=$1
REMOTE_USER=$2
REMOTE_PASSWORD=$3
REMOTE_CASSANDRA_PATH=$4

echo "  - ssh into the node  ${IP_ADDRESS} "
echo "  - executing the cleanup ... "
# versione non-bloccante (con nohup) aggiustare i parametri
#sshpass -p mUt1 ssh muti@${IP_ADDRESS} "nohup /home/muti/cassandra_new/bin/nodetool cleanup > /dev/null 2>&1 &"

#versione bloccante [ visto che vanno le cleanup fatte in serie, è meglio bloccante ]
#sshpass -p ${REMOTE_PASSWORD} ssh ${REMOTE_USER}@${IP_ADDRESS} "${REMOTE_CASSANDRA_PATH}/bin/nodetool cleanup > /dev/null 2>&1"
sshpass -p ${REMOTE_PASSWORD} ssh ${REMOTE_USER}@${IP_ADDRESS} "${REMOTE_CASSANDRA_PATH}/bin/nodetool cleanup"

echo "  - completed"

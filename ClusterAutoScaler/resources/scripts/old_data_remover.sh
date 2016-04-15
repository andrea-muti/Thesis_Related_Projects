#!/bin/bash

# triggers the removal of old data on the node specified
# This should be executed before triggering the startup of a node that was previoulsy decommissioned,
# otherwise, the startup will fail.

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ] 
  then
    echo ""
    echo " ERROR : not enough arguments"
    echo " usage: ./old_data_remover.sh <IP-address> <remote-ssh-username> <remote-ssh-password> <cassandra-path>" 
    echo ""
    exit 1
fi

IP_ADDRESS=$1
REMOTE_USER=$2
REMOTE_PASSWORD=$3
REMOTE_CASSANDRA_PATH=$4

echo ""
echo "  *******************************************"
echo "  *      Data Cleaner Executor Trigger      *"
echo "  *******************************************"
echo ""
echo ""
echo "  - ssh into the node  ${IP_ADDRESS} "
echo "  - removing old data ... "
# versione non-bloccante (con nohup)
#sshpass -p ${REMOTE_PASSWORD} ssh ${REMOTE_USER}@${IP_ADDRESS} "${REMOTE_CASSANDRA_PATH}/bin/cassandra -f > /dev/null 2>&1 &"

#versione bloccante [ visto che vanno le cleanup fatte in serie, è meglio bloccante ]
sshpass -p ${REMOTE_PASSWORD} ssh ${REMOTE_USER}@${IP_ADDRESS} "rm -rf ${REMOTE_CASSANDRA_PATH}/data/*"

echo "  - completed"

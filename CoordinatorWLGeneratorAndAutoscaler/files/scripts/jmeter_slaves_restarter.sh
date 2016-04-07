#!/bin/bash

# uccide brutalmente il jmeter slave in esecuzione

echo ""
echo "  ******************************************************"
echo "  *   SCRIPT FOR KILLING AND RESTARTING JMeter Slaves  *"
echo "  ******************************************************"
echo ""
echo ""
echo "  --- KILLING THE JMETER_SLAVES ----"
echo ""

num_iterations=5

echo "  ---------------------------------"
echo ""
echo "  - ssh into the superserver "

user="$(sshpass -p @ndr3@ ssh andrea@superserver whoami)"

for i in $(seq 0 $((num_iteratons))); do
	jmeter_pid="$(sshpass -p @ndr3@ ssh andrea@superserver pgrep -u $user -f jmeter)"; sshpass -p @ndr3@ ssh andrea@superserver kill -9 ${jmeter_pid}

	echo "  - killed jmeter "	
done

echo ""
echo "  ---------------------------------"

echo ""
echo "  - ssh into the server-virtuale-1 "

user="$(sshpass -p @ndr3@ ssh andrea@server-virtuale-1 whoami)"

for i in $(seq 0 $((num_iteratons))); do
	jmeter_pid="$(sshpass -p @ndr3@ ssh andrea@server-virtuale-1 pgrep -u $user -f jmeter)"; sshpass -p @ndr3@ ssh andrea@server-virtuale-1 kill -9 ${jmeter_pid}

	echo "  - killed jmeter "	
done

echo ""
echo "  ---------------------------------"

echo ""
echo "  - ssh into the server-virtuale-2 "

user="$(sshpass -p @ndr3@ ssh andrea@server-virtuale-2 whoami)"

for i in $(seq 0 $((num_iteratons))); do
	jmeter_pid="$(sshpass -p @ndr3@ ssh andrea@server-virtuale-2 pgrep -u $user -f jmeter)"; sshpass -p @ndr3@ ssh andrea@server-virtuale-2 kill -9 ${jmeter_pid}

	echo "  - killed jmeter "	
done

echo ""
echo "  ---------------------------------"


echo ""
echo "  - ssh into the midlab-server "

user="$(sshpass -p @ndr3@ ssh andrea@midlab-server whoami)"

for i in $(seq 0 $((num_iteratons))); do
	jmeter_pid="$(sshpass -p @ndr3@ ssh andrea@midlab-server pgrep -u $user -f jmeter)"; sshpass -p @ndr3@ ssh andrea@midlab-server kill -9 ${jmeter_pid}

	echo "  - killed jmeter "	
done

echo ""
echo "  ---------------------------------"


###############################################################################################################
echo ""
echo "  --- RESTARTING THE JMETER_SLAVES ----"
echo ""
echo "  ---------------------------------"
echo ""
echo "  - ssh into the superserver [1] "

sshpass -p @ndr3@ ssh andrea@superserver "nohup my_jmeter/apache-jmeter-2.13/bin/jmeter-server -Djava.rmi.server.hostname=192.168.2.119 > /dev/null 2>&1 &"
echo "  - jmeter-slave started @ 192.168.2.119"

echo "  ---------------------------------"
echo ""
echo "  - ssh into the superserver [2] "

sshpass -p @ndr3@ ssh andrea@superserver "nohup my_jmeter_2/apache-jmeter-2.13/bin/jmeter-server -Djava.rmi.server.hostname=192.168.2.119 > /dev/null 2>&1 &"
echo "  - jmeter-slave started @ 192.168.2.119:1100"


echo "  ---------------------------------"
echo ""
echo "  - ssh into the server-virtuale-1 "

sshpass -p @ndr3@ ssh andrea@server-virtuale-1 "nohup my_jmeter/apache-jmeter-2.13/bin/jmeter-server -Djava.rmi.server.hostname=192.168.1.113 > /dev/null 2>&1 &"
echo "  - jmeter-slave started @ 192.168.1.113"

echo "  ---------------------------------"
echo ""
echo "  - ssh into the server-virtuale-2 "

sshpass -p @ndr3@ ssh andrea@server-virtuale-2 "nohup my_jmeter/apache-jmeter-2.13/bin/jmeter-server -Djava.rmi.server.hostname=192.168.1.116 > /dev/null 2>&1 &"
echo "  - jmeter-slave started @ 192.168.1.116"

echo "  ---------------------------------"
echo ""
echo "  - ssh into the midlab-server "

sshpass -p @ndr3@ ssh andrea@midlab-server "nohup my_jmeter/apache-jmeter-2.13/bin/jmeter-server -Djava.rmi.server.hostname=192.168.0.133 > /dev/null 2>&1 &"
echo "  - jmeter-slave started @ 192.168.0.133"



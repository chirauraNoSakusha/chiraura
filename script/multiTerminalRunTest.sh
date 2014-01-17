#!/bin/bash

set -e

USAGE="USAGE: [NUM_OF_PEERS=...] [CLASSPATH=...] [WORK_DIR=...] [JAR=...] ${0}"

if [ ${#@} -gt 0 ]; then
    echo ${USAGE}
    exit
fi

PROJECT_ROOT=$(dirname ${0})/..
SCRIPT=$(basename ${0})
SCRIPT_BODY=${SCRIPT%.*}

NUM_OF_PEERS=${NUM_OF_PEERS:=5}
CLASSPATH=${CLASSPATH:=${PROJECT_ROOT}/bin}
WORK_DIR=${WORK_DIR:=${PROJECT_ROOT}/tmp/${SCRIPT_BODY}}

if [ -z "${NUM_OF_PEERS}" ]; then echo ${USAGE} 1>&2; exit 1; fi
if [ -z "${CLASSPATH}" ]; then echo ${USAGE} 1>&2; exit 1; fi
if [ -z "${WORK_DIR}" ]; then echo ${USAGE} 1>&2; exit 1; fi

trap 'kill $(jobs -rp); sleep 3; exit' INT

MAIN_PEER=$(java -classpath ${CLASSPATH} nippon.kawauso.chiraura.lib.test.IpGetter)
PORT_OFFSET=24804
BBS_PORT_OFFSET=22266

if [ "_"${JAR} = "_" ]; then
    COMMAND="java -classpath ${CLASSPATH} nippon.kawauso.chiraura.a.A"
else
    COMMAND="java -jar ${JAR}"
fi

for (( ID = 0; ID < NUM_OF_PEERS; ID ++ )); do
    # 初期個体の追加．
    if ! [ -d ${WORK_DIR}/${ID} ]; then
        mkdir -p ${WORK_DIR}/${ID}
    fi
    echo ${MAIN_PEER} $((PORT_OFFSET + (ID + NUM_OF_PEERS - 1) % NUM_OF_PEERS)) > ${WORK_DIR}/${ID}/peers.txt

    PORT=$((PORT_OFFSET + ID))
    BBS_PORT=$((BBS_PORT_OFFSET + ID))
    urxvt -e \
        ${COMMAND} \
        -root ${WORK_DIR}/${ID} \
        -port ${PORT} \
        -bbsPort ${BBS_PORT} \
        -consoleLogLevel ALL \
        -gui false &
    
    sleep 31
done

sleep 365d

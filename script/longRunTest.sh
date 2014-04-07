#!/bin/bash
# zsh はスクリプト中で jobs が動かないし， 
# 動いたとしても jobs -p でプロセス ID 以外も出力するし，
# 配列の添字が 1 からだし，わけ分からん．

set -e

USAGE="USAGE: [NUM_OF_RUN_PEERS=...] [CLASSPATH=...] [WORK_DIR=...] [MAIN_GUI=...] ${0}"

if [ ${#@} -gt 0 ]; then
    echo ${USAGE}
    exit
fi

PROJECT_ROOT=$(dirname ${0})/..
SCRIPT=$(basename ${0})
SCRIPT_BODY=${SCRIPT%.*}

NUM_OF_RUN_PEERS=${NUM_OF_RUN_PEERS:=10}
CLASSPATH=${CLASSPATH:=${PROJECT_ROOT}/bin}
WORK_DIR=${WORK_DIR:=${PROJECT_ROOT}/tmp}

if [ -z "${NUM_OF_RUN_PEERS}" ]; then echo ${USAGE} 1>&2; exit 1; fi
if [ -z "${CLASSPATH}" ]; then echo ${USAGE} 1>&2; exit 1; fi
if [ -z "${WORK_DIR}" ]; then echo ${USAGE} 1>&2; exit 1; fi

trap 'kill $(jobs -rp); sleep 3; exit' INT

PORT_OFFSET=24804
BBS_PORT_OFFSET=22266

LIFETIME=$((1000 * 60 * 30)) # 30 分．
INTERVAL=$((1000 * 31)) # 30 秒．
PUBLIC_KEY_LIFETIME=$((LIFETIME / 2))
COMMON_KEY_LIFETIME=$((LIFETIME / 5))

MAIN_ID=0
MAIN_LIFETIME=$((1000 * 60 * 60 * 24 * 365 * 100)) # 100 年．
MAIN_BOARD_RATE=0.1
MAIN_PORT=${PORT_OFFSET}
MAIN_BBS_PORT=${BBS_PORT_OFFSET}
MAIN_GUI=${MAIN_GUI:=true}

java -classpath ${CLASSPATH} nippon.kawauso.chiraura.a.TestUnit ${MAIN_LIFETIME} \
    Rom ${INTERVAL} ${MAIN_BOARD_RATE} \
    -root ${WORK_DIR}/${SCRIPT_BODY}_${MAIN_ID} \
    -port ${MAIN_PORT} \
    -bbsPort ${MAIN_BBS_PORT} \
    -publicKeyLifetime ${PUBLIC_KEY_LIFETIME} \
    -commonKeyLifetime ${COMMON_KEY_LIFETIME} \
    -gui ${MAIN_GUI} &

MAIN_PEER=${MAIN_PEER:="$(java -classpath ${CLASSPATH} nippon.kawauso.chiraura.lib.test.IpGetter) ${PORT_OFFSET}"}

NUM_OF_PEERS=$((NUM_OF_RUN_PEERS * 5))
WRITE_RATE=0.666

MASTER_LOG=${WORK_DIR}/${SCRIPT_BODY}_master.log
rm -f ${MASTER_LOG}

sleep 3

RUN_IDS=()
while true; do
    # SEVERE が出てたら停止．
    if [ $(grep -R SEVERE ${WORK_DIR}/${SCRIPT_BODY}_*/log | wc -l) -gt 0 ]; then
        kill $(jobs -rp)
        sleep 3
        echo "何か SEVERE ってます．"
        exit 1
    fi

    NUM_OF_CURRENT_RUN_PEERS=$(jobs -r | wc -l)

    # 何かの異常で全然死ななかったら停止．
    if ((NUM_OF_CURRENT_RUN_PEERS >= 2 * NUM_OF_RUN_PEERS)); then
        kill $(jobs -rp)
        sleep 3
        echo "何か死にません．"
        exit 1
    fi        

    # 古い番号を消す．
    if ((NUM_OF_CURRENT_RUN_PEERS <= ${#RUN_IDS[@]})); then
        RUN_IDS=(${RUN_IDS[@]:((${#RUN_IDS[@]} - (NUM_OF_CURRENT_RUN_PEERS - 1))):${#RUN_IDS[@]}})
    fi

    # 使ってない番号を探す．
    while true; do
        NOT_RUN_ID=$((1 + RANDOM % (NUM_OF_PEERS - 1)))
        
        SUCCESS=true
        for ((i = 0; i < ${#RUN_IDS[@]}; i++)); do
            if ((NOT_RUN_ID == RUN_IDS[i])); then
                SUCCESS=false
                break;
            fi
        done

        if ${SUCCESS}; then
            RUN_IDS[${#RUN_IDS[@]}]=${NOT_RUN_ID}
            ID=${NOT_RUN_ID}
            break;
        fi

        # # バグで無限ループに入っても大丈夫なように小休止．
        # sleep 1
    done

    AUTHOR="${ID} 番目の男"


    # 初期個体の追加．
    if ! [ -d ${WORK_DIR}/${SCRIPT_BODY}_${ID} ]; then
        mkdir -p ${WORK_DIR}/${SCRIPT_BODY}_${ID}
    fi
    echo ${MAIN_PEER} > ${WORK_DIR}/${SCRIPT_BODY}_${ID}/peers.txt

    PORT=$((PORT_OFFSET + ID))
    BBS_PORT=$((BBS_PORT_OFFSET + ID))
    java -classpath ${CLASSPATH} nippon.kawauso.chiraura.a.TestUnit ${LIFETIME} \
        Sequential ${INTERVAL} ${WRITE_RATE} "${AUTHOR}" \
        -root ${WORK_DIR}/${SCRIPT_BODY}_${ID} \
        -port ${PORT} \
        -bbsPort ${BBS_PORT} \
        -publicKeyLifetime ${PUBLIC_KEY_LIFETIME} \
        -commonKeyLifetime ${COMMON_KEY_LIFETIME} \
        -gui false &

    echo $(date) ID ${ID} RUN_IDS ${RUN_IDS[@]} >> ${MASTER_LOG}
    jobs -pl >> ${MASTER_LOG}

    sleep $((LIFETIME / NUM_OF_RUN_PEERS / 1000))
done

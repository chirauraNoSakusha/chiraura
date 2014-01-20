#!/bin/sh

set -e

SOURCE=${SOURCE:=$(dirname ${0})/../src}
DESTINATION=${DESTINATION:=${HOME}/tmp}
MAIN=${MAIN:=nippon.kawauso.chiraura.a.A}
TEST_CHECKER=${TEST_CHECKER:=nippon.kawauso.chiraura.util.TestChecker}
MOSAICER=${MOSAICER:=nippon.kawauso.chiraura.util.PeerMosaicer}
UNMOSAICER=${UNMOSAICER:=nippon.kawauso.chiraura.util.PeerUnmosaicer}

AUTHOR_HOST=${AUTHOR_HOST:=""}

DIR=$(date +%F-%H-%M-%S)

if [ -e ${DESTINATION}/${DIR} ]; then
    rm -rf ${DESTINATION}/${DIR}
fi

echo "コンパイル開始"
CLASS=${DESTINATION}/${DIR}/bin

mkdir -p ${CLASS}

javac -sourcepath ${SOURCE} -d ${CLASS} ${SOURCE}/$(echo ${MAIN} | sed 's/\./\//g').java


echo "JAR 作成開始"
RAW=${DESTINATION}/${DIR}/chiraura.jar

jar cfe ${RAW} ${MAIN} -C ${CLASS} .


echo "状態検査開始"
javac -sourcepath ${SOURCE} -d ${CLASS} ${SOURCE}/$(echo ${TEST_CHECKER} | sed 's/\./\//g').java

TEST_STATE=$(java -classpath ${CLASS} ${TEST_CHECKER})

if [ ${TEST_STATE} = "true" ]; then
    echo "非制限状態です。"
    exit 1
fi

JAR=${DESTINATION}/${DIR}/chiraura.jar

if [ -z "${AUTHOR_HOST}" ]; then
    echo "環境変数 AUTHOR_HOST として 初期個体が指定されていません。"
    echo "ここまでで終了します。"
    exit
fi

echo "初期個体の準備開始"
PEER=${DESTINATION}/${DIR}/peers.txt

javac -sourcepath ${SOURCE} -d ${CLASS} ${SOURCE}/$(echo ${MOSAICER} | sed 's/\./\//g').java

javac -sourcepath ${SOURCE} -d ${CLASS} ${SOURCE}/$(echo ${UNMOSAICER} | sed 's/\./\//g').java

if [ -e $(dirname "${0}")/peers.txt ]; then
    DECODED=$(java -classpath ${CLASS} ${UNMOSAICER} $(cat $(dirname "${0}")/peers.txt | sed 's/^\^\(.*\)$/\1/'))

    if ! [ "_${AUTHOR_HOST}" = "_${DECODED}" ]; then
        rm $(dirname "${0}")/peers.txt
    fi
fi

if ! [ -e $(dirname "${0}")/peers.txt ]; then
    # 改行は環境に依存するので念のため改行無し。
    printf "^"$(java -classpath ${CLASS} ${MOSAICER} ${AUTHOR_HOST}) > $(dirname "${0}")/peers.txt
fi

cp $(dirname "${0}")/peers.txt ${PEER}


echo "書庫作成開始"
OUTPUT=${DESTINATION}/${DIR}/chiraura_$(date +%F).zip

zip -q9 ${OUTPUT} -j ${JAR} ${PEER}


echo "チェックサム計算開始"
SUM=${DESTINATION}/${DIR}/chiraura.sum

rm -f ${SUM}
for i in md5sum sha1sum; do
    echo ${i} >> ${SUM}
    ${i} ${OUTPUT} >> ${SUM}
done

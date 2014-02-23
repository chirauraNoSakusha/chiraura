#!/bin/sh

set -e

TOP=${TOP:=$(dirname ${0})/..}
DESTINATION=${DESTINATION:=${HOME}/tmp/$(date +%F-%H-%M-%S)}
MAIN=${MAIN:=nippon.kawauso.chiraura.a.A}
TEST_CHECKER=${TEST_CHECKER:=nippon.kawauso.chiraura.util.TestChecker}
MOSAICER=${MOSAICER:=nippon.kawauso.chiraura.util.PeerMosaicer}
UNMOSAICER=${UNMOSAICER:=nippon.kawauso.chiraura.util.PeerUnmosaicer}

AUTHOR_HOST=${AUTHOR_HOST:=""}

if [ -e ${DESTINATION} ]; then
    rm -rf ${DESTINATION}
fi


echo "コンパイル開始。"
SOURCE=${TOP}/src
CLASS=${DESTINATION}/bin

mkdir -p ${CLASS}

javac -sourcepath ${SOURCE} -d ${CLASS} ${SOURCE}/$(echo ${MAIN} | sed 's/\./\//g').java


echo "JAR 作成開始。"
JAR=${DESTINATION}/chiraura.jar

jar cfe ${JAR} ${MAIN} -C ${CLASS} .


echo "状態検査開始。"
# 事後検査なのは、余計なことせずに、 余計なクラスを jar に入れずに済むから。

javac -sourcepath ${SOURCE} -d ${CLASS} ${SOURCE}/$(echo ${TEST_CHECKER} | sed 's/\./\//g').java

TEST_STATE=$(java -classpath ${CLASS} ${TEST_CHECKER})
if [ ${TEST_STATE} = "true" ]; then
    echo "非制限状態です。"
    echo "ここまでで終了します。"
    exit 0
fi


echo "初期個体の準備開始。"
PEER_SOURCE=${TOP}/peers.txt
PEER=${DESTINATION}/peers.txt

javac -sourcepath ${SOURCE} -d ${CLASS} ${SOURCE}/$(echo ${MOSAICER} | sed 's/\./\//g').java
javac -sourcepath ${SOURCE} -d ${CLASS} ${SOURCE}/$(echo ${UNMOSAICER} | sed 's/\./\//g').java

if [ -e ${PEER_SOURCE} ]; then
    DECODED=$(java -classpath ${CLASS} ${UNMOSAICER} $(cat ${PEER_SOURCE} | sed 's/^\^\(.*\)$/\1/'))

    if [ -n "${AUTHOR_HOST}" ] && [ "_${AUTHOR_HOST}" != "_${DECODED}" ]; then
        rm ${PEER_SOURCE}
    fi
fi

if ! [ -e ${PEER_SOURCE} ]; then
    if [ -z "${AUTHOR_HOST}" ]; then
        echo "初期個体が環境変数 AUTHOR_HOST としても ${PEER_SOURCE} としても指定されていません。"
        echo "ここまでで終了します。"
        exit
    fi

    # 改行は環境に依存するので念のため改行無し。
    printf "^"$(java -classpath ${CLASS} ${MOSAICER} ${AUTHOR_HOST}) > ${PEER_SOURCE}
fi

cp ${PEER_SOURCE} ${PEER}


echo "メニューの準備開始。"
MENU_SOURCE=${TOP}/menu.txt
MENU=${DESTINATION}/menu.txt

if ! [ -e ${MENU_SOURCE} ]; then
    echo "メニューが ${MENU_SOURCE} として指定されていません。"
    echo "ここまでで終了します。"
    exit
fi

# 改行を CRLF に。長い物に巻かれる。改行として解釈されなくなることはないし。
sed 's/$/\r/' ${MENU_SOURCE} > ${MENU}


echo "書庫作成開始。"
OUTPUT=${DESTINATION}/chiraura_$(date +%F).zip

zip -q9 ${OUTPUT} -j ${JAR} ${PEER} ${MENU}


echo "チェックサム計算開始。"
SUM=${DESTINATION}/chiraura.sum

rm -f ${SUM}
for i in md5sum sha1sum; do
    echo ${i} >> ${SUM}
    ${i} ${OUTPUT} >> ${SUM}
done


echo "${OUTPUT} の作成が完了しました。"

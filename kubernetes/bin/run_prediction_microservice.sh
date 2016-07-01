#!/bin/bash

set -o nounset
set -o errexit

if [ "$#" -ne 4 ]; then
    echo "need <microservice_name> <microservice_image> <microservice_version> <client>"
    exit -1
fi

STARTUP_DIR="$( cd "$( dirname "$0" )" && pwd )"
NAME=$1
IMAGE=$2
VERSION=$3
CLIENT=$4

function create_microservice_conf {
    
    mkdir -p ${STARTUP_DIR}/../conf/microservices
    if test -f "${STARTUP_DIR}/../conf/microservices/microservice-${NAME}.json"; 
    then 
	echo "The microservice already exists. Will make a backup to .prev";
	cp ${STARTUP_DIR}/../conf/microservices/microservice-${NAME}.json ${STARTUP_DIR}/../conf/microservices/microservice-${NAME}.json.prev
    fi
    cat ${STARTUP_DIR}/../conf/microservice.json.in | sed -e "s|%NAME%|${NAME}|" | sed -e "s|%IMAGE%|${IMAGE}|" | sed -e "s|%VERSION%|${VERSION}|" > ${STARTUP_DIR}/../conf/microservices/microservice-${NAME}.json

}


function run_microservice {

    kubectl apply -f ${STARTUP_DIR}/../conf/microservices/microservice-${NAME}.json

}

function configure_seldon {

    ${STARTUP_DIR}/seldon-cli predict_alg --action delete --client-name ${CLIENT} --predictor-name externalPredictionServer
    ${STARTUP_DIR}/seldon-cli predict_alg  --action add --client-name ${CLIENT} --predictor-name externalPredictionServer --config io.seldon.algorithm.external.url=http://${NAME}:5000/predict --config io.seldon.algorithm.external.name=${NAME}
    ${STARTUP_DIR}/seldon-cli predict_alg --action commit --client-name ${CLIENT}

}


function start_microservice {

    create_microservice_conf

    run_microservice
    
    configure_seldon

}


start_microservice "$@"





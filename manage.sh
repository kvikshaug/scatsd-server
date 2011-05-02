#!/usr/bin/env bash
set -e
PIDFILE=../../../scatsd.pid
LOGFILE=../../../scatsd.log

function start() {
  pushd "$(dirname $0)/target/scala_2.8.1/classes/"
  scala no.kvikshaug.scatsd.ScatsD > $LOGFILE 2>&1 &
  echo "$!" > $PIDFILE
  popd
}

function stop() {
  pushd "$(dirname $0)/target/scala_2.8.1/classes/"
  if [ -e "$PIDFILE" ]
  then
      kill "$(cat $PIDFILE)"
      rm "$PIDFILE"
  fi
  popd
}

function build() {
  sbt package
}

function usage() {
  echo "$0 [build|start|stop]"
  echo "build: builds scatsd"
  echo "start: starts scatsd"
  echo "stop: stops scatsd"
}

cd "$(dirname $0)"

if [ $# -lt 1 ]
then
    usage
    exit 1
fi

for arg in "$@"
do
    case "$arg" in
        "build")
          build
          ;;

        "start")
            start
            ;;

        "stop")
            stop
            ;;
        *)
            usage
            exit 1
    esac
 done

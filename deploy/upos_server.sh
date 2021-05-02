USER=ubuntu
JAVA_HOME=/usr/lib/jvm/jdk1.8.0_51
UPOS_SERVER_HOME=~/pos_server
JSVC=$UPOS_SERVER_HOME/jsvc
PID_FILE=$UPOS_SERVER_HOME/server.pid
OUT_FILE=$UPOS_SERVER_HOME/server.out
ERR_FILE=$UPOS_SERVER_HOME/server.err
 
CLASSPATH=\
$UPOS_SERVER_HOME/uchefpos.jar
 
MAIN_CLASS=com.uchef.upos.UPosServerDaemonStarter

jsvc_exec()
{
   rm -f $OUT_FILE
   $JSVC -user $USER -java-home $JAVA_HOME -pidfile $PID_FILE -outfile $OUT_FILE -errfile $OUT_FILE -cp $CLASSPATH $MAIN_CLASS
}

case "$1" in
  start)
    #
    # Start Daemon
    #
    rm -f $OUT_FILE
    $JSVC \
    -user $USER \
    -java-home $JAVA_HOME \
    -pidfile $PID_FILE \
    -outfile $OUT_FILE \
    -errfile $OUT_FILE\
    -cp $CLASSPATH \
    $MAIN_CLASS
    #
    # To get a verbose JVM
    #-verbose \
    # To get a debug of jsvc.
    #-debug \
    exit $?
    ;;
  stop)
    #
    # Stop Daemon
    #
    $JSVC \
    -stop \
    -nodetach \
    -java-home $JAVA_HOME \
    -pidfile $PID_FILE \
    -outfile $OUT_FILE \
    -errfile $OUT_FILE \
    -cp $CLASSPATH \
    $MAIN_CLASS
    exit $?
    ;;
  restart)
    exit $?
    ;; 
  *)
    echo "[Usage] upos_server.sh start | stop"
    exit 1;;
esac

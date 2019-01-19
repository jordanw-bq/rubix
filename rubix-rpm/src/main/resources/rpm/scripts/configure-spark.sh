#!/bin/bash

RUBIX_HOME_DIR=/usr/lib/rubix
RUBIX_CACHE_DIR=/var/lib/rubix/cache

# Configure Spark as RubiX client
SPARK_DEFAULTS_CONF_FILE="/etc/spark/conf/spark-defaults.conf"
SPARK_DRIVER_EXTRA_CP_OPTION="spark\.driver\.extraClassPath"
SPARK_EXECUTOR_EXTRA_CP_OPTION="spark\.executor\.extraClassPath"
RUBIX_EXTRA_CP=":${RUBIX_HOME_DIR}/lib/*"
sed -i "/^$SPARK_DRIVER_EXTRA_CP_OPTION/ s|$|$RUBIX_EXTRA_CP|" ${SPARK_DEFAULTS_CONF_FILE}
sed -i "/^$SPARK_EXECUTOR_EXTRA_CP_OPTION/ s|$|$RUBIX_EXTRA_CP|" ${SPARK_DEFAULTS_CONF_FILE}

echo "spark.hadoop.fs.s3.impl   com.qubole.rubix.hadoop2.CachingNativeS3FileSystem" >> ${SPARK_DEFAULTS_CONF_FILE}
echo "spark.hadoop.fs.s3n.impl  com.qubole.rubix.hadoop2.CachingNativeS3FileSystem" >> ${SPARK_DEFAULTS_CONF_FILE}
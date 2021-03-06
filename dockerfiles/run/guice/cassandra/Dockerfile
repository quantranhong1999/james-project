# Run James
#
# VERSION	1.0

FROM adoptopenjdk:11-jre-hotspot

# Ports that are used
#
# 25   SMTP without authentication
# 110  POP3
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 993  IMAP with socketTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 110 143 465 587 993 4000 8000

WORKDIR /root
RUN apt-get update && apt-get install -y unzip

# Get data we need to run James : build results and configuration
ADD destination/james-server-cassandra-guice.zip /root
RUN unzip james-server-cassandra-guice.zip && \
   cp -r /root/james-server-cassandra-guice/* /root && \
   rm james-server-cassandra-guice.zip && \
   rm -r james-server-cassandra-guice
ADD destination/james-server-cli.jar /root/james-cli.jar
ADD destination/james-server-cli.lib /root/james-server-cli.lib
ADD destination/glowroot/plugins /root/glowroot/plugins
ADD destination/glowroot/glowroot.jar /root/glowroot/glowroot.jar
ADD destination/glowroot/lib/glowroot-embedded-collector.jar /root/glowroot/lib/glowroot-embedded-collector.jar
ADD destination/glowroot/lib/glowroot-logging-logstash.jar /root/glowroot/lib/glowroot-logging-logstash.jar
ADD destination/glowroot/admin.json /root/glowroot/admin.json
ADD destination/run_james.sh /root/run_james.sh

# Data for ActiveMQ mailQueue
VOLUME /root/var
VOLUME /logs
VOLUME /root/conf
VOLUME /root/glowroot/plugins
VOLUME /root/glowroot/data

ENV PATH="$PATH:/root/glowroot/lib" \
    JVM_OPTIONS="" \
    GLOWROOT_ACTIVATED="false"

ENTRYPOINT ./run_james.sh

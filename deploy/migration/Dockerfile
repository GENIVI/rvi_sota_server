FROM java:8

RUN apt-get update && apt-get install -y mysql-client

WORKDIR /migrations

RUN git clone https://github.com/advancedtelematic/rvi_sota_server.git

RUN \
  mkdir flyway && \
  curl -O https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/3.2.1/flyway-commandline-3.2.1.tar.gz

RUN echo "0750456fff19128f3d42e45d2e2a6e318ecb947c flyway-commandline-3.2.1.tar.gz" | sha1sum -c -

RUN tar xvf flyway-commandline-3.2.1.tar.gz -C flyway --strip-components=1

ADD entrypoint.sh /migrations/entrypoint.sh

ADD wait_for_mysql.sh wait_for_mysql.sh

ENTRYPOINT /migrations/entrypoint.sh

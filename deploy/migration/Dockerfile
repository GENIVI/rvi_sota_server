FROM advancedtelematic/sbt:0.13.8

RUN apt-get update \
 && apt-get install -y git

RUN git clone https://github.com/advancedtelematic/rvi_sota_server.git
WORKDIR /app/rvi_sota_server

# fetch dependencies
RUN sbt flywayMigrate || true

CMD ["/usr/local/sbt/bin/sbt", "flywayMigrate"]

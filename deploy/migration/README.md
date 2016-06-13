# Migration running container

This container runs the migrations for the current code for rvi sota
server.

To build the container:

    docker build -t rvi-migrator .
    
Edit the `env` file, adjusting environment variables to your
environment. if running from docker-compose, those vars need to be
set.

    docker run \
        migration feat/docker_migrator ✗  docker run -it --env-file rvi-migrator
        
If the db is running on the docker host, then you'll need `--net=host`.

# Deploying with Docker Launcher

## Development System

For deploying a development system to your local machine, make sure you have
Docker Launcher and Docker installed and configured to your liking.

Then copy `docker-launcher.yml.example` to `docker-launcher.yml` and edit it to
match your preferences. You can then deploy a development system with

```sh
docker-launcher -c docker-launcher.yml deploy-sota-local.yml
```

To stop the running system run

```sh
docker-launcher --teardown -c docker-launcher.yml deploy-sota-local.yml
```

## CI/Staging remote system

For deploying a remote system for staging and CI, with a docker daemon
accessible from a single IP run

```sh
cd deploy/sota-docker-hub
ansible-playbook -i inventory launch-docker-hub.yml
```

You'll get prompted for the IP you want to allow to connect to the docker daemon.

**NOTE:** This depends on a properly configured `docker-launcher.yml`, even if
it is not using Docker Launcher.

To connect to the remote instance, find its IP from AWS Console and prepend your
docker commands with `-H tcp://<Address-of-staging-system>:2375` like this

```sh
docker -H tcp://<Address-of-staging-system>:2375 build -t something .
```

## Deploying against the staging system

```sh
export DOCKER_HOST=tcp://<Address-of-staging-system>:2375
docker-launcher -c deploy/docker-launcher.yml deploy/deploy-sota-local.yml
```

---
layout: page
title: "Deployment with Docker Launcher"
category: doc
date: 2015-08-26 14:09:53
order: 2
---

Docker Launcher launches projects using Docker and Ansible. It is a prerequisite for deploying the SOTA server, and can be installed from [its GitHub repository.](https://github.com/advancedtelematic/docker-launcher) **Please ensure you are using the latest version of Docker Launcher. Older versions may not correctly deploy the SOTA Server.**

# Deploying with Docker Launcher

## Development System

As a prerequisite to deploying a local development system, you must run

```sh
./sbt docker:publishLocal
```

Once this completes successfully, you may deploy with [Docker Launcher.](https://github.com/advancedtelematic/docker-launcher)
For deploying a development system to your local machine, make sure you have
Docker Launcher and Docker installed and configured to your liking. Docker Launcher currently requires Docker >= v.1.6.1 and Ansible v.1.9.2. Ansible 2.x has not yet been tested, and is not guaranteed to work.

First, enter the `deploy` directory, and copy the example Docker Launcher configuration file, then edit it to match your preferences. The only values you should need to change are the `user` and `email` fields in `docker-launcher.yml`. These should be a valid username and associated email address for Docker Hub.

```sh
cd deploy
cp docker-launcher.yml.example docker-launcher.yml
```

You can then deploy a development system with

```sh
docker-launcher -c docker-launcher.yml deploy-sota-local.yml
```

For a local deployment, a Docker Hub password is not actually required. When prompted for one, you can just press enter to continue.

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

**NOTE:** This depends on a properly configured `docker-launcher.yml`, even though
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

For migrations you need to tell flyway the connection details like so:

```sh
CORE_DB_USER=<user> CORE_DB_PASSWORD=<password> CORE_DB_URL=jdbc:mysql://<host>:3306/sota sbt core/flywayMigrate
```

With `<user>`, `<password>` and `<host>` replaced with suitable values.





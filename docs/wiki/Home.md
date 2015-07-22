# GENIVI SOTA Project

Welcome to the GENIVI SOTA wiki! This is the hub for documentation of all the components of the GENIVI SOTA project.

The project is comprised of four sub-projects--three for the SOTA server, and one for the client.

 - [core](https://github.com/advancedtelematic/sota-server/tree/master/core) - The central component of the SOTA server that implements package queuing and distribution
 - [external-resolver](https://github.com/advancedtelematic/sota-server/tree/master/external-resolver) - An implementation of a package resolution server, which maps VINs to a list of software components to be installed on a vehicle
 - [web-server](https://github.com/advancedtelematic/sota-server/tree/master/web-server) - The web interface to the SOTA server, allowing administration of vehicles and software components
 - [sota_client](https://github.com/advancedtelematic/sota_client) - The vehicle-resident SOTA download client

# Build Instructions

## Client

## Server

### Core

### Web Server

### External Resolver

# Test Configuration and Execution

## Client

## Server

# System Deployment

## Client

The *sota-client* project builds an RPM that can be installed on a target system, and includes Yocto recipes to allow it to be built into a GENIVI Demo Platform or AGL Reference Platform image.

## Server

For the server-side components, this project includes a [Docker Launcher](https://github.com/advancedtelematic/sota-server/wiki/Docker-Launcher) configuration file to allow the cluster of components to be deployed conveniently to a developer machine or IaaS cloud (e.g. AWS).

### Local

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

### AWS

For deploying a development system to AWS, make sure you have
Docker Launcher and Docker installed and configured to your liking.

Then copy `docker-launcher.yml.example` to `docker-launcher.yml` and edit it to
match your preferences. You can then deploy a development system with

```sh
docker-launcher -c docker-launcher.yml deploy-sota-aws.yml
```

To stop the running system run

```sh
docker-launcher --teardown -c docker-launcher.yml deploy-sota-aws.yml
```

# Architecture Documents


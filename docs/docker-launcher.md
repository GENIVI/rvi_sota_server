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

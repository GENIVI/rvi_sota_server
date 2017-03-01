# RVI Sota Server - API Test Collection 
> A collection of POSTMAN tests for your local build of the rvi_sota_server.

[![](https://at.projects.genivi.org/wiki/download/attachments/4784219/genivi-icon.jpg?version=1&modificationDate=1475025405000&api=v2)](https://github.com/advancedtelematic/rvi_sota_server)&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[![](https://app.shippable.com/mktg/images/logos/postman.png)](https://www.getpostman.com/) 
![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg) [![Open Source Love](https://badges.frapsoft.com/os/v2/open-source.svg?v=103)](https://github.com/advancedtelematic/rvi_sota_server) 

This test suite exercises almost all of the APIs of Core, Device Registry, and Resolver. You can run it interactively with [Postman](https://www.getpostman.com/), or from the command line with Newman. The quickest, easiest way to get it running is with Docker. Stand up your test deployment of rvi_sota_server with [docker-compose](http://advancedtelematic.github.io/rvi_sota_server/doc/deployment-with-dockercompose.html), then run all the tests with this one-liner:

```
docker run -v $(pwd):/etc/newman -t --network dockercompose_default postman/newman_alpine33 --collection="00-CompleteSystemTest-dockerized.postman_collection.json" --environment="rvi_sota_server-dockerized.postman_environment.json"
```

You can also install Newman locally with npm:

```
$ npm install newman --global;
```

The postman collections include a bunch of variables; you can import an appropriate environment into your Postman environment using `rvi_sota_server-LocalHost.postman_environment.json`.
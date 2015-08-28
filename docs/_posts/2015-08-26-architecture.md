---
layout: page
title: "Architecture"
category: dev
date: 2015-08-26 14:14:23
---

# SOTA Architecture

The SOTA System includes a client and server, communicating using the RVI protocol. The SOTA Server is comprised of three sub-modules:

 - [core](https://github.com/PDXostc/rvi_sota_server/tree/master/core) - The central component of the SOTA server that implements package queuing and distribution
 - [external-resolver](https://github.com/PDXostc/rvi_sota_server/tree/master/external-resolver) - An implementation of a package resolution server, which maps VINs to a list of software components to be installed on a vehicle
 - [web-server](https://github.com/PDXostc/rvi_sota_server/tree/master/web-server) - The web interface to the SOTA server, allowing administration of vehicles and software components

The client is a standalone project hosted [here](https://github.com/PDXostc/rvi_sota_client).

The system architecture is illustrated here:

![System Architecture Diagram](../images/System-Architecture-Diagram.svg)

The data model for the system is illustrated here:

![Data Model Diagram](../images/Data-Model.svg)

## Context Diagrams

### Level 0

![Level 0 Context Diagram](../images/Level-0-Context-Diagram.svg)

### Level 1 SOTA Server

![Level 1 Context Diagram](../images/Level-1-SOTA-Server-Context-Diagram.svg)

## Requirements

You can find a complete list of the [software requirements here](../ref/requirements.html) 

## Dependencies

You can find a complete list of [software dependencies here](../ref/dependencies.html)


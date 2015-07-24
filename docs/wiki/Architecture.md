# SOTA Architecture

The SOTA System includes a client and server, communicating using the RVI protocol. The SOTA Server is comprised of three sub-modules:

 - [core](https://github.com/advancedtelematic/sota-server/tree/master/core) - The central component of the SOTA server that implements package queuing and distribution
 - [external-resolver](https://github.com/advancedtelematic/sota-server/tree/master/external-resolver) - An implementation of a package resolution server, which maps VINs to a list of software components to be installed on a vehicle
 - [web-server](https://github.com/advancedtelematic/sota-server/tree/master/web-server) - The web interface to the SOTA server, allowing administration of vehicles and software components

The client is a standalone project hosted [here](https://github.com/advancedtelematic/sota-client).

The system architecture is illustrated here:

![System Architecture Diagram](images/System-Architecture-Diagram.png)

The data model for the system is illustrated here (click to enlarge):

<a href="images/Data-Model-large.png" border="0">
![Data Model Diagram](images/Data-Model.png)
</a>

## Requirements

You can find a complete list of the [software requirements here](Requirements) 

## Dependencies

You can find a complete list of [software dependencies here](Dependencies)

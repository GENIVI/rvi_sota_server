# Whitelisted Interactions

## <a name='TOC'>Table of Contents</a>

1. [SOTA-1](#sota-1) Add VIN and Package ([JIRA](https://advancedtelematic.atlassian.net/browse/SOTA-1))
    - [WL-1](#wl-1) Web Browser sends new VIN to Web Server
    - [WL-2](#wl-2) Web Server sends new package data to Core
    - [WL-3](#wl-3) Web Server sends new VIN to Core
    - [WL-4](#wl-4) Web Server sends new package data to Core
    - [WL-5](#wl-5) Web Server sends new package data to External Resolver
    - [WL-6](#wl-6) Web Server sends new VIN to External Resolver
    - [WL-7](#wl-7) External Resolver persists new VIN to External Resolver Database
    - [WL-8](#wl-8) External Resolver persists new package data to External Resolver Database
    - [WL-9](#wl-9) Core persists new VIN to Core Database
    - [WL-10](#wl-10) Core persists new package data to Core Database
1. [SOTA-2](#sota-2) Add basic filter with package dependency ([JIRA](https://advancedtelematic.atlassian.net/browse/SOTA-2))
    - [WL-11](#wl-11) Web Browser sends new filter data to Web Server
    - [WL-12](#wl-12) Web Server sends new filter data to External Resolver
    - [WL-13](#wl-13) External Resolver persists new filter data to External Resolver Database
    - [WL-14](#wl-14) Web Browser sends filter-to-package association to Web Server
    - [WL-15](#wl-15) Web Server sends filter-to-package association to External Resolver
    - [WL-16](#wl-16) External Resolver persists filter-to-package assocation to External Resolver Database
1. [SOTA-3](#sota-3) Initiate software update ([JIRA](https://advancedtelematic.atlassian.net/browse/SOTA-3))
    - [WL-17](#wl-17) Web Browser sends Queue Package Request to Web Server
    - [WL-18](#wl-18) Web Server sends Queue Package Request to Core
    - [WL-19](#wl-19) Core looks-up Package ID in Core Database
    - [WL-20](#wl-20) Core sends Resolve VIN Request to External Resolver
    - [WL-21](#wl-21) External Resolver looks-up Package ID filters in External Resolver Database
    - [WL-22](#wl-22) External Resolver looks-up VIN in External Resolver Database
    - [WL-23](#wl-23) External Resolver looks-up Package Dependencies in External Resolver Database
    - [WL-24](#wl-24) Core sends Software Update Metadata for VIN to RVI Node

## <a name="sota-1">[SOTA-1](https://advancedtelematic.atlassian.net/browse/SOTA-1) Add VIN and Package</a>

### <a name="wl-1">[WL-1](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-1) Web Browser sends new VIN to Web Server</a>

The Web Browser can send new VIN data to the Web Server that conform to the VIN standard (17 digits long, only capital letters and numbers), using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserterd' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN alread exists' message in the response body and a 409 status code.

### <a name="wl-2">[WL-2](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-2) Web Server sends new package data to Core</a>

The Web Server can send the new package data to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a new Package insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a new Package insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="wl-3">[WL-3](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-3) Web Server sends new VIN to Core</a>

The Web Server can send the VINs data to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="wl-4">[WL-4](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-4) Web Server sends new package data to Core</a>

The Web Server can send new Package data to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Package insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Package insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="wl-5">[WL-5](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-5) Web Server sends new package data to External Resolver</a>

The Web Server can send new VINs to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.


### <a name="wl-6">[WL-6](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-6) Web Server sends new VIN to External Resolver</a>

The Web Server can send new VINs to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.


### <a name="wl-7">[WL-7](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-7) External Resolver persists new VIN to External Resolver Database</a>

The External Resolver can persist new VIN data to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if the VIN already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-8">[WL-8](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-8) External Resolver persists new package data to External Resolver Database</a>

The External Resolver can persist new Package data to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if the Package already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-9">[WL-9](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-9) Core persists new VIN to Core Database</a>

Core can persist new VIN data to the Core Database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if a new database entry is created, it will respond with a 'Success' message.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if the VIN already exists, it will respond with a 'Record exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-10">[WL-10](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-10) Core persists new package data to Core Database</a>

Core can persist new package data to the Core Database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if a new database entry is created, it will respond with a 'Success' message.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if the Package already exists, it will respond with a 'Record exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

## <a name="sota-2">[SOTA-2](https://advancedtelematic.atlassian.net/browse/SOTA-2) Add basic filter with package dependency</a>

### <a name="wl-11">[WL-11](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-11) Web Browser sends new filter data to Web Server</a>

The Web Browser can send a new Filter's data to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new Filter insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new Filter insertion process and if a database entry already exists, it will respond with a 'Filter already exists' message in the response body and a 409 status code.

### <a name="wl-12">[WL-12](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-12) Web Server sends new filter data to External Resolver</a>

The Web Server can send a new Filter's data to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if a database entry already exists, it will respond with a 'Filter already exists' message in the response body and a 409 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if the Filter expression fails validation, it will respond with a 'Filter failed validation' message in the response body and a 406 status code.

### <a name="wl-13">[WL-13](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-13) External Resolver persists new filter data to External Resolver Database</a>

The External Resolver can persist a new Filter to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter data and if the Filter already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-14">[WL-14](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-14) Web Browser sends filter-to-package associate to Web Server</a>

The Web Browser can send a new association between a Filter and a Package to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new Filter/Package association insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new Filter/Package association insertion process and if a database entry already exists, it will respond with a 'Filter/Package association already exists' message in the response body and a 409 status code.

### <a name="wl-15">[WL-15](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-15) Web Server sends filter-to-package association to External Resolver</a>

The Web Server can send a new association between a Filter and a Package to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if a database entry already exists, it will respond with a 'Filter/Package association already exists' message in the response body and a 409 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if the Filter does not exist, it will respond with a 'Filter label does not exist' message in the response body and a 404 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if the Package does not exist, it will respond with a Package ID does not exist' message in the response body and a 404 status code.

### <a name="wl-16">[WL-16](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-16) External Resolver persists filter-to-package association to External Resolver Database</a>

The External Resolver can persist a new Filter/Package association to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Filter/Package association already exists, it will respond with a 'Record exists' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Filter does not exist exist, it will respond with a 'Filter does not exist' error message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Package does not exist exist, it will respond with a 'Package does not exist' error message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

## <a name="sota-3">[SOTA-3](https://advancedtelematic.atlassian.net/browse/SOTA-3) Initiate software update</a>

### <a name="wl-17">[WL-17](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-17) Web Browser sends Queue Package Require to Web Server</a>

The Web Browser can send a Queue Package Request [Package ID, Priority, Date/Time Interval] to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if a request is processed without errors, it will respond with a unique Install Request ID in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if the given package ID is not found, it will respond with a 'Package ID does not exist' message in the response body and a 404 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="wl-18">[WL-18](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-18) Web Server sends Queue Package Request to Core</a>

The Web Server can send a Queue Package Request [Package ID, Priority, Date/Time Interval] to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, Core can start a Queue Package Request process and if a request is processed without errors, it will respond with a unique Install Request ID in the response body and a 200 status code.
   * Upon the Web Server's request, Core can start a Queue Package Request process and if the given package ID is not found, it will respond with a 'Package ID does not exist' message in the response body and a 404 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="wl-19">[WL-19](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-19) Core looks-up Package ID in Core Database</a>

SOTA Core can perform a lookup operation for a Package ID to the SOTAServer database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given Package ID and if an entry is found, it will respond with the Package's data.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given Package ID and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-20">[WL-20](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-20) Core sends Resolve VIN Request to External Resolver</a>

SOTA Core can send a Resolve VIN request to the External Resolver using JSON over HTTP on port 80.

   * Upon the SOTA Core's request, the External Resolver can resolve the dependencies for all VINs involved and if the request is processed without errors, it will respond with the subset of all VINs that passed all filters in the response body and a 200 status code.
   * Upon SOTA Core's request, the External Resolver can resolve the dependencies for all VINs involved and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="wl-21">[WL-21](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-21) External Resolver looks-up Package ID filters in External Resolver Database</a>

The External Resolver can perform a lookup operation for all filters associated with a Package ID to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation for all filters associated with the given Package ID and if one or more entries are found, it will respond with the Filters' data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation for all filters associated with the given Package ID and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-22">[WL-22](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-22) External Resolver looks-up VIN in External Resolver Database</a>

The External Resolver can perform a lookup operation for a VIN to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given VIN and if an entry is found, it will respond with the VIN's data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given VIN and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-23">[WL-23](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-23) External Resolver looks-up Package Dependencies in External Resolver Database</a>

The External Resolver can perform a lookup operation for all the package dependencies of a VIN to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given VIN and if an entry is found, it will respond with all the software dependencies for the given VIN data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given VIN and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-24">[WL-24](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-24) Core sends Software Update Metadata for VIN to RVI Node</a>

Core can send a software update [main Package ID, dependent Package IDs to install, date/time interval, priority, creation date/timestamp] for each VIN to the RVI Node using JSON over HTTP on port 80.

   * Upon Core's request, the RVI Node can schedule the installation of the software packages listed for every given VIN if the task is scheduled without errors, it will respond with the subset of all VINs that passed all filters in the response body and a 200 status code.
   * Upon Core's request, the RVI Node can schedule the installation of the software packages listed for every given VIN and if any errors occur, it will respond with a 'Task scheduling' message in the response body and a 412 status code.


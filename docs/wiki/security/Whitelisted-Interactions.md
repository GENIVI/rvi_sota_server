## <a name='TOC'>Table of Contents</a>

1. [SOTA-1](#sota-1) Add VIN and Package ([JIRA](https://advancedtelematic.atlassian.net/browse/SOTA-1))
    - [WL-1](#wl-1) Web Server sends index.html page to Web Browser
    - [WL-2](#wl-2) Web Server sends Javascript scripts linked in index.html to Web Browser
    - [WL-3](#wl-3) Web Server sends CSS stylesheets linked in index.html to Web Browser
    - [WL-4](#wl-4) Web Browser sends new VIN to Web Server
    - [WL-5](#wl-5) Web Server sends new package data to SOTA Core
    - [WL-6](#wl-6) Web Server sends new VIN to SOTA Core
    - [WL-7](#wl-7) Web Server sends new package data to External Resolver
    - [WL-8](#wl-8) Web Server sends new VIN to External Resolver
    - [WL-9](#wl-9) Web Server sends new VIN to External Resolver
    - [WL-10](#wl-10) External Resolver persists new VIN to External Resolver Database
    - [WL-11](#wl-11) External Resolver persists new package data to External Resolver Database
    - [WL-12](#wl-12) SOTA Core persists new VIN to SOTA Core Database
    - [WL-13](#wl-13) SOTA Core persists new package data to SOTA Core Database
1. [SOTA-2](#sota-2) Add basic filter with package dependency ([JIRA](https://advancedtelematic.atlassian.net/browse/SOTA-2))
    - [WL-14](#wl-14) Web Browser sends new filter data to Web Server
    - [WL-15](#wl-15) Web Server sends new filter data to External Resolver
    - [WL-16](#wl-16) External Resolver persists new filter data to External Resolver Database
    - [WL-17](#wl-17) Web Browser sends filter-to-package association to Web Server
    - [WL-18](#wl-18) Web Server sends filter-to-package association to External Resolver
    - [WL-19](#wl-19) External Resolver persists filter-to-package assocation to External Resolver Database
1. [SOTA-3](#sota-3) Initiate software update ([JIRA](https://advancedtelematic.atlassian.net/browse/SOTA-3))
    - [WL-20](#wl-20) Web Browser sends Queue Package Request to Web Server
    - [WL-21](#wl-21) Web Server sends Queue Package Request to SOTA Core
    - [WL-22](#wl-22) SOTA Core looks-up Package ID in SOTA Core Database
    - [WL-23](#wl-23) SOTA Core sends Resolve VIN Request to External Resolver
    - [WL-24](#wl-24) External Resolver looks-up Package ID filters in External Resolver Database
    - [WL-25](#wl-25) External Resolver looks-up VIN in External Resolver Database
    - [WL-26](#wl-26) External Resolver looks-up Package Dependencies in External Resolver Database
    - [WL-27](#wl-27) SOTA Core sends Software Update Metadata for VIN to RVI Node
1. [SOTA-4](#sota-4) Accept and receive software updates ([JIRA](https://advancedtelematic.atlassian.net/browse/SOTA-4))
    - [WL-28](#wl-28) SOTA Core sends "Software Update Available" notification to RVI Node Server
    - [WL-29](#wl-29) RVI Node Server sends "Software Update Available" notification to RVI Node Client
    - [WL-30](#wl-30) RVI Node Client sends "Software Update Available" notification to SOTA Client
    - [WL-31](#wl-31) SOTA Client sends "Software Update Available" notification to Software Loading Manager
1. [SOTA-5](#sota-5) Accept and receive software updates ([JIRA](https://advancedtelematic.atlassian.net/browse/SOTA-5))
    - [WL-32](#wl-32) Software Loading Manager sends "Initiate Software Download" notification to SOTA Client
    - [WL-33](#wl-33) SOTA Client sends "Initiate Software Download" notification to RVI Node Client
    - [WL-34](#wl-34) RVI Node Client sends "Initiate Software Download" notification to RVI Node Server
    - [WL-35](#wl-35) RVI Node Server sends "Initiate Software Download" notification to SOTA Core
    - [WL-36](#wl-36) SOTA Core sends "Start Download" notification to RVI Node Server
    - [WL-37](#wl-37) RVI Node Server sends "Start Download" notification to RVI Node Client
    - [WL-38](#wl-38) RVI Node Client sends "Start Download" notification to SOTA Client
    - [WL-39](#wl-39) SOTA Client sends "Start Download" notification to Software Loading Manager
    - [WL-40](#wl-40) SOTA Core sends lowest numbered data block to RVI Node Server
    - [WL-41](#wl-41) RVI Node Server sends lowest numbered data block to RVI Node Client
    - [WL-42](#wl-42) RVI Node Client sends lowest numbered data block to SOTA Client
    - [WL-43](#wl-43) SOTA Client sends lowest numbered data block to Software Loading Manager
    - [WL-44](#wl-44) SOTA Core sends "Finalise Download" notification to RVI Node Server
    - [WL-45](#wl-45) RVI Node Server sends "Finalise Download" notification to RVI Node Client
    - [WL-46](#wl-46) RVI Node Client sends "Finalise Download" notification to SOTA Client
    - [WL-47](#wl-47) Software Loading Manager sends Install Report to SOTA Client
    - [WL-48](#wl-48) SOTA Client sends Install Report to RVI Node Client
    - [WL-49](#wl-49) RVI Node Client sends Install Report to RVI Node Server
    - [WL-50](#wl-50) RVI Node Server sends Install Report to SOTA Core


## <a name="sota-1">[SOTA-1](https://advancedtelematic.atlassian.net/browse/SOTA-1) Add VIN and Package</a>

### <a name="wl-1">[WL-1](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-1) Web Server sends index.html page to Web Browser</a>

The Web Server can send the index.html file to the Web Browser, over HTTP on port 80.

   * Upon the Web Browser's request for /index.html, the Web Server will respond with the index.html HTML file and a 200 status code.
   * Upon the Web Browser's request for another HTML file, the Web Server will respond with a response with 409 status code.

### <a name="wl-2">[WL-2](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-2) Web Server sends Javascript scripts linked in index.html to Web Browser</a>

The Web Server can send the Javascript scripts linked in index.html to the Web Browser over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server will respond with the Javascript files linked in and a 200 status code.
   * Upon the Web Browser's request for an unknown or not approved Javascript script, the Web Server will respond with a response with 409 status code.

### <a name="wl-3">[WL-3](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-3) Web Server sends CSS Stylesheet files linked in index.html to Web Browser</a>

The Web Server can send the CSS Stylesheet files linked in index.html to the Web Browser over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server will respond with the CSS Stylesheet files linked in and a 200 status code.
   * Upon the Web Browser's request for an unknown or not approved CSS Stylesheet file, the Web Server will respond with a response with 409 status code.

### <a name="wl-4">[WL-4](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-4) Web Browser sends new VIN to Web Server</a>

The Web Browser can send new VIN data to the Web Server that conform to the VIN standard (17 digits long, only capital letters and numbers), using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserterd' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN alread exists' message in the response body and a 409 status code.

### <a name="wl-5">[WL-5](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-5) Web Server sends new package data to SOTA Core</a>

The Web Server can send the new package data to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a new Package insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a new Package insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="wl-6">[WL-6](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-6) Web Server sends new VIN to SOTA Core</a>

The Web Server can send the VINs data to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="wl-7">[WL-7](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-7) Web Server sends new package data to External Resolver</a>

The Web Server can send new Package data to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Package insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Package insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="wl-8">[WL-8](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-8) Web Server sends new vins to External Resolver</a>

The Web Server can send new VINs to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.


### <a name="wl-9">[WL-9](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-9) Web Server sends new VIN to External Resolver</a>

The Web Server can send new VINs to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.


### <a name="wl-10">[WL-10](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-10) External Resolver persists new VIN to External Resolver Database</a>

The External Resolver can persist new VIN data to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if the VIN already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

## <a name="sota-2">[SOTA-2](https://advancedtelematic.atlassian.net/browse/SOTA-2) Add basic filter with package dependency</a>

### <a name="wl-11">[WL-11](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-11) External Resolver persists new package data to External Resolver Database</a>

The External Resolver can persist new Package data to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if the Package already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-12">[WL-12](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-12) Core persists new VIN to SOTA Core Database</a>

SOTA Core can persist new VIN data to the SOTA Core Database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if a new database entry is created, it will respond with a 'Success' message.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if the VIN already exists, it will respond with a 'Record exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-13">[WL-13](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-13) SOTA Core persists new package data to SOTA Core Database</a>

SOTA Core can persist new package data to the SOTA Core Database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if a new database entry is created, it will respond with a 'Success' message.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if the Package already exists, it will respond with a 'Record exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

## <a name="sota-2">[SOTA-2](https://advancedtelematic.atlassian.net/browse/SOTA-14) Add basic filter with package dependency</a>

### <a name="wl-14">[WL-14](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-14) Web Browser sends new filter data to Web Server</a>

The Web Browser can send a new Filter's data to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new Filter insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new Filter insertion process and if a database entry already exists, it will respond with a 'Filter already exists' message in the response body and a 409 status code.

### <a name="wl-15">[WL-15](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-15) Web Server sends new filter data to External Resolver</a>

The Web Server can send a new Filter's data to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if a database entry already exists, it will respond with a 'Filter already exists' message in the response body and a 409 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if the Filter expression fails validation, it will respond with a 'Filter failed validation' message in the response body and a 406 status code.

### <a name="wl-16">[WL-16](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-16) External Resolver persists new filter data to External Resolver Database</a>

The External Resolver can persist a new Filter to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter data and if the Filter already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-17">[WL-17](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-17) Web Browser sends filter-to-package associate to Web Server</a>

The Web Browser can send a new association between a Filter and a Package to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new Filter/Package association insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new Filter/Package association insertion process and if a database entry already exists, it will respond with a 'Filter/Package association already exists' message in the response body and a 409 status code.

### <a name="wl-18">[WL-18](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-18) Web Server sends filter-to-package association to External Resolver</a>

The Web Server can send a new association between a Filter and a Package to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if a database entry already exists, it will respond with a 'Filter/Package association already exists' message in the response body and a 409 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if the Filter does not exist, it will respond with a 'Filter label does not exist' message in the response body and a 404 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if the Package does not exist, it will respond with a Package ID does not exist' message in the response body and a 404 status code.

### <a name="wl-19">[WL-19](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-19) External Resolver persists filter-to-package association to External Resolver Database</a>

The External Resolver can persist a new Filter/Package association to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Filter/Package association already exists, it will respond with a 'Record exists' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Filter does not exist exist, it will respond with a 'Filter does not exist' error message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Package does not exist exist, it will respond with a 'Package does not exist' error message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

## <a name="sota-3">[SOTA-3](https://advancedtelematic.atlassian.net/browse/SOTA-3) Initiate software update</a>

### <a name="wl-20">[WL-20](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-20) Web Browser sends Queue Package Require to Web Server</a>

The Web Browser can send a Queue Package Request [Package ID, Priority, Date/Time Interval] to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if a request is processed without errors, it will respond with a unique Install Request ID in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if the given package ID is not found, it will respond with a 'Package ID does not exist' message in the response body and a 404 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="wl-21">[WL-21](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-21) Web Server sends Queue Package Request to Core</a>

The Web Server can send a Queue Package Request [Package ID, Priority, Date/Time Interval] to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, Core can start a Queue Package Request process and if a request is processed without errors, it will respond with a unique Install Request ID in the response body and a 200 status code.
   * Upon the Web Server's request, Core can start a Queue Package Request process and if the given package ID is not found, it will respond with a 'Package ID does not exist' message in the response body and a 404 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="wl-22">[WL-22](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-22) Core looks-up Package ID in Core Database</a>

SOTA Core can perform a lookup operation for a Package ID to the SOTAServer database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given Package ID and if an entry is found, it will respond with the Package's data.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given Package ID and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-23">[WL-23](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-23) Core sends Resolve VIN Request to External Resolver</a>

SOTA Core can send a Resolve VIN request to the External Resolver using JSON over HTTP on port 80.

   * Upon the SOTA Core's request, the External Resolver can resolve the dependencies for all VINs involved and if the request is processed without errors, it will respond with the subset of all VINs that passed all filters in the response body and a 200 status code.
   * Upon SOTA Core's request, the External Resolver can resolve the dependencies for all VINs involved and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="wl-24">[WL-24](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-24) External Resolver looks-up Package ID filters in External Resolver Database</a>

The External Resolver can perform a lookup operation for all filters associated with a Package ID to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation for all filters associated with the given Package ID and if one or more entries are found, it will respond with the Filters' data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation for all filters associated with the given Package ID and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-25">[WL-25](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-25) External Resolver looks-up VIN in External Resolver Database</a>

The External Resolver can perform a lookup operation for a VIN to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given VIN and if an entry is found, it will respond with the VIN's data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given VIN and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-26">[WL-26](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-26) External Resolver looks-up Package Dependencies in External Resolver Database</a>

The External Resolver can perform a lookup operation for all the package dependencies of a VIN to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given VIN and if an entry is found, it will respond with all the software dependencies for the given VIN data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given VIN and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="wl-27">[WL-27](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-27) Core sends Software Update Metadata for VIN to RVI Node</a>

Core can send a software update [main Package ID, dependent Package IDs to install, date/time interval, priority, creation date/timestamp] for each VIN to the RVI Node using JSON over HTTP on port 80.

   * Upon Core's request, the RVI Node can schedule the installation of the software packages listed for every given VIN if the task is scheduled without errors, it will respond with the subset of all VINs that passed all filters in the response body and a 200 status code.
   * Upon Core's request, the RVI Node can schedule the installation of the software packages listed for every given VIN and if any errors occur, it will respond with a 'Task scheduling' message in the response body and a 412 status code.

## <a name="sota-4">[SOTA-4](https://advancedtelematic.atlassian.net/browse/SOTA-4) Accept and receive software updates</a>

### <a name="wl-28">[WL-28](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-28) SOTA Core sends "Start Update Available" notification to RVI Node Server</a>

SOTA Core can send "Software Update Available" notifications [Package ID, Size, Download Index, Description] to RVI Node Server using JSON on port 80 over HTTP.

   * Upon SOTA Core's request, the RVI Node Server can start the software update process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon SOTA Core's request, the RVI Node Server can start the software update process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server X times to resume the update.

### <a name="wl-29">[WL-29](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-29) RVI Node Server sends "Software Update Available" notification to RVI Node Client</a>

RVI Node Server can send "Software Update Available" notifications [Package ID, Size, Download Index, Description] to RVI Node Client.

### <a name="wl-30">[WL-30](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-30) RVI Node Client sends "Software Update Available" notification to SOTA Client</a>

RVI Node Client can send "Software Update Available" notifications [Package ID, Size, Download Index, Description] to SOTA Client the  using JSON on port 80 over HTTP.

   * Upon the RVI Node Clients's request, the SOTA Client can start the software update process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the RVI Node Client's request, the SOTA Client can start the software update process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server X times to resume the update.

### <a name="wl-31">[WL-31](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-31) SOTA Client sends "Software Update Available" notification to Software Loading Manager</a>

SOTA Client can send "Software Update Available" notifications [Package ID, Size, Download Index, Description] to Software Loading Manager using JSON on port 80 over HTTP.

   * Upon SOTA Clients's request, Software Loading Manager can start the software update process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the SOTA Clients's request, Software Loading Manager can start the software update process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server X times to resume the update.

## <a name="sota-5">[SOTA-5](https://advancedtelematic.atlassian.net/browse/SOTA-5) Accept and receive software updates</a>

### <a name="wl-32">[WL-32](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-32) Software Loading Manager sends "Initiate Software Download" notification to SOTA Client</a>

Software Loading Manager can send a "Initiate Software Download" [Download Index] notification from to SOTA Client using JSON on port 80 over HTTP.

   * Upon the Software Loading Manager's request, SOTA Client can start the update download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the Software Loading Manager's request, SOTA Client can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the Software Loading Manager's "Cancel Software Download" request, SOTA Client can interrupt the update download process.

### <a name="wl-33">[WL-33](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-33) SOTA Client sends "Initiate Software Download" notification to RVI Node Client</a>

SOTA Client can accept a "Initiate Software Download" [Download Index] notification to RVI Node Client using JSON on port 80 over HTTP.

   * Upon the SOTA Client's request, RVI Node Client can start the update download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the SOTA Client's request, RVI Node Client can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the SOTA Client's "Cancel Software Download" request, RVI Node Client can interrupt the update download process.

### <a name="wl-34">[WL-34](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-34) RVI Node Client sends "Initiate Software Download" notification to RVI Node Server</a>

### <a name="wl-35">[WL-35](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-35) RVI Node Server sends "Initiate Software Download" notification to SOTA Core</a>

RVI Node Server can send a "Initiate Software Download" [Download Index] notification to SOTA Core using JSON on port 80 over HTTP.

   * Upon the RVI Node Server's request, SOTA Core can start the update download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the RVI Node Server's request, SOTA Core can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the RVI Node Server's "Cancel Software Download" request, SOTA Core can interrupt the update download process.

### <a name="wl-36">[WL-36](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-36) SOTA Core sends "Start Download" notification to RVI Node Server</a>

SOTA Core can send a "Start Download" notification to RVI Node Server using JSON on port 80 over HTTP.

   * Upon SOTA Core's request, RVI Node Server can start the download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the RVI Node Server's request, SOTA Core can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the RVI Node Server's "Cancel Software Download" request, SOTA Core can interrupt the update download process.

### <a name="wl-37">[WL-37](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-37) RVI Node Server sends "Start Download" notification to RVI Node Client</a>

RVI Node Server can send a "Start Download" notification to RVI Node Client.

### <a name="wl-38">[WL-38](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-38) RVI Node Client sends "Start Download" notification to SOTA Client</a>

RVI Node Client can send a "Start Download" notification to SOTA Client using JSON on port 80 over HTTP.

   * Upon RVI Node Client's request, SOTA Client can start the download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the RVI Node Client's request, SOTA Client can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the RVI Node Client's "Cancel Software Download" request, SOTA Client can interrupt the update download process.

### <a name="wl-39">[WL-39](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-39) SOTA Client sends "Start Download" notification to Software Loading Manager</a>

SOTA Client can send a "Start Download" notification to Software Loading Manager using JSON on port 80 over HTTP.

   * Upon SOTA Client's request, Software Loading Manager can start the download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the SOTA Client's request, Software Loading Manager can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the SOTA Client's "Cancel Software Download" request, Software Loading Manager can interrupt the update download process.

### <a name="wl-40">[WL-40](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-40) SOTA Core sends lowest numbered data block to RVI Node Server</a>

SOTA Core can send the lowest numbered data block to RVI Node Server using JSON on port 80 over HTTP.

   * Upon SOTA Core's request, RVI Node Server can accept the lowest numbered data block and if the data block is received without errors, it will acknowledge of successful data block receipt in the response body and a 200 status code.
   * Upon SOTA Core's request, RVI Node Server can accept the lowest numbered data block and if the data block has been received before the data block will be discarded and the next data block will be requested.
   * Upon SOTA Core's request, RVI Node Server can accept the lowest numbered data block and if the data block is interrupted due to network loss, it will attempt to reconnect X times and transmit again the data block.

### <a name="wl-41">[WL-41](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-41) RVI Node Server sends lowest numbered data block to RVI Node Client</a>

RVI Node Server can send the lowest numbered data block to RVI Node Client.

### <a name="wl-42">[WL-42](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-42) RVI Node Client sends lowest numbered data block to SOTA Client</a>

RVI Node Client can send the lowest numbered data block to SOTA Client using JSON on port 80 over HTTP.

   * Upon RVI Node Client's request, SOTA Client can accept the lowest numbered data block and if the data block is received without errors, it will acknowledge of successful data block receipt in the response body and a 200 status code.
   * Upon RVI Node Client's request, SOTA Client can accept the lowest numbered data block and if the data block has been received before the data block will be discarded and the next data block will be requested.

### <a name="wl-43">[WL-43](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-43) SOTA Client sends lowest numbered data block to Software Loading Manager</a>

SOTA Client can send the lowest numbered data block to Software Loading Manager using JSON on port 80 over HTTP.

   * Upon SOTA Client's request, Software Loading Manager can accept the lowest numbered data block and if the data block is received without errors, it will acknowledge of successful data block receipt in the response body and a 200 status code.
   * Upon SOTA Client's request, Software Loading Manager can accept the lowest numbered data block and if the data block has been received before the data block will be discarded and the next data block will be requested.

### <a name="wl-44">[WL-44](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-44) SOTA Core sends "Finalise Download" notification to RVI Node Server</a>

SOTA Core can send a "Finalize Download" notification to RVI Node Server using JSON on port 80 over HTTP.

   * Upon SOTA Core's request, RVI Node Server can confirm the completion of download process and if the download is finished without errors, it will respond with 'Download of *Package ID* complete' in the response body and a 200 status code.
   * Upon SOTA Core's request, RVI Node Server can confirm the completion of download process and if data blocks are missing, it will respond with 'Incomplete Download' in the response body and a 400 status code.
   * Upon the RVI Node Server's request, SOTA Core can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.

### <a name="wl-45">[WL-45](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-45) RVI Node Server sends "Finalise Download" notification to RVI Node Client</a>

RVI Node Server can send a "Finalize Download" notification to RVI Node Client.

### <a name="wl-46">[WL-46](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-46) RVI Node Client sends "Finalise Download" notification to SOTA Client</a>

RVI Node Client can send a "Finalize Download" notification to SOTA Client using JSON on port 80 over HTTP.

   * Upon RVI Node Client's request, SOTA Client can confirm the completion of download process and if the download is finished without errors, it will respond with 'Download of *Package ID* complete' in the response body and a 200 status code.
   * Upon RVI Node Client's request, SOTA Client can confirm the completion of download process and if data blocks are missing, it will respond with 'Incomplete Download' in the response body and a 400 status code.
   * Upon the RVI Node Client's request, SOTA Client can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.

### <a name="wl-47">[WL-47](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-47) Software Loading Manager sends Install Report to SOTA Client</a>

Software Loading Manager can send an Install Report to SOTA Client the using JSON on port 80 over HTTP.

   * Upon the Software Loading Manager's request, SOTA Client can accept the Install Report and if the installation was finished without errors, it will respond with '*Package ID* success' in the response body and a 200 status code.
   * Upon the Software Loading Manager's request, SOTA Client can accept the Install Report and if the VIN is already marked as complete, it will respond with '*Package ID* failed' in the response body and a 409 status code.
   * Upon the Software Loading Manager's request, SOTA Client can accept the Install Report and if the VIN is already marked as failed, it will respond with '*Package ID* failed' in the response body and a 409 status code.

### <a name="wl-48">[WL-48](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-48) SOTA Client sends Install Report to RVI Node Client</a>

SOTA Client can send an Install Report to RVI Node Client the  using JSON on port 80 over HTTP.

   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the installation was finished without errors, it will respond with '*Package ID* success' in the response body and a 200 status code.
   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the VIN is already marked as complete, it will respond with '*Package ID* failed' in the response body and a 409 status code.
   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the VIN is already marked as failed, it will respond with '*Package ID* failed' in the response body and a 409 status code.

### <a name="wl-49">[WL-49](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-49) SOTA Client sends Install Report to RVI Node Client</a>

RVI Node Client can send an Install Report to the RVI Node Server.

### <a name="wl-50">[WL-50](https://github.com/advancedtelematic/sota-server/wiki/permitted-interactions#wl-50) RVI Node Server sends Install Report to SOTA Core</a>

RVI Node Server can send an Install Report to the SOTA Core using JSON on port 80 over HTTP.

   * Upon the RVI Node Server's request, SOTA Core can accept the Install Report and if the installation was finished without errors, it will respond with '*Package ID* success' in the response body and a 200 status code.
   * Upon the RVI Node Server's request, SOTA Core can accept the Install Report and if the VIN is already marked as complete, it will respond with '*Package ID* failed' in the response body and a 409 status code.
   * Upon the RVI Node Server's request, SOTA Core can accept the Install Report and if the VIN is already marked as failed, it will respond with '*Package ID* failed' in the response body and a 409 status code.

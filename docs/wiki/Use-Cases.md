## <a name="TOC">Table of Contents</a>

1. [SOTA Server Configuration](#server-config)
    - [VIN-ADD](#VIN-ADD) Add a VIN to the system
    - [VIN-DEL](#VIN-DEL) Delete an existing VIN
    - [VIN-SEARCH](#VIN-SEARCH) Search for VINs
    - [VIN-SET-DATA-PLAN](#VIN-SET-DATA-PLAN) Associate Data Plan with VIN
    - [VIN-GET-UPDATE-HISTORY](#VIN-GET-UPDATE-HISTORY) Get installation history for VIN
    - [PACKAGE-ADD](#PACKAGE-ADD) Register a new Package with SOTA Server and External Resolver
    - [PACKAGE-SEARCH](#PACKAGE-SEARCH) Search for a Package
    - [PLAN-ADD](#PLAN-ADD) Register a new Data Plan
    - [PLAN-ADD-BILLING-CYCLE](#PLAN-ADD-BILLING-CYCLE) Associate a Billing Cycle with a Data Plan
    - [PLAN-SEARCH](#PLAN-SEARCH) Find existing Data Plan
    - [PLAN-GET-BILLING-CYCLES](#PLAN-GET-BILLING-CYCLES) Get Billing Cycles for a Data Plan
    - [PLAN-ADD-TRAFFIC](#PLAN-ADD-TRAFFIC) Add Traffic Information to active Billing Cycle for Data Plan associated with VIN

1. [External Resolver Configuration](#resolver-config)
    - [FILTER-ADD](#FILTER-ADD) Add a new Filter
    - [FILTER-SEARCH](#FILTER-SEARCH) Search for a Filter
    - [FILTER-VALIDATE](#FILTER-VALIDATE) Test validity of a Filter Expression
    - [FILTER-DELETE](#FILTER-DELETE) Delete a Filter
    - [FILTER-PACKAGE-ADD](#FILTER-PACKAGE-ADD) Associate a Package with a Filter
    - [FILTER-PACKAGE-DELETE](#FILTER-PACKAGE-DELETE) Remove a Package from a Filter
    - [FILTER-SEARCH-PACKAGE-BY-FILTER](#FILTER-SEARCH-PACKAGE-BY-FILTER) Get list of Packages by Filter
    - [FILTER-SEARCH-FILTER-BY-PACKAGE](#FILTER-SEARCH-FILTER-BY-PACKAGE) Get list of Filters by Package
    - [COMP-ADD](#COMP-ADD) Add a Component
    - [VIN-ADD-COMP](#VIN-ADD-COMP) Associate a Component with a VIN
    - [VIN-ADD-PACKAGE](#VIN-ADD-PACKAGE) Associate a Package with a VIN
    - [VIN-DELETE-PACKAGE](#VIN-DELETE-PACKAGE) Remove a Package from a VIN
    - [COMP-SEARCH](#COMP-SEARCH) Search for a Component
    - [VIN-SEARCH-BY-COMP](#VIN-SEARCH-BY-COMP) Find VINs by Component
    - [COMP-SEARCH-BY-VIN](#COMP-SEARCH-BY-VIN) Find Components associated with a VIN
    - [PACKAGE-ADD-DEPENDENCY](#PACKAGE-ADD-DEPENDENCY) Create Dependency between Packages
    - [PACKAGE-GET-DEPENDENCIES](#PACKAGE-GET-DEPENDENCIES) Get Dependencies for a Package
    - [PACKAGE-DEL-DEPENDENCY](#PACKAGE-DEL-DEPENDENCY) Remove Dependency between Packages
    - [PACKAGE-SEARCH-BY-VIN](#PACKAGE-SEARCH-BY-VIN) Find VINs by Package

1. [Installation Queue Management (SOTA Server)](#queue-management)
    - [QUEUE-REQUEST-ADD](#QUEUE-REQUEST-ADD) Queue a package for installation on VINs matching Filter
    - [QUEUE-REQUEST-CANCEL](#QUEUE-REQUEST-CANCEL) Cancel a previous Installation Request
    - [QUEUE-GET-STATUS](#QUEUE-GET-STATUS) Get status for an Installation Request
    - [QUEUE-GET-COMPLETED-VINS](#QUEUE-GET-COMPLETED-VINS) List VINs for which installation is complete for Installation Request
    - [QUEUE-GET-IN-FLIGHT-VINS](#QUEUE-GET-IN-FLIGHT-VINS) List VINs for which installation is ongoing for Installation Request
    - [QUEUE-GET-FAILED-VINS](#QUEUE-GET-FAILED-VINS) List VINs for which installation failed for Installation Request
    - [QUEUE-GET-NEXT-SOFTWARE-UPDATE](#QUEUE-GET-NEXT-SOFTWARE-UPDATE) Get current or next queued Installation Request for VIN
    - [QUEUE-INITIATE-XMIT](#QUEUE-INITIATE-XMIT) Trigger [DEV-WAKEUP](#DEV-WAKEUP) for sleeping VINs with queued Installation Requests
    - [QUEUE-PURGE](#QUEUE-PURGE) Purge failing or expired Installation Requests from Queue
    - [QUEUE-REQUEST-GET-ALL-PACKAGES](#QUEUE-REQUEST-GET-ALL-PACKAGES) Get list of Packages queued for a VIN

1. [Device Interaction](#device-interaction)
    - [DEV-WAKEUP](#DEV-WAKEUP) Send Wake-up event to VIN, triggering [DEV-CONNECT](#DEV-CONNECT)
    - [DEV-CONNECT](#DEV-CONNECT) Device connects to SOTA Server to process or resume Installation Request
    - [DEV-DISCONNECT](#DEV-DISCONNECT) SOTA Server instructs Device to disconnect
    - [UPDATE-NOTIFICATION](#UPDATE-NOTIFICATION) Send any queued Installation Requests ([QUEUE-GET-NEXT-SOFTWARE-UPDATE](#QUEUE-GET-NEXT-SOFTWARE-UPDATE)) to Device
    - [TRIGGER-TRANSFER-START](#TRIGGER-TRANSFER-START) Software Loading Manager requests download from SOTA Server via SOTA Client
    - [TRANSFER-START](#TRANSFER-START) SOTA Server sends Download metadata to Device
    - [TRANSFER-CHUNK](#TRANSFER-CHUNK) SOTA Server sends next Download chunk to Device
    - [TRANSFER-COMPLETE](#TRANSFER-COMPLETE) SOTA Server sends Finalize Download to Device
    - [INSTALL-SOFTWARE-UPDATE](#INSTALL-SOFTWARE-UPDATE) Device installs all received Packages 
    - [INSTALL-REPORT](#INSTALL-REPORT) Device reports Installation Result to SOTA Server
    - [GET-ALL-PACKAGES](#GET-ALL-PACKAGES) Get list of Packages installed on a VIN (from the Device)

## <a name="server-config">SOTA Server Configuration</a>

### <a name="VIN-ADD">[VIN-ADD](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-ADD) Add a VIN to the system</a>

   - Actors

       * Web Server
       * SOTA Server
       * Resolver

   - Preconditions

       * VIN does not already exist

   - Steps

       * E1 - An ADD-VIN command is sent from Web Server to SOTA Server
       * E2 - VIN is added to the SOTA Server Database
       * E3 - A success code is sent back to Web Server
       * E4 - An ADD-VIN command is snet to External Resolver from Web Server
       * E5 - VIN is added to External Resolver database
       * E6 - A success code is sent back to Web Server

   - Postconditions

       * VIN is part of the system

   - Exceptions

       * X1 - VIN already exists. Triggered by E2, E5

### <a name="VIN-DEL">[VIN-DEL](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-DEL) Delete an existing VIN</a>

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * VIN has been installed by [VIN-ADD](#VIN-ADD)

   - Steps

       * E1 - A delete VIN command is sent from Web Server to SOTA Server
       * E2 - All references to Packages being installed on given VIN are removed from SOTA Server Database
       * E3 - The VIN is removed from the SOTA Server Database
       * E4 - Any Data Plan references to the VIN are removed from the SOTA Server Database
       * E5 - A success code is sent back to the Web Server
       * E6 - A DELETE-VIN command is sent from Web Server to Resolver
       * E7 - All references to Components being installed on the given VIN are removed from the Resolver Database
       * E8 - All references to Packages being installed on the given VIN are removed from the Resolver Database
       * E9 - The VIN is removed from the Resolver Database
       * E10 - A success code is sent back to the Web Server

   - Exceptions

       * X1 - VIN does not exist. Triggered by E6

### <a name="VIN-SEARCH">[VIN-SEARCH](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-SEARCH) Search for VINs</a>

Searches and retrieves one or more VINs with their Packages and Components

   - Actors

       * Web Server
       * SOTA Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - A SEARCH-VIN command is sent from Web Server to SOTA Server. VIN is searched for using POSIX-style regular expressions.
       * E2 - The SOTA Server Database is searched for all VINs matching the given expression.
       * E3 - For each retrieved VIN, the part numbers of all installed Components are retrieved by the Web Server from the Resolver(!)
       * E4 - For each retrieved VIN, the IDs of all Installed Packages are retrieved
       * E5 - All matching VINs, with their retrieved Components and Installed Packages are returned by SOTA Server to Web Server

   - Exceptions

       * None

### <a name="VIN-SET-DATA-PLAN">[VIN-SET-DATA-PLAN](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-SET-DATA-PLAN) Associate Data Plan with VIN</a>

Associates a previously created data plan with a given VIN

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * None

   - Steps

       * E1 - A SET-VIN-DATA-PLAN command is sent from Web Server to SOTA Server with VIN and Data Plan ID.
       * E2 - The VIN is retrieved from the SOTA Server Database
       * E3 - The Data Plan is retrieved from the SOTA Server Database
       * E4 - A success code is sent back to the Web Server

   - Exceptions

       * X1 - VIN does not exist. Triggered by E2
       * X2 - Data Plan does not exist. Triggered by E3

### <a name="VIN-GET-UPDATE-HISTORY">[VIN-GET-UPDATE-HISTORY](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-GET-UPDATE-HISTORY) Get installation history for VIN</a>

All install requests, failed, pending, in-flight or completed are returned.

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * None

   - Steps

       * E1 - A GET-VIN-PACKAGE-HISTORY command is sent from Web Server to SOTA Server with a VIN.
       * E2 - The provided VIN is retrieved from the SOTA Server Database
       * E3 - All updates, completed, failed, in-flight or pending targeting the provided VIN are retieved from the SOTA Server Database, together with the IDs of all Packages included in the update for each VIN
       * E4 - A success code is sent back to the Web Server, with all updates, their package IDs, their status, and the completion / failure date

   - Exceptions

       * X1 - VIN does not exist. Triggered by E2

### <a name="PACKAGE-ADD">[PACKAGE-ADD](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PACKAGE-ADD) Register a new Package with SOTA Server and External Resolver</a>

Add a software Package that can be pushed to a specific Component on a VIN

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * Software package does not already exist 

   - Steps

       * E1 - An ADD-SOFTWARE-PACKAGE command is sent from Web Server to SOTA Server, together with an ID string, a version (major.minor.patch), a description, and a vendor
       * E2 - Software Package is added to SOTA Server Database
       * E3 - An ADD-SOFTWARE-PACKAGE command is sent from Web Server to External Resolver together with an ID string.
       * E4 - Software Package is added to External Resolver database
       * E5 - A success code is sent back to Web Server

   - Exceptions

       * X1 - Software Package with same ID String and Version is already registered with SOTA Server. Triggered by E2

### <a name="PACKAGE-SEARCH">[PACKAGE-SEARCH](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PACKAGE-SEARCH) Search for a Package</a>

Searches and retrieves data for software Packages from the system

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * None

   - Steps

       * E1 - A SEARCH-PACKAGE command is sent from Web Server to SOTA Server with a regular expression formatted Package ID and version string
       * E2 - The SOTA Server Database is searched for all Packages matching the search criteria.
       * E3 - All matching Package IDs, with their version, vendor and descriptions are returned.

   - Exceptions

       * None

### <a name="PLAN-ADD">[PLAN-ADD](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PLAN-ADD) Register a new Data Plan</a>

Add a Data Plan that can later be used by VINs. Billing Cycles are added to the Data Plan by [PLAN-ADD-BILLING-CYCLE](#PLAN-ADD-BILLING-CYCLE)

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * None

   - Steps

       * E1 - An ADD-DATA-PLAN command is sent from Web Server to SOTA Server with a Data Plan ID.
       * E2 - The Data Plan is added to SOTA Server Database.
       * E3 - A success code is sent back to Web Server

   - Exceptions

       * X1 - Data Plan already exists. Triggered by E2.

### <a name="PLAN-ADD-BILLING-CYCLE">[PLAN-ADD-BILLING-CYCLE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PLAN-ADD-BILLING-CYCLE) Associate a Billing Cycle with a Data Plan</a>

Add a billing cycle to a Data Plan previously created with [PLAN-ADD](#PLAN-ADD).

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * [PLAN-ADD](#PLAN-ADD) executed to provide a Data Plan to which to add a Billing Cycle

   - Steps

       * E1 - An ADD-BILLING-CYCLE command is sent from Web Server to SOTA Server with a Data Plan ID, a Billing Cycle start date / time, and a Billing Cycle pool size.
       * E2 - The Data Plan is retrieved from the SOTA Server Database
       * E3 - A Billing Cycle is created in the SOTA Server Database with zero bytes transmitted, the given start data, and the pool size.
       * E4 - A success code is returned by SOTA Server to Web Server

   - Exceptions

       * X1 - Data Plan does not exist. Triggered by E2.

### <a name="PLAN-SEARCH">[PLAN-SEARCH](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PLAN-SEARCH) Find existing Data Plan</a>

Search for a Data Plan previously added with PLAN-ADD.

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * None

   - Steps

       * E1 - A SEARCH-DATA-PLAN command is sent from Web Server to SOTA Server with a Data Plan ID regular expression
       * E2 - A success code is returned by SOTA Server to Web Server with all located Data Plan IDs

   - Exceptions

       * None

### <a name="PLAN-GET-BILLING-CYCLES">[PLAN-GET-BILLING-CYCLES](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PLAN-GET-BILLING-CYCLES) Get Billing Cycles for a Data Plan</a>

Retrieve billing cycles and their details belonging to a specific Data Plan.

   - Actors

       * Web Server
       * SOTA Server

   - Preconditions

       * None

   - Steps

       * E1 - A SEARCH-BILLING-CYCLES command is sent from Web Server to SOTA Server with a Data Plan ID, an earliest date / time, and a latest date / time.
       * E2 - The Data Plan is retrieved from SOTA Server Database.
       * E3 - All Billing Cycles belonging to the Data Plan, with a start date / time between the provided earliest and latest date / time, are retrieved from SOTA Server Database together with their pool size, and used data.
       * E4 - A success code is returned by SOTA Server to Web Server with all located Billing Cycles, their pool size and data usage.

   - Exceptions

       * X1 - Data Plan does not exist. Triggered by E2. 

### <a name="PLAN-ADD-TRAFFIC">[PLAN-ADD-TRAFFIC](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PLAN-ADD-TRAFFIC) Add Traffic Information to active Billing Cycle for Data Plan associated with VIN</a>

Add traffic information to the active Billing Cycle under the Data Plan associated with a specific VIN.

   - Actors

       * SOTA Server
       * Network Monitoring Process 

   - Preconditions

       * None

   - Steps

       * E1 - An ADD-DATA-TRAFFIC command is sent from an internal SOTA Server Network Monitoring Process to SOTA Server with a VIN and a byte count of transmitted data.
       * E2 - The VIN is retrieved from SOTA Server Database.
       * E3 - The Data Plan setup for the VIN created through the [VIN-SET-DATA-PLAN](#VIN-SET-DATA-PLAN) use case is retrieved from SOTA Server.
       * E4 - The Billing Cycle, owned by the Data Plan, that has the latest start date / time before the provided date / time stamp is retrieve from the SOTA Server Database.
       * E5 - The data usage for the given Billing Cycle is incremented by the byte count provided
       * E6 - A success code is returned to the Network Monitoring Process, together with the retrieved Data Plan ID, and the start date, pool size, and update data usage of the located Billing Cycle.

   - Exceptions

       * X1 - VIN does not exist. An error code is sent back to Web Server. Triggered by E2. 
       * A1 - No Data Plan is setup for VIN. A success code is sent back to Web Server. Triggered by E3.
       * A2 - No Billing Cycles have been added to the Data Plan. A success code is sent back to Web Server. Triggered by E4.

## <a name="resolver-config">External Resolver Configuration</a>

### <a name="FILTER-ADD">[FILTER-ADD](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#FILTER-ADD) Add a new Filter</a>

### <a name="FILTER-SEARCH">[FILTER-SEARCH](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#FILTER-SEARCH) Search for a Filter</a>

### <a name="FILTER-VALIDATE">[FILTER-VALIDATE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#FILTER-VALIDATE) Test validity of a Filter Expression</a>

### <a name="FILTER-DELETE">[FILTER-DELETE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#FILTER-DELETE) Delete a Filter</a>

### <a name="FILTER-PACKAGE-ADD">[FILTER-PACKAGE-ADD](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#FILTER-PACKAGE-ADD) Associate a Package with a Filter</a>

### <a name="FILTER-PACKAGE-DELETE">[FILTER-PACKAGE-DELETE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#FILTER-PACKAGE-DELETE) Remove a Package from a Filter</a>

### <a name="FILTER-SEARCH-PACKAGE-BY-FILTER">[FILTER-SEARCH-PACKAGE-BY-FILTER](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#FILTER-SEARCH-PACKAGE-BY-FILTER) Get list of Packages by Filter</a>

### <a name="FILTER-SEARCH-FILTER-BY-PACKAGE">[FILTER-SEARCH-FILTER-BY-PACKAGE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#FILTER-SEARCH-FILTER-BY-PACKAGE) Get list of Filters by Package</a>

### <a name="COMP-ADD">[COMP-ADD](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#COMP-ADD) Add a Component</a>

Adds a component that can subsequently be associated with one or more VINs

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - An ADD-COMPONENT command is sent from Web Server to External Resolver
       * E2 - The Component is added to External Resolver Database
       * E3 - A success code is sent back to Web Server

   - Exceptions

       * X1 - If Component exists, X1 is executed. Triggered at E1.


### <a name="VIN-ADD-COMP">[VIN-ADD-COMP](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-ADD-COMP) Associate a Component with a VIN</a>

Associates a previously configured Component with a VIN, indicating that the VIN has the given part number installed

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - An ADD-COMPONENT command is sent from Web Server to External Resolver
       * E2 - The provided VIN is retrieved from External Resolver Database
       * E3 - The provided Component is retrieved from External Resolver Database
       * E4 - The part provided Component is marked as installed on the VIN in External Resolver Database
       * E5 - A success code is sent back to Web Server

   - Exceptions

       * X1 - VIN does not exist - an error code is sent back to Web Server. Triggered at E2 if VIN does not exist.
       * X2 - Component does not exist - an error code is sent back to Web Server. Triggered at E3 if Component does not exist.

   - Postconditions

       * The association is registered in the External Resolver, and searchable with [VIN-SEARCH-BY-COMP](#VIN-SEARCH-BY-COMP) and [COMP-SEARCH-BY-VIN](#COMP-SEARCH-BY-VIN)

### <a name="VIN-ADD-PACKAGE">[VIN-ADD-PACKAGE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-ADD-PACKAGE) Associate a Package with a VIN</a>

Associates a previously provisioned software Package as being installed on a given VIN

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - An ADD-PACKAGE command is sent from Web Server to External Resolver with a Package and a VIN
       * E2 - The provided VIN is retrieved from External Resolver Database
       * E3 - The provided Package is retrieved from External Resolver Database
       * E4 - The Package is marked as installed on the VIN in External Resolver Database
       * E5 - A success code is sent back to Web Server

   - Exceptions

       * X1 - VIN does not exist - an error code is sent back to Web Server. Triggered at E2 if VIN does not exist.
       * X2 - Package does not exist - an error code is sent back to Web Server. Triggered at E3 if Package does not exist.

   - Postconditions

       * The association is registered in the External Resolver, and searchable with [PACKAGE-SEARCH-BY-VIN](#PACKAGE-SEARCH-BY-VIN)

### <a name="VIN-DELETE-PACKAGE">[VIN-DELETE-PACKAGE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-DELETE-PACKAGE) Remove a Package from a VIN</a>

Removes an existing reference for a software package as being install on a VIN

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - A DELETE-PACKAGE command is sent from Web Server to External Resolver with a Package and a VIN
       * E2 - The reference to the Package being installed on the VIN is removed
       * E3 - A success code is sent back to Web Server

   - Exceptions

       * X1 - The Package is not registered as installed on the VIN - an error code is sent back to Web Server. Triggered at E2.

   - Postconditions

       * None

### <a name="COMP-SEARCH">[COMP-SEARCH](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#COMP-SEARCH) Search for a Component</a>

Search for one or more components based on a regexp search pattern

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - A SEARCH-COMPONENT command is sent from Web Server to External Resolver with a POSIX-style regular expression for the part numbers of interest
       * E2 - The External Resolver Database is searched for all Components matching the part number regular expression
       * E3 - The part numbers of all matching Components are returned

   - Exceptions

       * None

   - Postconditions

       * None


### <a name="VIN-SEARCH-BY-COMP">[VIN-SEARCH-BY-COMP](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#VIN-SEARCH-BY-COMP) Find VINs by Component</a>

Find and return all VINs that have been associated with a specific Component

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - A SEARCH-COMPONENT command is sent from Web Server to External Resolver with specific Component ID (part number) of interest
       * E2 - The Component is retrieved from the External Resolver Database
       * E3 - All VINs associated with the Component ID (part number) are retrieved from the External Resolver Database
       * E4 - The retrieved VINs are returned by External Resolver to Web Server

   - Exceptions

       * X1 - Component does not exist - an error code is sent back to Web Server. Triggered at E2 if Component ID is not found.

   - Postconditions

       * None

### <a name="COMP-SEARCH-BY-VIN">[COMP-SEARCH-BY-VIN](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#COMP-SEARCH-BY-VIN) Find Components associated with a VIN</a>

Find and return part numbers of all Components installed on a specific VIN

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - A SEARCH-COMPONENT command is sent from Web Server to External Resolver with specific VIN (not regexp) of interest
       * E2 - The VIN is retrieved from the External Resolver Database
       * E3 - All Components associated with the VIN are retrieved from the External Resolver Database
       * E4 - The retrieved Components are returned by External Resolver to Web Server

   - Exceptions

       * X1 - VIN does not exist - an error code is sent back to Web Server. Triggered at E2 if VIN is not found.

   - Postconditions

       * None

### <a name="PACKAGE-ADD-DEPENDENCY">[PACKAGE-ADD-DEPENDENCY](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PACKAGE-ADD-DEPENDENCY) Create Dependency between Packages</a>

Specifies that a software Package needs another software Package in order to function properly when installed on a Component.

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * Both references software Packages have been added with PACKAGE_ADD

   - Steps

       * E1 - An ADD-PACKAGE-DEPENDENCY command is sent from Web Server to External Resolver with the Package ID that has a dependency and the Package ID that is depended upon.
       * E2 - The Package for the dependent Package ID is retrieved from the External Resolver Database.
       * E3 - The Package for the depended Package ID is retrieved from the External Resolver Database.
       * E4 - The unidirectional Dependency between the two Packages is stored in the External Resolver Database.
       * E5 - A success code is sent back to Web Server

   - Exceptions

       * X1 - Dependent Package ID does not exist. Triggered by E2
       * X2 - Depended Package ID does not exist. Triggered by E3

### <a name="PACKAGE-GET-DEPENDENCIES">[PACKAGE-GET-DEPENDENCIES](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PACKAGE-GET-DEPENDENCIES) Get Dependencies for a Package</a>

Retrieves the IDs of all Packages that the provided Package needs in order to operate on a Component. Recursive Dependencies are an option.

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - A GET-PACKAGE-DEPENDENCIES command is sent from Web Server to External Resolver with a software Package ID and an optional recursive resolve flag.
       * E2 - The Package is retrieved from the External Resolver Database.
       * E3 - The Package Dependencies are retrieved from the External Resolver Database.
       * E4 - If the recursive resolve flag is set, E3 is executed for each located Dependency, resulting in a complete Dependency Graph including all Packages needed to run the provided Package ID on a Component.
       * E5 - All retrieved Dependencies are returned, where each Dependency contains the Package ID of the depended-upon Package and the ID of the Package that is dependent on it.

            Dependency for A1 is { A1, { B1, B2 { C1, { D1, D2 } } } }
            Returns:
              D1 -> C1
              D2 -> C2
              C1 -> B2
              B2 -> A1
              B1 -> A1

   - Exceptions

       * X1 - Package does not exist. Triggered by E2

### <a name="PACKAGE-DEL-DEPENDENCY">[PACKAGE-DEL-DEPENDENCY](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PACKAGE-DEL-DEPENDENCY) Remove Dependency between Packages</a>

Deletes a dependency between two software packages previously added with [PACKAGE-ADD-DEPENDENCY](#PACKAGE-ADD-DEPENDENCY).

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * A Dependency has previously been set up by PACKAGE-ADD-DEPENDENCY

   - Steps

       * E1 - A DELETE-PACKAGE-DEPENDENCY command is sent from Web Server to External Resolver with the IDs of the dependent and depended-upon Packages 
       * E2 - Dependent Package is retrieved from External Resolver Database
       * E3 - Depended-upon Package is retrieved from External Resolver Database
       * E4 - External Resolver Database is searched for the matching unidircetional Dependency
       * E5 - The unidirectional dependency is deleted from the External Resolver Database
       * E6 - A success code is sent back to Web Server

   - Exceptions

       * X1 - Dependent Package does not exist. Triggered by E2
       * X2 - Depended-upon Package does not exist. Triggered by E3
       * X3 - Dependency relation could not be found. Triggered by E4

### <a name="PACKAGE-SEARCH-BY-VIN">[PACKAGE-SEARCH-BY-VIN](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#PACKAGE-SEARCH-BY-VIN) Find VINs by Package</a>

Retrieves all VINs with a specific Package installed on them

   - Actors

       * Web Server
       * External Resolver 

   - Preconditions

       * None

   - Steps

       * E1 - A SEARCH-PACKAGE-BY-VIN command is sent from Web Server to External Resolver 
       * E2 - Package is retrieved from External Resolver Database
       * E3 - All VINs with Package installed are retrieved from External Resolver Database
       * E4 - For each retrieved VIN with the Package installed, the Component in the VIN with the Package installed is retrieved
       * E6 - A success code is sent back to Web Server with the VIN-Component pair for all VINs that have the Package installed

   - Exceptions

       * X1 - Package does not exist. Triggered by E2

## <a name="queue-management">Installation Queue Management (SOTA Server)</a>

### <a name="QUEUE-REQUEST-ADD">[QUEUE-REQUEST-ADD](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-REQUEST-ADD) Queue a package for installation on VINs matching Filter</a>

### <a name="QUEUE-REQUEST-CANCEL">[QUEUE-REQUEST-CANCEL](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-REQUEST-CANCEL) Cancel a previous Installation Request</a>

### <a name="QUEUE-GET-STATUS">[QUEUE-GET-STATUS](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-GET-STATUS) Get status for an Installation Request</a>

### <a name="QUEUE-GET-COMPLETED-VINS">[QUEUE-GET-COMPLETED-VINS](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-GET-COMPLETED-VINS) List VINs for which installation is complete for Installation Request</a>

### <a name="QUEUE-GET-IN-FLIGHT-VINS">[QUEUE-GET-IN-FLIGHT-VINS](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-GET-IN-FLIGHT-VINS) List VINs for which installation is ongoing for Installation Request</a>

### <a name="QUEUE-GET-FAILED-VINS">[QUEUE-GET-FAILED-VINS](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-GET-FAILED-VINS) List VINs for which installation failed for Installation Request</a>

### <a name="QUEUE-GET-NEXT-SOFTWARE-UPDATE">[QUEUE-GET-NEXT-SOFTWARE-UPDATE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-GET-NEXT-SOFTWARE-UPDATE) Get current or next queued Installation Request for VIN</a>

### <a name="DEV-WAKEUP">[DEV-WAKEUP](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#DEV-WAKEUP) for sleeping VINs with queued Installation Requests</a>

### <a name="QUEUE-PURGE">[QUEUE-PURGE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-PURGE) Purge failing or expired Installation Requests from Queue</a>

### <a name="QUEUE-REQUEST-GET-ALL-PACKAGES">[QUEUE-REQUEST-GET-ALL-PACKAGES](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-REQUEST-GET-ALL-PACKAGES) Get list of Packages queued for a VIN</a>


## <a name="device-interaction">Device Interaction</a>

### <a name="DEV-WAKEUP">[DEV-WAKEUP](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#DEV-WAKEUP) Send Wake-up event to VIN, triggering [DEV-CONNECT](#DEV-CONNECT)</a>

### <a name="DEV-CONNECT">[DEV-CONNECT](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#DEV-CONNECT) Device connects to SOTA Server to process or resume Installation Request</a>

### <a name="DEV-DISCONNECT">[DEV-DISCONNECT](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#DEV-DISCONNECT) SOTA Server instructs Device to disconnect</a>

### <a name="QUEUE-GET-NEXT-SOFTWARE-UPDATE">[QUEUE-GET-NEXT-SOFTWARE-UPDATE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#QUEUE-GET-NEXT-SOFTWARE-UPDATE) to Device</a>

### <a name="TRIGGER-TRANSFER-START">[TRIGGER-TRANSFER-START](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#TRIGGER-TRANSFER-START) Software Loading Manager requests download from SOTA Server via SOTA Client</a>

### <a name="TRANSFER-START">[TRANSFER-START](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#TRANSFER-START) SOTA Server sends Download metadata to Device</a>

### <a name="TRANSFER-CHUNK">[TRANSFER-CHUNK](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#TRANSFER-CHUNK) SOTA Server sends next Download chunk to Device</a>

### <a name="TRANSFER-COMPLETE">[TRANSFER-COMPLETE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#TRANSFER-COMPLETE) SOTA Server sends Finalize Download to Device</a>

### <a name="INSTALL-SOFTWARE-UPDATE">[INSTALL-SOFTWARE-UPDATE](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#INSTALL-SOFTWARE-UPDATE) Device installs all received Packages </a>

### <a name="INSTALL-REPORT">[INSTALL-REPORT](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#INSTALL-REPORT) Device reports Installation Result to SOTA Server</a>

### <a name="GET-ALL-PACKAGES">[GET-ALL-PACKAGES](https://github.com/advancedtelematic/sota-server/wiki/Use-Cases#GET-ALL-PACKAGES) Get list of Packages installed on a VIN (from the Device)</a>




## <a name="TOC">Table of Contents</a>

1. [Implementation Phases](#implementation-phases)
1. [Requirements by component](#requirements-by-component)
    - [SRV](#SRV) Server General Requirements
        - [SRV-VIN](#SRV-VIN) Server VIN provisioning
        - [SRV-PACKAGE](#SRV-PACKAGE) Software packages
        - [SRV-PACKAGE-QUEUE](#SRV-PACKAGE-QUEUE) Software packages queuing
        - ~~[SRV-PACKAGE-DEPS](#SRV-PACKAGE-DEPS) Package dependency~~ Moved to [EXT-RESOLV](#EXT-RESOLV)
        - [SRV-TRAFFIC-SHAPING](#SRV-TRAFFIC-SHAPING) Server - Device Traffic control and shaping
        - [SRV-PACKAGE-TRANSMIT](#SRV-PACKAGE-TRANSMIT) Software package transmission
    - [DEV](#DEV) Device requirements
    - [API](#API) Server Web API
        - [API-VIN](#API-VIN) VIN management
        - [API-PACKAGE](#API-PACKAGE) Package management
        - [API-QUEUE](#API-QUEUE) Package queuing
        - [API-TRAFFIC-SHAPING](#API-TRAFFIC-SHAPING) Traffic shaping and control
    - [WEB](#WEB) Web system requirements
        - [WEB-VIN](#WEB-VIN) Web VIN management requirements
        - [WEB-PACKAGE](#WEB-PACKAGE) Web software package management requirements
        - [WEB-QUEUE](#WEB-QUEUE) Web queue management system
        - [WEB-TRAFFIC-SHAPING](#WEB-TRAFFIC-SHAPING) Web Traffic shaping and control
        - [WEB-RESOLV-FILTER](#WEB-RESOLV-FILTER) External resolver filter web UI
        - [WEB-RESOLV-COMP](#WEB-RESOLV-COMP) External resolver component web UI
    - [EXT-RESOLV](#EXT-RESOLV) External package to VIN resolver
        - [EXT-RESOLV-API](#EXT-RESOLV-API) External software package to VIN resolver API
        - [EXT-RESOLV-FILTER](#EXT-RESOLV-FILTER) Resolver filter management
        - [EXT-RESOLV-FILTER-API](#EXT-RESOLV-FILTER-API) Resolver filter management API
        - [EXT-RESOLV-VIN](#EXT-RESOLV-VIN) External resolver VIN management
        - [EXT-RESOLV-VIN-API](#EXT-RESOLV-VIN-API) External resolver VIN management API
        - [EXT-RESOLV-DEPS](#EXT-RESOLV-DEPS) External resolver package dependency
        - [EXT-RESOLV-DEPS-API](#EXT-RESOLV-DEPS-API) External resolver package dependency API
        - [EXT-RESOLV-COMP](#EXT-RESOLV-COMP) External resolver component management
        - [EXT-RESOLV-COMP-API](#EXT-RESOLV-COMP-API) External resolver component management API
        - [EXT-RESOLV-PACKAGE](#EXT-RESOLV-PACKAGE) External resolver package management
        - [EXT-RESOLV-PACKAGE-API](#EXT-RESOLV-PACKAGE-API) External resolver package management API
    - ~~[SRV-CAPACITY](#SRV-CAPACITY) Capacity of Server and External Resolver~~
    - ~~[SRV-BACKUP](#SRV-BACKUP) Backup of Server and External Resolver~~
    - ~~[SRV-UPGRADE](#SRV-UPGRADE) Upgrade of Server and External Resolver~~
    - ~~[SRV-LOGGING](#SRV-LOGGING) Logging of Server and External Resolver~~
    - ~~[SRV-MON](#SRV-MON) Monitoring of Server and External Resolver~~
1. [Requirements by Implementation Phase](#requirements-by-phase)
   - [FIRST](#requirements-first) Initial version
   - [COMP](#requirements-comp) Component management
   - [INST](#requirements-inst) Installation tracking
   - [DEPS](#requirements-deps) Package dependency tracking
   - [SCHED](#requirements-sched) Installation scheduling
   - [SHAPE](#requirements-shape) Traffic shaping
   - ~~[SCALE](#requirements-scale) Capacity for production deployment~~

## <a name="implementation-phases">Implementation Phases</a>

The implementation of the SOTA project is split into seven phases:

   * [FIRST](#requirements-first) Initial version of SOTA system
   * [COMP](#requirements-comp) Adds component management, associating one ore more components with a specific VIN
   * [INST](#requirements-inst) Installation tracking to record which packages are currently installed on which VINs / components
   * [DEPS](#requirements-deps) Package dependency tracking to create a per-vehicle update with necessary packages.
   * [SCHED](#requirements-sched) Installation scheduling, allows for priorities of updates and install period windows. 
   * [SHAPE](#requirements-shape) Traffic shaping allowing the integration of data plans and billing cycles to avoid data overrun costs
   * ~~[SCALE](#requirements-scale) Adds capacity for production deployment~~

## <a name="requirements-by-component">Requirements by Component</a>

### <a name="SRV">[SRV](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV)</a> Server General Requirements

   * <a name="SRV-1">[SRV-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-1)</a> *FIRST* The system shall use Remote Vehicle Interaction (RVI) for all device communication
   * <a name="SRV-2">[SRV-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-2)</a> *FIRST* The design implemented by SOTA Server and the Resolver shall be prepared for a production environment in regards to availability and capacity
   * <a name="SRV-3">[SRV-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-3)</a> *FIRST* The design shall be future expandable with failover sites
   * <a name="SRV-4">[SRV-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-4)</a> *FIRST* The design shall be future expandable with load sharing across multiple nodes
   * <a name="SRV-5">[SRV-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-5)</a> *FIRST* The design shall be future expandable with monitoring and alarm management using SNMP, Graphite, or similar
   * <a name="SRV-6">[SRV-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-6)</a> *FIRST* The design shall be future expandable with backup routines
   * <a name="SRV-7">[SRV-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-7)</a> *FIRST* The design shall be future expandable with runtime upgrade and downgrade procedures

### <a name="SRV-VIN">[SRV-VIN](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-VIN)</a> Server VIN provisioning

   * ~~<a name="SRV-VIN-1">[SRV-VIN-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-VIN-1)</a> *SCALE* The system shall manage up to 100 million VINs~~
   * <a name="SRV-VIN-2">[SRV-VIN-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-VIN-2)</a> *FIRST* A VIN shall be identified by an 1-64 byte string (VIN). Improved wording
   * ~~<a name="SRV-VIN-3">[SRV-VIN-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-VIN-3)</a> *COMP* A VIN shall be able to ber associated to up to 1000 components. Each component is a potential target for software images~~
   * ~~<a name="SRV-VIN-4">[SRV-VIN-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-VIN-4)</a> *INST* Each component associated with a VIN shall have a reference to its currently installed software image.~~
   * ~~<a name="SRV-VIN-5">[SRV-VIN-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-VIN-5)</a> *INST* The component reference to the software image shall be the ID string and version number~~
   * ~~<a name="SRV-VIN-6">[SRV-VIN-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-VIN-6)</a> *SCALE* A VIN shall be able to manage up to 5000 installed software images~~
   * <a name="SRV-VIN-7">[SRV-VIN-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-VIN-7)</a> *SHAPE* The VIN shall be associated with one data plan ID. Ties the VIN to a data plan, allowing us to control traffic to it

### <a name="SRV-PACKAGE">[SRV-PACKAGE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE)</a> Software packages

   * ~~<a name="SRV-PACKAGE-1">[SRV-PACKAGE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-1)</a> *SCALE* The system shall manage up to 10 million software packages~~
   * <a name="SRV-PACKAGE-2">[SRV-PACKAGE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-2)</a> *FIRST* A software package shall have an ID string
   * <a name="SRV-PACKAGE-3">[SRV-PACKAGE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-3)</a> *FIRST* A software package shall have a major.minor.patch formatted version number. The ID string plus version is the unique identifier of the package
   * <a name="SRV-PACKAGE-4">[SRV-PACKAGE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-4)</a> *FIRST* A software package shall have a description
   * <a name="SRV-PACKAGE-5">[SRV-PACKAGE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-5)</a> *FIRST* A software package shall have a vendor
   * <a name="SRV-PACKAGE-6">[SRV-PACKAGE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-6)</a> *COMP* The software ID string shall support regexp matching when searching
   * <a name="SRV-PACKAGE-7">[SRV-PACKAGE-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-7)</a> *COMP* The software version number shall support regexp matching when searching
   * ~~<a name="SRV-PACKAGE-8">[SRV-PACKAGE-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-8)</a> *INST* Each software package shall be maintain a list of all VINs it is installed on~~

### <a name="SRV-PACKAGE-QUEUE">[SRV-PACKAGE-QUEUE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE)</a> Software packages queuing

   * <a name="SRV-PACKAGE-QUEUE-1">[SRV-PACKAGE-QUEUE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-1)</a> *FIRST* The system shall be able to request a software package to be installed on a subset of all managed VINs
   * <a name="SRV-PACKAGE-QUEUE-2">[SRV-PACKAGE-QUEUE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-2)</a> *SCHED* The request shall have an earliest start date. Do not install before 2016-01-01.
   * <a name="SRV-PACKAGE-QUEUE-3">[SRV-PACKAGE-QUEUE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-3)</a> *SCHED* The request shall have a latest install completion date. Do not install after 2016-04-01
   * <a name="SRV-PACKAGE-QUEUE-4">[SRV-PACKAGE-QUEUE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-4)</a> *SCHED* If a software package cannot be installed on one ore more targeted VINs within the specified period, they failed VINs shall be logged
   * <a name="SRV-PACKAGE-QUEUE-5">[SRV-PACKAGE-QUEUE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-5)</a> *SCHED* The request shall have a priority from 1 to 100. Used when updates are queued to individual vehicles. See below
   * <a name="SRV-PACKAGE-QUEUE-6">[SRV-PACKAGE-QUEUE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-6)</a> *FIRST* A list of currently queued updates shall be maintained. One update consist of one or more software packages targeting a specific VIN.
   * <a name="SRV-PACKAGE-QUEUE-7">[SRV-PACKAGE-QUEUE-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-7)</a> *FIRST* Each queued update shall maintain a list of completed VINs that have received the update
   * <a name="SRV-PACKAGE-QUEUE-8">[SRV-PACKAGE-QUEUE-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-8)</a> *FIRST* Each queued update shall maintain a list of pending VINs that have not yet received the update
   * <a name="SRV-PACKAGE-QUEUE-9">[SRV-PACKAGE-QUEUE-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-9)</a> *DEPS* Each VIN targeted by a queued update shall maintain a list of packages that are rolled into the update for that specvific vin. All packages to be added to original package in order to satisfy dependencies are provided by EXT-RESOLV
   * <a name="SRV-PACKAGE-QUEUE-10">[SRV-PACKAGE-QUEUE-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-10)</a> *SCHED* Updates queued for a specific VIN shall be sorted primarily on ascending request priority. Allows high-priority updates to skip the queue and be pushed out earlier to the vehicle
   * <a name="SRV-PACKAGE-QUEUE-11">[SRV-PACKAGE-QUEUE-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-11)</a> *FIRST* Updates queued for a specific VIN shall be sorted secondarily on the time when the request was made.
   * <a name="SRV-PACKAGE-QUEUE-12">[SRV-PACKAGE-QUEUE-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-12)</a> *SHAPE* The software package install request shall have a data pool usage threshold
   * <a name="SRV-PACKAGE-QUEUE-13">[SRV-PACKAGE-QUEUE-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-13)</a> *SHAPE* The data plan usage threshold shall be specified as a decimal percentage
   * <a name="SRV-PACKAGE-QUEUE-14">[SRV-PACKAGE-QUEUE-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-14)</a> *SHAPE* The data plan usage threshold shall specify the maximum percentage of the data pool assigned to a VIN that can be used when the package transfer starts.
   * <a name="SRV-PACKAGE-QUEUE-15">[SRV-PACKAGE-QUEUE-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-15)</a> *SHAPE* If the data pool associated with a targeted VIN has a usage is greater than the specified threshold for the request, the update for the targeted VIN shall be rescheduled to the next billing cycle.
   * ~~<a name="SRV-PACKAGE-QUEUE-16">[SRV-PACKAGE-QUEUE-16](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-16)</a> *SHAPE* Updates queued for a specific VIN shall have an individual earliest start date, forcing it to be transmitted within a specific billing cycle.~~ Duplicate of SRV-PACKAGE-QUEUE-2
   * <a name="SRV-PACKAGE-QUEUE-17">[SRV-PACKAGE-QUEUE-17](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-17)</a> *SHAPE* The individual earliest start date shall not be later than the lastest install completion date specified in SRV-PACKAGE-QUEUE-3
   * <a name="SRV-PACKAGE-QUEUE-18">[SRV-PACKAGE-QUEUE-18](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-18)</a> *SHAPE* If the update for a specific VIN cannot be rescheduled to a billing cycle before the specified latest install competion date, the update shall fail.
   * <a name="SRV-PACKAGE-QUEUE-19">[SRV-PACKAGE-QUEUE-19](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-19)</a> *FIRST* The system shall send a resolve package ID to VIN request to the external resolver system in order to retrieve the VINs and dependencies that should have the package installed. See EXT-RESOLV for details
   * <a name="SRV-PACKAGE-QUEUE-20">[SRV-PACKAGE-QUEUE-20](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-20)</a> *INST* The system shall use EXT-RESOLV-PACKAGE-API to update the resolver with packages installed and removed from each VIN targeted by an update, as reported back by the device. Allows resolver to keep track of which packages are installed on which VIN.
   * <a name="SRV-PACKAGE-QUEUE-21">[SRV-PACKAGE-QUEUE-21](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-21)</a> *INST* "The system shall be able to queue a ""Get All Installed Packages"" command to a device in order to retrieve its currently installed packages". Used to synchronize Resolver's list of installed packages on a VIN with reality
   * <a name="SRV-PACKAGE-QUEUE-22">[SRV-PACKAGE-QUEUE-22](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-QUEUE-22)</a> *INST* "When a ""Get All Installed Packages"" result is received from a device, the EXT-RESOLV-PACKAGE-API shall be used to reset the resolver's list of installed packages for the given VIN."

### ~~<a name="SRV-PACKAGE-DEPS">[SRV-PACKAGE-DEPS](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-DEPS)</a> Package dependency~~ Moved to [EXT-RESOLV](#EXT-RESOLV)

   * ~~<a name="SRV-PACKAGE-DEPS-1">[SRV-PACKAGE-DEPS-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-DEPS-1)</a> *DEPS* Each VIN, as returned by the external resolver, shall be have a dependency check done for the package that is to be installed~~
   * ~~<a name="SRV-PACKAGE-DEPS-2">[SRV-PACKAGE-DEPS-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-DEPS-2)</a> *DEPS* The depency check shall compare the list of packages already installed on the VIN with the dependency graph of the new package to be installed. Which packages does the package about to be installed need on this specific VIN in order to function.~~
   * ~~<a name="SRV-PACKAGE-DEPS-3">[SRV-PACKAGE-DEPS-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-DEPS-3)</a> *DEPS* If an dependent package, required by the package to be installed, is currently not installed on the VIN, the required package will be added to the update for that specific VIN.~~
   * ~~<a name="SRV-PACKAGE-DEPS-4">[SRV-PACKAGE-DEPS-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-DEPS-4)</a> *DEPS* If installing one or more of the packages in an update on a VIN would break dependencies for packages already installed on that VIN, the update shall fail for the given VIN and be reported back to the server~~
   * ~~<a name="SRV-PACKAGE-DEPS-5">[SRV-PACKAGE-DEPS-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-DEPS-5)</a> *DEPS* A software package shall be dependent on up to 100 other software packages~~
   * ~~<a name="SRV-PACKAGE-DEPS-6">[SRV-PACKAGE-DEPS-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-DEPS-6)</a> *DEPS* Software package depencies shall form a graph of sub dependencies. A requires B, which requires C and D.~~
   * ~~<a name="SRV-PACKAGE-DEPS-7">[SRV-PACKAGE-DEPS-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-DEPS-7)</a> *DEPS* A dependency shall be identified by a software package ID string and a version number~~

### <a name="SRV-TRAFFIC-SHAPING">[SRV-TRAFFIC-SHAPING](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING)</a> Server - Device Traffic control and shaping

   * <a name="SRV-TRAFFIC-SHAPING-1">[SRV-TRAFFIC-SHAPING-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-1)</a> ** Server - Device Traffic control and shaping
   * <a name="SRV-TRAFFIC-SHAPING-1">[SRV-TRAFFIC-SHAPING-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-1)</a> *SHAPE* The SOTA server shall be manage data plans used to control when updates are to be sent to their targeted VINs
   * <a name="SRV-TRAFFIC-SHAPING-2">[SRV-TRAFFIC-SHAPING-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-2)</a> *SHAPE* Up to 1,000 data plans shall be managed by the SOTA server
   * <a name="SRV-TRAFFIC-SHAPING-3">[SRV-TRAFFIC-SHAPING-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-3)</a> *SHAPE* A  data plan shall specify a system-wide unique a data plan ID
   * <a name="SRV-TRAFFIC-SHAPING-4">[SRV-TRAFFIC-SHAPING-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-4)</a> *SHAPE* A single data plan profile shall manage up to 1000 billing cycles . One week billing cycles x 1000 is 20 years of billing
   * <a name="SRV-TRAFFIC-SHAPING-5">[SRV-TRAFFIC-SHAPING-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-5)</a> *SHAPE* The data plan shall specify if the data pool for each billing cycle is per VIN, or if it is shared across all VINs associated with the profile. Removed until further notice. For now all billing cycles will be pooled across all VINs
   * <a name="SRV-TRAFFIC-SHAPING-6">[SRV-TRAFFIC-SHAPING-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-6)</a> *SHAPE* Each billing cycle shall specify a date and time stamp when it starts
   * <a name="SRV-TRAFFIC-SHAPING-7">[SRV-TRAFFIC-SHAPING-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-7)</a> *SHAPE* Each billing cycle shall specify a data pool size in kilobytes
   * <a name="SRV-TRAFFIC-SHAPING-8">[SRV-TRAFFIC-SHAPING-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-8)</a> *SHAPE* A billing cycle shall become active when the start date/time stamp occurrs. 
   * <a name="SRV-TRAFFIC-SHAPING-9">[SRV-TRAFFIC-SHAPING-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-9)</a> *SHAPE* A billing cycle shall be deactivated when the next consecutive billing cycle is activated.
   * <a name="SRV-TRAFFIC-SHAPING-10">[SRV-TRAFFIC-SHAPING-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-10)</a> *SHAPE* The SOTA server shall be able to read data usage reports from an external source
   * <a name="SRV-TRAFFIC-SHAPING-11">[SRV-TRAFFIC-SHAPING-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-11)</a> *SHAPE* The SOTA server shall deduct data usage from the pool of the currently active billing cycle
   * <a name="SRV-TRAFFIC-SHAPING-12">[SRV-TRAFFIC-SHAPING-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-12)</a> *SHAPE* The SOTA server shall at all times know how data is left in a pool at any given time
   * <a name="SRV-TRAFFIC-SHAPING-13">[SRV-TRAFFIC-SHAPING-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-13)</a> *SHAPE* When a billing cycle becomes deactive, it shall be archived
   * <a name="SRV-TRAFFIC-SHAPING-14">[SRV-TRAFFIC-SHAPING-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-14)</a> *SHAPE* The architved billing cycle shall contain the number of bytes transmitted during the cycle
   * <a name="SRV-TRAFFIC-SHAPING-15">[SRV-TRAFFIC-SHAPING-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-TRAFFIC-SHAPING-15)</a> *SHAPE* Each billing cycle under a data plan shall be shared across all VINs using the given plan. Replaces SRV-TRAFFIC-SHAPING-5

### <a name="SRV-PACKAGE-TRANSMIT">[SRV-PACKAGE-TRANSMIT](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT)</a> Software package transmission

   * <a name="SRV-PACKAGE-TRANSMIT-1">[SRV-PACKAGE-TRANSMIT-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-1)</a> *FIRST* The Server shall be able to send a wakeup / shoulder tap SMS message to the vehicle, triggering it to connect back to it. Moved from SCHED to FIRST
   * <a name="SRV-PACKAGE-TRANSMIT-2">[SRV-PACKAGE-TRANSMIT-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-2)</a> *FIRST* When the vehicle connects back and identifies itself all updates queued for the VIN shall be transmitted. Moved from SCHED to FIRST
   * <a name="SRV-PACKAGE-TRANSMIT-3">[SRV-PACKAGE-TRANSMIT-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-3)</a> *FIRST* The updates shall be transmitted in the order they are sorted. Allows the server to keep track of which packages are installed where
   * ~~<a name="SRV-PACKAGE-TRANSMIT-4">[SRV-PACKAGE-TRANSMIT-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-4)</a> *FIRST* The update shall be downloadable in chunks.~~ Replaced by SRV-PACKAGE-TRANSMIT-10 - SRV-PACKAGE-TRANSMIT-XXX
   * ~~<a name="SRV-PACKAGE-TRANSMIT-5">[SRV-PACKAGE-TRANSMIT-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-5)</a> *FIRST* The package transfer shall be restartable in case the data link is interrupted~~
   * ~~<a name="SRV-PACKAGE-TRANSMIT-6">[SRV-PACKAGE-TRANSMIT-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-6)</a> *FIRST* The package transfer restart shall continue at the point the transmission was interrupted~~
   * ~~<a name="SRV-PACKAGE-TRANSMIT-7">[SRV-PACKAGE-TRANSMIT-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-7)</a> *INST* Once installed on a VIN, an installation acknolwedgement shall be sent back to the SOTA server~~
   * <a name="SRV-PACKAGE-TRANSMIT-8">[SRV-PACKAGE-TRANSMIT-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-8)</a> *INST* The installation acknowledgement shall be used to update the association between a VINs components and their installed software packages and versions
   * <a name="SRV-PACKAGE-TRANSMIT-9">[SRV-PACKAGE-TRANSMIT-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-9)</a> *INST* In case of an installation failure, there shall be an error code and error text returned to the SOTA server. Executes SRC-PACKAGE-QUEUE-22
   * <a name="SRV-PACKAGE-TRANSMIT-10">[SRV-PACKAGE-TRANSMIT-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-10)</a> *FIRST* "The Server shall send an ""Software Packages Available"" to a vehicle connected for which updates are queued."
   * <a name="SRV-PACKAGE-TRANSMIT-11">[SRV-PACKAGE-TRANSMIT-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-11)</a> *FIRST* "The ""Software Packages Available"" command shall contain a list of package IDs, descriptive text, and size of the update"
   * <a name="SRV-PACKAGE-TRANSMIT-12">[SRV-PACKAGE-TRANSMIT-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-12)</a> *FIRST* "The Server shall support an incoming ""Initiate Download"" received from the device."
   * <a name="SRV-PACKAGE-TRANSMIT-13">[SRV-PACKAGE-TRANSMIT-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-13)</a> *FIRST* "The ""Initiate Software Download"" command shall contain a list of package IDs to send to the device"
   * <a name="SRV-PACKAGE-TRANSMIT-14">[SRV-PACKAGE-TRANSMIT-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-14)</a> *FIRST* "The Server shall send a ""Start Download"" command to the device to initiate a new download"
   * <a name="SRV-PACKAGE-TRANSMIT-15">[SRV-PACKAGE-TRANSMIT-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-15)</a> *FIRST* "The ""Start Download"" command shall contain a list of package ID contained in the download, a download index, a file size, a chunk size, a target unit, and an install/upgrade/remove command."
   * <a name="SRV-PACKAGE-TRANSMIT-16">[SRV-PACKAGE-TRANSMIT-16](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-16)</a> *FIRST* "The Server shall send ""Package Chunk"" command containing a fragment (chunk) of a package"
   * <a name="SRV-PACKAGE-TRANSMIT-17">[SRV-PACKAGE-TRANSMIT-17](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-17)</a> *FIRST* "The ""Package Chunk"" command shall contain a data payload, a chunk index, and an download index refering to the the index provided by the ""Start Download"" command"
   * <a name="SRV-PACKAGE-TRANSMIT-18">[SRV-PACKAGE-TRANSMIT-18](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-18)</a> *FIRST* 
   * <a name="SRV-PACKAGE-TRANSMIT-19">[SRV-PACKAGE-TRANSMIT-19](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-19)</a> *FIRST* "The Server shall support an incoming ""Chunks Received"" command sent by the device"
   * <a name="SRV-PACKAGE-TRANSMIT-20">[SRV-PACKAGE-TRANSMIT-20](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-20)</a> *FIRST* "The ""Chunk Received"" shall contain a download index, and a list of  successfully received and stored chunks for that package."
   * <a name="SRV-PACKAGE-TRANSMIT-21">[SRV-PACKAGE-TRANSMIT-21](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-21)</a> *FIRST* The Server shall inspect the list of successfully received chunks and select as the next chunk to send the lowest indexed chunk not yet received by the device.
   * <a name="SRV-PACKAGE-TRANSMIT-22">[SRV-PACKAGE-TRANSMIT-22](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-22)</a> *FIRST* "The Server shall send a ""Finalize Download"" command when a ""Chunks Received"" is received from the device indicating that all chunks have been received and stored."
   * <a name="SRV-PACKAGE-TRANSMIT-23">[SRV-PACKAGE-TRANSMIT-23](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-23)</a> *INST* "The ""Finalize Download"" command shall contain a download index."
   * <a name="SRV-PACKAGE-TRANSMIT-24">[SRV-PACKAGE-TRANSMIT-24](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-24)</a> *INST* "The Server shall support an incoming ""Installation report"" command sent by the device"
   * <a name="SRV-PACKAGE-TRANSMIT-25">[SRV-PACKAGE-TRANSMIT-25](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-25)</a> *INST* "The ""Installation Report"" shall contain a package ID, a status code indicating success or failure, the currently running version of the package, and a descriptive text of the outcome.". Forwarded by SOTA Server to external resolver so that it can keep track of which packages are installed on which VINs
   * <a name="SRV-PACKAGE-TRANSMIT-26">[SRV-PACKAGE-TRANSMIT-26](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-26)</a> *INST* The Server shall forward the Installation report to the external resolver. As specified by SRV-PACKAGE-QUEUE-20
   * <a name="SRV-PACKAGE-TRANSMIT-27">[SRV-PACKAGE-TRANSMIT-27](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-27)</a> *FIRST* "If a chunk has been sent 5 times, but has not shown up as successfully received in subsequent ""Chunks Received"" reports, the download shall abort."
   * <a name="SRV-PACKAGE-TRANSMIT-28">[SRV-PACKAGE-TRANSMIT-28](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-28)</a> *FIRST* "If a chunk has been sent 5 times with no subsequent ""Chunks Received"" command being received at all within a given period of time, the download shall abort."
   * <a name="SRV-PACKAGE-TRANSMIT-29">[SRV-PACKAGE-TRANSMIT-29](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-29)</a> *INST* An aborted download shall be reported to thee external resolver
   * <a name="SRV-PACKAGE-TRANSMIT-30">[SRV-PACKAGE-TRANSMIT-30](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-30)</a> *INST* "An aborted download shall trigger a ""Abort Download"" command being sent to the device"
   * <a name="SRV-PACKAGE-TRANSMIT-31">[SRV-PACKAGE-TRANSMIT-31](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-PACKAGE-TRANSMIT-31)</a> *INST* "An ""Abort Download"" command shall contain the download index of the failed download". Either the device receives it and cancels the download, or the device will time out the download and cancel it.

### <a name="DEV">[DEV](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV)</a> Device requirements
   
   * <a name="DEV-1">[DEV-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-1)</a> *FIRST* The device shall receive and process wakeup / shoulder tap SMS. Please see Appendix B, Use Cases DEV_*, TRANSFER_*, and INSTALL_* for a detailed description of protocol flow.
   * <a name="DEV-2">[DEV-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-2)</a> *FIRST* The device shall, when a shoulder tap SMS is received, connect back to the SOTA server. Moved from SCHED to FIRST
   * <a name="DEV-3">[DEV-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-3)</a> *FIRST* The device shall identify itself to the SOTA server
   * <a name="DEV-4">[DEV-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-4)</a> *FIRST* The device shall receive chunks for an update
   * <a name="DEV-5">[DEV-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-5)</a> *FIRST* The device shall acknolwedge the reception and local storage of each received chunk
   * <a name="DEV-6">[DEV-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-6)</a> *FIRST* The device shall reassemble the chunks for an update
   * <a name="DEV-7">[DEV-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-7)</a> *FIRST* The device shall validate the integrity of the update. Will be covered by RVI
   * <a name="DEV-8">[DEV-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-8)</a> *FIRST* The device shall authenticate the identity of the sender. Will be covered by RVI
   * <a name="DEV-9">[DEV-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-9)</a> *FIRST* The device shall authorize the sender. Will be covered by RVI
   * ~~<a name="DEV-10">[DEV-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-10)</a> *FIRST* The device shall interface with the local package manager~~
   * ~~<a name="DEV-11">[DEV-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-11)</a> *INST* The device shall report installation success back to the SOTA server. Forwarded by SOTA Server to external resolver so that it can maintain a list of currently installed packages.~~
   * <a name="DEV-12">[DEV-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-12)</a> *INST* The device shall report installation failure back to the SOTA server. Installa
   * <a name="DEV-13">[DEV-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-13)</a> *INST* In case of installation failure, the device shall report an error code and an error text back to the server
   * <a name="DEV-14">[DEV-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-14)</a> *INST* "The device shall support a ""Get currently installed packages command"" (GetCurrentPackages)". Needed sync up a mismatch between a device's view of installed packages and that of the backend server.
   * <a name="DEV-15">[DEV-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-15)</a> *INST* When a GetCurrentPackages command is received, the device shall report back a list of currently installed packages
   * <a name="DEV-16">[DEV-16](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-16)</a> *INST* Each package in a report shall be identified by its package ID string and version number
   * <a name="DEV-17">[DEV-17](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-17)</a> *INST* There shall be a resend attempt in case reporting of package installation results or currently installed packages fails
   * <a name="DEV-18">[DEV-18](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-18)</a> *INST* The device shall, when it connects to the SOTA server, validate the authenticity of the SOTA server. Both client and server side validation are needed.
   * <a name="DEV-19">[DEV-19](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-19)</a> *FIRST* The device shall use RVI for all server communication.
   * <a name="DEV-20">[DEV-20](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-20)</a> *FIRST* The device software shall execute on top of the latest version of Genivi Demo Platform
   * <a name="DEV-21">[DEV-21](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-21)</a> *FIRST* The device software shall execute on top of the latest version of Automotive Grade Linux Distribution
   * <a name="DEV-22">[DEV-22](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-22)</a> *FIRST* The device shall interact with the local Genivi Software Loading Manager (GSLM) through DBUS using a protocol supplied by Genivi. Package manager renamed to Genivi Software Loading Manager
   * <a name="DEV-23">[DEV-23](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-23)</a> *FIRST* There shall be a DBUS command to send an install command to the local GSLM. Package manager renamed to Genivi Software Loading Manager
   * <a name="DEV-24">[DEV-24](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-24)</a> *FIRST* There shall be a DBUS command to send an upgrade command to the local GSLM. Package manager renamed to Genivi Software Loading Manager
   * <a name="DEV-25">[DEV-25](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-25)</a> *FIRST* There shall be a DBUS command to send a remove command to the local GSLM. Package manager renamed to Genivi Software Loading Manager
   * <a name="DEV-26">[DEV-26](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-26)</a> *FIRST* There shall ba a DBUS command to retrieve a list of all currently installed software from the local GSLM
   * <a name="DEV-27">[DEV-27](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-27)</a> *FIRST* All DBUS commands shall return an error/success code and a descriptive text that can be forwarded to SOTA Serevr.
   * <a name="DEV-28">[DEV-28](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-28)</a> *INST* The device shall be able to report locally changed software packages to the SOTA Server
   * <a name="DEV-29">[DEV-29](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-29)</a> *INST* The device shall receive information about locally changed packages through a DBUS command
   * <a name="DEV-30">[DEV-30](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-30)</a> *INST* The report shall contain the package ID, timestamp, and operation (install, upgrade, remove) carried out locally.
   * <a name="DEV-31">[DEV-31](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-31)</a> *FIRST* All DBUS commands shall be compliant with call structure of the Genivi Software Loading Manager. Protocol will be specified by Genivi
   * <a name="DEV-32">[DEV-32](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-32)</a> *FIRST* The device shall use RVI to communicate with the server
   * <a name="DEV-33">[DEV-33](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-33)</a> *FIRST* The device shall use the JSON Data Link and JSON Protocol supplied by the RVI project for its server communication to ensure JSON-based traffic. All traffic sent between server and client will be JSON formatted, regardless of communication channel (SMS, WiFi, 3G, etc). Other protocols (HTTP, OMA-DM FUMA, etc) can be implemented as RVI plugins.
   * <a name="DEV-34">[DEV-34](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-34)</a> *FIRST* "The device shall support an incoming ""Software Packages Available"" command received from the server"
   * <a name="DEV-35">[DEV-35](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-35)</a> *FIRST* "The ""Software Packages Available"" command shall contain a list of package IDs, descriptive text, and size of the download"
   * <a name="DEV-36">[DEV-36](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-36)</a> *FIRST* "The device shall forward the ""Software Packages Available"" command through DBUS to the GSLM ". The Software Loader Manager will interface the HMI to pop a confirmation dialog
   * <a name="DEV-37">[DEV-37](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-37)</a> *FIRST* "The device shall support an incoming ""Initiate Download"" received through DBUS from the GSLM.". The user selected one or more packages on the HMI and clicked ok
   * <a name="DEV-38">[DEV-38](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-38)</a> *FIRST* "The ""Initiate Software Download"" command shall contain a list of package IDs to download and install". "Package IDs are selected from those provided by the ""Software Packages Available"""
   * <a name="DEV-39">[DEV-39](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-39)</a> *FIRST* "The ""Initiate Software Download"" command received from the GSLM shall be forwarded to the SOTA server to initiate the download.". "Will result in a ""Start Download"" command being sent from the Server"
   * <a name="DEV-40">[DEV-40](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-40)</a> *FIRST* "The device shall support an incoming ""Start Download"" command to initiate a new download"
   * <a name="DEV-41">[DEV-41](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-41)</a> *FIRST* "The ""Start Download"" command shall contain a list of package ID contained in the download, a download index, a file size, a chunk size, a target unit, and an install/upgrade/remove command.". "Multple packages may be contained in a single download. Packages can either be dependencies, or bundled packages from the ""Initiate Software Download"" package. Target tells the GSLM if this is a local package, or if it is destined for a module managed by the Module Loader."
   * <a name="DEV-42">[DEV-42](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-42)</a> *FIRST* "The device shall support an incoming ""Package Chunk"" command containing a fragment (chunk) of a package"
   * <a name="DEV-43">[DEV-43](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-43)</a> *FIRST* "The ""Package Chunk"" command shall contain a data payload, a chunk index, and an download index refering to the the index provided by the ""Start Download"" command". "Download index allows multiple donwloads to happen in parallell. payload size is specified by chunk size in ""Start Download"""
   * <a name="DEV-44">[DEV-44](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-44)</a> *FIRST* The device shall store each received chunk on in secondary storage. Downloaded images are reassembled, chunk by chunk on the device side.
   * <a name="DEV-45">[DEV-45](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-45)</a> *FIRST* "The device shall send a ""Chunks Received"" report back to the SOTA Server"
   * <a name="DEV-46">[DEV-46](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-46)</a> *FIRST* "The ""Chunk Received"" shall contain a download index, and a list of  successfully received and stored chunks for that package.". [1-10,12-15,21,23,25,27-30]
   * <a name="DEV-47">[DEV-47](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-47)</a> *FIRST* "The ""Chunk Received"" command shall be sent at after ""Package Chunk"" command has been successfully stored.". Overkill, but increases robustness.
   * <a name="DEV-48">[DEV-48](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-48)</a> *FIRST* "The device shall support an incoming ""Finalize Download command to finish the download". "Will only be sent when ""Chunks received"" reports that all chunks have been received."
   * <a name="DEV-49">[DEV-49](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-49)</a> *FIRST* "The ""Finalize Download"" command shall contain a download index.". Clears the device to start the installation process.
   * <a name="DEV-50">[DEV-50](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-50)</a> *FIRST* The device shall verify that all chunks have been received when a download is finalized.
   * <a name="DEV-51">[DEV-51](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-51)</a> *FIRST* The device shall verify the source and authenticity of the download
   * <a name="DEV-52">[DEV-52](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-52)</a> *FIRST* If either verification fails, an install failure shall be sent back to the SOTA server for all Package IDs in the download
   * <a name="DEV-53">[DEV-53](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-53)</a> *FIRST* "The device shall forward the finalized download to the GSLM together with the install/upgrade/remove command and target unit specified in the ""Start Download"" command.". Commands to be sent are specified by DEV-23 - DEV-27
   * <a name="DEV-54">[DEV-54](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-54)</a> *INST* "The device shall support an incoming DBUS ""Installation Report' command from the local GSLM."
   * <a name="DEV-55">[DEV-55](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-55)</a> *INST* "The ""Installation Report"" shall contain a package ID, a status code indicating success or failure, the currently running version of the package, and a descriptive text of the outcome.". The running version can either be the new version, the existing version, or a reverted factory version.
   * <a name="DEV-56">[DEV-56](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-56)</a> *INST* The device shall forward the installation report to the SOTA Server. SOTA Server will forward it to the external resolver, allowing it to maintain its database of installed packages.
   * <a name="DEV-57">[DEV-57](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-57)</a> *INST* "If no additional ""Package Chunks"" are received for an ongoing download within a given timeout period, the download shall abort"
   * <a name="DEV-58">[DEV-58](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-58)</a> *INST* "If a ""Start Download"" command is received with a download index equal to that of an ongoing download, the ongoing download shall be aborted to make way for the new download.". Allows timed out downloads to be restarted.
   * <a name="DEV-59">[DEV-59](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-59)</a> *INST* "The device shall support an incoming ""Abort Download"" command "
   * <a name="DEV-60">[DEV-60](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-60)</a> *INST* "The ""Abort Download"" command shall contain the download index"
   * <a name="DEV-61">[DEV-61](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-61)</a> *INST* An aborted download shall delete any stored data on the device.
   * <a name="DEV-62">[DEV-62](https://github.com/advancedtelematic/sota-server/wiki/Requirements#DEV-62)</a> *INST* "If the download index of an ""Abort Download"" command cannot be found, the command shall silently be ignored.". """Start Download"" command was lost and never received by client"

### <a name="API">[API](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API)</a> Server Web API

   * <a name="API-1">[API-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-1)</a> *FIRST* The Server shall support an API, allowing its functionality to be accessed by external apps and services
   * <a name="API-2">[API-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-2)</a> *FIRST* The API shall be based on Restful HTTP with JSON.bodies

### <a name="API-VIN">[API-VIN](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN)</a> Server Web API - VIN management

   * <a name="API-VIN-1">[API-VIN-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-1)</a> *FIRST* The API shall have a command to add VINs
   * <a name="API-VIN-2">[API-VIN-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-2)</a> *FIRST* The API shall have a command to delete VINs
   * ~~<a name="API-VIN-3">[API-VIN-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-3)</a> *FIRST* The API shall have a call to search for and return VINs using regexp wildcards.~~ Duplicate of API-VIN-7 - 8
   * ~~<a name="API-VIN-4">[API-VIN-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-4)</a> *COMP* The API shall have a call to associate a component to an existing VIN. Moved to Resolver~~
   * ~~<a name="API-VIN-5">[API-VIN-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-5)</a> *INST* The API shall have a call to associate an software image to a VIN. Moved to Resolver~~
   * ~~<a name="API-VIN-6">[API-VIN-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-6)</a> *INST* The software image associated with a VIN shall be associated with a specific component installed on that VIN. Removed association between package and specific component. Packages are now generically installed on a VIN without component association.~~
   * <a name="API-VIN-7">[API-VIN-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-7)</a> *FIRST* The API shall have a VIN search command
   * <a name="API-VIN-8">[API-VIN-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-8)</a> *FIRST* The search command shall support a VIN regexp to match against
   * ~~<a name="API-VIN-9">[API-VIN-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-9)</a> *COMP* The VINs returned by the search shall each have their associated components listed.~~ Moved to Resolver
   * <a name="API-VIN-10">[API-VIN-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-10)</a> *INST* The VINs returned by the search shall each have their associated installed software packages listed
   * <a name="API-VIN-11">[API-VIN-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-11)</a> *INST* The API shall have a command to list all historic package updates sent to a VIN since the VIN was created
   * <a name="API-VIN-12">[API-VIN-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-12)</a> *INST* Each update returned by a historic list command shall contain a result code reflecting success or failure of installing the package
   * <a name="API-VIN-13">[API-VIN-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-13)</a> *INST* Each update returned by a historic list command shall contain all dependent-upon packages transmitted with the original package in order to satisfy all dependencies of the installed package
   * <a name="API-VIN-14">[API-VIN-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-VIN-14)</a> *INST* Each update returned by a historic list command shall contain a time stamp of when the update completed or failed

### <a name="API-PACKAGE">[API-PACKAGE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE)</a> Server Web API - package management

   * <a name="API-PACKAGE-1">[API-PACKAGE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-1)</a> *FIRST* The API shall have a command to upload packages to the system
   * <a name="API-PACKAGE-2">[API-PACKAGE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-2)</a> *FIRST* Each package shall have an ID string specified
   * <a name="API-PACKAGE-3">[API-PACKAGE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-3)</a> *FIRST* Each package shall have a version number specified so that ID string plus version number creates a unique queue.
   * ~~<a name="API-PACKAGE-4">[API-PACKAGE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-4)</a> *DEPS* Each package shall have an optional list of dependencies specified.~~ Moved to resolver
   * <a name="API-PACKAGE-5">[API-PACKAGE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-5)</a> *FIRST* The API shall have a command to list search for software packages
   * <a name="API-PACKAGE-6">[API-PACKAGE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-6)</a> *FIRST* The search command shall support regexp matching for the ID string and the version number
   * ~~<a name="API-PACKAGE-7">[API-PACKAGE-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-7)</a> *INST* The API shall have a command to list all the VINs that a specific version of a software package is installed on.~~ Database of which packages are installed on which VIN now handled by resolver
   * ~~<a name="API-PACKAGE-8">[API-PACKAGE-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-8)</a> *INST* The API shall have a command to list all the VINs that a specific version of a software package is queued for installation on.~~ Duplicate of API-QUEUE-7
   * ~~<a name="API-PACKAGE-9">[API-PACKAGE-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-PACKAGE-9)</a> *DEPS* The API shall have a command to list the dependencies for a specific package.~~ Moved to resolver

### <a name="API-QUEUE">[API-QUEUE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE)</a> Server Web API - package queuing

   * <a name="API-QUEUE-1">[API-QUEUE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-1)</a> *FIRST* The API shall have a command to request that an package is to be installed
   * ~~<a name="API-QUEUE-2">[API-QUEUE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-2)</a> *FIRST* The install command shall provide a filter label to be applied to the request. Not necessary with external resolver~~
   * <a name="API-QUEUE-3">[API-QUEUE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-3)</a> *FIRST* The install command shall return a unique install ID for the install request
   * <a name="API-QUEUE-4">[API-QUEUE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-4)</a> *SCHED* The API shall have a command to cancel a previously queued install request
   * <a name="API-QUEUE-5">[API-QUEUE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-5)</a> *SCHED* Canceling an install request will delete any pending updates that have yet to be transmitted to their targeted VINs.
   * <a name="API-QUEUE-6">[API-QUEUE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-6)</a> *SCHED* Canceling an install request shall not affect any packages already installed on their targeted VINs.
   * <a name="API-QUEUE-7">[API-QUEUE-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-7)</a> *SCHED* The API shall have a command to list all VINs targeted by a specific install request, identified by the install ID
   * <a name="API-QUEUE-8">[API-QUEUE-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-8)</a> *SCHED* The list command shall return all VINs which the install request was successfully completed on
   * <a name="API-QUEUE-9">[API-QUEUE-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-9)</a> *SCHED* The success report for a VIN shall include a date and time stamp.
   * <a name="API-QUEUE-10">[API-QUEUE-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-10)</a> *SCHED* The list command shall return all VINs for which the install request is still pending on the server
   * <a name="API-QUEUE-11">[API-QUEUE-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-11)</a> *SCHED* The list command shall return all VINs for which the install request failed
   * <a name="API-QUEUE-12">[API-QUEUE-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-12)</a> *SCHED* The failure report for a VIN shall include a date and time stramp.
   * <a name="API-QUEUE-13">[API-QUEUE-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-13)</a> *SCHED* The failure report for a VIN shall include a reason code such as time out, dependency failure, etc.
   * <a name="API-QUEUE-14">[API-QUEUE-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-14)</a> *SCHED* The failure report for a VIN shall include a reason text.
   * <a name="API-QUEUE-15">[API-QUEUE-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-15)</a> *SCHED* The list command shall, for each returned VIN, list the software packages included in the update, including dependencies
   * <a name="API-QUEUE-16">[API-QUEUE-16](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-16)</a> *SCHED* The list command shall return all VINs for which the install request has started transmission, but has not yet completed
   * <a name="API-QUEUE-17">[API-QUEUE-17](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-QUEUE-17)</a> *FIRST* "The API shall have a command to queue an ""Get All Installed Packages"" command for a given VIN"

### <a name="API-TRAFFIC-SHAPING">[API-TRAFFIC-SHAPING](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING)</a> Server Web API - Traffic shaping and control

   * <a name="API-TRAFFIC-SHAPING-1">[API-TRAFFIC-SHAPING-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-1)</a> *SHAPE* The API shall have a command to add a data plan
   * <a name="API-TRAFFIC-SHAPING-2">[API-TRAFFIC-SHAPING-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-2)</a> *SHAPE* The added data plan shall have a unique data plan ID
   * ~~<a name="API-TRAFFIC-SHAPING-3">[API-TRAFFIC-SHAPING-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-3)</a> *SHAPE* The added data plan shall specify if the billing cycles' data pools are shared across all VIN or is specified per VIN.~~ All plans will be pooled across all VINs for now
   * ~~<a name="API-TRAFFIC-SHAPING-4">[API-TRAFFIC-SHAPING-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-4)</a> *SHAPE* The API shall have a command to delete an existing data plan and its billing cycles.~~ Not necessary at a first implementation
   * <a name="API-TRAFFIC-SHAPING-5">[API-TRAFFIC-SHAPING-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-5)</a> *SHAPE* The API shall have a command to add a billing cycle to an existing data plan
   * <a name="API-TRAFFIC-SHAPING-6">[API-TRAFFIC-SHAPING-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-6)</a> *SHAPE* The added billing cycle shall have a start date and time stamp
   * <a name="API-TRAFFIC-SHAPING-7">[API-TRAFFIC-SHAPING-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-7)</a> *SHAPE* The added billing cycle shall have a data pool size, specified in kilobytes.
   * <a name="API-TRAFFIC-SHAPING-8">[API-TRAFFIC-SHAPING-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-8)</a> *SHAPE* The billing cycle shall be identified by its associated data plan and start date/time stamp.
   * ~~<a name="API-TRAFFIC-SHAPING-9">[API-TRAFFIC-SHAPING-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-9)</a> *SHAPE* The API shall have a command to delete an existing billing cycle.~~ Not necessary at a first implementation
   * <a name="API-TRAFFIC-SHAPING-10">[API-TRAFFIC-SHAPING-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-10)</a> *SHAPE* The API shall have a command to add transmitted bytes to the currently active billing cycle of a specific data plan. Increases usage of the given billing cycle
   * <a name="API-TRAFFIC-SHAPING-11">[API-TRAFFIC-SHAPING-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-11)</a> *SHAPE* The API shall have a command to retrieve the data pool size of the current billing cycle of a specific data plan
   * <a name="API-TRAFFIC-SHAPING-12">[API-TRAFFIC-SHAPING-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-12)</a> *SHAPE* The API shall have a command to retrieve the number of used bytes in the current billing cycle of a specific data plan
   * <a name="API-TRAFFIC-SHAPING-13">[API-TRAFFIC-SHAPING-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#API-TRAFFIC-SHAPING-13)</a> *SHAPE* The API shall have a command to list all billing cycles created under a data plan

### <a name="WEB">[WEB](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB)</a> Web system requirements

   * <a name="WEB-1">[WEB-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-1)</a> *FIRST* The web system shall act as a front end toward the SOTA system
   * <a name="WEB-2">[WEB-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-2)</a> *FIRST* The web system shall use the Web  API of the SOTA system
   * ~~<a name="WEB-3">[WEB-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-3)</a> *SCALE* The web system shall have a provisioning system for adding users~~
   * ~~<a name="WEB-4">[WEB-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-4)</a> *SCALE* The web system shall have a provisioning system for deleting users~~
   * ~~<a name="WEB-5">[WEB-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-5)</a> *SCALE* Each user shall have a username~~
   * ~~<a name="WEB-6">[WEB-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-6)</a> *SCALE* Each user shall have a password~~
   * ~~<a name="WEB-7">[WEB-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-7)</a> *SCALE* The web system shall have a pre-configured admin user with a pre-configured password.~~
   * ~~<a name="WEB-8">[WEB-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-8)</a> *SCALE* Only the admin user shall be able to add and delete other users~~
   * ~~<a name="WEB-9">[WEB-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-9)</a> *SCALE* All users in the system shall have full access to all web functions, except add/delete users. For now. Different access levels will come later.~~

### <a name="WEB-VIN">[WEB-VIN](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN)</a> Web VIN management requirements

   * <a name="WEB-VIN-1">[WEB-VIN-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-1)</a> *FIRST* The web system  shall have a UI to add VINs
   * <a name="WEB-VIN-2">[WEB-VIN-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-2)</a> *SCHED* The web system shall have a UI to delete VINs
   * <a name="WEB-VIN-3">[WEB-VIN-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-3)</a> *SCHED* The web system shall have a UI to search for VINs
   * <a name="WEB-VIN-4">[WEB-VIN-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-4)</a> *SCHED* The web system's VINs shall be searchable by regular expressions
   * <a name="WEB-VIN-5">[WEB-VIN-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-5)</a> *SCHED* Each VIN by a search shall be clickable
   * <a name="WEB-VIN-6">[WEB-VIN-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-6)</a> *SCHED* Clicking on a VIN from the search result shall bring up a property screen for the VIN
   * <a name="WEB-VIN-7">[WEB-VIN-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-7)</a> *COMP* The VIN property screen shall list all components installed on the VIN, as retrieved from the external resolver
   * ~~<a name="WEB-VIN-8">[WEB-VIN-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-8)</a> *INST* Each component on a VIN property screen shall be listed with its currently installed software image and version.~~ Packages no longer associated with target components on a VIN.
   * <a name="WEB-VIN-9">[WEB-VIN-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-9)</a> *INST* The VIN property screen shall list all installed software packages (including dependencies), as retrieved from the external resolver
   * ~~<a name="WEB-VIN-10">[WEB-VIN-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-10)</a> *COMP* The VIN property screen shall have a button for adding a component on the external Resolver.~~ Duplicate of WEB-RESOLV-COMP-1
   * ~~<a name="WEB-VIN-11">[WEB-VIN-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-11)</a> *COMP* Adding a component shall specify the component part number.~~ Duplicate of WEB-RESOLV-COMP-1
   * <a name="WEB-VIN-12">[WEB-VIN-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-12)</a> *INST* The VIN property screen shall have a button for adding a (manually installed) software package on a VIN. API Call sent to the Resolver
   * <a name="WEB-VIN-13">[WEB-VIN-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-13)</a> *INST* The software package added to the system shall be specified with a ID string
   * <a name="WEB-VIN-14">[WEB-VIN-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-14)</a> *INST* The software package added to the system shall be specified with a version number
   * <a name="WEB-VIN-15">[WEB-VIN-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-15)</a> *INST* The software package added to the system shall be specified with a description
   * ~~<a name="WEB-VIN-16">[WEB-VIN-16](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-16)</a> *INST* The software package shall be assocaited with a component installed on the VIN.~~ Packages no longer associated with target components on a VIN.
   * <a name="WEB-VIN-17">[WEB-VIN-17](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-17)</a> *INST* The VIN property screen shall have a button to list all software packages currently queued to it
   * <a name="WEB-VIN-18">[WEB-VIN-18](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-18)</a> *FIRST* A VIN added, deleted, or modified by the web system shall update both the server and the external resolver
   * <a name="WEB-VIN-19">[WEB-VIN-19](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-VIN-19)</a> *INST* The VIN property screen shall have a button to re-synchronize the list of installed packages with those actually installed on device

### <a name="WEB-PACKAGE">[WEB-PACKAGE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE)</a> Web software package management requirements

   * <a name="WEB-PACKAGE-1">[WEB-PACKAGE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-1)</a> *FIRST* The web system shall have a UI to upload packages to the system.
   * <a name="WEB-PACKAGE-2">[WEB-PACKAGE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-2)</a> *FIRST* The upload screen shall have a software package ID string
   * <a name="WEB-PACKAGE-3">[WEB-PACKAGE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-3)</a> *FIRST* The upload screen shall have a software version 
   * <a name="WEB-PACKAGE-4">[WEB-PACKAGE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-4)</a> *FIRST* The upload screen shall have a description
   * <a name="WEB-PACKAGE-5">[WEB-PACKAGE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-5)</a> *FIRST* The upload screen shall have a vendor
   * <a name="WEB-PACKAGE-6">[WEB-PACKAGE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-6)</a> *DEPS* The upload screen shall allow to specify dependencies on one or more exisiting software packages. Interfaces resolver to handle dependencies
   * <a name="WEB-PACKAGE-7">[WEB-PACKAGE-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-7)</a> *FIRST* The web system shall have a UI to search for software packages
   * <a name="WEB-PACKAGE-8">[WEB-PACKAGE-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-8)</a> *FIRST* The search command shall support regexp matching for the ID string and the version number
   * <a name="WEB-PACKAGE-9">[WEB-PACKAGE-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-9)</a> *FIRST* Each software package in the returned search result list shall be clickable
   * <a name="WEB-PACKAGE-10">[WEB-PACKAGE-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-10)</a> *FIRST* Clicking on an package from the search result shall bring up a property screen for the package
   * <a name="WEB-PACKAGE-11">[WEB-PACKAGE-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-11)</a> *FIRST* The package property screen shall show the package ID string
   * <a name="WEB-PACKAGE-12">[WEB-PACKAGE-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-12)</a> *FIRST* The package property screen shall show the version number
   * <a name="WEB-PACKAGE-13">[WEB-PACKAGE-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-13)</a> *FIRST* The package property screen shall show the description
   * <a name="WEB-PACKAGE-14">[WEB-PACKAGE-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-14)</a> *FIRST* The package property screen shall show the vendor
   * <a name="WEB-PACKAGE-15">[WEB-PACKAGE-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-15)</a> *DEPS* The package property screen shall show all the software package dependencies the shown package has. Interfaces resolver to handle dependencies
   * <a name="WEB-PACKAGE-16">[WEB-PACKAGE-16](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-16)</a> *INST* The package property screen shall have a button to list all VINs that the package is installed on. Interfaces resolver to retrieve lsit
   * <a name="WEB-PACKAGE-17">[WEB-PACKAGE-17](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-17)</a> *INST* Clicking on the installed VIN  button shall bring up a list of all VINs with the package installed
   * <a name="WEB-PACKAGE-18">[WEB-PACKAGE-18](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-18)</a> *INST* The package property screen shall have a button to list all VINs that the package is queued for
   * <a name="WEB-PACKAGE-19">[WEB-PACKAGE-19](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-19)</a> *FIRST* A package added, deleted, or modified by the web system shall update both the server and the external resolver
   * <a name="WEB-PACKAGE-20">[WEB-PACKAGE-20](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-PACKAGE-20)</a> *FIRST* The package property screen shall have a button to list all filters that will be executed when the package is resolved to VINs. "Will queue a ""Get All Installed Packages"" command to the given VIN, using API-QUEUE-17"

### <a name="WEB-QUEUE">[WEB-QUEUE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE)</a> Web queue management system

   * <a name="WEB-QUEUE-1">[WEB-QUEUE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-1)</a> *FIRST* The web system shall have a user interface for creating an update to be pushed to one or more VINs
   * <a name="WEB-QUEUE-2">[WEB-QUEUE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-2)</a> *FIRST* The create update screen shall specify the software package and version to push
   * ~~<a name="WEB-QUEUE-3">[WEB-QUEUE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-3)</a> *FIRST* The create update screen shall specify the filter tag to apply.~~ Not applicable with external resolver
   * <a name="WEB-QUEUE-4">[WEB-QUEUE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-4)</a> *SCHED* The create update screen shall specify the earliest start date for the update to be installed
   * <a name="WEB-QUEUE-5">[WEB-QUEUE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-5)</a> *SCHED* The create update screen shall specify the latest end date for the update to be installed
   * <a name="WEB-QUEUE-6">[WEB-QUEUE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-6)</a> *SCHED* The create update screen shall specify a priority
   * <a name="WEB-QUEUE-7">[WEB-QUEUE-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-7)</a> *FIRST* The create update screen shall have a button to contact external resolver and list all VINs that would receive the update. Will invoke external resolver to map package ID to VINs
   * <a name="WEB-QUEUE-8">[WEB-QUEUE-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-8)</a> *FIRST* The web system shall have a user interface to list all created updates in the system
   * <a name="WEB-QUEUE-9">[WEB-QUEUE-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-9)</a> *FIRST* Each listed update shall be shown with its software package and filter label
   * <a name="WEB-QUEUE-10">[WEB-QUEUE-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-10)</a> *FIRST* Each listed update shall be clickable
   * <a name="WEB-QUEUE-11">[WEB-QUEUE-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-11)</a> *FIRST* Clicking on the update shall bring up the update property screen
   * <a name="WEB-QUEUE-12">[WEB-QUEUE-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-12)</a> *FIRST* The update property screen shall show the information provided by WEB-QUEUE-[2-6]
   * <a name="WEB-QUEUE-13">[WEB-QUEUE-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-13)</a> *FIRST* The update property screen shall show the total number of VINs targeted by the update
   * <a name="WEB-QUEUE-14">[WEB-QUEUE-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-14)</a> *INST* The update property screen shall show the total number of VINs that have had the update successfully installed
   * <a name="WEB-QUEUE-15">[WEB-QUEUE-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-15)</a> *INST* The update property screen shall show the total number of VINs that have failed to have the update installed
   * <a name="WEB-QUEUE-16">[WEB-QUEUE-16](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-16)</a> *INST* The update property screen shall show the total number of VINs that are still waiting to receive the update
   * <a name="WEB-QUEUE-17">[WEB-QUEUE-17](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-17)</a> *INST* The update property screen shall be able to list all VINs that have had the update succsessfully installed
   * <a name="WEB-QUEUE-18">[WEB-QUEUE-18](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-18)</a> *INST* The update property screen shall be able to list all VINs that failed to have the update installed
   * <a name="WEB-QUEUE-19">[WEB-QUEUE-19](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-19)</a> *FIRST* The update property screen shall be able to list all VINs that are still waiting to recveive the update
   * <a name="WEB-QUEUE-20">[WEB-QUEUE-20](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-20)</a> *FIRST* Each VIN listed in WEB-QUEUE-[17-19] shall be clickable
   * <a name="WEB-QUEUE-21">[WEB-QUEUE-21](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-21)</a> *COMP* Clicking on a VIN shall list all software packages and version included in the update for the given VIN
   * <a name="WEB-QUEUE-22">[WEB-QUEUE-22](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-22)</a> *SCHED* "The update property screen shall have a ""cancel update"" button."
   * <a name="WEB-QUEUE-23">[WEB-QUEUE-23](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-23)</a> *SCHED* "Clicking on the ""cancel update"" button shall cancel any updates to VINs that are not yet complete"
   * <a name="WEB-QUEUE-24">[WEB-QUEUE-24](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-QUEUE-24)</a> *SCHED* "Clicking on the ""cancel update"" button shall delete the update itself."

### <a name="WEB-TRAFFIC-SHAPING">[WEB-TRAFFIC-SHAPING](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING)</a> Web - Traffic shaping and control

   * <a name="WEB-TRAFFIC-SHAPING-1">[WEB-TRAFFIC-SHAPING-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-1)</a> *SHAPE* The web system shall have a user interface to add a data plan
   * <a name="WEB-TRAFFIC-SHAPING-2">[WEB-TRAFFIC-SHAPING-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-2)</a> *SHAPE* The add data plan screen shall have a unique data plan ID
   * ~~<a name="WEB-TRAFFIC-SHAPING-3">[WEB-TRAFFIC-SHAPING-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-3)</a> *SHAPE* The add data plan screen shall specify if the data pool size is per VIN or is shared across all participating VINs.~~ Not needed in a first implenentation
   * <a name="WEB-TRAFFIC-SHAPING-4">[WEB-TRAFFIC-SHAPING-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-4)</a> *SHAPE* The add data plan shall have a command to delete an existing data plan and its billing cycles. Was previously erroneously removed instead of the line above.
   * <a name="WEB-TRAFFIC-SHAPING-5">[WEB-TRAFFIC-SHAPING-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-5)</a> *SHAPE* The web system shall have a user interface to add billing cycles to a data plan
   * <a name="WEB-TRAFFIC-SHAPING-6">[WEB-TRAFFIC-SHAPING-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-6)</a> *SHAPE* An added billing cycle shall be entered with a start date / time stamp
   * <a name="WEB-TRAFFIC-SHAPING-7">[WEB-TRAFFIC-SHAPING-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-7)</a> *SHAPE* An added billing cycle shall be entered with a data pool size in kilobytes
   * <a name="WEB-TRAFFIC-SHAPING-8">[WEB-TRAFFIC-SHAPING-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-8)</a> *SHAPE* The web system shall be able to list all data plans and their properties
   * <a name="WEB-TRAFFIC-SHAPING-9">[WEB-TRAFFIC-SHAPING-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-9)</a> *SHAPE* The web system shall be able to list all billing cycles added to a data plan and their properties
   * ~~<a name="WEB-TRAFFIC-SHAPING-10">[WEB-TRAFFIC-SHAPING-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-10)</a> *SHAPE* The web system shall be able to delete an existing billing cycle under a data plan.~~ Not needed in a first implementation
   * <a name="WEB-TRAFFIC-SHAPING-11">[WEB-TRAFFIC-SHAPING-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-11)</a> *SHAPE* The web system shall be able to show the current data pool usage for an existing billing cycle
   * <a name="WEB-TRAFFIC-SHAPING-12">[WEB-TRAFFIC-SHAPING-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-TRAFFIC-SHAPING-12)</a> *SHAPE* The web system shall be able to update the data pool usage for an existing billing cycle by setting a kilobyte value

### <a name="WEB-RESOLV-FILTER">[WEB-RESOLV-FILTER](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER)</a> External resolver filter web UI

   * <a name="WEB-RESOLV-FILTER-1">[WEB-RESOLV-FILTER-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-1)</a> *FIRST* The web system shall have a user interface for adding install filters on the external resolver
   * <a name="WEB-RESOLV-FILTER-2">[WEB-RESOLV-FILTER-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-2)</a> *FIRST* The add install filter screen shall have a filter label
   * <a name="WEB-RESOLV-FILTER-3">[WEB-RESOLV-FILTER-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-3)</a> *FIRST* The add install filter screen shall have a text field for a boolean expression
   * <a name="WEB-RESOLV-FILTER-4">[WEB-RESOLV-FILTER-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-4)</a> *FIRST* The add install filter screen shall have a button to syntax check the boolean expression
   * <a name="WEB-RESOLV-FILTER-5">[WEB-RESOLV-FILTER-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-5)</a> *FIRST* In case the syntax check fails, an error code and text should be showed
   * <a name="WEB-RESOLV-FILTER-6">[WEB-RESOLV-FILTER-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-6)</a> *FIRST* The web system shall have a button to list all filters on the external resolver
   * <a name="WEB-RESOLV-FILTER-7">[WEB-RESOLV-FILTER-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-7)</a> *FIRST* Each filter returned in the list result shall be clicklable
   * <a name="WEB-RESOLV-FILTER-8">[WEB-RESOLV-FILTER-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-8)</a> *FIRST* Clicking on a filter in the list result shall bring up the filter property screen retrieved from the external resolver
   * <a name="WEB-RESOLV-FILTER-9">[WEB-RESOLV-FILTER-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-9)</a> *FIRST* The property screen shall be able to edit all filter properties
   * <a name="WEB-RESOLV-FILTER-10">[WEB-RESOLV-FILTER-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-10)</a> *FIRST* The property screen shall support syntax checking of changed boolean expression
   * <a name="WEB-RESOLV-FILTER-11">[WEB-RESOLV-FILTER-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-11)</a> *FIRST* "The property screen shall have a ""delete filter"" to remove a filter from the external resolver"
   * <a name="WEB-RESOLV-FILTER-12">[WEB-RESOLV-FILTER-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-12)</a> *FIRST* "The property screen shall have a ""list associated packages"" to list all packages that will have the filter executed when resolved"
   * <a name="WEB-RESOLV-FILTER-13">[WEB-RESOLV-FILTER-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-13)</a> *FIRST* The property screen shall be able to add a filter to a package. The filter(s) associated with a package will be run over all VINs when the given package is resolved.
   * <a name="WEB-RESOLV-FILTER-14">[WEB-RESOLV-FILTER-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-FILTER-14)</a> *FIRST* The property screen shall be able to delete a filter from a package

### <a name="WEB-RESOLV-COMP">[WEB-RESOLV-COMP](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP)</a> External resolver component web UI

   * <a name="WEB-RESOLV-COMP-1">[WEB-RESOLV-COMP-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-1)</a> *COMP* The web system  shall have a UI to add components to the external resolver using a component part number
   * <a name="WEB-RESOLV-COMP-2">[WEB-RESOLV-COMP-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-2)</a> *COMP* The web system shall have a UI to delete components from the external resolver
   * <a name="WEB-RESOLV-COMP-3">[WEB-RESOLV-COMP-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-3)</a> *COMP* The web system shall have a UI to search for components in the external resolver
   * <a name="WEB-RESOLV-COMP-4">[WEB-RESOLV-COMP-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-4)</a> *COMP* The web system's components shall be searchable by part number regular expressions
   * <a name="WEB-RESOLV-COMP-5">[WEB-RESOLV-COMP-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-5)</a> *COMP* The componens returned by a search shall be clickable
   * <a name="WEB-RESOLV-COMP-6">[WEB-RESOLV-COMP-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-6)</a> *COMP* Clicking on a component from the search result shall bring up a property screen for the component
   * <a name="WEB-RESOLV-COMP-7">[WEB-RESOLV-COMP-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-7)</a> *COMP* The component property screen shall show the part number
   * <a name="WEB-RESOLV-COMP-8">[WEB-RESOLV-COMP-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-8)</a> *COMP* The component property screen shall show the description
   * <a name="WEB-RESOLV-COMP-9">[WEB-RESOLV-COMP-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-9)</a> *COMP* The component property screen shall have a button to list all VINs that the component is installed on
   * <a name="WEB-RESOLV-COMP-10">[WEB-RESOLV-COMP-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#WEB-RESOLV-COMP-10)</a> *COMP* The web system shall have a UI to add a component to a specific VIN using the external resolver.

### <a name="EXT-RESOLV">[EXT-RESOLV](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV)</a> External package to VIN resolver

   * <a name="EXT-RESOLV">[EXT-RESOLV](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV)</a> ** External package to VIN resolver
   * <a name="EXT-RESOLV-1">[EXT-RESOLV-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-1)</a> *FIRST* The system shall rely on an external system, the Resolver, to translate a software package to VINs that are to have the package installed.
   * <a name="EXT-RESOLV-2">[EXT-RESOLV-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-2)</a> *FIRST* The resolver shall have a server-side WebAPI to handle resolve requests.
   * <a name="EXT-RESOLV-3">[EXT-RESOLV-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-3)</a> *FIRST* A resolve request, sent to the external resolver by the system, shall contain a software package ID string.
   * <a name="EXT-RESOLV-4">[EXT-RESOLV-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-4)</a> *FIRST* A resolve request shall return a list of zero or more VIN numbers that the sofware package should be installed on

### <a name="EXT-RESOLV-API">[EXT-RESOLV-API](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-API)</a> External software package to VIN resolver API

   * <a name="EXT-RESOLV-API-1">[EXT-RESOLV-API-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-API-1)</a> *FIRST* The Resolver shall support an API, allowing its functionality to be accessed by external apps and services
   * <a name="EXT-RESOLV-API-2">[EXT-RESOLV-API-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-API-2)</a> *FIRST* The API shall be based on Restful HTTP with JSON.bodies

### <a name="EXT-RESOLV-FILTER">[EXT-RESOLV-FILTER](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER)</a> Resolver filter management

   * <a name="EXT-RESOLV-FILTER-1">[EXT-RESOLV-FILTER-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-1)</a> *FIRST* A resolve request shall retrieve the VINs to install an package on by executing one or more filters
   * <a name="EXT-RESOLV-FILTER-2">[EXT-RESOLV-FILTER-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-2)</a> *FIRST* A single filter shall be associated with zero or more software package IDs
   * <a name="EXT-RESOLV-FILTER-3">[EXT-RESOLV-FILTER-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-3)</a> *FIRST* The software package ID string of a resolve request shall be used retrieve the all filters associated with the package ID.
   * <a name="EXT-RESOLV-FILTER-4">[EXT-RESOLV-FILTER-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-4)</a> *FIRST* Each filter retrieved for an package ID in a resolve request shall be run on all VINs in order to filter out those VINs that should receive the update. All filters are AND-ed together.
   * <a name="EXT-RESOLV-FILTER-5">[EXT-RESOLV-FILTER-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-5)</a> *FIRST* Only VINs that pass all filters associated with the software package ID shall be returned by the resolve request
   * <a name="EXT-RESOLV-FILTER-6">[EXT-RESOLV-FILTER-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-6)</a> *FIRST* The filter shall specify a boolean expression that has to be true for a specific VIN in order for the software package to be queued to that VIN.

       "vin_matches(""SAJNX5745SC??????"") 

       Install if:
       If the VIN starts with the ""SAJNX5745SC"""

   * <a name="EXT-RESOLV-FILTER-7">[EXT-RESOLV-FILTER-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-7)</a> *COMP* The boolean expression shall have operands that identify specific components by their part number

      "vin_matches(""SAJNX5745SC??????"") AND has_component(""IVI_hardware_4711_rev_a"")

       Install if:
       VIN starts with ""SAJNX5745SC"" and ""IVI_board_4711_rev_a"" is installed"

   * <a name="EXT-RESOLV-FILTER-8">[EXT-RESOLV-FILTER-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-8)</a> *COMP* The component part number in the expression shall support regexp matching

       "vin_matches(""SAJNX5745SC??????"") AND has_component(""IVI_hardware_4711_rev_*"")
       
       Install if:
       VIN starts with ""SAJNX5745SC"" and ""IVI_board_4711_rev"" is installed, regardless of its revision suffix."

   * <a name="EXT-RESOLV-FILTER-9">[EXT-RESOLV-FILTER-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-9)</a> *INST* The boolean expression shall have operands that identify the currently installed packages packages.

       "vin_matches(""SAJNX5745SC??????"") OR 
       (has_package(""IVI_image"", ""1.[1-3].*) AND 
       has_component(""IVI_backseat_screen_rev_1""))

       Install if
       
       VIN starts with ""SAJNX5745SC"", or package IVI_image 1.1.0 - 1.3.9 is installed together with component  ""IVI_backseat_screen_rev_1"".

   * <a name="EXT-RESOLV-FILTER-10">[EXT-RESOLV-FILTER-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-10)</a> *INST* The currently installed package is identified by its ID string and version number
   * <a name="EXT-RESOLV-FILTER-11">[EXT-RESOLV-FILTER-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-11)</a> *INST* The ID string and version number of the currently installed package shall support regexp matching
   * <a name="EXT-RESOLV-FILTER-12">[EXT-RESOLV-FILTER-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-12)</a> *FIRST* The VIN operand shall support regexp matching
   * <a name="EXT-RESOLV-FILTER-13">[EXT-RESOLV-FILTER-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-13)</a> *FIRST* The finished boolean expression shall be labeled and stored as a named, reusable filter

### <a name="EXT-RESOLV-FILTER-API">[EXT-RESOLV-FILTER-API](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API)</a> External resolver feature management API

   * <a name="EXT-RESOLV-FILTER-API-1">[EXT-RESOLV-FILTER-API-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-1)</a> *FIRST* The API shall have a command to add new filters
   * <a name="EXT-RESOLV-FILTER-API-2">[EXT-RESOLV-FILTER-API-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-2)</a> *FIRST* The added filter shall have a boolean expression
   * <a name="EXT-RESOLV-FILTER-API-3">[EXT-RESOLV-FILTER-API-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-3)</a> *FIRST* The added filter shall have a unique label
   * <a name="EXT-RESOLV-FILTER-API-4">[EXT-RESOLV-FILTER-API-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-4)</a> *FIRST* The API shall havea a command to check the syntax of a boolean expression
   * <a name="EXT-RESOLV-FILTER-API-5">[EXT-RESOLV-FILTER-API-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-5)</a> *FIRST* The syntax check shall return ok or an error code and text 
   * <a name="EXT-RESOLV-FILTER-API-6">[EXT-RESOLV-FILTER-API-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-6)</a> *COMP* The API shall have a command to delete filters identified by their label
   * <a name="EXT-RESOLV-FILTER-API-7">[EXT-RESOLV-FILTER-API-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-7)</a> *FIRST* The API shall have a command to associate a package ID string to a filter. Used during resolve package -> VINs to figure out which filter to apply to th egiven VINs
   * <a name="EXT-RESOLV-FILTER-API-8">[EXT-RESOLV-FILTER-API-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-8)</a> *FIRST* The API shall have a command to dis-associate a package ID string from a filter
   * <a name="EXT-RESOLV-FILTER-API-9">[EXT-RESOLV-FILTER-API-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-9)</a> *FIRST* The API shall have a command to list all filters associated with a package ID string
   * <a name="EXT-RESOLV-FILTER-API-10">[EXT-RESOLV-FILTER-API-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-10)</a> *FIRST* The  filters associated with the given package ID shall have their filter labels returned
   * <a name="EXT-RESOLV-FILTER-API-11">[EXT-RESOLV-FILTER-API-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-11)</a> *FIRST* The API shall have a command to list all packages associated with a filter label.
   * <a name="EXT-RESOLV-FILTER-API-12">[EXT-RESOLV-FILTER-API-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-FILTER-API-12)</a> *FIRST* The packages associated with the given filter label shall have their package ID strings returned.

### <a name="EXT-RESOLV-VIN">[EXT-RESOLV-VIN](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN)</a> External resolver VIN management

   * <a name="EXT-RESOLV-VIN-1">[EXT-RESOLV-VIN-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN-1)</a> *SCALE* The resolver shall manage up to 100 million VINs
   * <a name="EXT-RESOLV-VIN-2">[EXT-RESOLV-VIN-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN-2)</a> *FIRST* A VIN shall use a VIN number as its primary identifier
   * <a name="EXT-RESOLV-VIN-3">[EXT-RESOLV-VIN-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN-3)</a> *COMP* A VIN shall be able to ber associated to up to 1000 components
   * <a name="EXT-RESOLV-VIN-4">[EXT-RESOLV-VIN-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN-4)</a> *INST* A VIN shall be able to handle up to 5000 installed software packages

### <a name="EXT-RESOLV-VIN-API">[EXT-RESOLV-VIN-API](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN-API)</a> External resolver VIN API management

   * <a name="EXT-RESOLV-VIN-API-1">[EXT-RESOLV-VIN-API-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN-API-1)</a> *FIRST* The API shall have a command to add VINs
   * <a name="EXT-RESOLV-VIN-API-2">[EXT-RESOLV-VIN-API-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN-API-2)</a> *FIRST* The API shall have a command to delete VINs
   * <a name="EXT-RESOLV-VIN-API-3">[EXT-RESOLV-VIN-API-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-VIN-API-3)</a> *COMP* The API shall have a call to specify a component has been installed in an VIN

### <a name="EXT-RESOLV-DEPS">[EXT-RESOLV-DEPS](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS)</a> External resolver package dependency

   * <a name="EXT-RESOLV-DEPS-1">[EXT-RESOLV-DEPS-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-1)</a> *DEPS* Each VIN returned by a resolver for a specific package shall have a dependency check done for that package
   * <a name="EXT-RESOLV-DEPS-2">[EXT-RESOLV-DEPS-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-2)</a> *DEPS* The depency check shall compare the list of packages already installed on the VIN with the dependency graph of the new package to be installed
     Which packages does the package about to be installed need on this specific VIN in order to function.
   * <a name="EXT-RESOLV-DEPS-3">[EXT-RESOLV-DEPS-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-3)</a> *DEPS* If an dependent package, required by the package to be installed, is currently not installed on the VIN, the required package will be provided with the given VIN when returned to the SOTA Server.
   * <a name="EXT-RESOLV-DEPS-4">[EXT-RESOLV-DEPS-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-4)</a> *DEPS* If installing one or more of the packages in an update on a VIN would break dependencies for packages already installed on that VIN, an error will be logged for the given update by the resolver and the VIN is removed from the set of VINs returned by the resolver
   * <a name="EXT-RESOLV-DEPS-5">[EXT-RESOLV-DEPS-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-5)</a> *DEPS* A software package shall be dependent on up to 100 other software packages
   * <a name="EXT-RESOLV-DEPS-6">[EXT-RESOLV-DEPS-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-6)</a> *DEPS* Software package depencies shall form a graph of sub dependencies.
     A requires B, which requires C and D.
   * <a name="EXT-RESOLV-DEPS-7">[EXT-RESOLV-DEPS-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-7)</a> *DEPS* A dependency shall be identified by a software package ID string and a version number

### <a name="EXT-RESOLV-DEPS-API">[EXT-RESOLV-DEPS-API](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-API)</a> External resolver package dependency API

   * <a name="EXT-RESOLV-DEPS-API-1">[EXT-RESOLV-DEPS-API-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-API-1)</a> *DEPS* The resolver shall have a command to add a dependency from one package toward another
   * <a name="EXT-RESOLV-DEPS-API-2">[EXT-RESOLV-DEPS-API-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-API-2)</a> *DEPS* The resolver shall have a command to delete a dependency from one package toward another
   * <a name="EXT-RESOLV-DEPS-API-3">[EXT-RESOLV-DEPS-API-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-DEPS-API-3)</a> *DEPS* The resolver shall have a command to list all dependencies of a package. Can be called recursively to get entire dependency graph

### <a name="EXT-RESOLV-COMP">[EXT-RESOLV-COMP](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP)</a> External resolver component management

   * <a name="EXT-RESOLV-COMP-1">[EXT-RESOLV-COMP-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-1)</a> *COMP* The resolver system shall manage up to 1 million components. Used by filtering process to require that specific components are installed in order for a package to be installed
   * <a name="EXT-RESOLV-COMP-2">[EXT-RESOLV-COMP-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-2)</a> *COMP* A component has a part number. Main identifier used by software packages.
   * <a name="EXT-RESOLV-COMP-3">[EXT-RESOLV-COMP-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-3)</a> *COMP* A component has a description
   * <a name="EXT-RESOLV-COMP-4">[EXT-RESOLV-COMP-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-4)</a> *COMP* A component can have one or more VINs associated with it. Each VIN has zero or more components installed on it.

### <a name="EXT-RESOLV-COMP-API">[EXT-RESOLV-COMP-API](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-API)</a> External resolver component management API

   * <a name="EXT-RESOLV-COMP-API-1">[EXT-RESOLV-COMP-API-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-API-1)</a> *COMP* The API shall have a command to add a component to the system with a part number
   * <a name="EXT-RESOLV-COMP-API-2">[EXT-RESOLV-COMP-API-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-API-2)</a> *COMP* The API shall have a command to specify that a component is installed on a specific VIN. "Will make has_component(""xxx"") return true in a filter run over the given VIN."
   * <a name="EXT-RESOLV-COMP-API-3">[EXT-RESOLV-COMP-API-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-API-3)</a> *COMP* The API shall have a command to search for and return components using regexp wildcards
   * <a name="EXT-RESOLV-COMP-API-4">[EXT-RESOLV-COMP-API-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-API-4)</a> *COMP* The returned components shall be listed with their part number
   * <a name="EXT-RESOLV-COMP-API-5">[EXT-RESOLV-COMP-API-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-API-5)</a> *COMP* The returned components shall be listed with their descriptions
   * <a name="EXT-RESOLV-COMP-API-6">[EXT-RESOLV-COMP-API-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-COMP-API-6)</a> *COMP* The API shall have a command to list all VINs that a component is installed on

### <a name="EXT-RESOLV-PACKAGE">[EXT-RESOLV-PACKAGE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE)</a> External resolver package management

   * ~~<a name="EXT-RESOLV-PACKAGE-1">[EXT-RESOLV-PACKAGE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-1)</a> *SCALE* The resolver shall manage up to 10 million software packages~~
   * <a name="EXT-RESOLV-PACKAGE-2">[EXT-RESOLV-PACKAGE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-2)</a> *FIRST* A software package shall have an ID string
   * <a name="EXT-RESOLV-PACKAGE-3">[EXT-RESOLV-PACKAGE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-3)</a> *FIRST* A software package shall have a major.minor.patch formatted version number. The ID string plus version is the unique identifier of the package
   * <a name="EXT-RESOLV-PACKAGE-4">[EXT-RESOLV-PACKAGE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-4)</a> *FIRST* A software package shall have a description
   * <a name="EXT-RESOLV-PACKAGE-5">[EXT-RESOLV-PACKAGE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-5)</a> *FIRST* A software package shall have a vendor
   * <a name="EXT-RESOLV-PACKAGE-6">[EXT-RESOLV-PACKAGE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-6)</a> *INST* Each software package shall be maintain a list of all VINs it is installed on. Used by filter has_package() operand

### <a name="EXT-RESOLV-PACKAGE-API">[EXT-RESOLV-PACKAGE-API](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API)</a> External resolver package API

   * <a name="EXT-RESOLV-PACKAGE-API-1">[EXT-RESOLV-PACKAGE-API-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API-1)</a> *FIRST* The API shall have a command to specify a package to the external resolver
   * <a name="EXT-RESOLV-PACKAGE-API-2">[EXT-RESOLV-PACKAGE-API-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API-2)</a> *FIRST* Each package shall have an ID string specified
   * <a name="EXT-RESOLV-PACKAGE-API-3">[EXT-RESOLV-PACKAGE-API-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API-3)</a> *FIRST* Each software package shall have a major.minor.patch formatted version number. The ID string plus version is the unique identifier of the package
   * <a name="EXT-RESOLV-PACKAGE-API-4">[EXT-RESOLV-PACKAGE-API-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API-4)</a> *FIRST* Each package shall have a version number specified so that ID string plus version number creates a unique queue.
   * <a name="EXT-RESOLV-PACKAGE-API-5">[EXT-RESOLV-PACKAGE-API-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API-5)</a> *INST* The resolver shall have a command to specify that a given package has been installed on a VIN. Called by the SOTA Server when a device reports that an update has been installed.
   * <a name="EXT-RESOLV-PACKAGE-API-6">[EXT-RESOLV-PACKAGE-API-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API-6)</a> *INST* The resolver shall have a command to specify that a given package has been deleted from a VIN. Called by the SOTA Server when a device reports that an update has been updated/removed
   * <a name="EXT-RESOLV-PACKAGE-API-7">[EXT-RESOLV-PACKAGE-API-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API-7)</a> *INST* The resolver shall have a command to search for all packages installed on a VIN
   * <a name="EXT-RESOLV-PACKAGE-API-8">[EXT-RESOLV-PACKAGE-API-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#EXT-RESOLV-PACKAGE-API-8)</a> *INST* The resolver shall have a command to search for all VINs that a package is installed on.

### ~~<a name="SRV-CAPACITY">[SRV-CAPACITY](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY)</a> Capacity of Server and External resolver~~

   * ~~<a name="SRV-CAPACITY-1">[SRV-CAPACITY-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-1)</a> *SCALE* The system shall support a cold standby~~
   * ~~<a name="SRV-CAPACITY-2">[SRV-CAPACITY-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-2)</a> *SCALE* The system shall provide 99.9% uptime, yielding a maximum of 43.8 minutes downtime per month.~~
   * ~~<a name="SRV-CAPACITY-3">[SRV-CAPACITY-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-3)</a> *SCALE* The uptime includes maintenance, upgrades, backup, and other administrative routines~~
   * ~~<a name="SRV-CAPACITY-4">[SRV-CAPACITY-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-4)</a> *SCALE* The system shall handle a load capacity of 200 chunks of package data being transmitted per second to vehicles~~
   * ~~<a name="SRV-CAPACITY-5">[SRV-CAPACITY-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-5)</a> *SCALE* Each chunk shall be 100KBytes, rendering the total chunking bandwidth to 20MByte/Sec.~~
   * ~~<a name="SRV-CAPACITY-6">[SRV-CAPACITY-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-6)</a> *SCALE* The system shall queue at least 100 packages per seconds to their target VINs~~
   * ~~<a name="SRV-CAPACITY-7">[SRV-CAPACITY-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-7)</a> *SCALE* The target VINs shall be selected from a fleet of 1,000,000 VINs.~~
   * ~~<a name="SRV-CAPACITY-8">[SRV-CAPACITY-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-8)</a> *SCALE* The VINs filtered out by a queueing operation shall be 100,000, rendering the total queuing time to 1000 seconds for all targbeted VINs~~
   * ~~<a name="SRV-CAPACITY-9">[SRV-CAPACITY-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-9)</a> *SCALE* At no time shall transactional latency be greater than 500 msec.~~
   * ~~<a name="SRV-CAPACITY-10">[SRV-CAPACITY-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-10)</a> *SCALE* Transactional latency is defined as the number of milliseconds elasped from that the transaction was read from the NIC to the time the result was sent back over the NIC.~~
   * ~~<a name="SRV-CAPACITY-11">[SRV-CAPACITY-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-11)</a> *SCALE* A transaction is defined as a request sent from either a vehicle, an external system, or the Web UI to the system~~
   * ~~<a name="SRV-CAPACITY-12">[SRV-CAPACITY-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-12)</a> *SCALE* SRV-CAPCITY-4 to SRV-CAPACITY-11 shall be handled in parallel.~~
   * ~~<a name="SRV-CAPACITY-13">[SRV-CAPACITY-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-CAPACITY-13)</a> *SCALE* SRV-CAPACITY-1 - 12 applies both to the server and the external resolver.~~

### ~~<a name="SRV-BACKUP">[SRV-BACKUP](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP)</a> Backup of Server and External Resolver~~

   * ~~<a name="SRV-BACKUP-1">[SRV-BACKUP-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-1)</a> *SCALE* The system shall have backup routines~~
   * ~~<a name="SRV-BACKUP-2">[SRV-BACKUP-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-2)</a> *SCALE* The backup shall be documented as a part of the maintenance instructions~~
   * ~~<a name="SRV-BACKUP-3">[SRV-BACKUP-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-3)</a> *SCALE* A freshly installed SOTA system with the backup applied shall render a system identical to the originally backed up system~~
   * ~~<a name="SRV-BACKUP-4">[SRV-BACKUP-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-4)</a> *SCALE* The backup system shall be able to selectively restore only VINs in both the external Resolver and the Server. Modifed wording to cover both resolver and system~~
   * ~~<a name="SRV-BACKUP-5">[SRV-BACKUP-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-5)</a> *SCALE* The backup system shall be able to selectively restore only components in both the external Resolver and the server. Modifed wording to cover both resolver and system~~
   * ~~<a name="SRV-BACKUP-6">[SRV-BACKUP-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-6)</a> *SCALE* The backup system shall be able to selectively restore only packages~~
   * ~~<a name="SRV-BACKUP-7">[SRV-BACKUP-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-7)</a> *SCALE* The backup system shall be able to selectively restore only the package queues and package transmission status~~
   * ~~<a name="SRV-BACKUP-8">[SRV-BACKUP-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-8)</a> *SCALE* The backup system shall be able to selectively restore only data plans and billing cycles~~
   * ~~<a name="SRV-BACKUP-9">[SRV-BACKUP-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-9)</a> *SCALE* The backup system shall be able to selectively restore only filters~~
   * ~~<a name="SRV-BACKUP-10">[SRV-BACKUP-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-BACKUP-10)</a> *SCALE* SRV-BACKUP-1 - 10  applies both to the server and the external resolver.~~

### ~~<a name="SRV-UPGRADE">[SRV-UPGRADE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-UPGRADE)</a> Upgrade of Server and External Resolver~~

   * ~~<a name="SRV-UPGRADE-1">[SRV-UPGRADE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-UPGRADE-1)</a> *SCALE* The system shall be upgradable.~~
   * ~~<a name="SRV-UPGRADE-2">[SRV-UPGRADE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-UPGRADE-2)</a> *SCALE* The upgrade shall support a rollback to its previous state~~
   * ~~<a name="SRV-UPGRADE-3">[SRV-UPGRADE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-UPGRADE-3)</a> *SCALE* The upgrade shall be done with no uptime impact.~~
   * ~~<a name="SRV-UPGRADE-4">[SRV-UPGRADE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-UPGRADE-4)</a> *SCALE* The upgrade can be done with capacity degradation if, for example, one side of a cluster is upgraded at the time. Negative requirement. Removed.~~
   * ~~<a name="SRV-UPGRADE-5">[SRV-UPGRADE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-UPGRADE-5)</a> *SCALE* The capacity during an upgrade shall at all times stay above 40% of the whole system's capacity.~~
   * ~~<a name="SRV-UPGRADE-6">[SRV-UPGRADE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-UPGRADE-6)</a> *SCALE* SRV-UPGRADE-1 - 5  applies both to the server and the external resolver.~~

### ~~<a name="SRV-LOGGING">[SRV-LOGGING](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-LOGGING)</a> Logging of Server and External Resolver~~

   * ~~<a name="SRV-LOGGING-1">[SRV-LOGGING-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-LOGGING-1)</a> *SCALE* The system shall support logging.~~
   * ~~<a name="SRV-LOGGING-2">[SRV-LOGGING-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-LOGGING-2)</a> *SCALE* Logging shall have at least 5 different log levels.~~
   * ~~<a name="SRV-LOGGING-3">[SRV-LOGGING-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-LOGGING-3)</a> *SCALE* Log levels shall be settable while the system is running~~
   * ~~<a name="SRV-LOGGING-4">[SRV-LOGGING-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-LOGGING-4)</a> *SCALE* Logs shall rotate so that they never consume more than a given amount of disk space~~
   * ~~<a name="SRV-LOGGING-5">[SRV-LOGGING-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-LOGGING-5)</a> *SCALE* The amount of disk space consumed by logs shall be settable while the system is running~~
   * ~~<a name="SRV-LOGGING-6">[SRV-LOGGING-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-LOGGING-6)</a> *SCALE* SRV-LOGGING-1 - 5  applies both to the server and the external resolver.~~

### ~~<a name="SRV-MON">[SRV-MON](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON)</a> Monitoring of Server and External Resolver~~

   * ~~<a name="SRV-MON-1">[SRV-MON-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-1)</a> *SCALE* The system shall be monitorable via SNMP, graphite, or similar open standard~~
   * ~~<a name="SRV-MON-2">[SRV-MON-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-2)</a> *SCALE* All SRV-MON requirements above and below applies both to the server and the extrernal resolver.~~
   * ~~<a name="SRV-MON-ALARM-1">[SRV-MON-ALARM-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-ALARM-1)</a> *SCALE* Monitoring shall provide alarms~~
   * ~~<a name="SRV-MON-ALARM-2">[SRV-MON-ALARM-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-ALARM-2)</a> *SCALE* Alarms shall be triggered by resource exhaustion~~
   * ~~<a name="SRV-MON-ALARM-3">[SRV-MON-ALARM-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-ALARM-3)</a> *SCALE* Alarms shall be triggered by software component failures and restarts~~
   * ~~<a name="SRV-MON-ALARM-4">[SRV-MON-ALARM-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-ALARM-4)</a> *SCALE* Alarms shall be triggered by excessive latency~~
   * ~~<a name="SRV-MON-ALARM-5">[SRV-MON-ALARM-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-ALARM-5)</a> *SCALE* Alarms shall be triggered by failed external systems such as provisioning~~
   * ~~<a name="SRV-MON-ALARM-6">[SRV-MON-ALARM-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-ALARM-6)</a> *SCALE* Alarms shall be triggered by hardware failures~~
   * ~~<a name="SRV-MON-ALARM-7">[SRV-MON-ALARM-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-ALARM-7)</a> *SCALE* Alarms shall be manually acknolwedged by an operator action~~
   * ~~<a name="SRV-MON-COUNT-1">[SRV-MON-COUNT-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-COUNT-1)</a> *SCALE* Monitoring shall provide counters~~
   * ~~<a name="SRV-MON-COUNT-2">[SRV-MON-COUNT-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-COUNT-2)</a> *SCALE* Counters shall be persistent across system restarts~~
   * ~~<a name="SRV-MON-COUNT-3">[SRV-MON-COUNT-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-COUNT-3)</a> *SCALE* Counters shall be kept for number of transactions processed by the system~~
   * ~~<a name="SRV-MON-COUNT-4">[SRV-MON-COUNT-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-COUNT-4)</a> *SCALE* Counters shall be kept for number of packages sent to vehicles~~
   * ~~<a name="SRV-MON-COUNT-5">[SRV-MON-COUNT-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-COUNT-5)</a> *SCALE* Counters shall be kept for number of kilobytes sent to vehicles~~
   * ~~<a name="SRV-MON-COUNT-6">[SRV-MON-COUNT-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-COUNT-6)</a> *SCALE* Counters shall be kept for number of kilobytes received from vehicles~~
   * ~~<a name="SRV-MON-COUNT-7">[SRV-MON-COUNT-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-COUNT-7)</a> *SCALE* Counters shall be kept for number of sessions from vehicles~~
   * ~~<a name="SRV-MON-GAUGE-1">[SRV-MON-GAUGE-1](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-1)</a> *SCALE* Monitoring shall provide gauges~~
   * ~~<a name="SRV-MON-GAUGE-2">[SRV-MON-GAUGE-2](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-2)</a> *SCALE* Each gauge shall provide an average value  over the last 10 seconds~~
   * ~~<a name="SRV-MON-GAUGE-3">[SRV-MON-GAUGE-3](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-3)</a> *SCALE* Each gauge shall provide an average value  over the last 60 seconds~~
   * ~~<a name="SRV-MON-GAUGE-4">[SRV-MON-GAUGE-4](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-4)</a> *SCALE* Each gauge shall provide an average value  over the last 600 seconds~~
   * ~~<a name="SRV-MON-GAUGE-5">[SRV-MON-GAUGE-5](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-5)</a> *SCALE* Each gauge shall provide an average value  over the last 3600 seconds~~
   * ~~<a name="SRV-MON-GAUGE-6">[SRV-MON-GAUGE-6](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-6)</a> *SCALE* Each gauge shall provide an average value  over the last 86400 seconds~~
   * ~~<a name="SRV-MON-GAUGE-7">[SRV-MON-GAUGE-7](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-7)</a> *SCALE* Each gauge shall provide a min and max measured value  over the last 10 seconds~~
   * ~~<a name="SRV-MON-GAUGE-8">[SRV-MON-GAUGE-8](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-8)</a> *SCALE* Each gauge shall provide a min and max measured value over the last 60 seconds~~
   * ~~<a name="SRV-MON-GAUGE-9">[SRV-MON-GAUGE-9](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-9)</a> *SCALE* Each gauge shall provide a min and max measured value over the last 600 seconds~~
   * ~~<a name="SRV-MON-GAUGE-10">[SRV-MON-GAUGE-10](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-10)</a> *SCALE* Each gauge shall provide a min and max measured value over the last 3600 seconds~~
   * ~~<a name="SRV-MON-GAUGE-11">[SRV-MON-GAUGE-11](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-11)</a> *SCALE* Each gauge shall provide a min and max measured value over the last 86400 seconds~~
   * ~~<a name="SRV-MON-GAUGE-12">[SRV-MON-GAUGE-12](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-12)</a> *SCALE* Monitoring shall gauge transactions per second~~
   * ~~<a name="SRV-MON-GAUGE-13">[SRV-MON-GAUGE-13](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-13)</a> *SCALE* Monitoring shall gauge transactional latency~~
   * ~~<a name="SRV-MON-GAUGE-14">[SRV-MON-GAUGE-14](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-14)</a> *SCALE* Monitoring shall gauge disk consumption~~
   * ~~<a name="SRV-MON-GAUGE-15">[SRV-MON-GAUGE-15](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-15)</a> *SCALE* Monitoring shall gauge virtual memory consumption~~
   * ~~<a name="SRV-MON-GAUGE-16">[SRV-MON-GAUGE-16](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-16)</a> *SCALE* Monitoring shall gauge bytes/second transmitted to all vehicles~~
   * ~~<a name="SRV-MON-GAUGE-17">[SRV-MON-GAUGE-17](https://github.com/advancedtelematic/sota-server/wiki/Requirements#SRV-MON-GAUGE-17)</a> *SCALE* Monitoring shall gauge bytes/second transmitted from all vehicles~~

## <a name="requirements-by-phase">Requirements by Implementation Phase</a>

### <a name="requirements-first">[FIRST](https://github.com/advancedtelematic/sota-server/wiki/Requirements#requirements-first) - Initial version of SOTA system

   * [SRV-1](#SRV-1) The system shall use Remote Vehicle Interaction (RVI) for all device communication
   * [SRV-2](#SRV-2) The design implemented by SOTA Server and the Resolver shall be prepared for a production environment in regards to availability and capacity
   * [SRV-3](#SRV-3) The design shall be future expandable with failover sites
   * [SRV-4](#SRV-4) The design shall be future expandable with load sharing across multiple nodes
   * [SRV-5](#SRV-5) The design shall be future expandable with monitoring and alarm management using SNMP, Graphite, or similar
   * [SRV-6](#SRV-6) The design shall be future expandable with backup routines
   * [SRV-7](#SRV-7) The design shall be future expandable with runtime upgrade and downgrade procedures
   * [SRV-VIN-2](#SRV-VIN-2) A VIN shall be identified by an 1-64 byte string (VIN). Improved wording
   * [SRV-PACKAGE-2](#SRV-PACKAGE-2) A software package shall have an ID string
   * [SRV-PACKAGE-3](#SRV-PACKAGE-3) A software package shall have a major.minor.patch formatted version number. The ID string plus version is the unique identifier of the package
   * [SRV-PACKAGE-4](#SRV-PACKAGE-4) A software package shall have a description
   * [SRV-PACKAGE-5](#SRV-PACKAGE-5) A software package shall have a vendor
   * [SRV-PACKAGE-QUEUE-1](#SRV-PACKAGE-QUEUE-1) The system shall be able to request a software package to be installed on a subset of all managed VINs
   * [SRV-PACKAGE-QUEUE-6](#SRV-PACKAGE-QUEUE-6) A list of currently queued updates shall be maintained. One update consist of one or more software packages targeting a specific VIN.
   * [SRV-PACKAGE-QUEUE-7](#SRV-PACKAGE-QUEUE-7) Each queued update shall maintain a list of completed VINs that have received the update
   * [SRV-PACKAGE-QUEUE-8](#SRV-PACKAGE-QUEUE-8) Each queued update shall maintain a list of pending VINs that have not yet received the update
   * [SRV-PACKAGE-QUEUE-11](#SRV-PACKAGE-QUEUE-11) Updates queued for a specific VIN shall be sorted secondarily on the time when the request was made.
   * [SRV-PACKAGE-QUEUE-19](#SRV-PACKAGE-QUEUE-19) The system shall send a resolve package ID to VIN request to the external resolver system in order to retrieve the VINs and dependencies that should have the package installed. See EXT-RESOLV for details
   * [SRV-PACKAGE-TRANSMIT-1](#SRV-PACKAGE-TRANSMIT-1) The Server shall be able to send a wakeup / shoulder tap SMS message to the vehicle, triggering it to connect back to it. Moved from SCHED to FIRST
   * [SRV-PACKAGE-TRANSMIT-2](#SRV-PACKAGE-TRANSMIT-2) When the vehicle connects back and identifies itself all updates queued for the VIN shall be transmitted. Moved from SCHED to FIRST
   * [SRV-PACKAGE-TRANSMIT-3](#SRV-PACKAGE-TRANSMIT-3) The updates shall be transmitted in the order they are sorted. Allows the server to keep track of which packages are installed where
   * ~~[SRV-PACKAGE-TRANSMIT-4](#SRV-PACKAGE-TRANSMIT-4) The update shall be downloadable in chunks.~~ Replaced by SRV-PACKAGE-TRANSMIT-10 - SRV-PACKAGE-TRANSMIT-XXX
   * ~~[SRV-PACKAGE-TRANSMIT-5](#SRV-PACKAGE-TRANSMIT-5) The package transfer shall be restartable in case the data link is interrupted~~
   * ~~[SRV-PACKAGE-TRANSMIT-6](#SRV-PACKAGE-TRANSMIT-6) The package transfer restart shall continue at the point the transmission was interrupted~~
   * [SRV-PACKAGE-TRANSMIT-10](#SRV-PACKAGE-TRANSMIT-10) "The Server shall send an ""Software Packages Available"" to a vehicle connected for which updates are queued."
   * [SRV-PACKAGE-TRANSMIT-11](#SRV-PACKAGE-TRANSMIT-11) "The ""Software Packages Available"" command shall contain a list of package IDs, descriptive text, and size of the update"
   * [SRV-PACKAGE-TRANSMIT-12](#SRV-PACKAGE-TRANSMIT-12) "The Server shall support an incoming ""Initiate Download"" received from the device."
   * [SRV-PACKAGE-TRANSMIT-13](#SRV-PACKAGE-TRANSMIT-13) "The ""Initiate Software Download"" command shall contain a list of package IDs to send to the device"
   * [SRV-PACKAGE-TRANSMIT-14](#SRV-PACKAGE-TRANSMIT-14) "The Server shall send a ""Start Download"" command to the device to initiate a new download"
   * [SRV-PACKAGE-TRANSMIT-15](#SRV-PACKAGE-TRANSMIT-15) "The ""Start Download"" command shall contain a list of package ID contained in the download, a download index, a file size, a chunk size, a target unit, and an install/upgrade/remove command."
   * [SRV-PACKAGE-TRANSMIT-16](#SRV-PACKAGE-TRANSMIT-16) "The Server shall send ""Package Chunk"" command containing a fragment (chunk) of a package"
   * [SRV-PACKAGE-TRANSMIT-17](#SRV-PACKAGE-TRANSMIT-17) "The ""Package Chunk"" command shall contain a data payload, a chunk index, and an download index refering to the the index provided by the ""Start Download"" command"
   * [SRV-PACKAGE-TRANSMIT-18](#SRV-PACKAGE-TRANSMIT-18) 
   * [SRV-PACKAGE-TRANSMIT-19](#SRV-PACKAGE-TRANSMIT-19) "The Server shall support an incoming ""Chunks Received"" command sent by the device"
   * [SRV-PACKAGE-TRANSMIT-20](#SRV-PACKAGE-TRANSMIT-20) "The ""Chunk Received"" shall contain a download index, and a list of  successfully received and stored chunks for that package."
   * [SRV-PACKAGE-TRANSMIT-21](#SRV-PACKAGE-TRANSMIT-21) The Server shall inspect the list of successfully received chunks and select as the next chunk to send the lowest indexed chunk not yet received by the device.
   * [SRV-PACKAGE-TRANSMIT-22](#SRV-PACKAGE-TRANSMIT-22) "The Server shall send a ""Finalize Download"" command when a ""Chunks Received"" is received from the device indicating that all chunks have been received and stored."
   * [SRV-PACKAGE-TRANSMIT-27](#SRV-PACKAGE-TRANSMIT-27) "If a chunk has been sent 5 times, but has not shown up as successfully received in subsequent ""Chunks Received"" reports, the download shall abort."
   * [SRV-PACKAGE-TRANSMIT-28](#SRV-PACKAGE-TRANSMIT-28) "If a chunk has been sent 5 times with no subsequent ""Chunks Received"" command being received at all within a given period of time, the download shall abort."
   * [DEV-1](#DEV-1) The device shall receive and process wakeup / shoulder tap SMS. Please see Appendix B, Use Cases DEV_*, TRANSFER_*, and INSTALL_* for a detailed description of protocol flow.
   * [DEV-2](#DEV-2) The device shall, when a shoulder tap SMS is received, connect back to the SOTA server. Moved from SCHED to FIRST
   * [DEV-3](#DEV-3) The device shall identify itself to the SOTA server
   * [DEV-4](#DEV-4) The device shall receive chunks for an update
   * [DEV-5](#DEV-5) The device shall acknolwedge the reception and local storage of each received chunk
   * [DEV-6](#DEV-6) The device shall reassemble the chunks for an update
   * [DEV-7](#DEV-7) The device shall validate the integrity of the update. Will be covered by RVI
   * [DEV-8](#DEV-8) The device shall authenticate the identity of the sender. Will be covered by RVI
   * [DEV-9](#DEV-9) The device shall authorize the sender. Will be covered by RVI
   * ~~[DEV-10](#DEV-10) The device shall interface with the local package manager~~
   * [DEV-19](#DEV-19) The device shall use RVI for all server communication.
   * [DEV-20](#DEV-20) The device software shall execute on top of the latest version of Genivi Demo Platform
   * [DEV-21](#DEV-21) The device software shall execute on top of the latest version of Automotive Grade Linux Distribution
   * [DEV-22](#DEV-22) The device shall interact with the local Genivi Software Loading Manager (GSLM) through DBUS using a protocol supplied by Genivi. Package manager renamed to Genivi Software Loading Manager
   * [DEV-23](#DEV-23) There shall be a DBUS command to send an install command to the local GSLM. Package manager renamed to Genivi Software Loading Manager
   * [DEV-24](#DEV-24) There shall be a DBUS command to send an upgrade command to the local GSLM. Package manager renamed to Genivi Software Loading Manager
   * [DEV-25](#DEV-25) There shall be a DBUS command to send a remove command to the local GSLM. Package manager renamed to Genivi Software Loading Manager
   * [DEV-26](#DEV-26) There shall ba a DBUS command to retrieve a list of all currently installed software from the local GSLM
   * [DEV-27](#DEV-27) All DBUS commands shall return an error/success code and a descriptive text that can be forwarded to SOTA Serevr.
   * [DEV-31](#DEV-31) All DBUS commands shall be compliant with call structure of the Genivi Software Loading Manager. Protocol will be specified by Genivi
   * [DEV-32](#DEV-32) The device shall use RVI to communicate with the server
   * [DEV-33](#DEV-33) The device shall use the JSON Data Link and JSON Protocol supplied by the RVI project for its server communication to ensure JSON-based traffic. All traffic sent between server and client will be JSON formatted, regardless of communication channel (SMS, WiFi, 3G, etc). Other protocols (HTTP, OMA-DM FUMA, etc) can be implemented as RVI plugins.
   * [DEV-34](#DEV-34) "The device shall support an incoming ""Software Packages Available"" command received from the server"
   * [DEV-35](#DEV-35) "The ""Software Packages Available"" command shall contain a list of package IDs, descriptive text, and size of the download"
   * [DEV-36](#DEV-36) "The device shall forward the ""Software Packages Available"" command through DBUS to the GSLM ". The Software Loader Manager will interface the HMI to pop a confirmation dialog
   * [DEV-37](#DEV-37) "The device shall support an incoming ""Initiate Download"" received through DBUS from the GSLM.". The user selected one or more packages on the HMI and clicked ok
   * [DEV-38](#DEV-38) "The ""Initiate Software Download"" command shall contain a list of package IDs to download and install". "Package IDs are selected from those provided by the ""Software Packages Available"""
   * [DEV-39](#DEV-39) "The ""Initiate Software Download"" command received from the GSLM shall be forwarded to the SOTA server to initiate the download.". "Will result in a ""Start Download"" command being sent from the Server"
   * [DEV-40](#DEV-40) "The device shall support an incoming ""Start Download"" command to initiate a new download"
   * [DEV-41](#DEV-41) "The ""Start Download"" command shall contain a list of package ID contained in the download, a download index, a file size, a chunk size, a target unit, and an install/upgrade/remove command.". "Multple packages may be contained in a single download. Packages can either be dependencies, or bundled packages from the ""Initiate Software Download"" package. Target tells the GSLM if this is a local package, or if it is destined for a module managed by the Module Loader."
   * [DEV-42](#DEV-42) "The device shall support an incoming ""Package Chunk"" command containing a fragment (chunk) of a package"
   * [DEV-43](#DEV-43) "The ""Package Chunk"" command shall contain a data payload, a chunk index, and an download index refering to the the index provided by the ""Start Download"" command". "Download index allows multiple donwloads to happen in parallell. payload size is specified by chunk size in ""Start Download"""
   * [DEV-44](#DEV-44) The device shall store each received chunk on in secondary storage. Downloaded images are reassembled, chunk by chunk on the device side.
   * [DEV-45](#DEV-45) "The device shall send a ""Chunks Received"" report back to the SOTA Server"
   * [DEV-46](#DEV-46) "The ""Chunk Received"" shall contain a download index, and a list of  successfully received and stored chunks for that package.". [1-10,12-15,21,23,25,27-30]
   * [DEV-47](#DEV-47) "The ""Chunk Received"" command shall be sent at after ""Package Chunk"" command has been successfully stored.". Overkill, but increases robustness.
   * [DEV-48](#DEV-48) "The device shall support an incoming ""Finalize Download command to finish the download". "Will only be sent when ""Chunks received"" reports that all chunks have been received."
   * [DEV-49](#DEV-49) "The ""Finalize Download"" command shall contain a download index.". Clears the device to start the installation process.
   * [DEV-50](#DEV-50) The device shall verify that all chunks have been received when a download is finalized.
   * [DEV-51](#DEV-51) The device shall verify the source and authenticity of the download
   * [DEV-52](#DEV-52) If either verification fails, an install failure shall be sent back to the SOTA server for all Package IDs in the download
   * [DEV-53](#DEV-53) "The device shall forward the finalized download to the GSLM together with the install/upgrade/remove command and target unit specified in the ""Start Download"" command.". Commands to be sent are specified by DEV-23 - DEV-27
   * [API-1](#API-1) The Server shall support an API, allowing its functionality to be accessed by external apps and services
   * [API-2](#API-2) The API shall be based on Restful HTTP with JSON.bodies
   * [API-VIN-1](#API-VIN-1) The API shall have a command to add VINs
   * [API-VIN-2](#API-VIN-2) The API shall have a command to delete VINs
   * ~~[API-VIN-3](#API-VIN-3) The API shall have a call to search for and return VINs using regexp wildcards.~~ Duplicate of API-VIN-7 - 8
   * [API-VIN-7](#API-VIN-7) The API shall have a VIN search command
   * [API-VIN-8](#API-VIN-8) The search command shall support a VIN regexp to match against
   * [API-PACKAGE-1](#API-PACKAGE-1) The API shall have a command to upload packages to the system
   * [API-PACKAGE-2](#API-PACKAGE-2) Each package shall have an ID string specified
   * [API-PACKAGE-3](#API-PACKAGE-3) Each package shall have a version number specified so that ID string plus version number creates a unique queue.
   * [API-PACKAGE-5](#API-PACKAGE-5) The API shall have a command to list search for software packages
   * [API-PACKAGE-6](#API-PACKAGE-6) The search command shall support regexp matching for the ID string and the version number
   * [API-QUEUE-1](#API-QUEUE-1) The API shall have a command to request that an package is to be installed
   * ~~[API-QUEUE-2](#API-QUEUE-2) The install command shall provide a filter label to be applied to the request. Not necessary with external resolver~~
   * [API-QUEUE-3](#API-QUEUE-3) The install command shall return a unique install ID for the install request
   * [API-QUEUE-17](#API-QUEUE-17) "The API shall have a command to queue an ""Get All Installed Packages"" command for a given VIN"
   * [WEB-1](#WEB-1) The web system shall act as a front end toward the SOTA system
   * [WEB-2](#WEB-2) The web system shall use the Web  API of the SOTA system
   * [WEB-VIN-1](#WEB-VIN-1) The web system  shall have a UI to add VINs
   * [WEB-VIN-18](#WEB-VIN-18) A VIN added, deleted, or modified by the web system shall update both the server and the external resolver
   * [WEB-PACKAGE-1](#WEB-PACKAGE-1) The web system shall have a UI to upload packages to the system.
   * [WEB-PACKAGE-2](#WEB-PACKAGE-2) The upload screen shall have a software package ID string
   * [WEB-PACKAGE-3](#WEB-PACKAGE-3) The upload screen shall have a software version 
   * [WEB-PACKAGE-4](#WEB-PACKAGE-4) The upload screen shall have a description
   * [WEB-PACKAGE-5](#WEB-PACKAGE-5) The upload screen shall have a vendor
   * [WEB-PACKAGE-7](#WEB-PACKAGE-7) The web system shall have a UI to search for software packages
   * [WEB-PACKAGE-8](#WEB-PACKAGE-8) The search command shall support regexp matching for the ID string and the version number
   * [WEB-PACKAGE-9](#WEB-PACKAGE-9) Each software package in the returned search result list shall be clickable
   * [WEB-PACKAGE-10](#WEB-PACKAGE-10) Clicking on an package from the search result shall bring up a property screen for the package
   * [WEB-PACKAGE-11](#WEB-PACKAGE-11) The package property screen shall show the package ID string
   * [WEB-PACKAGE-12](#WEB-PACKAGE-12) The package property screen shall show the version number
   * [WEB-PACKAGE-13](#WEB-PACKAGE-13) The package property screen shall show the description
   * [WEB-PACKAGE-14](#WEB-PACKAGE-14) The package property screen shall show the vendor
   * [WEB-PACKAGE-19](#WEB-PACKAGE-19) A package added, deleted, or modified by the web system shall update both the server and the external resolver
   * [WEB-PACKAGE-20](#WEB-PACKAGE-20) The package property screen shall have a button to list all filters that will be executed when the package is resolved to VINs. "Will queue a ""Get All Installed Packages"" command to the given VIN, using API-QUEUE-17"
   * [WEB-QUEUE-1](#WEB-QUEUE-1) The web system shall have a user interface for creating an update to be pushed to one or more VINs
   * [WEB-QUEUE-2](#WEB-QUEUE-2) The create update screen shall specify the software package and version to push
   * ~~[WEB-QUEUE-3](#WEB-QUEUE-3) The create update screen shall specify the filter tag to apply.~~ Not applicable with external resolver
   * [WEB-QUEUE-7](#WEB-QUEUE-7) The create update screen shall have a button to contact external resolver and list all VINs that would receive the update. Will invoke external resolver to map package ID to VINs
   * [WEB-QUEUE-8](#WEB-QUEUE-8) The web system shall have a user interface to list all created updates in the system
   * [WEB-QUEUE-9](#WEB-QUEUE-9) Each listed update shall be shown with its software package and filter label
   * [WEB-QUEUE-10](#WEB-QUEUE-10) Each listed update shall be clickable
   * [WEB-QUEUE-11](#WEB-QUEUE-11) Clicking on the update shall bring up the update property screen
   * [WEB-QUEUE-12](#WEB-QUEUE-12) The update property screen shall show the information provided by WEB-QUEUE-[2-6]
   * [WEB-QUEUE-13](#WEB-QUEUE-13) The update property screen shall show the total number of VINs targeted by the update
   * [WEB-QUEUE-19](#WEB-QUEUE-19) The update property screen shall be able to list all VINs that are still waiting to recveive the update
   * [WEB-QUEUE-20](#WEB-QUEUE-20) Each VIN listed in WEB-QUEUE-[17-19] shall be clickable
   * [WEB-RESOLV-FILTER-1](#WEB-RESOLV-FILTER-1) The web system shall have a user interface for adding install filters on the external resolver
   * [WEB-RESOLV-FILTER-2](#WEB-RESOLV-FILTER-2) The add install filter screen shall have a filter label
   * [WEB-RESOLV-FILTER-3](#WEB-RESOLV-FILTER-3) The add install filter screen shall have a text field for a boolean expression
   * [WEB-RESOLV-FILTER-4](#WEB-RESOLV-FILTER-4) The add install filter screen shall have a button to syntax check the boolean expression
   * [WEB-RESOLV-FILTER-5](#WEB-RESOLV-FILTER-5) In case the syntax check fails, an error code and text should be showed
   * [WEB-RESOLV-FILTER-6](#WEB-RESOLV-FILTER-6) The web system shall have a button to list all filters on the external resolver
   * [WEB-RESOLV-FILTER-7](#WEB-RESOLV-FILTER-7) Each filter returned in the list result shall be clicklable
   * [WEB-RESOLV-FILTER-8](#WEB-RESOLV-FILTER-8) Clicking on a filter in the list result shall bring up the filter property screen retrieved from the external resolver
   * [WEB-RESOLV-FILTER-9](#WEB-RESOLV-FILTER-9) The property screen shall be able to edit all filter properties
   * [WEB-RESOLV-FILTER-10](#WEB-RESOLV-FILTER-10) The property screen shall support syntax checking of changed boolean expression
   * [WEB-RESOLV-FILTER-11](#WEB-RESOLV-FILTER-11) "The property screen shall have a ""delete filter"" to remove a filter from the external resolver"
   * [WEB-RESOLV-FILTER-12](#WEB-RESOLV-FILTER-12) "The property screen shall have a ""list associated packages"" to list all packages that will have the filter executed when resolved"
   * [WEB-RESOLV-FILTER-13](#WEB-RESOLV-FILTER-13) The property screen shall be able to add a filter to a package. The filter(s) associated with a package will be run over all VINs when the given package is resolved.
   * [WEB-RESOLV-FILTER-14](#WEB-RESOLV-FILTER-14) The property screen shall be able to delete a filter from a package
   * [EXT-RESOLV-1](#EXT-RESOLV-1) The system shall rely on an external system, the Resolver, to translate a software package to VINs that are to have the package installed.
   * [EXT-RESOLV-2](#EXT-RESOLV-2) The resolver shall have a server-side WebAPI to handle resolve requests.
   * [EXT-RESOLV-3](#EXT-RESOLV-3) A resolve request, sent to the external resolver by the system, shall contain a software package ID string.
   * [EXT-RESOLV-4](#EXT-RESOLV-4) A resolve request shall return a list of zero or more VIN numbers that the sofware package should be installed on
   * [EXT-RESOLV-API-1](#EXT-RESOLV-API-1) The Resolver shall support an API, allowing its functionality to be accessed by external apps and services
   * [EXT-RESOLV-API-2](#EXT-RESOLV-API-2) The API shall be based on Restful HTTP with JSON.bodies
   * [EXT-RESOLV-FILTER-1](#EXT-RESOLV-FILTER-1) A resolve request shall retrieve the VINs to install an package on by executing one or more filters
   * [EXT-RESOLV-FILTER-2](#EXT-RESOLV-FILTER-2) A single filter shall be associated with zero or more software package IDs
   * [EXT-RESOLV-FILTER-3](#EXT-RESOLV-FILTER-3) The software package ID string of a resolve request shall be used retrieve the all filters associated with the package ID.
   * [EXT-RESOLV-FILTER-4](#EXT-RESOLV-FILTER-4) Each filter retrieved for an package ID in a resolve request shall be run on all VINs in order to filter out those VINs that should receive the update. All filters are AND-ed together.
   * [EXT-RESOLV-FILTER-5](#EXT-RESOLV-FILTER-5) Only VINs that pass all filters associated with the software package ID shall be returned by the resolve request
   * [EXT-RESOLV-FILTER-6](#EXT-RESOLV-FILTER-6) The filter shall specify a boolean expression that has to be true for a specific VIN in order for the software package to be queued to that VIN.
   * [EXT-RESOLV-FILTER-12](#EXT-RESOLV-FILTER-12) The VIN operand shall support regexp matching
   * [EXT-RESOLV-FILTER-13](#EXT-RESOLV-FILTER-13) The finished boolean expression shall be labeled and stored as a named, reusable filter
   * [EXT-RESOLV-FILTER-API-1](#EXT-RESOLV-FILTER-API-1) The API shall have a command to add new filters
   * [EXT-RESOLV-FILTER-API-2](#EXT-RESOLV-FILTER-API-2) The added filter shall have a boolean expression
   * [EXT-RESOLV-FILTER-API-3](#EXT-RESOLV-FILTER-API-3) The added filter shall have a unique label
   * [EXT-RESOLV-FILTER-API-4](#EXT-RESOLV-FILTER-API-4) The API shall havea a command to check the syntax of a boolean expression
   * [EXT-RESOLV-FILTER-API-5](#EXT-RESOLV-FILTER-API-5) The syntax check shall return ok or an error code and text 
   * [EXT-RESOLV-FILTER-API-7](#EXT-RESOLV-FILTER-API-7) The API shall have a command to associate a package ID string to a filter. Used during resolve package -> VINs to figure out which filter to apply to th egiven VINs
   * [EXT-RESOLV-FILTER-API-8](#EXT-RESOLV-FILTER-API-8) The API shall have a command to dis-associate a package ID string from a filter
   * [EXT-RESOLV-FILTER-API-9](#EXT-RESOLV-FILTER-API-9) The API shall have a command to list all filters associated with a package ID string
   * [EXT-RESOLV-FILTER-API-10](#EXT-RESOLV-FILTER-API-10) The  filters associated with the given package ID shall have their filter labels returned
   * [EXT-RESOLV-FILTER-API-11](#EXT-RESOLV-FILTER-API-11) The API shall have a command to list all packages associated with a filter label.
   * [EXT-RESOLV-FILTER-API-12](#EXT-RESOLV-FILTER-API-12) The packages associated with the given filter label shall have their package ID strings returned.
   * [EXT-RESOLV-VIN-2](#EXT-RESOLV-VIN-2) A VIN shall use a VIN number as its primary identifier
   * [EXT-RESOLV-VIN-API-1](#EXT-RESOLV-VIN-API-1) The API shall have a command to add VINs
   * [EXT-RESOLV-VIN-API-2](#EXT-RESOLV-VIN-API-2) The API shall have a command to delete VINs
   * [EXT-RESOLV-PACKAGE-2](#EXT-RESOLV-PACKAGE-2) A software package shall have an ID string
   * [EXT-RESOLV-PACKAGE-3](#EXT-RESOLV-PACKAGE-3) A software package shall have a major.minor.patch formatted version number. The ID string plus version is the unique identifier of the package
   * [EXT-RESOLV-PACKAGE-4](#EXT-RESOLV-PACKAGE-4) A software package shall have a description
   * [EXT-RESOLV-PACKAGE-5](#EXT-RESOLV-PACKAGE-5) A software package shall have a vendor
   * [EXT-RESOLV-PACKAGE-API-1](#EXT-RESOLV-PACKAGE-API-1) The API shall have a command to specify a package to the external resolver
   * [EXT-RESOLV-PACKAGE-API-2](#EXT-RESOLV-PACKAGE-API-2) Each package shall have an ID string specified
   * [EXT-RESOLV-PACKAGE-API-3](#EXT-RESOLV-PACKAGE-API-3) Each software package shall have a major.minor.patch formatted version number. The ID string plus version is the unique identifier of the package
   * [EXT-RESOLV-PACKAGE-API-4](#EXT-RESOLV-PACKAGE-API-4) Each package shall have a version number specified so that ID string plus version number creates a unique queue.

### <a name="requirements-comp">[COMP](https://github.com/advancedtelematic/sota-server/wiki/Requirements#requirements-comp) - Adds component management, associating one ore more components with a specific VIN</a>

   * ~~[SRV-VIN-3](#SRV-VIN-3) A VIN shall be able to ber associated to up to 1000 components. Each component is a potential target for software images~~
   * [SRV-PACKAGE-6](#SRV-PACKAGE-6) The software ID string shall support regexp matching when searching
   * [SRV-PACKAGE-7](#SRV-PACKAGE-7) The software version number shall support regexp matching when searching
   * ~~[API-VIN-4](#API-VIN-4) The API shall have a call to associate a component to an existing VIN. Moved to Resolver~~
   * ~~[API-VIN-9](#API-VIN-9) The VINs returned by the search shall each have their associated components listed.~~ Moved to Resolver
   * [WEB-VIN-7](#WEB-VIN-7) The VIN property screen shall list all components installed on the VIN, as retrieved from the external resolver
   * ~~[WEB-VIN-10](#WEB-VIN-10) The VIN property screen shall have a button for adding a component on the external Resolver.~~ Duplicate of WEB-RESOLV-COMP-1
   * ~~[WEB-VIN-11](#WEB-VIN-11) Adding a component shall specify the component part number.~~ Duplicate of WEB-RESOLV-COMP-1
   * [WEB-QUEUE-21](#WEB-QUEUE-21) Clicking on a VIN shall list all software packages and version included in the update for the given VIN
   * [WEB-RESOLV-COMP-1](#WEB-RESOLV-COMP-1) The web system  shall have a UI to add components to the external resolver using a component part number
   * [WEB-RESOLV-COMP-2](#WEB-RESOLV-COMP-2) The web system shall have a UI to delete components from the external resolver
   * [WEB-RESOLV-COMP-3](#WEB-RESOLV-COMP-3) The web system shall have a UI to search for components in the external resolver
   * [WEB-RESOLV-COMP-4](#WEB-RESOLV-COMP-4) The web system's components shall be searchable by part number regular expressions
   * [WEB-RESOLV-COMP-5](#WEB-RESOLV-COMP-5) The componens returned by a search shall be clickable
   * [WEB-RESOLV-COMP-6](#WEB-RESOLV-COMP-6) Clicking on a component from the search result shall bring up a property screen for the component
   * [WEB-RESOLV-COMP-7](#WEB-RESOLV-COMP-7) The component property screen shall show the part number
   * [WEB-RESOLV-COMP-8](#WEB-RESOLV-COMP-8) The component property screen shall show the description
   * [WEB-RESOLV-COMP-9](#WEB-RESOLV-COMP-9) The component property screen shall have a button to list all VINs that the component is installed on
   * [WEB-RESOLV-COMP-10](#WEB-RESOLV-COMP-10) The web system shall have a UI to add a component to a specific VIN using the external resolver.
   * [EXT-RESOLV-FILTER-7](#EXT-RESOLV-FILTER-7) The boolean expression shall have operands that identify specific components by their part number
   * [EXT-RESOLV-FILTER-8](#EXT-RESOLV-FILTER-8) The component part number in the expression shall support regexp matching
   * [EXT-RESOLV-FILTER-API-6](#EXT-RESOLV-FILTER-API-6) The API shall have a command to delete filters identified by their label
   * [EXT-RESOLV-VIN-3](#EXT-RESOLV-VIN-3) A VIN shall be able to ber associated to up to 1000 components
   * [EXT-RESOLV-VIN-API-3](#EXT-RESOLV-VIN-API-3) The API shall have a call to specify a component has been installed in an VIN
   * [EXT-RESOLV-COMP-1](#EXT-RESOLV-COMP-1) The resolver system shall manage up to 1 million components. Used by filtering process to require that specific components are installed in order for a package to be installed
   * [EXT-RESOLV-COMP-2](#EXT-RESOLV-COMP-2) A component has a part number. Main identifier used by software packages.
   * [EXT-RESOLV-COMP-3](#EXT-RESOLV-COMP-3) A component has a description
   * [EXT-RESOLV-COMP-4](#EXT-RESOLV-COMP-4) A component can have one or more VINs associated with it. Each VIN has zero or more components installed on it.
   * [EXT-RESOLV-COMP-API-1](#EXT-RESOLV-COMP-API-1) The API shall have a command to add a component to the system with a part number
   * [EXT-RESOLV-COMP-API-2](#EXT-RESOLV-COMP-API-2) The API shall have a command to specify that a component is installed on a specific VIN. "Will make has_component(""xxx"") return true in a filter run over the given VIN."
   * [EXT-RESOLV-COMP-API-3](#EXT-RESOLV-COMP-API-3) The API shall have a command to search for and return components using regexp wildcards
   * [EXT-RESOLV-COMP-API-4](#EXT-RESOLV-COMP-API-4) The returned components shall be listed with their part number
   * [EXT-RESOLV-COMP-API-5](#EXT-RESOLV-COMP-API-5) The returned components shall be listed with their descriptions
   * [EXT-RESOLV-COMP-API-6](#EXT-RESOLV-COMP-API-6) The API shall have a command to list all VINs that a component is installed on

### INST - Installation tracking to record which packages are currently installed on which VINs / components.

   * ~~[SRV-VIN-4](#SRV-VIN-4) Each component associated with a VIN shall have a reference to its currently installed software image.~~
   * ~~[SRV-VIN-5](#SRV-VIN-5) The component reference to the software image shall be the ID string and version number~~
   * ~~[SRV-PACKAGE-8](#SRV-PACKAGE-8) Each software package shall be maintain a list of all VINs it is installed on~~
   * [SRV-PACKAGE-QUEUE-20](#SRV-PACKAGE-QUEUE-20) The system shall use EXT-RESOLV-PACKAGE-API to update the resolver with packages installed and removed from each VIN targeted by an update, as reported back by the device. Allows resolver to keep track of which packages are installed on which VIN.
   * [SRV-PACKAGE-QUEUE-21](#SRV-PACKAGE-QUEUE-21) "The system shall be able to queue a ""Get All Installed Packages"" command to a device in order to retrieve its currently installed packages". Used to synchronize Resolver's list of installed packages on a VIN with reality
   * [SRV-PACKAGE-QUEUE-22](#SRV-PACKAGE-QUEUE-22) "When a ""Get All Installed Packages"" result is received from a device, the EXT-RESOLV-PACKAGE-API shall be used to reset the resolver's list of installed packages for the given VIN."
   * ~~[SRV-PACKAGE-TRANSMIT-7](#SRV-PACKAGE-TRANSMIT-7) Once installed on a VIN, an installation acknolwedgement shall be sent back to the SOTA server~~
   * [SRV-PACKAGE-TRANSMIT-8](#SRV-PACKAGE-TRANSMIT-8) The installation acknowledgement shall be used to update the association between a VINs components and their installed software packages and versions
   * [SRV-PACKAGE-TRANSMIT-9](#SRV-PACKAGE-TRANSMIT-9) In case of an installation failure, there shall be an error code and error text returned to the SOTA server. Executes SRC-PACKAGE-QUEUE-22
   * [SRV-PACKAGE-TRANSMIT-23](#SRV-PACKAGE-TRANSMIT-23) "The ""Finalize Download"" command shall contain a download index."
   * [SRV-PACKAGE-TRANSMIT-24](#SRV-PACKAGE-TRANSMIT-24) "The Server shall support an incoming ""Installation report"" command sent by the device"
   * [SRV-PACKAGE-TRANSMIT-25](#SRV-PACKAGE-TRANSMIT-25) "The ""Installation Report"" shall contain a package ID, a status code indicating success or failure, the currently running version of the package, and a descriptive text of the outcome.". Forwarded by SOTA Server to external resolver so that it can keep track of which packages are installed on which VINs
   * [SRV-PACKAGE-TRANSMIT-26](#SRV-PACKAGE-TRANSMIT-26) The Server shall forward the Installation report to the external resolver. As specified by SRV-PACKAGE-QUEUE-20
   * [SRV-PACKAGE-TRANSMIT-29](#SRV-PACKAGE-TRANSMIT-29) An aborted download shall be reported to thee external resolver
   * [SRV-PACKAGE-TRANSMIT-30](#SRV-PACKAGE-TRANSMIT-30) "An aborted download shall trigger a ""Abort Download"" command being sent to the device"
   * [SRV-PACKAGE-TRANSMIT-31](#SRV-PACKAGE-TRANSMIT-31) "An ""Abort Download"" command shall contain the download index of the failed download". Either the device receives it and cancels the download, or the device will time out the download and cancel it.
   * [DEV-1](#DEV-1) *FIRST* The device shall receive and process wakeup / shoulder tap SMS. Please see Appendix B, Use Cases DEV_*, TRANSFER_*, and INSTALL_* for a detailed description of protocol flow.
   * ~~[DEV-11](#DEV-11) The device shall report installation success back to the SOTA server. Forwarded by SOTA Server to external resolver so that it can maintain a list of currently installed packages.~~
   * [DEV-12](#DEV-12) The device shall report installation failure back to the SOTA server. Installa
   * [DEV-13](#DEV-13) In case of installation failure, the device shall report an error code and an error text back to the server
   * [DEV-14](#DEV-14) "The device shall support a ""Get currently installed packages command"" (GetCurrentPackages)". Needed sync up a mismatch between a device's view of installed packages and that of the backend server.
   * [DEV-15](#DEV-15) When a GetCurrentPackages command is received, the device shall report back a list of currently installed packages
   * [DEV-16](#DEV-16) Each package in a report shall be identified by its package ID string and version number
   * [DEV-17](#DEV-17) There shall be a resend attempt in case reporting of package installation results or currently installed packages fails
   * [DEV-18](#DEV-18) The device shall, when it connects to the SOTA server, validate the authenticity of the SOTA server. Both client and server side validation are needed.
   * [DEV-28](#DEV-28) The device shall be able to report locally changed software packages to the SOTA Server
   * [DEV-29](#DEV-29) The device shall receive information about locally changed packages through a DBUS command
   * [DEV-30](#DEV-30) The report shall contain the package ID, timestamp, and operation (install, upgrade, remove) carried out locally.
   * [DEV-54](#DEV-54) "The device shall support an incoming DBUS ""Installation Report' command from the local GSLM."
   * [DEV-55](#DEV-55) "The ""Installation Report"" shall contain a package ID, a status code indicating success or failure, the currently running version of the package, and a descriptive text of the outcome.". The running version can either be the new version, the existing version, or a reverted factory version.
   * [DEV-56](#DEV-56) The device shall forward the installation report to the SOTA Server. SOTA Server will forward it to the external resolver, allowing it to maintain its database of installed packages.
   * [DEV-57](#DEV-57) "If no additional ""Package Chunks"" are received for an ongoing download within a given timeout period, the download shall abort"
   * [DEV-58](#DEV-58) "If a ""Start Download"" command is received with a download index equal to that of an ongoing download, the ongoing download shall be aborted to make way for the new download.". Allows timed out downloads to be restarted.
   * [DEV-59](#DEV-59) "The device shall support an incoming ""Abort Download"" command "
   * [DEV-60](#DEV-60) "The ""Abort Download"" command shall contain the download index"
   * [DEV-61](#DEV-61) An aborted download shall delete any stored data on the device.
   * [DEV-62](#DEV-62) "If the download index of an ""Abort Download"" command cannot be found, the command shall silently be ignored.". """Start Download"" command was lost and never received by client"
   * ~~[API-VIN-5](#API-VIN-5) The API shall have a call to associate an software image to a VIN. Moved to Resolver~~
   * ~~[API-VIN-6](#API-VIN-6) The software image associated with a VIN shall be associated with a specific component installed on that VIN. Removed association between package and specific component. Packages are now generically installed on a VIN without component association.~~
   * [API-VIN-10](#API-VIN-10) The VINs returned by the search shall each have their associated installed software packages listed
   * [API-VIN-11](#API-VIN-11) The API shall have a command to list all historic package updates sent to a VIN since the VIN was created
   * [API-VIN-12](#API-VIN-12) Each update returned by a historic list command shall contain a result code reflecting success or failure of installing the package
   * [API-VIN-13](#API-VIN-13) Each update returned by a historic list command shall contain all dependent-upon packages transmitted with the original package in order to satisfy all dependencies of the installed package
   * [API-VIN-14](#API-VIN-14) Each update returned by a historic list command shall contain a time stamp of when the update completed or failed
   * ~~[API-PACKAGE-7](#API-PACKAGE-7) The API shall have a command to list all the VINs that a specific version of a software package is installed on.~~ Database of which packages are installed on which VIN now handled by resolver
   * ~~[API-PACKAGE-8](#API-PACKAGE-8) The API shall have a command to list all the VINs that a specific version of a software package is queued for installation on.~~ Duplicate of API-QUEUE-7
   * ~~[WEB-VIN-8](#WEB-VIN-8) Each component on a VIN property screen shall be listed with its currently installed software image and version.~~ Packages no longer associated with target components on a VIN.
   * [WEB-VIN-9](#WEB-VIN-9) The VIN property screen shall list all installed software packages (including dependencies), as retrieved from the external resolver
   * [WEB-VIN-12](#WEB-VIN-12) The VIN property screen shall have a button for adding a (manually installed) software package on a VIN. API Call sent to the Resolver
   * [WEB-VIN-13](#WEB-VIN-13) The software package added to the system shall be specified with a ID string
   * [WEB-VIN-14](#WEB-VIN-14) The software package added to the system shall be specified with a version number
   * [WEB-VIN-15](#WEB-VIN-15) The software package added to the system shall be specified with a description
   * ~~[WEB-VIN-16](#WEB-VIN-16) The software package shall be assocaited with a component installed on the VIN.~~ Packages no longer associated with target components on a VIN.
   * [WEB-VIN-17](#WEB-VIN-17) The VIN property screen shall have a button to list all software packages currently queued to it
   * [WEB-VIN-19](#WEB-VIN-19) The VIN property screen shall have a button to re-synchronize the list of installed packages with those actually installed on device
   * [WEB-PACKAGE-16](#WEB-PACKAGE-16) The package property screen shall have a button to list all VINs that the package is installed on. Interfaces resolver to retrieve lsit
   * [WEB-PACKAGE-17](#WEB-PACKAGE-17) Clicking on the installed VIN  button shall bring up a list of all VINs with the package installed
   * [WEB-PACKAGE-18](#WEB-PACKAGE-18) The package property screen shall have a button to list all VINs that the package is queued for
   * [WEB-QUEUE-14](#WEB-QUEUE-14) The update property screen shall show the total number of VINs that have had the update successfully installed
   * [WEB-QUEUE-15](#WEB-QUEUE-15) The update property screen shall show the total number of VINs that have failed to have the update installed
   * [WEB-QUEUE-16](#WEB-QUEUE-16) The update property screen shall show the total number of VINs that are still waiting to receive the update
   * [WEB-QUEUE-17](#WEB-QUEUE-17) The update property screen shall be able to list all VINs that have had the update succsessfully installed
   * [WEB-QUEUE-18](#WEB-QUEUE-18) The update property screen shall be able to list all VINs that failed to have the update installed
   * [EXT-RESOLV-FILTER-9](#EXT-RESOLV-FILTER-9) The boolean expression shall have operands that identify the currently installed packages packages.
   * [EXT-RESOLV-FILTER-10](#EXT-RESOLV-FILTER-10) The currently installed package is identified by its ID string and version number
   * [EXT-RESOLV-FILTER-11](#EXT-RESOLV-FILTER-11) The ID string and version number of the currently installed package shall support regexp matching
   * [EXT-RESOLV-VIN-4](#EXT-RESOLV-VIN-4) A VIN shall be able to handle up to 5000 installed software packages
   * [EXT-RESOLV-PACKAGE-6](#EXT-RESOLV-PACKAGE-6) Each software package shall be maintain a list of all VINs it is installed on. Used by filter has_package() operand
   * [EXT-RESOLV-PACKAGE-API-5](#EXT-RESOLV-PACKAGE-API-5) The resolver shall have a command to specify that a given package has been installed on a VIN. Called by the SOTA Server when a device reports that an update has been installed.
   * [EXT-RESOLV-PACKAGE-API-6](#EXT-RESOLV-PACKAGE-API-6) The resolver shall have a command to specify that a given package has been deleted from a VIN. Called by the SOTA Server when a device reports that an update has been updated/removed
   * [EXT-RESOLV-PACKAGE-API-7](#EXT-RESOLV-PACKAGE-API-7) The resolver shall have a command to search for all packages installed on a VIN
   * [EXT-RESOLV-PACKAGE-API-8](#EXT-RESOLV-PACKAGE-API-8) The resolver shall have a command to search for all VINs that a package is installed on.

### <a name="requirements-deps">[DEPS](https://github.com/advancedtelematic/sota-server/wiki/Requirements#requirements-deps) Package dependency tracking to create a per-vehicle update with necessary packages</a>

   * [SRV-PACKAGE-QUEUE-9](#SRV-PACKAGE-QUEUE-9) Each VIN targeted by a queued update shall maintain a list of packages that are rolled into the update for that specvific vin. All packages to be added to original package in order to satisfy dependencies are provided by EXT-RESOLV
   * ~~[SRV-PACKAGE-DEPS-1](#SRV-PACKAGE-DEPS-1) Each VIN, as returned by the external resolver, shall be have a dependency check done for the package that is to be installed~~
   * ~~[SRV-PACKAGE-DEPS-2](#SRV-PACKAGE-DEPS-2) The depency check shall compare the list of packages already installed on the VIN with the dependency graph of the new package to be installed. Which packages does the package about to be installed need on this specific VIN in order to function.~~
   * ~~[SRV-PACKAGE-DEPS-3](#SRV-PACKAGE-DEPS-3) If an dependent package, required by the package to be installed, is currently not installed on the VIN, the required package will be added to the update for that specific VIN.~~
   * ~~[SRV-PACKAGE-DEPS-4](#SRV-PACKAGE-DEPS-4) If installing one or more of the packages in an update on a VIN would break dependencies for packages already installed on that VIN, the update shall fail for the given VIN and be reported back to the server~~
   * ~~[SRV-PACKAGE-DEPS-5](#SRV-PACKAGE-DEPS-5) A software package shall be dependent on up to 100 other software packages~~
   * ~~[SRV-PACKAGE-DEPS-6](#SRV-PACKAGE-DEPS-6) Software package depencies shall form a graph of sub dependencies. A requires B, which requires C and D.~~
   * ~~[SRV-PACKAGE-DEPS-7](#SRV-PACKAGE-DEPS-7) A dependency shall be identified by a software package ID string and a version number~~
   * ~~[API-PACKAGE-4](#API-PACKAGE-4) Each package shall have an optional list of dependencies specified.~~ Moved to resolver
   * ~~[API-PACKAGE-9](#API-PACKAGE-9) The API shall have a command to list the dependencies for a specific package.~~ Moved to resolver
   * [WEB-PACKAGE-6](#WEB-PACKAGE-6) The upload screen shall allow to specify dependencies on one or more exisiting software packages. Interfaces resolver to handle dependencies
   * [WEB-PACKAGE-15](#WEB-PACKAGE-15) The package property screen shall show all the software package dependencies the shown package has. Interfaces resolver to handle dependencies
   * [EXT-RESOLV-DEPS-1](#EXT-RESOLV-DEPS-1) Each VIN returned by a resolver for a specific package shall have a dependency check done for that package
   * [EXT-RESOLV-DEPS-2](#EXT-RESOLV-DEPS-2) The depency check shall compare the list of packages already installed on the VIN with the dependency graph of the new package to be installed
   * [EXT-RESOLV-DEPS-3](#EXT-RESOLV-DEPS-3) If an dependent package, required by the package to be installed, is currently not installed on the VIN, the required package will be provided with the given VIN when returned to the SOTA Server.
   * [EXT-RESOLV-DEPS-4](#EXT-RESOLV-DEPS-4) If installing one or more of the packages in an update on a VIN would break dependencies for packages already installed on that VIN, an error will be logged for the given update by the resolver and the VIN is removed from the set of VINs returned by the resolver
   * [EXT-RESOLV-DEPS-5](#EXT-RESOLV-DEPS-5) A software package shall be dependent on up to 100 other software packages
   * [EXT-RESOLV-DEPS-6](#EXT-RESOLV-DEPS-6) Software package depencies shall form a graph of sub dependencies.
   * [EXT-RESOLV-DEPS-7](#EXT-RESOLV-DEPS-7) A dependency shall be identified by a software package ID string and a version number
   * [EXT-RESOLV-DEPS-API-1](#EXT-RESOLV-DEPS-API-1) The resolver shall have a command to add a dependency from one package toward another
   * [EXT-RESOLV-DEPS-API-2](#EXT-RESOLV-DEPS-API-2) The resolver shall have a command to delete a dependency from one package toward another
   * [EXT-RESOLV-DEPS-API-3](#EXT-RESOLV-DEPS-API-3) The resolver shall have a command to list all dependencies of a package. Can be called recursively to get entire dependency graph

### <a name="requirements-sched">[SCHED](https://github.com/advancedtelematic/sota-server/wiki/Requirements#requirements-sched) Installation scheduling, allows for priorities of updates and install period windows.</a>

   * [SRV-PACKAGE-QUEUE-2](#SRV-PACKAGE-QUEUE-2) The request shall have an earliest start date. Do not install before 2016-01-01.
   * [SRV-PACKAGE-QUEUE-3](#SRV-PACKAGE-QUEUE-3) The request shall have a latest install completion date. Do not install after 2016-04-01
   * [SRV-PACKAGE-QUEUE-4](#SRV-PACKAGE-QUEUE-4) If a software package cannot be installed on one ore more targeted VINs within the specified period, they failed VINs shall be logged
   * [SRV-PACKAGE-QUEUE-5](#SRV-PACKAGE-QUEUE-5) The request shall have a priority from 1 to 100. Used when updates are queued to individual vehicles. See below
   * [SRV-PACKAGE-QUEUE-10](#SRV-PACKAGE-QUEUE-10) Updates queued for a specific VIN shall be sorted primarily on ascending request priority. Allows high-priority updates to skip the queue and be pushed out earlier to the vehicle
   * [API-QUEUE-4](#API-QUEUE-4) The API shall have a command to cancel a previously queued install request
   * [API-QUEUE-5](#API-QUEUE-5) Canceling an install request will delete any pending updates that have yet to be transmitted to their targeted VINs.
   * [API-QUEUE-6](#API-QUEUE-6) Canceling an install request shall not affect any packages already installed on their targeted VINs.
   * [API-QUEUE-7](#API-QUEUE-7) The API shall have a command to list all VINs targeted by a specific install request, identified by the install ID
   * [API-QUEUE-8](#API-QUEUE-8) The list command shall return all VINs which the install request was successfully completed on
   * [API-QUEUE-9](#API-QUEUE-9) The success report for a VIN shall include a date and time stamp.
   * [API-QUEUE-10](#API-QUEUE-10) The list command shall return all VINs for which the install request is still pending on the server
   * [API-QUEUE-11](#API-QUEUE-11) The list command shall return all VINs for which the install request failed
   * [API-QUEUE-12](#API-QUEUE-12) The failure report for a VIN shall include a date and time stramp.
   * [API-QUEUE-13](#API-QUEUE-13) The failure report for a VIN shall include a reason code such as time out, dependency failure, etc.
   * [API-QUEUE-14](#API-QUEUE-14) The failure report for a VIN shall include a reason text.
   * [API-QUEUE-15](#API-QUEUE-15) The list command shall, for each returned VIN, list the software packages included in the update, including dependencies
   * [API-QUEUE-16](#API-QUEUE-16) The list command shall return all VINs for which the install request has started transmission, but has not yet completed
   * [WEB-VIN-2](#WEB-VIN-2) The web system shall have a UI to delete VINs
   * [WEB-VIN-3](#WEB-VIN-3) The web system shall have a UI to search for VINs
   * [WEB-VIN-4](#WEB-VIN-4) The web system's VINs shall be searchable by regular expressions
   * [WEB-VIN-5](#WEB-VIN-5) Each VIN by a search shall be clickable
   * [WEB-VIN-6](#WEB-VIN-6) Clicking on a VIN from the search result shall bring up a property screen for the VIN
   * [WEB-QUEUE-4](#WEB-QUEUE-4) The create update screen shall specify the earliest start date for the update to be installed
   * [WEB-QUEUE-5](#WEB-QUEUE-5) The create update screen shall specify the latest end date for the update to be installed
   * [WEB-QUEUE-6](#WEB-QUEUE-6) The create update screen shall specify a priority
   * [WEB-QUEUE-22](#WEB-QUEUE-22) "The update property screen shall have a ""cancel update"" button."
   * [WEB-QUEUE-23](#WEB-QUEUE-23) "Clicking on the ""cancel update"" button shall cancel any updates to VINs that are not yet complete"
   * [WEB-QUEUE-24](#WEB-QUEUE-24) "Clicking on the ""cancel update"" button shall delete the update itself."

## <a name="requirements-shape">[SHAPE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#requirements-shape) Traffic shaping allowing the integration of data plans and billing cycles to avoid data overrun costs.</a>

   * [SRV-VIN-7](#SRV-VIN-7) The VIN shall be associated with one data plan ID. Ties the VIN to a data plan, allowing us to control traffic to it
   * [SRV-PACKAGE-QUEUE-12](#SRV-PACKAGE-QUEUE-12) The software package install request shall have a data pool usage threshold
   * [SRV-PACKAGE-QUEUE-13](#SRV-PACKAGE-QUEUE-13) The data plan usage threshold shall be specified as a decimal percentage
   * [SRV-PACKAGE-QUEUE-14](#SRV-PACKAGE-QUEUE-14) The data plan usage threshold shall specify the maximum percentage of the data pool assigned to a VIN that can be used when the package transfer starts.
   * [SRV-PACKAGE-QUEUE-15](#SRV-PACKAGE-QUEUE-15) If the data pool associated with a targeted VIN has a usage is greater than the specified threshold for the request, the update for the targeted VIN shall be rescheduled to the next billing cycle.
   * ~~[SRV-PACKAGE-QUEUE-16](#SRV-PACKAGE-QUEUE-16) Updates queued for a specific VIN shall have an individual earliest start date, forcing it to be transmitted within a specific billing cycle.~~ Duplicate of SRV-PACKAGE-QUEUE-2
   * [SRV-PACKAGE-QUEUE-17](#SRV-PACKAGE-QUEUE-17) The individual earliest start date shall not be later than the lastest install completion date specified in SRV-PACKAGE-QUEUE-3
   * [SRV-PACKAGE-QUEUE-18](#SRV-PACKAGE-QUEUE-18) If the update for a specific VIN cannot be rescheduled to a billing cycle before the specified latest install competion date, the update shall fail.
   * [SRV-TRAFFIC-SHAPING-1](#SRV-TRAFFIC-SHAPING-1) The SOTA server shall be manage data plans used to control when updates are to be sent to their targeted VINs
   * [SRV-TRAFFIC-SHAPING-2](#SRV-TRAFFIC-SHAPING-2) Up to 1,000 data plans shall be managed by the SOTA server
   * [SRV-TRAFFIC-SHAPING-3](#SRV-TRAFFIC-SHAPING-3) A  data plan shall specify a system-wide unique a data plan ID
   * [SRV-TRAFFIC-SHAPING-4](#SRV-TRAFFIC-SHAPING-4) A single data plan profile shall manage up to 1000 billing cycles . One week billing cycles x 1000 is 20 years of billing
   * [SRV-TRAFFIC-SHAPING-5](#SRV-TRAFFIC-SHAPING-5) The data plan shall specify if the data pool for each billing cycle is per VIN, or if it is shared across all VINs associated with the profile. Removed until further notice. For now all billing cycles will be pooled across all VINs
   * [SRV-TRAFFIC-SHAPING-6](#SRV-TRAFFIC-SHAPING-6) Each billing cycle shall specify a date and time stamp when it starts
   * [SRV-TRAFFIC-SHAPING-7](#SRV-TRAFFIC-SHAPING-7) Each billing cycle shall specify a data pool size in kilobytes
   * [SRV-TRAFFIC-SHAPING-8](#SRV-TRAFFIC-SHAPING-8) A billing cycle shall become active when the start date/time stamp occurrs. 
   * [SRV-TRAFFIC-SHAPING-9](#SRV-TRAFFIC-SHAPING-9) A billing cycle shall be deactivated when the next consecutive billing cycle is activated.
   * [SRV-TRAFFIC-SHAPING-10](#SRV-TRAFFIC-SHAPING-10) The SOTA server shall be able to read data usage reports from an external source
   * [SRV-TRAFFIC-SHAPING-11](#SRV-TRAFFIC-SHAPING-11) The SOTA server shall deduct data usage from the pool of the currently active billing cycle
   * [SRV-TRAFFIC-SHAPING-12](#SRV-TRAFFIC-SHAPING-12) The SOTA server shall at all times know how data is left in a pool at any given time
   * [SRV-TRAFFIC-SHAPING-13](#SRV-TRAFFIC-SHAPING-13) When a billing cycle becomes deactive, it shall be archived
   * [SRV-TRAFFIC-SHAPING-14](#SRV-TRAFFIC-SHAPING-14) The architved billing cycle shall contain the number of bytes transmitted during the cycle
   * [SRV-TRAFFIC-SHAPING-15](#SRV-TRAFFIC-SHAPING-15) Each billing cycle under a data plan shall be shared across all VINs using the given plan. Replaces SRV-TRAFFIC-SHAPING-5
   * [API-TRAFFIC-SHAPING-1](#API-TRAFFIC-SHAPING-1) The API shall have a command to add a data plan
   * [API-TRAFFIC-SHAPING-2](#API-TRAFFIC-SHAPING-2) The added data plan shall have a unique data plan ID
   * ~~[API-TRAFFIC-SHAPING-3](#API-TRAFFIC-SHAPING-3) The added data plan shall specify if the billing cycles' data pools are shared across all VIN or is specified per VIN.~~ All plans will be pooled across all VINs for now
   * ~~[API-TRAFFIC-SHAPING-4](#API-TRAFFIC-SHAPING-4) The API shall have a command to delete an existing data plan and its billing cycles.~~ Not necessary at a first implementation
   * [API-TRAFFIC-SHAPING-5](#API-TRAFFIC-SHAPING-5) The API shall have a command to add a billing cycle to an existing data plan
   * [API-TRAFFIC-SHAPING-6](#API-TRAFFIC-SHAPING-6) The added billing cycle shall have a start date and time stamp
   * [API-TRAFFIC-SHAPING-7](#API-TRAFFIC-SHAPING-7) The added billing cycle shall have a data pool size, specified in kilobytes.
   * [API-TRAFFIC-SHAPING-8](#API-TRAFFIC-SHAPING-8) The billing cycle shall be identified by its associated data plan and start date/time stamp.
   * ~~[API-TRAFFIC-SHAPING-9](#API-TRAFFIC-SHAPING-9) The API shall have a command to delete an existing billing cycle.~~ Not necessary at a first implementation
   * [API-TRAFFIC-SHAPING-10](#API-TRAFFIC-SHAPING-10) The API shall have a command to add transmitted bytes to the currently active billing cycle of a specific data plan. Increases usage of the given billing cycle
   * [API-TRAFFIC-SHAPING-11](#API-TRAFFIC-SHAPING-11) The API shall have a command to retrieve the data pool size of the current billing cycle of a specific data plan
   * [API-TRAFFIC-SHAPING-12](#API-TRAFFIC-SHAPING-12) The API shall have a command to retrieve the number of used bytes in the current billing cycle of a specific data plan
   * [API-TRAFFIC-SHAPING-13](#API-TRAFFIC-SHAPING-13) The API shall have a command to list all billing cycles created under a data plan
   * [WEB-TRAFFIC-SHAPING-1](#WEB-TRAFFIC-SHAPING-1) The web system shall have a user interface to add a data plan
   * [WEB-TRAFFIC-SHAPING-2](#WEB-TRAFFIC-SHAPING-2) The add data plan screen shall have a unique data plan ID
   * ~~[WEB-TRAFFIC-SHAPING-3](#WEB-TRAFFIC-SHAPING-3) The add data plan screen shall specify if the data pool size is per VIN or is shared across all participating VINs.~~ Not needed in a first implenentation
   * [WEB-TRAFFIC-SHAPING-4](#WEB-TRAFFIC-SHAPING-4) The add data plan shall have a command to delete an existing data plan and its billing cycles. Was previously erroneously removed instead of the line above.
   * [WEB-TRAFFIC-SHAPING-5](#WEB-TRAFFIC-SHAPING-5) The web system shall have a user interface to add billing cycles to a data plan
   * [WEB-TRAFFIC-SHAPING-6](#WEB-TRAFFIC-SHAPING-6) An added billing cycle shall be entered with a start date / time stamp
   * [WEB-TRAFFIC-SHAPING-7](#WEB-TRAFFIC-SHAPING-7) An added billing cycle shall be entered with a data pool size in kilobytes
   * [WEB-TRAFFIC-SHAPING-8](#WEB-TRAFFIC-SHAPING-8) The web system shall be able to list all data plans and their properties
   * [WEB-TRAFFIC-SHAPING-9](#WEB-TRAFFIC-SHAPING-9) The web system shall be able to list all billing cycles added to a data plan and their properties
   * ~~[WEB-TRAFFIC-SHAPING-10](#WEB-TRAFFIC-SHAPING-10) The web system shall be able to delete an existing billing cycle under a data plan.~~ Not needed in a first implementation
   * [WEB-TRAFFIC-SHAPING-11](#WEB-TRAFFIC-SHAPING-11) The web system shall be able to show the current data pool usage for an existing billing cycle
   * [WEB-TRAFFIC-SHAPING-12](#WEB-TRAFFIC-SHAPING-12) The web system shall be able to update the data pool usage for an existing billing cycle by setting a kilobyte value

## ~~<a name="requirements-scale">[SCALE](https://github.com/advancedtelematic/sota-server/wiki/Requirements#requirements-scale) Adds capacity for production deployment </a>~~

   * ~~[SRV-VIN-1](#SRV-VIN-1) The system shall manage up to 100 million VINs~~
   * ~~[SRV-VIN-6](#SRV-VIN-6) A VIN shall be able to manage up to 5000 installed software images~~
   * ~~[SRV-PACKAGE-1](#SRV-PACKAGE-1) The system shall manage up to 10 million software packages~~
   * ~~[WEB-3](#WEB-3) The web system shall have a provisioning system for adding users~~
   * ~~[WEB-4](#WEB-4) The web system shall have a provisioning system for deleting users~~
   * ~~[WEB-5](#WEB-5) Each user shall have a username~~
   * ~~[WEB-6](#WEB-6) Each user shall have a password~~
   * ~~[WEB-7](#WEB-7) The web system shall have a pre-configured admin user with a pre-configured password.~~
   * ~~[WEB-8](#WEB-8) Only the admin user shall be able to add and delete other users~~
   * ~~[WEB-9](#WEB-9) All users in the system shall have full access to all web functions, except add/delete users. For now. Different access levels will come later.~~
   * [EXT-RESOLV-VIN-1](#EXT-RESOLV-VIN-1) The resolver shall manage up to 100 million VINs
   * ~~[EXT-RESOLV-PACKAGE-1](#EXT-RESOLV-PACKAGE-1) The resolver shall manage up to 10 million software packages~~
   * ~~[SRV-CAPACITY-1](#SRV-CAPACITY-1) The system shall support a cold standby~~
   * ~~[SRV-CAPACITY-2](#SRV-CAPACITY-2) The system shall provide 99.9% uptime, yielding a maximum of 43.8 minutes downtime per month.~~
   * ~~[SRV-CAPACITY-3](#SRV-CAPACITY-3) The uptime includes maintenance, upgrades, backup, and other administrative routines~~
   * ~~[SRV-CAPACITY-4](#SRV-CAPACITY-4) The system shall handle a load capacity of 200 chunks of package data being transmitted per second to vehicles~~
   * ~~[SRV-CAPACITY-5](#SRV-CAPACITY-5) Each chunk shall be 100KBytes, rendering the total chunking bandwidth to 20MByte/Sec.~~
   * ~~[SRV-CAPACITY-6](#SRV-CAPACITY-6) The system shall queue at least 100 packages per seconds to their target VINs~~
   * ~~[SRV-CAPACITY-7](#SRV-CAPACITY-7) The target VINs shall be selected from a fleet of 1,000,000 VINs.~~
   * ~~[SRV-CAPACITY-8](#SRV-CAPACITY-8) The VINs filtered out by a queueing operation shall be 100,000, rendering the total queuing time to 1000 seconds for all targbeted VINs~~
   * ~~[SRV-CAPACITY-9](#SRV-CAPACITY-9) At no time shall transactional latency be greater than 500 msec.~~
   * ~~[SRV-CAPACITY-10](#SRV-CAPACITY-10) Transactional latency is defined as the number of milliseconds elasped from that the transaction was read from the NIC to the time the result was sent back over the NIC.~~
   * ~~[SRV-CAPACITY-11](#SRV-CAPACITY-11) A transaction is defined as a request sent from either a vehicle, an external system, or the Web UI to the system~~
   * ~~[SRV-CAPACITY-12](#SRV-CAPACITY-12) SRV-CAPCITY-4 to SRV-CAPACITY-11 shall be handled in parallel.~~
   * ~~[SRV-CAPACITY-13](#SRV-CAPACITY-13) SRV-CAPACITY-1 - 12 applies both to the server and the external resolver.~~
   * ~~[SRV-BACKUP-1](#SRV-BACKUP-1) The system shall have backup routines~~
   * ~~[SRV-BACKUP-2](#SRV-BACKUP-2) The backup shall be documented as a part of the maintenance instructions~~
   * ~~[SRV-BACKUP-3](#SRV-BACKUP-3) A freshly installed SOTA system with the backup applied shall render a system identical to the originally backed up system~~
   * ~~[SRV-BACKUP-4](#SRV-BACKUP-4) The backup system shall be able to selectively restore only VINs in both the external Resolver and the Server. Modifed wording to cover both resolver and system~~
   * ~~[SRV-BACKUP-5](#SRV-BACKUP-5) The backup system shall be able to selectively restore only components in both the external Resolver and the server. Modifed wording to cover both resolver and system~~
   * ~~[SRV-BACKUP-6](#SRV-BACKUP-6) The backup system shall be able to selectively restore only packages~~
   * ~~[SRV-BACKUP-7](#SRV-BACKUP-7) The backup system shall be able to selectively restore only the package queues and package transmission status~~
   * ~~[SRV-BACKUP-8](#SRV-BACKUP-8) The backup system shall be able to selectively restore only data plans and billing cycles~~
   * ~~[SRV-BACKUP-9](#SRV-BACKUP-9) The backup system shall be able to selectively restore only filters~~
   * ~~[SRV-BACKUP-10](#SRV-BACKUP-10) SRV-BACKUP-1 - 10  applies both to the server and the external resolver.~~
   * ~~[SRV-UPGRADE-1](#SRV-UPGRADE-1) The system shall be upgradable.~~
   * ~~[SRV-UPGRADE-2](#SRV-UPGRADE-2) The upgrade shall support a rollback to its previous state~~
   * ~~[SRV-UPGRADE-3](#SRV-UPGRADE-3) The upgrade shall be done with no uptime impact.~~
   * ~~[SRV-UPGRADE-4](#SRV-UPGRADE-4) The upgrade can be done with capacity degradation if, for example, one side of a cluster is upgraded at the time. Negative requirement. Removed.~~
   * ~~[SRV-UPGRADE-5](#SRV-UPGRADE-5) The capacity during an upgrade shall at all times stay above 40% of the whole system's capacity.~~
   * ~~[SRV-UPGRADE-6](#SRV-UPGRADE-6) SRV-UPGRADE-1 - 5  applies both to the server and the external resolver.~~
   * ~~[SRV-LOGGING-1](#SRV-LOGGING-1) The system shall support logging.~~
   * ~~[SRV-LOGGING-2](#SRV-LOGGING-2) Logging shall have at least 5 different log levels.~~
   * ~~[SRV-LOGGING-3](#SRV-LOGGING-3) Log levels shall be settable while the system is running~~
   * ~~[SRV-LOGGING-4](#SRV-LOGGING-4) Logs shall rotate so that they never consume more than a given amount of disk space~~
   * ~~[SRV-LOGGING-5](#SRV-LOGGING-5) The amount of disk space consumed by logs shall be settable while the system is running~~
   * ~~[SRV-LOGGING-6](#SRV-LOGGING-6) SRV-LOGGING-1 - 5  applies both to the server and the external resolver.~~
   * ~~[SRV-MON-1](#SRV-MON-1) The system shall be monitorable via SNMP, graphite, or similar open standard~~
   * ~~[SRV-MON-2](#SRV-MON-2) All SRV-MON requirements above and below applies both to the server and the extrernal resolver.~~
   * ~~[SRV-MON-ALARM-1](#SRV-MON-ALARM-1) Monitoring shall provide alarms~~
   * ~~[SRV-MON-ALARM-2](#SRV-MON-ALARM-2) Alarms shall be triggered by resource exhaustion~~
   * ~~[SRV-MON-ALARM-3](#SRV-MON-ALARM-3) Alarms shall be triggered by software component failures and restarts~~
   * ~~[SRV-MON-ALARM-4](#SRV-MON-ALARM-4) Alarms shall be triggered by excessive latency~~
   * ~~[SRV-MON-ALARM-5](#SRV-MON-ALARM-5) Alarms shall be triggered by failed external systems such as provisioning~~
   * ~~[SRV-MON-ALARM-6](#SRV-MON-ALARM-6) Alarms shall be triggered by hardware failures~~
   * ~~[SRV-MON-ALARM-7](#SRV-MON-ALARM-7) Alarms shall be manually acknolwedged by an operator action~~
   * ~~[SRV-MON-COUNT-1](#SRV-MON-COUNT-1) Monitoring shall provide counters~~
   * ~~[SRV-MON-COUNT-2](#SRV-MON-COUNT-2) Counters shall be persistent across system restarts~~
   * ~~[SRV-MON-COUNT-3](#SRV-MON-COUNT-3) Counters shall be kept for number of transactions processed by the system~~
   * ~~[SRV-MON-COUNT-4](#SRV-MON-COUNT-4) Counters shall be kept for number of packages sent to vehicles~~
   * ~~[SRV-MON-COUNT-5](#SRV-MON-COUNT-5) Counters shall be kept for number of kilobytes sent to vehicles~~
   * ~~[SRV-MON-COUNT-6](#SRV-MON-COUNT-6) Counters shall be kept for number of kilobytes received from vehicles~~
   * ~~[SRV-MON-COUNT-7](#SRV-MON-COUNT-7) Counters shall be kept for number of sessions from vehicles~~
   * ~~[SRV-MON-GAUGE-1](#SRV-MON-GAUGE-1) Monitoring shall provide gauges~~
   * ~~[SRV-MON-GAUGE-2](#SRV-MON-GAUGE-2) Each gauge shall provide an average value  over the last 10 seconds~~
   * ~~[SRV-MON-GAUGE-3](#SRV-MON-GAUGE-3) Each gauge shall provide an average value  over the last 60 seconds~~
   * ~~[SRV-MON-GAUGE-4](#SRV-MON-GAUGE-4) Each gauge shall provide an average value  over the last 600 seconds~~
   * ~~[SRV-MON-GAUGE-5](#SRV-MON-GAUGE-5) Each gauge shall provide an average value  over the last 3600 seconds~~
   * ~~[SRV-MON-GAUGE-6](#SRV-MON-GAUGE-6) Each gauge shall provide an average value  over the last 86400 seconds~~
   * ~~[SRV-MON-GAUGE-7](#SRV-MON-GAUGE-7) Each gauge shall provide a min and max measured value  over the last 10 seconds~~
   * ~~[SRV-MON-GAUGE-8](#SRV-MON-GAUGE-8) Each gauge shall provide a min and max measured value over the last 60 seconds~~
   * ~~[SRV-MON-GAUGE-9](#SRV-MON-GAUGE-9) Each gauge shall provide a min and max measured value over the last 600 seconds~~
   * ~~[SRV-MON-GAUGE-10](#SRV-MON-GAUGE-10) Each gauge shall provide a min and max measured value over the last 3600 seconds~~
   * ~~[SRV-MON-GAUGE-11](#SRV-MON-GAUGE-11) Each gauge shall provide a min and max measured value over the last 86400 seconds~~
   * ~~[SRV-MON-GAUGE-12](#SRV-MON-GAUGE-12) Monitoring shall gauge transactions per second~~
   * ~~[SRV-MON-GAUGE-13](#SRV-MON-GAUGE-13) Monitoring shall gauge transactional latency~~
   * ~~[SRV-MON-GAUGE-14](#SRV-MON-GAUGE-14) Monitoring shall gauge disk consumption~~
   * ~~[SRV-MON-GAUGE-15](#SRV-MON-GAUGE-15) Monitoring shall gauge virtual memory consumption~~
   * ~~[SRV-MON-GAUGE-16](#SRV-MON-GAUGE-16) Monitoring shall gauge bytes/second transmitted to all vehicles~~
   * ~~[SRV-MON-GAUGE-17](#SRV-MON-GAUGE-17) Monitoring shall gauge bytes/second transmitted from all vehicles~~

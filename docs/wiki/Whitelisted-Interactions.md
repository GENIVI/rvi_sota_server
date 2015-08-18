## <a name='TOC'>List of whitelisted interactions per boundary</a>

1. [B1](#b-1) Web Browser - Web Server
    - [WL-B1-1](#WL-B1-1) Web Server sends index.html page to Web Browser.
    - [WL-B1-2](#WL-B1-2) Web Server sends Javascript scripts linked in index.html to Web Browser.
    - [WL-B1-3](#WL-B1-3) Web Server sends CSS stylesheets linked in index.html to Web Browser.
    - [WL-B1-4](#WL-B1-4) Web Browser sends new VIN to Web Server.
    - [WL-B1-5](#WL-B1-5) Web Browser sends new campaign to Web Server.
    - [WL-B1-6](#WL-B1-6) Web Browser sends new filter data to Web Server.
    - [WL-B1-7](#WL-B1-7) Web Browser sends filter-to-package association to Web Server.
    - [WL-B1-8](#WL-B1-8) Web Browser sends Queue Package Request to Web Server.
    - [WL-B1-9](#WL-B1-9) Web Server sends login page and assets to Web Browser.
    - [WL-B1-10](#WL-B1-10) Web Browser sends login credentials to Web Server.
    - [WL-B1-11](#WL-B1-11) Web Server sends session cookie to Web Browser.
    - [WL-B1-12](#WL-B1-12) Web Browser sends session cookie to Web Server.
    - [WL-B1-13](#WL-B1-13) Web Server sends a redirection to login page for unauthenticated users.
    - [WL-B1-14](#WL-B1-14) Web Server sends a redirection back to the last requested page for authenticated users.
    - [WL-B1-15](#WL-B1-15) Web Browser sends a request for an Admin GUI resource to Web Server, along with a session cookie.
    - [WL-B1-16](#WL-B1-16) Web Server sends Admin GUI resource HTML and associated assets to Web Browser.
    - [WL-B1-17](#WL-B1-17) Web Server sends a redirection to the login page for unauthenticated users.
    - [WL-B1-18](#WL-B1-18) Web Browser sends a List Queue Package Request to Web Server.
    - [WL-B1-19](#WL-B1-19) Web Browser sends a GET Queued Request to Web Server.
    - [WL-B1-20](#WL-B1-20) Web Browser sends Search VINs Request to Web Server.
    - [WL-B1-21](#WL-B1-21) Web Browser sends the binary package and its metadata to Web Server.
    - [WL-B1-22](#WL-B1-22) Web Browser sends Search Filters Request to Web Server.
    - [WL-B1-23](#WL-B1-23) Web Browser sends List Filters Request to Web Server.
    - [WL-B1-24](#WL-B1-24) Web Browser sends Update Filter Request to Web Server.
    - [WL-B1-25](#WL-B1-25) Web Browser sends Delete Filter Request to Web Server.
    - [WL-B1-26](#WL-B1-26) Web Browser sends Updates Packages per VIN Request to Web Server.
    - [WL-B1-27](#WL-B1-27) Web Browser sends View Packages per VIN Request to Web Server.
    - [WL-B1-28](#WL-B1-28) Web Browser sends View VINs per Package Request to Web Server.
    - [WL-B1-29](#WL-B1-29) Web Browser sends new component data to Web Server.
    - [WL-B1-30](#WL-B1-20) Web Browser sends Search Component Request to Web Server.


2. [B2](#b-2) Web Server - SOTA Core
    - [WL-B2-1](#WL-B2-1) Web Server sends new package data to SOTA Core.
    - [WL-B2-2](#WL-B2-2) Web Server sends new VIN to SOTA Core.
    - [WL-B2-3](#WL-B2-3) Web Server sends new campaign data to SOTA Core.
    - [WL-B2-4](#WL-B2-4) Web Server sends Queue Package Request to SOTA Core.
    - [WL-B2-5](#WL-B2-5) Web Server sends List Queue Package Request to SOTA Core.
    - [WL-B2-6](#WL-B2-6) Web Server sends a GET Queued Request to SOTA Core.
    - [WL-B2-7](#WL-B2-7) Web Server sends Search VINs Request to SOTA Core.
    - [WL-B2-8](#WL-B2-8) Web Server sends the new package metadata to SOTA Core.
    - [WL-B2-9](#WL-B2-9) Web Server sends Updates Packages per VIN Request to SOTA Core.
    - [WL-B2-10](#WL-B2-10) Web Server sends View Packages per VIN Request to SOTA Core.
    - [WL-B2-11](#WL-B2-11) Web Server sends View VINs per Package Request to SOTA Core.
    - [WL-B2-12](#WL-B2-12) Web Server sends new component data to SOTA Core.
    - [WL-B2-13](#WL-B2-13) Web Server sends Search Component Request to SOTA Core.

3. [B3](#b-3) Web Server - External Resolver
    - [WL-B3-1](#WL-B3-1) Web Server sends new package data to External Resolver.
    - [WL-B3-2](#WL-B3-2) Web Server sends new VIN to External Resolver.
    - [WL-B3-3](#WL-B3-3) Web Server sends filter-to-package association to External Resolver.
    - [WL-B3-4](#WL-B3-4) Web Server sends new filter data to External Resolver.
    - [WL-B3-5](#WL-B5-1) Web Server sends Resolve VIN Request to External Resolver.
    - [WL-B3-6](#WL-B3-6) Web Server sends Search Filters Request to External Resolver.
    - [WL-B3-7](#WL-B3-7) Web Server sends List Filters Request to External Resolver.
    - [WL-B3-8](#WL-B3-8) Web Server sends Update Filter Request to External Resolver.
    - [WL-B3-9](#WL-B3-9) Web Server sends Delete Filter Request to External Resolver.


4. [B4](#b-4) SOTA Core - SOTA Core Database
    - [WL-B4-1](#WL-B4-1) SOTA Core persists new VIN to SOTA Core Database.
    - [WL-B4-2](#WL-B4-2) SOTA Core persists new package data to SOTA Core Database.
    - [WL-B4-3](#WL-B4-3) SOTA Core looks-up Package ID in SOTA Core Database.
    - [WL-B4-4](#WL-B4-4) SOTA Core looks-up Updates in SOTA Core Database.
    - [WL-B4-5](#WL-B4-5) SOTA Core looks-up VINs in SOTA Core Database.
    - [WL-B4-6](#WL-B4-6) SOTA Core looks-up for Packages per VIN in SOTA Core Database.
    - [WL-B4-7](#WL-B4-7) SOTA Core updates Packages per VIN in SOTA Core Database.
    - [WL-B4-8](#WL-B4-6) SOTA Core looks-up for VINs per Package in SOTA Core Database.
    - [WL-B4-9](#WL-B4-9) SOTA Core persists new Component to SOTA Core Database.
    - [WL-B4-10](#WL-B4-10) SOTA Core looks-up Components in SOTA Core Database.


5. [B5](#b-5) SOTA Core - External Resolver
    - [WL-B5-1](#WL-B5-1) SOTA Core sends Resolve VIN Request to External Resolver.


6. [B6](#b-6) External Resolver - External Resolver Database
    - [WL-B6-1](#WL-B6-1) External Resolver persists new VIN to External Resolver Database.
    - [WL-B6-2](#WL-B6-2) External Resolver persists new package data to External Resolver Database.
    - [WL-B6-3](#WL-B6-3) External Resolver persists new filter data to External Resolver Database.
    - [WL-B6-4](#WL-B6-4) External Resolver persists filter-to-package association to External Resolver Database.
    - [WL-B6-5](#WL-B6-5) External Resolver looks-up Package ID filters in External Resolver Database.
    - [WL-B6-6](#WL-B6-6) External Resolver looks-up VIN in External Resolver Database.
    - [WL-B6-7](#WL-B6-7) External Resolver looks-up Package Dependencies in External Resolver Database.
    - [WL-B6-8](#WL-B6-8) External Resolver looks-up Filters in External Resolver Database.
    - [WL-B6-9](#WL-B6-9) External Resolver updates Filters in External Resolver Database.
    - [WL-B6-10](#WL-B6-10) External Resolver deletes Filters in External Resolver Database.


7. [B7](#b-7) SOTA Core - RVI Node Server
    - [WL-B7-1](#WL-B7-1) SOTA Core sends Software Update Metadata for VIN to RVI Node Server.
    - [WL-B7-2](#WL-B7-2) SOTA Core sends "Software Update Available" notification to RVI Node Server.
    - [WL-B7-3](#WL-B7-3) RVI Node Server sends "Initiate Software Download" notification to SOTA Core.
    - [WL-B7-4](#WL-B7-4) SOTA Core sends "Start Download" notification to RVI Node Server.
    - [WL-B7-5](#WL-B7-5) SOTA Core sends lowest numbered data block to RVI Node Server.
    - [WL-B7-6](#WL-B7-6) SOTA Core sends "Finalize Download" notification to RVI Node Server.
    - [WL-B7-7](#WL-B7-7) RVI Node Server sends Install Report to SOTA Core.


8. [B8](#b-8) RVI Node Server - RVI Node Client
    - [WL-B8-1](#WL-B8-1) RVI Node Server sends "Software Update Available" notification to RVI Node Client.
    - [WL-B8-2](#WL-B8-2) RVI Node Server sends "Start Download" notification to RVI Node Client.
    - [WL-B8-3](#WL-B8-3) RVI Node Server sends lowest numbered data block to RVI Node Client.
    - [WL-B8-4](#WL-B8-4) RVI Node Server sends "Finalize Download" notification to RVI Node Client.
    - [WL-B8-5](#WL-B8-5) RVI Node Client sends Install Report to RVI Node Server.
    - [WL-B8-6](#WL-B8-6) RVI Node Client sends "Initiate Software Download" notification to RVI Node Server.


9. [B9](#b-9) RVI Node Client- SOTA Client
    - [WL-B9-1](#WL-B9-1) RVI Node Client sends "Software Update Available" notification to SOTA Client.
    - [WL-B9-2](#WL-B9-2) RVI Node Client sends "Initiate Software Download" notification to RVI Node Server.
    - [WL-B9-3](#WL-B9-3) RVI Node Client sends "Start Download" notification to SOTA Client.
    - [WL-B9-4](#WL-B9-4) RVI Node Client sends lowest numbered data block to SOTA Client.
    - [WL-B9-5](#WL-B9-5) RVI Node Client sends "Finalize Download" notification to SOTA Client.
    - [WL-B9-6](#WL-B9-6) SOTA Client sends Install Report to RVI Node Client.


10. [B10](#b-10) SOTA Client - Software Loading Manager
    - [WL-B10-1](#WL-B10-1) SOTA Client sends "Software Update Available" notification to Software Loading Manager.
    - [WL-B10-2](#WL-B10-2) Software Loading Manager sends "Initiate Software Download" notification to SOTA Client.
    - [WL-B10-3](#WL-B10-3) SOTA Client sends "Initiate Software Download" notification to RVI Node Client.
    - [WL-B10-4](#WL-B10-4) SOTA Client sends "Start Download" notification to Software Loading Manager.
    - [WL-B10-5](#WL-B10-5) SOTA Client sends lowest numbered data block to Software Loading Manager.
    - [WL-B10-6](#WL-B10-6) Software Loading Manager sends Install Report to SOTA Client.


11. [B11](#b-11) Charging & Billing API - SOTA Core


12. [B12](#b-12) Logistics & Provisioning API - SOTA Core


13. [B13](#b-13) Web Server - Physical Package Repository / Filesystem
    - [WL-B13-1](#WL-B13-1) Web Server sends the binary package to Filesystem.


## <a name="b-1">[B-1](#) Web Browser - Web Server</a>

### <a name="WL-B1-1">[WL-B1-1](#WL-B1-1) Web Server sends index.html page to Web Browser</a>

The Web Server can send the index.html file to the Web Browser, over HTTP on port 80.

   * Upon the Web Browser's request for /index.html, the Web Server will respond with the index.html HTML file and a 200 status code.
   * Upon the Web Browser's request for another HTML file, the Web Server will respond with a response with 409 status code.

### <a name="WL-B1-2">[WL-B1-2](#WL-B1-2) Web Server sends Javascript scripts linked in index.html to Web Browser</a>

The Web Server can send the Javascript scripts linked in index.html to the Web Browser over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server will respond with the Javascript files linked in and a 200 status code.
   * Upon the Web Browser's request for an unknown or not approved Javascript script, the Web Server will respond with a response with 409 status code.

### <a name="WL-B1-3">[WL-B1-3](#WL-B1-3) Web Server sends CSS stylesheets linked in index.html to Web Browser</a>

The Web Server can send the CSS Stylesheet files linked in index.html to the Web Browser over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server will respond with the CSS Stylesheet files linked in and a 200 status code.
   * Upon the Web Browser's request for an unknown or not approved CSS Stylesheet file, the Web Server will respond with a response with 409 status code.

### <a name="WL-B1-4">[WL-B1-4](#WL-B1-4) Web Browser sends new VIN to Web Server</a>

The Web Browser can send new VIN data to the Web Server that conform to the VIN standard (17 digits long, only capital letters and numbers), using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserterd' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN alread exists' message in the response body and a 409 status code.

### <a name="WL-B1-5">[WL-B1-5](#WL-B1-5) Web Browser sends new campaign data to Web Server</a>

The Web Browser can send new a Campaign (Package ID, Priority, Start Time, End Time) to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browsers's request, the Web Server can look for the Package ID lookup process and if the Package exists, it will respond with a 'Campaign created' message in the response body and a 200 status code.
   * Upon the Web Browsers's request, the Web Server can lookup for the Package ID, and if the Package does not exist, it will respond with a 'Unknown Package ID' message in the response body and a 404 status code.

### <a name="WL-B1-6">[WL-B1-6](#WL-B1-6) Web Browser sends new filter data to Web Server</a>

The Web Browser can send a new Filter's data to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new Filter insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new Filter insertion process and if a database entry already exists, it will respond with a 'Filter already exists' message in the response body and a 409 status code.

### <a name="WL-B1-7">[WL-B1-7](#WL-B1-7) Web Browser sends filter-to-package association to Web Server</a>

The Web Browser can send a new association between a Filter and a Package to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new Filter/Package association insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new Filter/Package association insertion process and if a database entry already exists, it will respond with a 'Filter/Package association already exists' message in the response body and a 409 status code.

### <a name="WL-B1-8">[WL-B1-8](#WL-B1-8) Web Browser sends Queue Package Request to Web Server</a>

The Web Browser can send a Queue Package Request [Package ID, Priority, Date/Time Interval] to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if a request is processed without errors, it will respond with a unique Install Request ID in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if the given package ID is not found, it will respond with a 'Package ID does not exist' message in the response body and a 404 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="WL-B1-9">[WL-B1-9](#WL-B1-9) Web Server sends login page and assets to Web Browser</a>

   * Upon receiving unauthenticated request from Web Browser, Web Server sends login page HTML and assets over HTTPS (port 443) to Web Browser.

### <a name="WL-B1-10">[WL-B1-10](#WL-B1-10) Web Browser sends login credentials to Web Server</a>

   * When user enters and submit credentials in Web Browser, Web Browser sends the credentials to Web Server over HTTPS POST (port 443),
   * If the credentials are valid, Web Server sends Admin GUI page content and session cookie to Web Browser over HTTPS (port 443)
   * If the credentials are invalid, Web Server sends Access Denied (HTTP 401) to Web Browser over HTTPS (port 443)

### <a name="WL-B1-11">[WL-B1-11](#WL-B1-11) Web Server sends session cookie to Web Browser</a>

   * In response to a request from a Web Browser, Web Server may include an HTTP Cookie to establish a session

### <a name="WL-B1-12">[WL-B1-12](#WL-B1-12) Web Browser sends session cookie to Web Server</a>

   * When requesting resources from Web Server, Web Browser may include any locally stored HTTP Cookie associated with the Admin GUI domain

### <a name="WL-B1-13">[WL-B1-13](#WL-B1-13) Web Server sends a redirection to login page for unauthenticated users</a>

   * In response to HTTPS (port 443) requests from a Web Browser, the Web Server may send HTTPS 301 redirect responses to unauthenticated clients

### <a name="WL-B1-14">[WL-B1-14](#WL-B1-14) Web Server sends a redirection back to the last requested page for authenticated users</a>

   * In response to HTTPS (port 443) requests from a Web Browser that include valid login credentials, the Web Server may send an HTTPS 301 response to authenticated clients for protected resources.

### <a name="WL-B1-15">[WL-B1-15](#WL-B1-15) Web Browser sends a request for an Admin GUI resource to Web Server, along with a session cookie</a>

   * Web Browser may send HTTPS (port 443) requests to Web Server on behalf of user for protected Admin GUI resources
   * Requests may include any locally stored Cookies associated with the Admin GUI Domain

### <a name="WL-B1-16">[WL-B1-16](#WL-B1-16) Web Server sends Admin GUI resource HTML and associated assets to Web Browser</a>

   * In response to authenticated HTTPS (port 443) requests from Web Browser for protected Admin GUI resources, Web Server may send back associated HTML and resources to render resource details and necessary hyperlinks, JavaScript code, assets, etc. to Web Browser.

### <a name="WL-B1-17">[WL-B1-17](#WL-B1-17) Web Server sends a redirection to the login page for unauthenticated users</a>

   * In response to unauthenticated HTTPS (port 443) requests from Web Browser, Web Server may send an HTTPS 301 response to direct Web Browser to login page

### <a name="WL-B1-18">[WL-B1-18](#WL-B1-18) Web Browser sends a List Queue Package Request to Web Server</a>

The Web Browser can send a List Queue Package Request {[Package ID, Priority, Date/Time Interval], [...]} to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a List Queue Package Request process and if a request is processed without errors, it will respond with a unique Install Request ID in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a List Queue Package Request process and if the given package ID is not found, it will respond with a 'Package ID does not exist' message in the response body and a 404 status code.
   * Upon the Web Browser's request, the Web Server can start a List Queue Package Request process and if no filters are associated with the one of the packages, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="WL-B1-19">[WL-B1-19](#WL-B1-19) Web Browser sends a GET Queued Request to Web Server</a>

The Web Browser can send a get Queued Package Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a get Queued Package Request process and if a request is processed without errors, it will respond with a list of queued update requests in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a get Queued Package Request process and if there are no pending update requests, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a List Queue Package Request process and if no filters are associated with the one of the packages, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="WL-B1-20">[WL-B1-20](#WL-B1-20) Web Browser sends Search VINs Request to Web Server</a>

The Web Browser can send a Search VINs Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Search VINs Request process and if a request is processed without errors, it will respond with a list of VINs matching the search criteria in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Search VINs Request process and if there are no VINs matching the search criteria, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Search VINs Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B1-21">[WL-B1-21](#WL-B1-21) Web Browser sends the binary package and its metadata to Web Server</a>

The Web Browser can upload a binary package and a POST request for its associated data to the Web Server.

  * Upon the Web Browser's request, the Web Server can receive a binary package and its associated metadata
    and perform an Upload New Package process and if the request is processed without errors, it will respond
    with a message informing for successful persistence in the response body and a 200 status code.
  * Upon the Web Browser's request, the Web Server can receive a binary package and its associated metadata
    and perform an Upload New Package process and if the request is processed with errors, it will respond
    with a message informing for the generated error in the response body and a 500 status code.
  * Upon the Web Browser's request, Web Server can receive the metadata associated with a new package and if
    SOTA Core fails to authenticate, it will respond with an 'Authentication Failed' message in the response 
    body and a 404 status code.

### <a name="WL-B1-22">[WL-B1-22](#WL-B1-22) Web Browser sends Search Filters Request to Web Server</a>

The Web Browser can send a Search Filters Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Search Filters Request process and if a request is processed without errors, it will respond with a list of Filters matching the search criteria in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Search Filters Request process and if there are no Filters matching the search criteria, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Search Filters Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B1-23">[WL-B1-23](#WL-B1-23) Web Browser sends List Filters Request to Web Server</a>

The Web Browser can send a List Filters Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Search List Request process and if a request is processed without errors, it will respond with a list of available Filters in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Search Filters Request process and if there are no available Filters, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a List Filters Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B1-24">[WL-B1-24](#WL-B1-24) Web Browser sends Update Filter Request to Web Server</a>

The Web Browser can send a Update Filter Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Update Filter Request process and if a request is processed without errors, it will respond with a list of available Filters in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Update Filter Request process and if there is no Filter with the given Filter ID, it will respond with a 'Filter does not exist' message in the response body and a 409 status code.
   * Upon the Web Browser's request, the Web Server can start a Update Filter Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B1-25">[WL-B1-25](#WL-B1-25) Web Browser sends Delete Filter Request to Web Server</a>

The Web Browser can send a Delete Filter Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Delete Filter Request process and if a request is processed without errors, it will respond with a list of available Filters in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Delete Filter Request process and if there is no Filter with the given Filter ID, it will respond with a 'Filter does not exist' message in the response body and a 409 status code.
   * Upon the Web Browser's request, the Web Server can start a Delete Filter Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B1-26">[WL-B1-26](#WL-B1-26) Web Browser sends Update Packages per VIN Request to Web Server</a>

The Web Browser can send an Update Packages per VIN Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start an Update Packages per VIN Request process and if a request is processed without errors, it will respond with the modified package data for the selected VIN in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start an Update Packages per VIN Request process and if the given VIN does not exist, it will respond with a 'VIN does not exist' message in the response body and a 409 status code.
   * Upon the Web Browser's request, the Web Server can start an Update Packages per VIN Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B1-27">[WL-B1-27](#WL-B1-26) Web Browser sends View Packages per VIN Request to Web Server</a>

The Web Browser can send a View Packages per VIN Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a View Packages per VIN Request process and if a request is processed without errors, it will respond with the installed packages on the selected VIN in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a View Packages per VIN Request process and if the given VIN does not exist, it will respond with a 'VIN does not exist' message in the response body and a 409 status code.
   * Upon the Web Browser's request, the Web Server can start a View Packages per VIN Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B1-28">[WL-B1-28](#WL-B1-28) Web Browser sends View VINs per Package Request to Web Server</a>

The Web Browser can send a View VINs per Package Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a View VINs per Package Request Request process and if a request is processed without errors, it will respond with the VINs that have installed the selected Package in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a View VINs per Package Request process and if the given Package does not exist, it will respond with a 'Package does not exist' message in the response body and a 409 status code.
   * Upon the Web Browser's request, the Web Server can start a View VINs per Package Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B1-29">[WL-B1-29](#WL-B1-29) Web Browser sends new component data to Web Server</a>

The Web Browser can send a new Component's data to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a new Component insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a new Component insertion process and if a database entry already exists, it will respond with a 'Component already exists' message in the response body and a 409 status code.

### <a name="WL-B1-30">[WL-B1-30](#WL-B1-30) Web Browser sends Search Components Request to Web Server</a>

The Web Browser can send a Search Components Request to the Web Server using JSON over HTTP on port 80.

   * Upon the Web Browser's request, the Web Server can start a Search Components Request process and if a request is processed without errors, it will respond with a list of Components matching the search criteria (regex, ID/IDs or name) in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Search Components Request process and if there are no Components matching the search criteria, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Browser's request, the Web Server can start a Search Components Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.


## <a name="b-2">[B-2](#) Web Server - SOTA Core</a>

### <a name="WL-B2-1">[WL-B2-1](#WL-B2-1) Web Server sends new package data to SOTA Core</a>

The Web Server can send the new package data to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a new Package insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a new Package insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="WL-B2-2">[WL-B2-2](#WL-B2-2) Web Server sends new VIN to SOTA Core</a>

The Web Server can send the VINs data to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="WL-B2-3">[WL-B2-3](#WL-B2-3) Web Server sends new campaign data to SOTA Core</a>

The Web Server can send new a Campaign (Package ID, Priority, Start Time, End Time) to SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can look for the Package ID lookup process and if the Package exists, it will respond with a 'Campaign created' message in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can lookup for the Package ID, and if the Package does not exist, it will respond with a 'Unknown Package ID' message in the response body and a 404 status code.

### <a name="WL-B2-4">[WL-B2-4](#WL-B2-4) Web Server sends Queue Package Request to SOTA Core</a>

The Web Server can send a Queue Package Request [Package ID, Priority, Date/Time Interval] to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, Core can start a Queue Package Request process and if a request is processed without errors, it will respond with a unique Install Request ID in the response body and a 200 status code.
   * Upon the Web Server's request, Core can start a Queue Package Request process and if the given package ID is not found, it will respond with a 'Package ID does not exist' message in the response body and a 404 status code.
   * Upon the Web Browser's request, the Web Server can start a Queue Package Request process and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="WL-B2-5">[WL-B2-5](#WL-B2-5) Web Server sends a List Queue Package Request to SOTA Core</a>

The Web Server can send a List Queue Package Request {[Package ID, Priority, Date/Time Interval], [...]} to SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a List Queue Package Request process and if a request is processed without errors, it will respond with a unique Install Request ID in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a List Queue Package Request process and if the given package ID is not found, it will respond with a 'Package ID does not exist' message in the response body and a 404 status code.
   * Upon the Web Server's request, SOTA Core can start a List Queue Package Request process and if no filters are associated with the one of the packages, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="WL-B2-6">[WL-B2-6](#WL-B2-6) Web Server sends a GET Queued Request to SOTA Core</a>

The Web Server can send a get Queued Package Request to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, the SOTA Core can start a get Queued Package Request process and if a request is processed without errors, it will respond with a list of queued update requests in the response body and a 200 status code.
   * Upon the Web Server's request, the SOTA Core can start a get Queued Package Request process and if there are no pending update requests, it will respond with an empty list of queued update requests in the response body and a 200 status code.
   * Upon the Web Server's request, the SOTA Core can start a get Queued Package Request process and if no filters are associated with the one of the packages, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="WL-B2-7">[WL-B2-7](#WL-B2-7) Web Server sends Search VINs Request to SOTA Core</a>

The Web Server can send a Search VINs Request to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a Search VINs Request process and if a request is processed without errors, it will respond with a list of VINs matching the search criteria in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a Search VINs Request process and if there are no VINs matching the search criteria, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a Search VINs Request process and if no filters are associated with the one of the packages, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="WL-B2-8">[WL-B2-8](#WL-B2-8) Web Server sends the new package metadata to SOTA Core</a>

The Web Server can send the new package metadata to SOTA Core.

  * Upon the Web Server's request, SOTA Core can receive the metadata associated with a new package
    and perform an Upload New Package process and if the request is processed without errors, it will respond
    with a message informing for successful persistence in the response body and a 200 status code.
  * Upon the Web Server's request, SOTA Core can receive the metadata associated with a new package
    and perform an Upload New Package process and if the request is processed with errors, it will respond
    with a message informing for the generated error in the response body and a 500 status code.
  * Upon the Web Server's request, SOTA Core can receive the metadata associated with a new package and if SOTA
    Core fails to authenticate, it will respond with an 'Authentication Failed' message in the response body
    and a 404 status code.

### <a name="WL-B2-9">[WL-B2-9](#WL-B2-9) Web Server sends Update Packages per VIN Request to SOTA Core</a>

The Web Server can send an Update Packages per VIN Request to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, the SOTA Core can start an Update Packages per VIN Request process and if a request is processed without errors, it will respond with the modified package data for the selected VIN in the response body and a 200 status code.
   * Upon the Web Server's request, the SOTA Core can start an Update Packages per VIN Request process and if the given VIN does not exist, it will respond with a 'VIN does not exist' message in the response body and a 409 status code.
   * Upon the Web Server's request, the SOTA Core can start an Update Packages per VIN Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B2-10">[WL-B2-10](#WL-B2-10) Web Server sends View Packages per VIN Request to SOTA Core</a>

The Web Server can send a View Packages per VIN Request to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, the SOTA Core can start a View Packages per VIN Request process and if a request is processed without errors, it will respond with the installed packages on the selected VIN in the response body and a 200 status code.
   * Upon the Web Server's request, the SOTA Core can start a View Packages per VIN Request process and if the given VIN does not exist, it will respond with a 'VIN does not exist' message in the response body and a 409 status code.
   * Upon the Web Server's request, the SOTA Core can start a Update Package per VIN Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B2-11">[WL-B2-11](#WL-B2-11) Web Server sends View VINs per Package Request to SOTA Core</a>

The Web Server can send a View VINs per Package Request to SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a View VINs per Package Request Request process and if a request is processed without errors, it will respond with the VINs that have installed the selected Package in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a View VINs per Package Request process and if the given Package does not exist, it will respond with a 'Package does not exist' message in the response body and a 409 status code.
   * Upon the Web Server's request, SOTA Core can start a View VINs per Package Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B2-12">[WL-B2-12](#WL-B2-12) Web Server sends new component data to SOTA Core</a>

The Web Server can send new a Component to SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can look for the Component ID lookup process and if the Component exists, it will respond with a 'Component already created' message in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can lookup for the Component ID, and if the Component does not exist, it will respond with a 'Unknown Package ID' message in the response body and a 404 status code.

### <a name="WL-B2-13">[WL-B2-13](#WL-B2-7) Web Server sends Search Component Request to SOTA Core</a>

The Web Server can send a Search Components Request to the SOTA Core using JSON over HTTP on port 80.

   * Upon the Web Server's request, SOTA Core can start a Search Components Request process and if a request is processed without errors, it will respond with a list of Components matching the search criteria (regex, ID/IDs or name) in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a Search Components Request process and if there are no Components matching the search criteria, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Server's request, SOTA Core can start a Search Components Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.


## <a name="b-3">[B-3](#) Web Server - External Resolver</a>

### <a name="WL-B3-1">[WL-B3-1](#WL-B3-1) Web Server sends new package data to External Resolver</a>

The Web Server can send new Package data to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Package insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Package insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="WL-B3-2">[WL-B3-2](#WL-B3-2) Web Server sends new VIN to External Resolver</a>

The Web Server can send new VINs to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new VIN insertion process and if a database entry already exists, it will respond with a 'VIN already exists' message in the response body and a 409 status code.

### <a name="WL-B3-3">[WL-B3-3](#WL-B3-3) Web Server sends filter-to-package association to External Resolver</a>

The Web Server can send a new association between a Filter and a Package to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if a database entry already exists, it will respond with a 'Filter/Package association already exists' message in the response body and a 409 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if the Filter does not exist, it will respond with a 'Filter label does not exist' message in the response body and a 404 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter/Package association insertion process and if the Package does not exist, it will respond with a Package ID does not exist' message in the response body and a 404 status code.

### <a name="WL-B3-4">[WL-B3-4](#WL-B3-4) Web Server sends new filter data to External Resolver</a>

The Web Server can send a new Filter's data to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if a new database entry is created, it will respond with a 'Row Inserted' message in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if a database entry already exists, it will respond with a 'Filter already exists' message in the response body and a 409 status code.
   * Upon the Web Server's request, the External Resolver can start a new Filter insertion process and if the Filter expression fails validation, it will respond with a 'Filter failed validation' message in the response body and a 406 status code.

### <a name="WL-B3-5">[WL-B3-5](#WL-B3-5) Web Server sends Resolve VIN Request to External Resolver</a>

Web Server can send a Resolve VIN request to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can resolve the dependencies for all VINs involved and if the request is processed without errors, it will respond with the subset of all VINs that passed all filters in the response body and a 200 status code.
   * Upon Web Server's request, the External Resolver can resolve the dependencies for all VINs involved and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.

### <a name="WL-B3-6">[WL-B3-6](#WL-B3-6) Web Server sends Search Filters Request to External Resolver</a>

The Web Server can send a Search Filters Request to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a Search Filters Request process and if a request is processed without errors, it will respond with a list of Filters matching the search criteria in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a Search Filters Request process and if there are no Filters matching the search criteria, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a Search Filters Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B3-7">[WL-B3-7](#WL-B3-7) Web Server sends List Filters Request to External Resolver</a>

The Web Server can send a List Filters Request to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a Search List Request process and if a request is processed without errors, it will respond with a list of available Filters in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a Search Filters Request process and if there are no available Filters, it will respond with an empty list in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a List Filters Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B3-8">[WL-B3-8](#WL-B3-8) Web Server sends Update Filter Request to External Resolver</a>

The Web Server can send a Update Filter Request to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a Update Filter Request process and if a request is processed without errors, it will respond with a list of available Filters in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a Update Filter Request process and if there is no Filter with the given Filter ID, it will respond with a 'Filter does not exist' message in the response body and a 409 status code.
   * Upon the Web Server's request, the External Resolver can start a Update Filter Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.

### <a name="WL-B3-9">[WL-B3-9](#WL-B3-9) Web Server sends Delete Filter Request to Web Server</a>

The Web Server can send a Delete Filter Request to the External Resolver using JSON over HTTP on port 80.

   * Upon the Web Server's request, the External Resolver can start a Delete Filter Request process and if a request is processed without errors, it will respond with a list of available Filters in the response body and a 200 status code.
   * Upon the Web Server's request, the External Resolver can start a Delete Filter Request process and if there is no Filter with the given Filter ID, it will respond with a 'Filter does not exist' message in the response body and a 409 status code.
   * Upon the Web Server's request, the External Resolver can start a Delete Filter Request process and if an error occurs, it will respond with an error message in the response body and a 404 status code.


## <a name="b-4">[B-4](#) SOTA Core - SOTA Core Database</a>

### <a name="WL-B4-1">[WL-B4-1](#WL-B4-1) SOTA Core persists new VIN to SOTA Core Database</a>

SOTA Core can persist new VIN data to the SOTA Core Database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if a new database entry is created, it will respond with a 'Success' message.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if the VIN already exists, it will respond with a 'Record exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-2">[WL-B4-2](#WL-B4-2) SOTA Core persists new package data to SOTA Core Database</a>

SOTA Core can persist new package data to the SOTA Core Database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if a new database entry is created, it will respond with a 'Success' message.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if the Package already exists, it will respond with a 'Record exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-3">[WL-B4-3](#WL-B4-3) SOTA Core lookups Package ID in SOTA Core Database</a>

SOTA Core can perform a lookup operation for a Package ID in the SOTAServer database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given Package ID and if an entry is found, it will respond with the Package's data.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given Package ID and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-4">[WL-B4-4](#WL-B4-4) SOTA Core looks-up Updates in SOTA Core Database</a>

SOTA Core can perform a lookup operation for an Update in the SOTAServer database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given Update ID and if an entry is found, it will respond with the Package's data.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given Update ID and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-5">[WL-B4-5](#WL-B4-5) SOTA Core looks-up VINs in SOTA Core Database</a>

SOTA Core can perform a lookup operation for VINs matching the given criteria in the SOTAServer database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given search criteria and if an entry is found, it will respond with the VINs' data.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given search criteria and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-6">[WL-B4-6](#WL-B4-6) SOTA Core looks-up for Packages per VIN in SOTA Core Database</a>

SOTA Core can perform a lookup operation for the installed Packages on a given VIN in the SOTAServer database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given VIN and if an entry is found, it will respond with Package data associated with the VIN.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given VIN and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-7">[WL-B4-7](#WL-B4-7) SOTA Core updates Packages per VIN in SOTA Core Database</a>

SOTA Core can perform an UPDATE operation for the packages associated with a given VIN in the SOTAServer database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an UPDATE operation with the package data for the selected VIN and if an entry is found, it will respond with the VINs' data.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given VIN and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-8">[WL-B4-8](#WL-B4-8) SOTA Core looks-up for VINs per Package in SOTA Core Database</a>

SOTA Core can perform a lookup operation for the VINs with have installed the Package with the given Package ID in the SOTA Server database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given Package ID and if an entry is found, it will respond with the VINs who have installed the given package.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given Package ID and if no VINs are found, it will respond with a 'No VINs have this package installed' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-9">[WL-B4-9](#WL-B4-1) SOTA Core persists new Component to SOTA Core Database</a>

SOTA Core can persist new Component data to the SOTA Core Database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Component data and if a new database entry is created, it will respond with a 'Success' message.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Component data and if the VIN already exists, it will respond with a 'Record exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B4-10">[WL-B4-10](#WL-B4-10) SOTA Core lookups Component ID in SOTA Core Database</a>

SOTA Core can perform a regex-based lookup operation for a Component or Componets in the SOTAServer database in the Database Server over TCP on port 3306.

   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given Component ID and if an entry is found, it will respond with the Package's data.
   * If SOTA Core authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given Component ID and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.


## <a name="b-5">[B-5](#) SOTA Core - External Resolver</a>

### <a name="WL-B5-1">[WL-B5-1](#WL-B5-1) SOTA Core sends Resolve VIN Request to External Resolver</a>

SOTA Core can send a Resolve VIN request to the External Resolver using JSON over HTTP on port 80.

   * Upon the SOTA Core's request, the External Resolver can resolve the dependencies for all VINs involved and if the request is processed without errors, it will respond with the subset of all VINs that passed all filters in the response body and a 200 status code.
   * Upon SOTA Core's request, the External Resolver can resolve the dependencies for all VINs involved and if no filters are associated with the package, it will respond with a 'No filters associated with package' message in the response body and a 404 status code.


## <a name="b-6">[B-6](#) External Resolver - External Resolver Database</a>

### <a name="WL-B6-1">[WL-B6-1](#WL-B6-1) External Resolver persists new VIN to External Resolver Database</a>

The External Resolver can persist new VIN data to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new VIN data and if the VIN already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B6-2">[WL-B6-2](#WL-B6-2) External Resolver persists new package data to External Resolver Database</a>

The External Resolver can persist new Package data to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Package data and if the Package already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B6-3">[WL-B6-3](#WL-B6-3) External Resolver persists new filter data to External Resolver Database</a>

The External Resolver can persist a new Filter to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter data and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter data and if the Filter already exists, it will respond with a 'Record exists' message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B6-4">[WL-B6-4](#WL-B6-4) External Resolver persists filter-to-package association to External Resolver Database</a>

The External Resolver can persist a new Filter/Package association to the External Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if a new database entry is created, it will respond with a 'Success' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Filter/Package association already exists, it will respond with a 'Record exists' message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Filter does not exist exist, it will respond with a 'Filter does not exist' error message.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an INSERT operation with the new Filter/Package association and if the Package does not exist exist, it will respond with a 'Package does not exist' error message.
   * If the External Resolver does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B6-5">[WL-B6-5](#WL-B6-5) External Resolver looks-up Package ID filters in External Resolver Database</a>

The External Resolver can perform a lookup operation for all filters associated with a Package ID to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation for all filters associated with the given Package ID and if one or more entries are found, it will respond with the Filters' data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation for all filters associated with the given Package ID and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B6-6">[WL-B6-6](#WL-B6-6) External Resolver looks-up VIN in External Resolver Database</a>

The External Resolver can perform a lookup operation for a VIN to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given VIN and if an entry is found, it will respond with the VIN's data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given VIN and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B6-7">[WL-B6-7](#WL-B6-7) External Resolver looks-up Package Dependencies in External Resolver Database</a>

The External Resolver can perform a lookup operation for all the package dependencies of a VIN to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given VIN and if an entry is found, it will respond with all the software dependencies for the given VIN data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given VIN and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B6-8">[WL-B6-8](#WL-B6-8) External Resolver looks-up Filters in External Resolver Database</a>

The External Resolver can perform a lookup operation for all the Filters to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a SELECT operation with the given Filter ID or IDs and if an entry is found, it will respond with all the software dependencies for the given VIN data.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an SELECT operation with the given Filter ID or IDs and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.

### <a name="WL-B6-9">[WL-B6-9](#WL-B6-9) External Resolver updates Filters in External Resolver Database</a>

The External Resolver can perform an update operation for one or many Filters to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an UPDATE operation with the given Filter ID or IDs and if an entry is found, it will respond with the number of Filters correctly updated.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform an UPDATE operation with the given Filter ID or IDs and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.


### <a name="WL-B6-10">[WL-B6-10](#WL-B6-10) External Resolver deletes Filters in External Resolver Database</a>

The External Resolver can perform a delete operation for one or many Filters to the Resolver database in the Database Server over TCP on port 3306.

   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a DELETE operation with the given Filter ID or IDs and if an entry is found, it will respond with the number of Filters deleted.
   * If the External Resolver authenticates successfully with the correct Username/Password credentials, upon its request, the Database Server can perform a DELETE operation with the given Filter ID or IDs and if no entry is found, it will respond with a 'Record does not exists' message.
   * If SOTA Server does not authenticate successfully due to incorrect Username/Password credentials against the Database Server, the Database Server should reject the connection.



## <a name="b-7">[B-7](#) SOTA Core - RVI Node Server</a>

### <a name="WL-B7-1">[WL-B7-1](#WL-B7-1) SOTA Core sends Software Update Metadata for VIN to RVI Node Server</a>

Core can send a software update [main Package ID, dependent Package IDs to install, date/time interval, priority, creation date/timestamp] for each VIN to the RVI Node using JSON over HTTP on port 80.

   * Upon Core's request, the RVI Node can schedule the installation of the software packages listed for every given VIN if the task is scheduled without errors, it will respond with the subset of all VINs that passed all filters in the response body and a 200 status code.
   * Upon Core's request, the RVI Node can schedule the installation of the software packages listed for every given VIN and if any errors occur, it will respond with a 'Task scheduling' message in the response body and a 412 status code.

### <a name="WL-B7-2">[WL-B7-2](#WL-B7-2) SOTA Core sends "Software Update Available" notification to RVI Node Server</a>

SOTA Core can send "Software Update Available" notifications [Package ID, Size, Download Index, Description] to RVI Node Server using JSON on port 80 over HTTP.

   * Upon SOTA Core's request, the RVI Node Server can start the software update process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon SOTA Core's request, the RVI Node Server can start the software update process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server X times to resume the update.

### <a name="WL-B7-3">[WL-B7-3](#WL-B7-3) RVI Node Server sends "Initiate Software Download" notification to SOTA Core</a>

RVI Node Server can send a "Initiate Software Download" [Download Index] notification to SOTA Core using JSON on port 80 over HTTP.

   * Upon the RVI Node Server's request, SOTA Core can start the update download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the RVI Node Server's request, SOTA Core can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the RVI Node Server's "Cancel Software Download" request, SOTA Core can interrupt the update download process.

### <a name="WL-B7-4">[WL-B7-4](#WL-B7-4) SOTA Core sends "Start Download" notification to RVI Node Server</a>

SOTA Core can send a "Start Download" notification to RVI Node Server using JSON on port 80 over HTTP.

   * Upon SOTA Core's request, RVI Node Server can start the download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the RVI Node Server's request, SOTA Core can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the RVI Node Server's "Cancel Software Download" request, SOTA Core can interrupt the update download process.

### <a name="WL-B7-5">[WL-B7-5](#WL-B7-5) SOTA Core sends lowest numbered data block to RVI Node Server</a>

SOTA Core can send the lowest numbered data block to RVI Node Server using JSON on port 80 over HTTP.

   * Upon SOTA Core's request, RVI Node Server can accept the lowest numbered data block and if the data block is received without errors, it will acknowledge of successful data block receipt in the response body and a 200 status code.
   * Upon SOTA Core's request, RVI Node Server can accept the lowest numbered data block and if the data block has been received before the data block will be discarded and the next data block will be requested.
   * Upon SOTA Core's request, RVI Node Server can accept the lowest numbered data block and if the data block is interrupted due to network loss, it will attempt to reconnect X times and transmit again the data block.

### <a name="WL-B7-6">[WL-B7-6](#WL-B7-6) SOTA Core sends "Finalise Download" notification to RVI Node Server</a>

SOTA Core can send a "Finalize Download" notification to RVI Node Server using JSON on port 80 over HTTP.

   * Upon SOTA Core's request, RVI Node Server can confirm the completion of download process and if the download is finished without errors, it will respond with 'Download of *Package ID* complete' in the response body and a 200 status code.
   * Upon SOTA Core's request, RVI Node Server can confirm the completion of download process and if data blocks are missing, it will respond with 'Incomplete Download' in the response body and a 400 status code.
   * Upon the RVI Node Server's request, SOTA Core can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.

### <a name="WL-B7-7">[WL-B7-7](#WL-B7-7) SOTA Core sends Install Report to RVI Node Client</a>

SOTA Client can send an Install Report to RVI Node Client the  using JSON on port 80 over HTTP.

   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the installation was finished without errors, it will respond with '*Package ID* success' in the response body and a 200 status code.
   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the VIN is already marked as complete, it will respond with '*Package ID* failed' in the response body and a 409 status code.
   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the VIN is already marked as failed, it will respond with '*Package ID* failed' in the response body and a 409 status code.


## <a name="b-8">[B-8](#) RVI Node Server - RVI Node Client</a>

### <a name="WL-B8-1">[WL-B8-1](#WL-B8-1) RVI Node Server sends "Software Update Available" notification to RVI Node Client</a>

RVI Node Server can send "Software Update Available" notifications [Package ID, Size, Download Index, Description] to RVI Node Client.

### <a name="WL-B8-2">[WL-B8-2](#WL-B8-2) RVI Node Server sends "Start Download" notification to RVI Node Client</a>

RVI Node Server can send a "Start Download" notification to RVI Node Client.

### <a name="WL-B8-3">[WL-B8-3](#WL-B8-3) RVI Node Server sends lowest numbered data block to RVI Node Client</a>

RVI Node Server can send the lowest numbered data block to RVI Node Client.

### <a name="WL-B8-4">[WL-B8-4](#WL-B8-4) RVI Node Server sends "Finalise Download" notification to RVI Node Client</a>

RVI Node Server can send a "Finalize Download" notification to RVI Node Client.

### <a name="WL-B8-5">[WL-B8-5](#WL-B8-5) RVI Node Client sends Install Report to RVI Node Server</a>

RVI Node Client can send an Install Report to the RVI Node Server.

### <a name="WL-B8-6">[WL-B8-6](#WL-B8-6) RVI Node Client sends "Initiate Software Download" notification to RVI Node Server</a>

The RVI Node Client can send "Initiate Software Download" notification to RVI Node Server.


## <a name="b-9">[B-9](#) RVI Node Client- SOTA Client</a>

### <a name="WL-B9-1">[WL-B9-1](#WL-B9-1) RVI Node Client sends "Software Update Available" notification to SOTA Client</a>

RVI Node Client can send "Software Update Available" notifications [Package ID, Size, Download Index, Description] to SOTA Client the  using JSON on port 80 over HTTP.

   * Upon the RVI Node Clients's request, the SOTA Client can start the software update process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the RVI Node Client's request, the SOTA Client can start the software update process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server X times to resume the update.

### <a name="WL-B9-2">[WL-B9-2](#WL-B9-2) RVI Node Client sends "Initiate Software Download" notification to RVI Node Server</a>

The RVI Node Client can send "Initiate Software Download" notification to RVI Node Server.

### <a name="WL-B9-3">[WL-B9-3](#WL-B9-3) RVI Node Client sends "Start Download" notification to SOTA Client</a>

RVI Node Client can send a "Start Download" notification to SOTA Client using JSON on port 80 over HTTP.

   * Upon RVI Node Client's request, SOTA Client can start the download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the RVI Node Client's request, SOTA Client can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the RVI Node Client's "Cancel Software Download" request, SOTA Client can interrupt the update download process.

### <a name="WL-B9-4">[WL-B9-4](#WL-B9-4) RVI Node Client sends lowest numbered data block to SOTA Client</a>

RVI Node Client can send the lowest numbered data block to SOTA Client using JSON on port 80 over HTTP.

   * Upon RVI Node Client's request, SOTA Client can accept the lowest numbered data block and if the data block is received without errors, it will acknowledge of successful data block receipt in the response body and a 200 status code.
   * Upon RVI Node Client's request, SOTA Client can accept the lowest numbered data block and if the data block has been received before the data block will be discarded and the next data block will be requested.

### <a name="WL-B9-5">[WL-B9-5](#WL-B9-5) RVI Node Client sends "Finalize Download" notification to SOTA Client</a>

RVI Node Client can send a "Finalize Download" notification to SOTA Client using JSON on port 80 over HTTP.

   * Upon RVI Node Client's request, SOTA Client can confirm the completion of download process and if the download is finished without errors, it will respond with 'Download of *Package ID* complete' in the response body and a 200 status code.
   * Upon RVI Node Client's request, SOTA Client can confirm the completion of download process and if data blocks are missing, it will respond with 'Incomplete Download' in the response body and a 400 status code.
   * Upon the RVI Node Client's request, SOTA Client can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.

### <a name="WL-B9-6">[WL-B9-6](#WL-B9-6) SOTA Client sends Install Report to RVI Node Client</a>

SOTA Client can send an Install Report to RVI Node Client the using JSON on port 80 over HTTP.

   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the installation was finished without errors, it will respond with '*Package ID* success' in the response body and a 200 status code.
   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the VIN is already marked as complete, it will respond with '*Package ID* failed' in the response body and a 409 status code.
   * Upon the SOTA Client's request, RVI Node Client can accept the Install Report and if the VIN is already marked as failed, it will respond with '*Package ID* failed' in the response body and a 409 status code.


## <a name="b-10">[B-10](#) SOTA Client - Software Loading Manager</a>

### <a name="WL-B10-1">[WL-B10-1](#WL-B10-1) SOTA Client sends "Software Update Available" notification to Software Loading Manager</a>

SOTA Client can send "Software Update Available" notifications [Package ID, Size, Download Index, Description] to Software Loading Manager using JSON on port 80 over HTTP.

   * Upon SOTA Clients's request, Software Loading Manager can start the software update process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the SOTA Clients's request, Software Loading Manager can start the software update process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server X times to resume the update.

### <a name="WL-B10-2">[WL-B10-2](#WL-B10-2) Software Loading Manager sends "Initiate Software Download" notification to SOTA Client</a>

Software Loading Manager can send a "Initiate Software Download" [Download Index] notification from to SOTA Client using JSON on port 80 over HTTP.

   * Upon the Software Loading Manager's request, SOTA Client can start the update download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the Software Loading Manager's request, SOTA Client can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the Software Loading Manager's "Cancel Software Download" request, SOTA Client can interrupt the update download process.

### <a name="WL-B10-3">[WL-B10-3](#WL-B10-3) SOTA Client sends "Initiate Software Download" notification to RVI Node Client</a>

SOTA Client can accept a "Initiate Software Download" [Download Index] notification to RVI Node Client using JSON on port 80 over HTTP.

   * Upon the SOTA Client's request, RVI Node Client can start the update download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the SOTA Client's request, RVI Node Client can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the SOTA Client's "Cancel Software Download" request, RVI Node Client can interrupt the update download process.

### <a name="WL-B10-4">[WL-B10-4](#WL-B10-4) SOTA Client sends "Start Download" notification to Software Loading Manager</a>

SOTA Client can send a "Start Download" notification to Software Loading Manager using JSON on port 80 over HTTP.

   * Upon SOTA Client's request, Software Loading Manager can start the download process and if the update is finished without errors, it will respond with 'Installation of *Package ID* complete' in the response body and a 200 status code.
   * Upon the SOTA Client's request, Software Loading Manager can start the update download process and if the update is interrupted due to lost network, it will try to reconnect to RVI Node Server to resume the update.
   * Upon the SOTA Client's "Cancel Software Download" request, Software Loading Manager can interrupt the update download process.

### <a name="WL-B10-5">[WL-B10-5](#WL-B10-5) SOTA Client sends lowest numbered data block to Software Loading Manager</a>

SOTA Client can send the lowest numbered data block to Software Loading Manager using JSON on port 80 over HTTP.

   * Upon SOTA Client's request, Software Loading Manager can accept the lowest numbered data block and if the data block is received without errors, it will acknowledge of successful data block receipt in the response body and a 200 status code.
   * Upon SOTA Client's request, Software Loading Manager can accept the lowest numbered data block and if the data block has been received before the data block will be discarded and the next data block will be requested.

### <a name="WL-B10-6">[WL-B10-6](#WL-B10-6) Software Loading Manager sends Install Report to SOTA Client</a>

Software Loading Manager can send an Install Report to SOTA Client the using JSON on port 80 over HTTP.

   * Upon the Software Loading Manager's request, SOTA Client can accept the Install Report and if the installation was finished without errors, it will respond with '*Package ID* success' in the response body and a 200 status code.
   * Upon the Software Loading Manager's request, SOTA Client can accept the Install Report and if the VIN is already marked as complete, it will respond with '*Package ID* failed' in the response body and a 409 status code.
   * Upon the Software Loading Manager's request, SOTA Client can accept the Install Report and if the VIN is already marked as failed, it will respond with '*Package ID* failed' in the response body and a 409 status code.


## <a name="b-13">[B-13](#) Web Server - Physical Package Repository / Filesystem</a>

### <a name="WL-B13-1">[WL-B13-1](#WL-B13-1) Web Server sends the binary package to Filesystem</a>

The Web Server can upload a binary package  to the Filesystem.

  * Upon the Web Server's request, the Filesystem can write a binary package if the request is processed
    without errors, it will respond with a success error code.
  * Upon the Web Server's request, the Filesystem can write a binary package if the request is processed
    with errors, it will respond with a failure error code.

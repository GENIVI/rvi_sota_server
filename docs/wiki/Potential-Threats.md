# Potential Security Threats

| ID                                | Label                                                 | STRIDE Attribute       | Assets                                                                         | Interactions                               | Attack potential vector                 | Attack potential rating | Damage potential vector | Damage potential rating | Risk | Diagram                               |
|-----------------------------------|-------------------------------------------------------|------------------------|--------------------------------------------------------------------------------|--------------------------------------------|-----------------------------------------|-------------------------|-------------------------|-------------------------|------|---------------------------------------|
| +++                               | +++++++++++++++++++++++                               |                        | +++++++++++++++++++                                                            | +++++++++++++++++++++++++++++              | +++++++++++++++++++++++++++++++         | +++++                   | ++++++++++++++++++      | +++++                   | +++  | +++++++++++++++++++++++++++++++++++++ |
| <a name="t01_row">[T01](#t01)</a> | Spoofing of Admin UI                                  | Spoofing               | Client credentials                                                             | Anyone -&gt; Web API                       | exp:pro acc:unl tim:day equ:std kno:pub | 4                       | fin:xxx ope:xxx saf:xxx | 0                       | 0    | Level 0 - Context Diagram             |
| <a name="t02_row">[T02](#t02)</a> | Node sniffing                                         | Information Disclosure | Client credentials                                                             | Anyone -&gt; Web API                       | exp:exp acc:unl tim:wee equ:std kno:pub | 11                      | fin:xxx ope:xxx saf:xxx | 0                       | 0    | Level 0 - Context Diagram             |
| <a name="t03_row">[T03](#t03)</a> | Invalid package configuration injection               | Tampering              | Vehicle installed software state                                               | Admin UI -&gt; Web API                     |                                         | 0                       |                         | 0                       | 0    | Level 0 - Context Diagram             |
| <a name="t04_row">[T04](#t04)</a> | Repudiation of vehicle SW configuration               | Repudiation            | Vehicle installed software state                                               | Admin UI -&gt; Web API                     |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t05_row">[T05](#t05)</a> | Denial of Service attack                              | Denial of service      | Quality of Service                                                             | Anyone -&gt; Web API                       |                                         | 0                       |                         | 0                       | 0    | Level 0 - Context Diagram             |
| <a name="t06_row">[T06](#t06)</a> | Brute force password cracking                         | Elevation of Privilege | Acces to vehicle data, vehicle installed software state                        | Admin UI -&gt; Web API                     |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t07_row">[T07](#t07)</a> | Spoofing of External Resolver                         | Spoofing               | Vehicle installed software state                                               | External Resolver -&gt; SOTA Server        |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t08_row">[T08](#t08)</a> | Tampering of package dependencies                     | Tampering              | Vehicle installed software state, vehicle software security                    | External Resolver -&gt; SOTA Server        |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t09_row">[T09](#t09)</a> | Information leak of installed packages per VIN        | Information Disclosure | Vehicle installed software state                                               | External Resolver -&gt; SOTA Server        |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t10_row">[T10](#t10)</a> | RVI node spoofing                                     | Spoofing               | Information on vehicle software state                                          | SOTA Server -&gt; RVI Node                 |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t11_row">[T11](#t11)</a> | Retrieving false package installation results         | Tampering              | Package information, software configuration per VIN                            | RVI Node -&gt; SOTA Server                 |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t12_row">[T12](#t12)</a> | Denying the installation of a software package        | Repudiation            | Vehicle software security                                                      | RVI Node -&gt; SOTA Server                 |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t13_row">[T13](#t13)</a> | RVI node sniffing                                     | Information Disclosure | Vehicle installed software state                                               | SOTA Server -&gt; RVI Node                 |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t14_row">[T14](#t14)</a> | Denial of Service attack                              | Denial of service      | Service Availability                                                           | Anyone -&gt; RVI Node                      |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t15_row">[T15](#t15)</a> | Logistics & Provisioning API Spoofing                 | Spoofing               | Vehicle software security                                                      | Logistics & Provisioning -&gt; SOTA Server |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t16_row">[T16](#t16)</a> | Associating a part number with a malicious package    | Tampering              | Vehicle software security                                                      | Logistics & Provisioning -&gt; SOTA Server |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t17_row">[T17](#t17)</a> | VIN, part number, configurations compromise           | Information Disclosure | Corporate data, vehicle software configuration, vehicle software security.     | Logistics & Provisioning -&gt; SOTA Server |                                         |                         |                         |                         | 0    |                                       |
| <a name="t18_row">[T18](#t18)</a> | Charging & Billing API Spoofing                       | Spoofing               | Financial loss                                                                 | Charging & Billing -&gt; SOTA Server       |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t19_row">[T19](#t19)</a> | Associating an update with the wrong cost             | Tampering              | Financial loss                                                                 | Charging & Billing -&gt; SOTA Server       |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t20_row">[T20](#t20)</a> | VIN, configurations, financial information compromise | Information Disclosure | Financial loss                                                                 | Charging & Billing -&gt; SOTA Server       |                                         |                         |                         |                         | 0    | Level 0 - Context Diagram             |
| <a name="t21_row">[T21](#t21)</a> | Spoofing SOTA Core Server                             | Spoofing               | User data, VINs, Package information, Vehicle configurations                   | SOTA Core Server -&gt; MariaDB             |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |
| <a name="t22_row">[T22](#t22)</a> | Persistence of false data                             | Tampering              | User data, VINs, Package information, Vehicle configurations                   | Anyone -&gt; MariaDB                       |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |
| <a name="t23_row">[T23](#t23)</a> | Compromise of sensitive data                          | Information Disclosure | User data, VINs, Package information, Vehicle configurations                   | Anyone -&gt; MariaDB                       |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |
| <a name="t24_row">[T24](#t24)</a> | Denial of Service attack                              | Denial of service      | Service Availability                                                           | Anyone -&gt; MariaDB                       |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |
| <a name="t25_row">[T25](#t25)</a> | Getting admin rights                                  | Elevation of Privilege | User data, VINs, Package information, Vehicle configurations, data store state | Anyone -&gt; MariaDB                       |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |
| <a name="t26_row">[T26](#t26)</a> | Spoofing Internal Resolver                            | Spoofing               | User data, VINs, Package information, Vehicle configurations                   | Internal Resolver -&gt; MariaDB            |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |
| <a name="t27_row">[T27](#t27)</a> | In-vehicle process spoofing                           | Spoofing               | VINs, Package information                                                      | Anyone -&gt; SOTA Client                   |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |
| <a name="t28_row">[T28](#t28)</a> | SOTA Client sniffing                                  | Tampering              | VINs, Package information                                                      | In vehicle process -&gt; SOTA Client       |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |
| <a name="t29_row">[T29](#t29)</a> | Denial of Service attack                              | Denial of service      | Service Availability                                                           | Anyone -&gt; SOTA Client                   |                                         |                         |                         |                         | 0    | Level 1 - SOTA Server Context Diagram |

## [T01](#t01_row)

-   **Description:** A malicious person may try to gain administrator-level access to the web server's admin console to gain information about the system's structure.
-   **Rationale:**

## [T02](#t02_row)

-   **Description:** Sniffing software installed on the load balancing node may lead to the leak of the credentials of all clients connecting to the given cluster.
-   **Rationale:** A node sniffer could intercept the credentials of all incoming client connections.

## [T03](#t03_row)

-   **Description:** An invalid combination of software packages or versions may be attempted to be installed in order to create exploits or vulnerabilities.
-   **Rationale:**

## [T04](#t04_row)

-   **Description:** A configuration that may create exploits or vulnerabilities on the vehicle's software environment may be injected and a modified web interface may be used to repudiate the traces of the installation of the malicious configuration to a group of vehicles.
-   **Rationale:** Javascript code running on the browser can be modified and a repudiation attack against a group of vehicles may be attempted.

## [T05](#t05_row)

-   **Description:** A large amount of false or dummy requests from a malicious group may saturate the load balancer and prevent the service of legitimate clients.
-   **Rationale:** An easy to orchestrate DOS attack may disrupt the system's operations.

## [T06](#t06_row)

-   **Description:** A password cracker may break an account and provide access to a malicious, unauthorized user.
-   **Rationale:** Weak passwords may be cracked in a short amount of time with a password cracker.

## [T07](#t07_row)

-   **Description:** A malicious person may use a fake external resolver to gain information about the workings of the SOTA server and leak information about VINs and the software packages they have installed.
-   **Rationale:** A fake external resolver may be used to gain information about the SOTA server which may be used in a composite attack vector.

## [T08](#t08_row)

-   **Description:** A maliciously compiled dependency tree may include dependencies that open vulnerabilities or provide access to attackers, or it sets package versions known to have bugs or open vulnerabilities.
-   **Rationale:** A package that may open a backdoor, or that functions as a Trojan can be added as a package dependency.

## [T09](#t09_row)

-   **Description:** A verbose API may reveal information on which software packages are installed on which vehicle, which is unecessary on a need-to-know basis.
-   **Rationale:**

## [T10](#t10_row)

-   **Description:** An RVI node may be spoofed and become a leaking sink for vehicle and package data.
-   **Rationale:** A spoofed RVI node may crause a huge leak of sensitive information.

## [T11](#t11_row)

-   **Description:** A compromised RVI node may send incorrect status reports for package installation in order to skip the installation of bugfixes or exploit fixes, intercept packages, and acquire information about VINs and their software configuration.
-   **Rationale:** Knowing or sending over to a spoofed vehicle software packages may help to analyze them and find potential attack vectors.

## [T12](#t12_row)

-   **Description:** A compromised RVI node may block the installation of security-critical software packages and return a false status that they were installed, leaving open security vulnerabilities.
-   **Rationale:** A non-installed package may leave backdoors and exploits open for attackers.

## [T13](#t13_row)

-   **Description:** Sniffing software installed on a RVI node can intercept VINs, their configuration, and the latest package configuration for every VIN.
-   **Rationale:** A node sniffer may intercept all VINs and their associated software packages.

## [T14](#t14_row)

-   **Description:** A Denial-Of-Service (DOS) attack may block the installation of software packages or updates.
-   **Rationale:** A DOS attack on the RVI node/s may block the installation of zero-days or other crucial updates and leave vehicles vulnerable for a prolonged period of time.

## [T15](#t15_row)

-   **Description:** An attacker may use a spoofed Logistics API to install trojans or packages with known vulnerabilities.
-   **Rationale:** Responses from a spoofed Logistics API may lead to the installation of malicious or vulnerable packages.

## [T16](#t16_row)

-   **Description:** An attacker may assign a valid part number to a malicious package which may provide backdoor or related system vulnerabilities after being installed.
-   **Rationale:** A malicious packaged related with a valid part number will be installed without any warning or any alarm raised.

## [T17](#t17_row)

-   **Description:** A malicious person may try to intercept the data exchanged between the SOTA server and the Logistics & Provisioning API.
-   **Rationale:** Information leak may compromise sensitive corporate and vehicle data.

## [T18](#t18_row)

-   **Description:** An attacker may used a spoofed Billing API to install updates without being charged or by charging a third person excessively.
-   **Rationale:** Responses from a spoofed Billing API may lead to the installation of updates for no or excessive cost.

## [T19](#t19_row)

-   **Description:** A compromised Charging & Billing endpoint may provide false charging information.
-   **Rationale:**

## [T20](#t20_row)

-   **Description:** A malicious person may try to intercept the data exchanged between the SOTA server and the Charging & Billing API.
-   **Rationale:** Information leak may compromise sensitive corporate and vehicle data.

## [T21](#t21_row)

-   **Description:** A spoofed SOTA Server may retrieve most of the sensitive data stored in the data store.
-   **Rationale:** A spoofed SOTA Server may retrieve most of the sensitive data stored in the datastore.

## [T22](#t22_row)

-   **Description:** A MariaDB client with access to the data store can manipulate the persisted data.
-   **Rationale:** Persisting false data in the datastore may open the door for more pervasive attack vectors.

## [T23](#t23_row)

-   **Description:** A MariaDB client with access to the data store can retrieve all of the sensitive data stored in it.
-   **Rationale:**

## [T24](#t24_row)

-   **Description:** An attacker may orchestrate a Denial-Of-Service (DOS) attack to interrupt the system's operation or as part of a phishing attack.
-   **Rationale:**

## [T25](#t25_row)

-   **Description:** A malicious user may pursue elevating his access rights to administrator or superuser, allowing him to perform any arbitrary operation on the data store.
-   **Rationale:** Getting administrator rights can lead to data theft, tampering and complete loss of data.

## [T26](#t26_row)

-   **Description:** A spoofed External Resolver may retrieve most of the sensitive data stored in the data store.
-   **Rationale:** A spoofed External Resolver may retrieve most of the data stored in the datastore.

## [T27](#t27_row)

-   **Description:** A malicious in-vehicle process can attempt to exchange data with the SOTA Client and intercept information about the vehicle's software state.
-   **Rationale:** A third party process can intercept information about every package installed from an unsecured client.

## [T28](#t28_row)

-   **Description:** A malicious in-vehicle process can attempt to intercept the communication between the SOTA Client and the RVI Node and alter the contents of the messages before delivering them to the SOTA Client.
-   **Rationale:** A third party process may attempt to intercept the communication between the SOTA Client and the RVI node and alter the contents of the received data.

## [T29](#t29_row)

-   **Description:** An attacker may orchestrate a Denial-Of-Service (DOS) attack to interrupt the system's operation or as part of a phisinh attack.
-   **Rationale:**


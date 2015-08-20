# SOTA Admin GUI User Manual

This is the user manual for the SOTA Admin Web GUI. Development is currently at an early stage, but this document will be updated as features are added.

## Signing in

Once the sota-server is built and running, the Admin Browser will be available at <http://localhost:9000>. User authentication is not yet implemented.

## Managing Vehicles

### Adding a VIN

To add a VIN, click the *Vehicles* tab at the top of the page. Enter a valid VIN number (exactly 17 characters long and consisting only of alphanumeric characters) in the **Vehicle Name** field, and click the **Add Vehicle** button. If the addition was successful, the newly added VIN will appear in the list at the bottom of the page. If not, an error message will be displayed.

### Managing VINs

#### Adding a software package to a VIN

#### Adding a hardware component to a VIN

### VIN filters

You can create filters to powerfully select sub-groups of VINs. For example, you might wish to create a filter that matches only vehicles that have the hardware component **AcmeDVDPlayer**, so that you can send a firmware update to only the relevant vehicles.

#### Filter syntax

SOTA has a filter syntax to create these filters; currently the valid terms are **vin_matches**, **has_package**, and **has_component**, and the boolean operators **NOT**, **AND**, and **OR**.

Example: For a filter that matched all VINs beginning with 12ABC, that have the AcmeDVDPlayer installed, the syntax would be `vin_matches "^12ABC" AND has_component "AcmeDVDPlayer"`.

#### Creating a VIN filter

To create a new filter, click the **Filters** tab at the top of the page. Enter a unique name in the **Filter Name** field, and enter your expression in the **Filter Expression** field. Click the **Add Filter** button. If the filter was successfully created, you should see a message saying `Added Filter [filtername] successfully.` If the name you gave was already taken, or if the filter's syntax was invalid, you will see an error message instead.

## Managing Software Packages

### Adding a software package 

To add a software package, you will need to upload a package file and give it a unique **Package ID**. A package ID consists of a package name and a version number. The name must be alphanumeric, and the version must consist of three numbers separated by periods. (For example, 3.0.4 and 0.115.33 are both valid version numbers, but 1.3 and 2.7.22b are not.) 

Click the **Packages** tab at the top of the page. Enter a Package Name, Version, and optionally a description and vendor name in the appropriate fields. Then, select a file to upload. Click **Add Package**. If the upload was successful, you will see a status message confirming it. If there was a problem, you will see an appropriate error message.

### Searching for packages 

### Associating a package with a filter

To associate a package with a filter, click the **Associations** tab at the top of the page. Enter a package name, version and filter name in the appropriate fields, then click **Create Association**.

This functionality will be used to select a group of vehicles to send specific software updates to.

## Managing Hardware Components

### Adding a hardware component

### Searching for vehicles by installed component 

## Triggering Updates

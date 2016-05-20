define(function(require) {

  var React = require('react'),
      Router = require('react-router'),
      SyncPackages = require('../vehicles/sync-packages'),
      AddPackageManually = require('../packages/add-package-manually-component'),
      ListOfPackages = require('../packages/list-of-packages'),
      ListOperationResultsForVin = require('../operation-results/list-operation-results-for-vin'),
      QueuedPackages = require('../packages/list-of-queued-packages-for-vin'),
      PackageHistory = require('../packages/package-update-history-for-vin'),
      AddComponent = require('../components/add-component-to-vin'),
      ComponentsOnVin = require('../components/list-of-components-on-vin'),
      FirmwareOnVin = require('../firmware/list-of-firmware-on-vin'),
      db = require('stores/db'),
      SearchBar = require('../search-bar');

  var VehiclesPageComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    render: function() {
      var params = this.context.router.getCurrentParams();
      return (
      <div>
        <div>
          <h1>Vehicles &gt; {params.vin}</h1>
        </div>
        <div className="row">
          <div className="col-md-12">
            <h2>Installed Packages</h2>
            <ListOfPackages
              Packages={db.packagesForVin}
              PollEventName="poll-packages"
              DispatchObject={{actionType: 'get-packages-for-vin', vin: params.vin}}
              DisplayCampaignLink={false}/>
            <AddPackageManually Vin={params.vin}/>
            <SyncPackages Vin={params.vin}/>
            <h2>Installed Firmware</h2>
            <FirmwareOnVin Firmware={db.firmwareOnVin} Vin={params.vin}/>
            <h2>Installed Components</h2>
            <ComponentsOnVin Components={db.componentsOnVin} Vin={params.vin}/>
            <AddComponent Vin={params.vin}/>
            <ListOperationResultsForVin OperationResultsForVin={db.operationResultsForVin} Vin={params.vin}/>
            <h2>Package Updates</h2>
            <QueuedPackages Packages={db.packageQueueForVin} Vin={params.vin}/>
            <PackageHistory Packages={db.packageHistoryForVin} Vin={params.vin}/>
          </div>
        </div>
      </div>
    );}
  });

  return VehiclesPageComponent;

});

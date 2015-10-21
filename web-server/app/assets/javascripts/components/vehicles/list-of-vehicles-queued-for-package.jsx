define(function(require) {

  var React = require('react'),
      _ = require('underscore'),
      Router = require('react-router'),
      Fluxbone = require('../../mixins/fluxbone'),
      togglePanel = require('../../mixins/toggle-panel'),
      SotaDispatcher = require('sota-dispatcher');

  var ListOfVehiclesQueuedForPackage = React.createClass({
    mixins: [togglePanel],
    componentWillUnmount: function(){
      this.props.Vehicles.removeWatch("poll-vehicles-queued-for-package");
    },
    componentWillMount: function(){
      this.props.Vehicles.addWatch("poll-vehicles-queued-for-package", _.bind(this.forceUpdate, this, null));
    },
    refreshData: function() {
      SotaDispatcher.dispatch({actionType: 'get-vehicles-queued-for-package', name: this.props.PackageName, version: this.props.PackageVersion});
    },
    label: "Queued Vehicles",
    panel: function() {
      var vehicles = _.map(this.props.Vehicles.deref(), function(vehicle) {
        return (
          <tr key={vehicle}>
            <td>
              <Router.Link to='vehicle' params={{vin: vehicle}}>
              { vehicle }
              </Router.Link>
            </td>
          </tr>
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                VINs
              </td>
            </tr>
          </thead>
          <tbody>
            { vehicles }
          </tbody>
        </table>
      );
    }
  });

  return ListOfVehiclesQueuedForPackage;
});

define(function(require) {

  var React = require('react'),
      _ = require('underscore'),
      Router = require('react-router'),
      Fluxbone = require('../../mixins/fluxbone'),
      SotaDispatcher = require('sota-dispatcher');

  var ListOfVehicles = React.createClass({
    componentWillUnmount: function(){
      this.props.Vehicles.removeWatch("poll-vehicles");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({actionType: 'search-vehicles-by-regex', regex: "."});
      this.props.Vehicles.addWatch("poll-vehicles", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var vehicles = _.map(this.props.Vehicles.deref(), function(vehicle) {
        return (
          <tr key={vehicle.deviceId}>
            <td>
              <Router.Link to='vehicle' params={{vin: vehicle.deviceName}}>
              { vehicle.deviceName }
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
                VIN
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

  return ListOfVehicles;
});

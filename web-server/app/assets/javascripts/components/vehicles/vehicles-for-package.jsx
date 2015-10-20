define(function(require) {

  var React = require('react'),
      _ = require('underscore'),
      db = require('stores/db'),
      ListOfVehicles = require('./list-of-vehicles'),
      togglePanel = require('../../mixins/toggle-panel'),
      SotaDispatcher = require('sota-dispatcher');

  var VehiclesForPackage = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [togglePanel],
    componentWillUnmount: function(){
      this.props.VehiclesForPackage.removeWatch("poll-vehicles-for-package");
    },
    componentWillMount: function(){
      this.refreshData();
      this.props.VehiclesForPackage.addWatch("poll-vehicles-for-package", _.bind(this.forceUpdate, this, null));
    },
    refreshData: function() {
      var params = this.context.router.getCurrentParams();
      SotaDispatcher.dispatch({
        actionType: 'get-vehicles-for-package',
        name: params.name,
        version: params.version
      });
    },
    label: "Vehicles with this package",
    panel: function() {
      var vehicles = _.map(this.props.VehiclesForPackage.deref(), function(vehicle) {
        return (
          <li className='list-group-item'>
            {vehicle}
          </li>
        );
      });
      return (
        <div>
          <ul className='list-group'>
            {vehicles}
          </ul>
        </div>
      );
    }
  });

  return VehiclesForPackage;
});

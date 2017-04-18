define(function(require) {

  var React = require('react'),
      _ = require('underscore'),
      Router = require('react-router'),
      togglePanel = require('../../mixins/toggle-panel'),
      SotaDispatcher = require('sota-dispatcher');

  var VehiclesListPanel = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [togglePanel],
    componentWillUnmount: function(){
      this.props.Vins.removeWatch(this.props.PollEventName);
      if(this.props.VehiclesPollEventName)
        this.props.Vehicles.removeWatch(this.props.VehiclesPollEventName);
    },
    componentWillMount: function(){
      this.refreshData();
      this.props.Vins.addWatch(this.props.PollEventName, _.bind(this.forceUpdate, this, null));
      if(this.props.VehiclesPollEventName)
        this.props.Vehicles.addWatch(this.props.VehiclesPollEventName, _.bind(this.forceUpdate, this, null));
    },
    refreshData: function() {
      SotaDispatcher.dispatch(this.props.DispatchObject);
      if(this.props.VehiclesDispatchObject)
        SotaDispatcher.dispatch(this.props.VehiclesDispatchObject);
    },
    label: function() {return this.props.Label},
    panel: function() {
      var vehicles = [];
      if(!_.isUndefined(this.props.Vehicles.deref())) {
        vehicles = _.map(this.props.Vins.deref(), function(vehicle) {
          var foundDevice = _.findWhere(this.props.Vehicles.deref(), {uuid: vehicle.vin});
          return (
            <li className='list-group-item' key={vehicle.vin}>
              <Router.Link to='vehicle' params={{vin: vehicle.vin, id: foundDevice.deviceName}}>
                  {foundDevice.deviceName}
              </Router.Link>
            </li>
          );
        }, this);
      }
      return (
        <div>
          <ul className='list-group'>
            {vehicles}
          </ul>
        </div>
      );
    }
  });

  return VehiclesListPanel;
});

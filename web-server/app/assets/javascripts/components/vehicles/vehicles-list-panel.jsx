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
      this.props.Vehicles.removeWatch(this.props.PollEventName);
    },
    componentWillMount: function(){
      this.refreshData();
      this.props.Vehicles.addWatch(this.props.PollEventName, _.bind(this.forceUpdate, this, null));
    },
    refreshData: function() {
      SotaDispatcher.dispatch(this.props.DispatchObject);
    },
    label: function() {return this.props.Label},
    panel: function() {
      var vehicles = _.map(this.props.Vehicles.deref(), function(vehicle) {
        console.log(vehicle)
        return (
          <li className='list-group-item' key={vehicle}>
            <Router.Link to='vehicle' params={{vin: vehicle.vin}}>
            {vehicle}
            </Router.Link>
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

  return VehiclesListPanel;
});

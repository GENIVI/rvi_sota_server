define(function(require) {

  var React = require('react'),
      _ = require('underscore'),
      db = require('stores/db'),
      ListOfVehicles = require('./list-of-vehicles'),
      SotaDispatcher = require('sota-dispatcher');

  var VehiclesForPackage = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.VehiclesForPackage.removeWatch("poll-vehicles-for-package");
    },
    componentWillMount: function(){
      this.fetchVehiclesForPackage();
      this.props.VehiclesForPackage.addWatch("poll-vehicles-for-package", _.bind(this.forceUpdate, this, null));
    },
    fetchVehiclesForPackage: function() {
      var params = this.context.router.getCurrentParams();
      SotaDispatcher.dispatch({
        actionType: 'get-vehicles-for-package',
        name: params.name,
        version: params.version
      });
    },
    getInitialState: function() {
      return {collapsed: true};
    },
    toggleVins: function() {
      this.fetchVehiclesForPackage();
      this.setState({collapsed: !this.state.collapsed});
    },
    render: function() {
      var vehicles = _.map(this.props.VehiclesForPackage.deref(), function(vehicle) {
        return (
          <li className='list-group-item'>
            {vehicle}
          </li>
        );
      });
      return (
        <div>
          <button type="button" className="btn btn-primary" onClick={this.toggleVins}>
            {this.state.collapsed ? 'View' : 'Hide'} Vehicles with this package
          </button>
          <div className={this.state.collapsed ? 'hide' : ''}>
            <ul className='list-group'>
              {vehicles}
            </ul>
          </div>
        </div>
      );
    }
  });

  return VehiclesForPackage;
});

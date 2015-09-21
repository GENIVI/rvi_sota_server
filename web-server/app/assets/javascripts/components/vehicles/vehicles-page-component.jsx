define(function(require) {

  var React = require('react'),
      VehiclesComponent = require('./vehicles-component'),
      VehiclesHeaderComponent = require('./vehicles-header-component'),
      SearchBar = require('../search-bar'),
      VehicleStore = require('../../stores/vehicles');

  var VehiclesPageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <div>
          <VehiclesHeaderComponent/>
        </div>
        <div className="row">
          <div className="col-md-12">
            <SearchBar label="Filter" event="vehicles-filter"/>
            <VehiclesComponent VehicleStore={VehicleStore}/>
          </div>
        </div>
      </div>
    );}
  });

  return VehiclesPageComponent;

});

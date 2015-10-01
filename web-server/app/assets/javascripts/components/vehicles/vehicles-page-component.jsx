define(function(require) {

  var React = require('react'),
      ListOfVehicles = require('./list-of-vehicles'),
      VehiclesHeaderComponent = require('./vehicles-header-component'),
      db = require('stores/db'),
      Errors = require('../errors'),
      SearchBar = require('../search-bar');

  var VehiclesPageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <div>
          <VehiclesHeaderComponent/>
        </div>
        <div className="row">
          <div className="col-md-12">
            <Errors />
            <SearchBar label="Filter" event="search-vehicles-by-regex"/>
            <ListOfVehicles Vehicles={db.searchableVehicles}/>
          </div>
        </div>
      </div>
    );}
  });

  return VehiclesPageComponent;

});

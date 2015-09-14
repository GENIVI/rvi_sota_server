define(function(require) {
  var React = require('react'),
      VehicleStore = require('../../stores/vehicles'),
      AddVehicleComponent = require('./add-vehicle-component');

  var VehiclesHeaderComponent = React.createClass({
    render: function() {
      return (
      <div>
        <div className="row">
          <div className="col-md-12">
            <h1>
              Vehicle Identification Numbers
            </h1>
          </div>
        </div>
        <div className="row">
          <div className="col-md-8">
            <p>
            </p>
          </div>
        </div>
        <AddVehicleComponent VehicleStore={VehicleStore}/>
      </div>
    );}
  });

  return VehiclesHeaderComponent;

});

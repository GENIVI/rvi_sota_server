define(['jquery', 'react', '../mixins/fluxbone', 'sota-dispatcher'], function($, React, Fluxbone, SotaDispatcher) {

  var VehicleComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Vehicle')
    ],
    handleUpdateVehicle: function() {
      SotaDispatcher.dispatch({
        actionType: "package-updateVehicle",
        package: this.props.Vehicle
      });
    },
    render: function() {
      return (
        <li className="list-group-item">
          { this.props.Vehicle.get('vin') }
        </li>
      );
    }
  });

  return VehicleComponent;
});

define(['react', '../mixins/fluxbone', './vehicle-component', 'sota-dispatcher'], function(React, Fluxbone, VehicleComponent, SotaDispatcher) {

  var Vehicles = React.createClass({
    mixins: [
      Fluxbone.Mixin("VehicleStore", "sync change")
    ],
    render: function() {
      var vehicles = this.props.VehicleStore.models.map(function(Vehicle) {
        return (
          <VehicleComponent Vehicle = { Vehicle }/>
        );
      });
      return (
        <div>
          <ul className="list-group">
            { vehicles }
          </ul>
        </div>
      );
    }
  });

  return Vehicles;
});

define(['react', '../../mixins/fluxbone', './vehicle-component', 'sota-dispatcher'], function(React, Fluxbone, VehicleComponent, SotaDispatcher) {

  var Vehicles = React.createClass({
    mixins: [
      Fluxbone.Mixin("VehicleStore", "sync")
    ],
    render: function() {
      var vehicles = this.props.VehicleStore.models.map(function(Vehicle) {
        return (
          <VehicleComponent Vehicle = { Vehicle }/>
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                VINs
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

  return Vehicles;
});

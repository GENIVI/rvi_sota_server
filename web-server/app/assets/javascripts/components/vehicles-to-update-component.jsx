define(['react', '../mixins/fluxbone', './vehicle-component', 'sota-dispatcher'], function(React, Fluxbone, VehicleComponent, SotaDispatcher) {

  var VehiclesToUpdate = React.createClass({
    mixins: [
      Fluxbone.Mixin("store", "sync change")
    ],
    getVINs: function() {
      SotaDispatcher.dispatch({
        actionType: "get-affected-vins"
      });
    },
    render: function() {
      var vehicles = this.props.store.getVINs().map(function(vin) {
        return (
          <li className="list-group-item">
            { vin }
          </li>
        );
      });
      return (
        <div>
          <button type="button" className="btn btn-primary" onClick={this.getVINs}>View Affected VINs</button>
          <ul className="list-group">
            { vehicles }
          </ul>
        </div>
      );
    }
  });

  return VehiclesToUpdate;
});

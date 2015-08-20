define(['react', '../mixins/serialize-form', '../mixins/request-status', 'sota-dispatcher'], function(React, serializeForm, RequestStatus, SotaDispatcher) {

  var AddVehicleComponent = React.createClass({
    mixins: [
      RequestStatus.Mixin("VehicleStore")
    ],
    handleSubmit: function(e) {
      e.preventDefault();

      payload = serializeForm(this.refs.form);
      SotaDispatcher.dispatch({
        actionType: 'vehicle-add',
        vehicle: payload
      });
    },
    render: function() {
      return (
        <div>
          <form ref='form' onSubmit={this.handleSubmit}>
            <div className="form-group">
              <label htmlFor="name">Vehicle Name</label>
              <input type="text" className="form-control" name="vin" ref="vin" placeholder="VIN"/>
            </div>
            <div className="form-group">
              <button type="submit" className="btn btn-primary">Add Vehicle</button>
            </div>
            <div className="form-group">
              { this.state.postStatus }
            </div>
          </form>
        </div>
      );
    }
  });

  return AddVehicleComponent;
});

define(['react', '../../mixins/serialize-form', '../../mixins/request-status', 'sota-dispatcher'], function(React, serializeForm, RequestStatus, SotaDispatcher) {

  var AddVehicleComponent = React.createClass({
    toggleForm: function() {
      this.setState({showForm: !this.state.showForm});
    },
    getInitialState: function() {
      return {showForm: false};
    },
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
      var form = (
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
      );
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <button className="btn btn-primary pull-right" onClick={this.toggleForm}>
                { this.state.showForm ? "HIDE" : "NEW VIN" }
              </button>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-md-12">
              { this.state.showForm ? form : null }
            </div>
          </div>
        </div>
      );
    }
  });

  return AddVehicleComponent;
});

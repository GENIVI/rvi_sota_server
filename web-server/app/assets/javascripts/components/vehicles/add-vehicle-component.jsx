define(function(require) {
  var React = require('react'),
      serializeForm = require('../../mixins/serialize-form'),
      toggleForm = require('../../mixins/toggle-form'),
      db = require('../../stores/db'),
      SotaDispatcher = require('sota-dispatcher');

  var AddVehicleComponent = React.createClass({
    mixins: [
      toggleForm
    ],
    handleSubmit: function(e) {
      e.preventDefault();

      payload = serializeForm(this.refs.form);
      SotaDispatcher.dispatch({
        actionType: 'create-vehicle',
        vehicle: payload
      });
    },
    buttonLabel: "NEW VIN",
    form: function() {
      return (
        <form ref='form' onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Vehicle Name</label>
            <input type="text" className="form-control" name="vin" ref="vin" placeholder="VIN"/>
          </div>
          <div className="form-group">
            <button type="submit" className="btn btn-primary">Add Vehicle</button>
          </div>
        </form>
      );
    }
  });

  return AddVehicleComponent;
});

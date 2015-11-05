define(function(require) {

    var React = require('react'),
        serializeForm = require('../../mixins/serialize-form'),
        SotaDispatcher = require('sota-dispatcher'),
        toggleForm = require('../../mixins/toggle-form');

    var AddComponent = React.createClass({
      mixins: [
        toggleForm
      ],
      handleSubmit: function(e) {
        e.preventDefault();

        var partNumber = React.findDOMNode(this.refs.partNumber).value.trim();
        SotaDispatcher.dispatch({
            actionType: 'add-component-to-vin',
            partNumber: partNumber,
            vin: this.props.Vin
        });
      },
      buttonLabel: "Add Installed Component",
      form: function() {
        return (
          <div>
            <form ref='form' onSubmit={this.handleSubmit} encType="multipart/form-data">
              <div className="form-group">
                <label htmlFor="partNumber">Part Number</label>
                <input type="text" className="form-control" name="partNumber" ref="partNumber" placeholder="SomePart01"/>
              </div>
              <div className="form-group">
                <button type="submit" className="btn btn-primary">Add Component</button>
              </div>
            </form>
          </div>
        );}
    });

    return AddComponent;
});

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

        var payload = serializeForm(this.refs.form);
        SotaDispatcher.dispatch({
            actionType: 'create-component',
            component: payload
        });
      },
      buttonLabel: "NEW COMPONENT",
      form: function() {
        return (
          <div>
            <form ref='form' onSubmit={this.handleSubmit} encType="multipart/form-data">
              <div className="form-group">
                <label htmlFor="partNumber">Part Number</label>
                <input type="text" className="form-control" name="partNumber" ref="partNumber" placeholder="SomePart01"/>
              </div>
              <div className="form-group">
                <label htmlFor="description">Component Description</label>
                <textarea type="text" className="form-control" id="description" ref="description" name="description" placeholder='Description of the component' />
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

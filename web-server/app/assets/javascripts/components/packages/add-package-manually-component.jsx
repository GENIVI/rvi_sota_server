define(function(require) {

  var $ = require('jquery'),
      React = require('react'),
      _ = require('underscore'),
      Errors = require('../errors'),
      toggleForm = require('../../mixins/toggle-form'),
      sendRequest = require('../../mixins/send-request'),
      SotaDispatcher = require('sota-dispatcher');

  var AddPackageManuallyComponent = React.createClass({
    mixins: [
      toggleForm
    ],
    handleSubmit: function(e) {
      e.preventDefault();

      var pkgName = React.findDOMNode(this.refs.name).value.trim();
      var pkgVersion = React.findDOMNode(this.refs.version).value.trim();

      sendRequest.doPut('/api/v1/vehicles/' + this.props.Vin + "/package/" + pkgName + "/" + pkgVersion)
        .success(_.bind(function() {
          SotaDispatcher.dispatch({actionType: 'get-packages-for-vin', vin: this.props.Vin});
        }, this));

      React.findDOMNode(this.refs.name).value = '';
      React.findDOMNode(this.refs.version).value = '';
    },
    buttonLabel: "Add Installed Package",
    form: function() {
      return (
        <div>
          <form ref='form' onSubmit={this.handleSubmit}>
            <div className="form-group">
              <label htmlFor="name">Package Name</label>
              <input type="text" className="form-control" name="name" ref="name" placeholder="Package Name"/>
            </div>
            <div className="form-group">
              <label htmlFor="version">Version</label>
              <input type="text" className="form-control" name="version" ref="version" placeholder="1.0.0"/>
            </div>
            <div className="form-group">
              <button type="submit" className="btn btn-primary">Add Package</button>
            </div>
          </form>
          <Errors/>
        </div>
      );
    }
  });

  return AddPackageManuallyComponent;
});

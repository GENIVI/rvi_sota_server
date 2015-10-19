define(function(require) {

  var $ = require('jquery'),
      React = require('react'),
      _ = require('underscore'),
      serializeForm = require('../../mixins/serialize-form'),
      Router = require('react-router'),
      sendRequest = require('../../mixins/send-request');
      HandleFailMixin = require('../../mixins/handle-fail'),
      toggleForm = require('../../mixins/toggle-form'),
      SotaDispatcher = require('sota-dispatcher');

  var AddPackageComponent = React.createClass({
    mixins: [
      toggleForm,
      Router.Navigation,
      HandleFailMixin
    ],
    handleSubmit: function(e) {
      e.preventDefault();

      var payload = serializeForm(this.refs.form);
      //need to create this attribute since packages sent from core/resolver have it
      payload.id = {name: payload.name, version: payload.version};

      var data = new FormData();
      _.each(payload, function(k,v) {
        data.append(k, v);
      });

      var file = $('.file-upload')[0].files[0];
      data.append('file', file);

      var url = '/api/v1/packages/' + payload.name + '/' + payload.version;
      sendRequest.doPut(url, data, {form: true})
        .done(_.bind(function(pkgId) {
          this.transitionTo("/packages/" + pkgId.name + "/" + pkgId.version);
        }, this, payload.id))
        .fail(_.bind(function(xhr) {
          this.trigger("error", this, xhr);
        }, this));
    },
    buttonLabel: "NEW PACKAGE",
    form: function() {
      return (
        <div>
          <form ref='form' onSubmit={this.handleSubmit} encType="multipart/form-data">
            <div className="form-group">
              <label htmlFor="name">Package Name</label>
              <input type="text" className="form-control" name="name" ref="name" placeholder="Package Name"/>
            </div>
            <div className="form-group">
              <label htmlFor="version">Version</label>
              <input type="text" className="form-control" name="version" ref="version" placeholder="1.0.0"/>
            </div>
            <div className="form-group">
              <label htmlFor="description">Description</label>
              <input type="text" className="form-control" name="description" ref="description" placeholder="Description text"/>
            </div>
            <div className="form-group">
              <label htmlFor="vendor">Vendor</label>
              <input type="text" className="form-control" name="vendor" ref="vendor" placeholder="Vendor name"/>
            </div>
            <div className="form-group">
              <label htmlFor="binary">Package Binary</label>
              <input type="file" className="file-upload" name="file" />
            </div>
            <div className="form-group">
              <button type="submit" className="btn btn-primary">Add PACKAGE</button>
            </div>
          </form>
        </div>
      );
    }
  });

  return AddPackageComponent;
});

define(['jquery', 'react', '../mixins/serialize-form', '../mixins/fluxbone', '../mixins/request-status', './package-component', 'sota-dispatcher'], function($, React, serializeForm, Fluxbone, RequestStatus, PackageComponent, SotaDispatcher) {

  var Packages = React.createClass({
    mixins: [
      Fluxbone.Mixin("PackageStore"),
      RequestStatus.Mixin("PackageStore")
    ],
    handleSubmit: function(e) {
      e.preventDefault();

      var payload = serializeForm(this.refs.form);

      var file = $('.file-upload')[0].files[0];
      payload.binary = file;

      SotaDispatcher.dispatch({
        actionType: 'package-add',
        package: payload
      });
    },
    render: function() {
      var packages = this.props.PackageStore.models.map(function(package) {
        return (
          <PackageComponent Package = { package }/>
        );
      });
      return (
        <div>
          <form ref='form' onSubmit={this.handleSubmit} encType="multipart/form-data">
            <div className="form-group">
              <label htmlFor="name">Package Name</label>
              <input type="text" className="form-control" name="name" ref="name" placeholder="PACKAGE NAME"/>
            </div>
            <div className="form-group">
              <label htmlFor="version">Version</label>
              <input type="text" className="form-control" name="version" ref="version" placeholder="10"/>
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
              <input type="file" className="file-upload" name="binary" />
            </div>
            <div className="form-group">
              <button type="submit" className="btn btn-primary">Add PACKAGE</button>
            </div>
            <div className="form-group">
              { this.state.postStatus }
            </div>
          </form>
          <ul className="list-group">
            { packages }
          </ul>
        </div>
      );
    }
  });

  return Packages;
});

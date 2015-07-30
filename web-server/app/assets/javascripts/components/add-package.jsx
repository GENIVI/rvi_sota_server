define(['jquery', 'react', '../mixins/handle-fail', './update-package', '../util'], function($, React, HandleFailMixin, UpdatePackage, util) {

  var AddPackage = React.createClass({
    mixins: [HandleFailMixin],
    handleSubmit: function(e) {
      e.preventDefault();

      payload = util.serializeForm(this.refs.form);
      this.sendRequest(this.props.url, payload);
    },
    onSuccess: function(data) {
      this.setState({postStatus: "Added PACKAGE \"" + payload.name + "\" successfully"});
      React.render(<UpdatePackage url="/api/v1/install_campaigns" val={ payload.name } data={ data }/>, $('#updatePackage')[0]);
    },
    render: function() { return (
        <form ref='form' onSubmit={this.handleSubmit}>
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
          <button type="submit" className="btn btn-primary">Add PACKAGE</button>
          {this.state.postStatus}
        </form>
      );}
  });

  return AddPackage;
});

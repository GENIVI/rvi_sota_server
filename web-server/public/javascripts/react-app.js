(function () {
  var AddVin = React.createClass({
    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({vinPostStatus: ""});
      var vinText = React.findDOMNode(this.refs.vin).value.trim();
      if(!vinText || vinText.length !== 17 || !/^[A-Z0-9]+$/.test(vinText)) {
        this.setState({vinPostStatus: "VINs must consist of numbers and uppercase letters only."});
        return;
      }

      $.ajax({
        type: "POST",
        url: this.props.url,
        dataType: 'json',
        data: JSON.stringify({ "vin": vinText }),
        contentType: "application/json"
      })
        .done(function(data) {
          this.setState({vinPostStatus: "Added VIN \"" + vinText + "\" successfully"});
          React.findDOMNode(this.refs.vin).value = '';
        }.bind(this))
        .fail(function(data) {
          var res = JSON.parse(data.responseText);
          this.setState({vinPostStatus: res.errorMsg});
        }.bind(this));
    },
    getInitialState: function() {
      return {vinPostStatus : ""};
    },
    render: function() { return (
        <form onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="vin">VIN</label>
            <input type="text" className="form-control" id="vin" ref="vin" placeholder="VIN"/>
          </div>
          <button type="submit" className="btn btn-primary">Add VIN</button>
          {this.state.vinPostStatus}
        </form>
      );}
  });

  var AddPackage = React.createClass({
    handleSubmit: function(e) {
      e.preventDefault();
      var vals = [];
      $.each(this.refs.form.getDOMNode().elements, function(i,el) { vals.push($(el).val())});
      var package = {
        name: vals[0],
        version: vals[1]
      };
      $.ajax({
        type: "POST",
        url: this.props.url,
        data: JSON.stringify({ package: package }),
        dataType: 'json',
        contentType: "application/json"
      })
        .done(function(data) {
          this.setState({packagePostStatus: "Added PACKAGE \"" + package.name + "\" successfully"});
          React.findDOMNode(this.refs.package).value = '';
          React.render(<UpdatePackage url="/api/v1/install_campaigns" val={ package.name } data={ data }/>, document.getElementById('updatePackage'));
        }.bind(this))
        .fail(function(data) {
          this.setState({packagePostStatus: ""});
          console.log("error", data.responseText);
          var data = { id: 123 };
          React.render(<UpdatePackage url="/api/v1/install_campaigns" val={ package.name } data={ data }/>, document.getElementById('updatePackage'));
        }.bind(this));
    },
    getInitialState: function() {
      return {packagePostStatus : ""};
    },
    render: function() { return (
        <form ref='form' onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Package Name</label>
            <input type="text" className="form-control" id="name" ref="name" placeholder="PACKAGE NAME"/>
          </div>
          <div className="form-group">
            <label htmlFor="version">Version</label>
            <input type="text" className="form-control" id="version" ref="version" placeholder="10"/>
          </div>
          <button type="submit" className="btn btn-primary">Add PACKAGE</button>
          {this.state.packagePostStatus}
        </form>
      );}
  });

  var UpdatePackage = React.createClass({
    handleSubmit: function(e) {
      e.preventDefault();
      var timestamp = new Date();
      var payload = {
        id: this.props.data.id,
        priority: 10,
        start: timestamp,
        end: new Date(timestamp.getTime() + 10*60000)
      }
      $.ajax({
        type: "POST",
        dataType: 'json',
        url: this.props.url,
        data: JSON.stringify({ package: payload }),
        contentType: "application/json"
      })
        .done(function(data) {
          this.setState({packagePostStatus: "Updated PACKAGE \"" + this.props.val + "\" successfully"});
          React.findDOMNode(this.refs.package).value = '';
        }.bind(this))
        .fail(function(data) {
          var res = JSON.parse(data.responseText)
          this.setState({packagePostStatus: res.errorMsg});
        }.bind(this));
    },
    getInitialState: function() {
      return {packagePostStatus : ""};
    },
    render: function() { return (
        <form onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="package">{ this.props.val }</label>
            <input type="hidden" className="form-control" id="package" ref="package" value={ this.props.val }/>
          </div>
          <button type="submit" className="btn btn-primary">Update</button>
          {this.state.packagePostStatus}
        </form>
    );}
  });

  React.render(<AddVin url="/api/v1/vins" />, document.getElementById('app'));
  React.render(<AddPackage url="/api/v1/packages"/>, document.getElementById('package'));
})();

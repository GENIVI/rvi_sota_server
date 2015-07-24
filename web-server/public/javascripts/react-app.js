(function () {
  var HandleFailMixin = {
    getInitialState: function() {
      return {postStatus : ""};
    },
    sendPostRequest: function(url, data) {
      return $.ajax({
        type: "POST",
        url: url,
        dataType: 'json',
        data: JSON.stringify(data),
        contentType: "application/json"
      });
    },
    sendRequest: function(url, data) {
      this.sendPostRequest(url, data)
        .success(this.onSuccess.bind(this))
        .fail(this.onFail.bind(this));
    },
    onFail: function(data) {
      var res = JSON.parse(data.responseText);
      this.setState({postStatus: res.errorMsg});
    }
  };

  var AddVin = React.createClass({
    mixins: [HandleFailMixin],
    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({postStatus: ""});

      payload = serializeForm(this.refs.form);
      if (this.validate(payload.vin) === false) {
        this.setState({postStatus: "VINs must consist of numbers and uppercase letters only."});
        return;
      }
      this.sendRequest(this.props.url, payload);
    },
    onSuccess: function(data) {
      this.setState({postStatus: "Added VIN \"" + vinText + "\" successfully"});
      React.findDOMNode(this.refs.vin).value = '';
    },
    validate: function(vinText) {
      if (!vinText || vinText.length !== 17 || !/^[A-Z0-9]+$/.test(vinText)) {
        this.setState({postStatus: "VINs must consist of numbers and uppercase letters only."});
        return false;
      }
      return true;
    },
    render: function() { return (
        <form ref='form' onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="vin">VIN</label>
            <input type="text" className="form-control" id="vin" ref="vin" name="vin" placeholder="VIN"/>
          </div>
          <button type="submit" className="btn btn-primary">Add VIN</button>
          {this.state.postStatus}
        </form>
      );}
  });

  var AddPackage = React.createClass({
    mixins: [HandleFailMixin],
    handleSubmit: function(e) {
      e.preventDefault();

      payload = serializeForm(this.refs.form);
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
            <label htmlFor="verdor">Verdor</label>
            <input type="text" className="form-control" name="vendor" ref="vendor" placeholder="Vendor name"/>
          </div>
          <button type="submit" className="btn btn-primary">Add PACKAGE</button>
          {this.state.postStatus}
        </form>
      );}
  });

  var UpdatePackage = React.createClass({
    mixins: [HandleFailMixin],
    handleSubmit: function(e) {
      e.preventDefault();
      var timestamp = new Date();
      var payload = {
        packageId: this.props.data.id,
        priority: 10,
        startAfter: this.formatDatetime(timestamp),
        endBefore: this.formatDatetime(new Date(timestamp.getTime() + 10*60000))
      }
      this.sendRequest(this.props.url, payload);
    },
    onSuccess: function(data) {
      this.setState({postStatus: "Updated PACKAGE \"" + this.props.val + "\" successfully"});
    },
    formatDatetime: function(now) {
        tzo = -now.getTimezoneOffset(),
        dif = tzo >= 0 ? '+' : '-',
        pad = function(num) {
            var norm = Math.abs(Math.floor(num));
            return (norm < 10 ? '0' : '') + norm;
        };
      return now.getFullYear()
        + '-' + pad(now.getMonth()+1)
        + '-' + pad(now.getDate())
        + 'T' + pad(now.getHours())
        + ':' + pad(now.getMinutes())
        + ':' + pad(now.getSeconds())
        + dif + pad(tzo / 60)
        + ':' + pad(tzo % 60);
    },
    render: function() { return (
        <form onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="package">{ this.props.val }</label>
            <input type="hidden" className="form-control" id="package" ref="package" value={ this.props.val }/>
          </div>
          <button type="submit" className="btn btn-primary">Update</button>
          {this.state.postStatus}
        </form>
    );}
  });

  $.fn.serializeObject = function()
  {
      var o = {};
      var a = this.serializeArray();
      $.each(a, function() {
          if (o[this.name] !== undefined) {
              if (!o[this.name].push) {
                  o[this.name] = [o[this.name]];
              }
              o[this.name].push(this.value || '');
          } else {
              o[this.name] = this.value || '';
          }
      });
      return o;
  };

  var serializeForm = function(formRef) {
    return $(formRef.getDOMNode()).serializeObject();
  };


  React.render(<AddVin url="/api/v1/vins" />, document.getElementById('app'));
  React.render(<AddPackage url="/api/v1/packages"/>, document.getElementById('package'));
})();

define(['jquery', 'react', '../mixins/handle-fail', '../util'], function($, React, HandleFailMixin, util) {

  var AddVin = React.createClass({
    mixins: [HandleFailMixin],
    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({postStatus: ""});

      payload = util.serializeForm(this.refs.form);
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

  return AddVin;
});

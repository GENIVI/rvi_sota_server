define(['jquery', 'react', '../mixins/handle-fail', '../mixins/serialize-form'], function($, React, HandleFailMixin, serializeForm) {

  var AddVin = React.createClass({
    mixins: [HandleFailMixin],
    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({postStatus: ""});

      payload = serializeForm(this.refs.form);
      this.sendPutRequest(this.props.url + "/" + payload.vin, payload);
    },
    onSuccess: function(data) {
      this.setState({postStatus: "PUT VIN successfully"});
      React.findDOMNode(this.refs.vin).value = '';
    },
    render: function() { return (
        <form ref='form' onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="vin">VIN</label>
            <input type="text" className="form-control" id="vin" ref="vin" name="vin" placeholder="VIN"/>
          </div>
          <button type="submit" className="btn btn-primary">Add Vehicle</button>
          {this.state.postStatus}
        </form>
      );}
  });

  return AddVin;
});

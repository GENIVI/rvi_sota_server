define(['jquery', 'react', '../mixins/handle-fail'], function($, React, HandleFailMixin) {

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
      };

      this.sendRequest(this.props.url, payload);
    },
    onSuccess: function(data) {
      this.setState({postStatus: "Updated PACKAGE \"" + this.props.val + "\" successfully"});
    },
    formatDatetime: function(now) {
      var tzo = -now.getTimezoneOffset();
      var dif = tzo >= 0 ? '+' : '-';

      var pad = function(num) {
        var norm = Math.abs(Math.floor(num));
        return (norm < 10 ? '0' : '') + norm;
      };

      return now.getFullYear() +
        '-' + pad(now.getMonth()+1) +
        '-' + pad(now.getDate()) +
        'T' + pad(now.getHours()) +
        ':' + pad(now.getMinutes()) +
        ':' + pad(now.getSeconds()) +
        dif + pad(tzo / 60) +
        ':' + pad(tzo % 60);
    },
    render: function() {
      return (
        <form onSubmit={this.handleSubmit}>
          <div className="form-group">
            <label htmlFor="package">{ this.props.val }</label>
            <input type="hidden" className="form-control" id="package" ref="package" value={ this.props.val }/>
          </div>
          <button type="submit" className="btn btn-primary">Update</button>
          {this.state.postStatus}
        </form>
      );
    }
  });

  return UpdatePackage;
});

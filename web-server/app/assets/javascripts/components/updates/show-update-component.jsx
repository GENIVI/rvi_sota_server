define(function(require) {
  var _ = require('underscore'),
      React = require('react'),
      db = require('stores/db'),
      SotaDispatcher = require('sota-dispatcher'),
      ShowStatus = require('./show-status');

  var ShowUpdateComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Update.removeWatch("poll-update");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({
        actionType: 'get-update',
        id: this.context.router.getCurrentParams().id
      });
      this.props.Update.addWatch("poll-update", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var rows = _.map(this.props.Update.deref(), function(value, key) {
        return (
          <tr key={key}>
            <td>
              {key}
            </td>
            <td>
              {value}
            </td>
          </tr>
        );
      });
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <h1>
                Update ID: {this.props.Update.deref().id}
              </h1>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-md-12">
              <table className="table table-striped table-bordered">
                <tbody>
                  { rows }
                </tbody>
              </table>
            </div>
          </div>
          <ShowStatus UpdateStatus={db.updateStatus}/>
        </div>
      );
    }
  });

  return ShowUpdateComponent;
});

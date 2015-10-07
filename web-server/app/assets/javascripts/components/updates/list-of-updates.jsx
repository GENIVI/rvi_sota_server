define(function(require) {
  var _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher'),
      Router = require('react-router'),
      db = require('../../stores/db'),
      React = require('react');

  var ListOfUpdates = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Updates.removeWatch("poll-updates");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({actionType: 'get-updates'});
      this.props.Updates.addWatch("poll-updates", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var rows = _.map(this.props.Updates.deref(), function(update) {
        var startend = update.periodOfValidity.split("/");
        return (
          <tr key={update.id}>
            <td>
              {update.packageId.name}
            </td>
            <td>
              {update.packageId.version}
            </td>
            <td>
              {startend[0]}
            </td>
            <td>
              {startend[1]}
            </td>
            <td>
              {update.priority}
            </td>
            <td>
              <Router.Link to='update' params={{id: update.id}}>
                Details
              </Router.Link>
            </td>
          </tr>
        );
      });
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <h1>
                Updates
              </h1>
            </div>
          </div>
          <div className="row">
            <div className="col-md-8">
              <p>
              </p>
            </div>
          </div>
          <table className="table table-striped table-bordered">
            <thead>
              <tr>
                <td>
                  Package
                </td>
                <td>
                  Version
                </td>
                <td>
                  Start
                </td>
                <td>
                  End
                </td>
                <td>
                  Priority
                </td>
                <td>
                </td>
              </tr>
            </thead>
            <tbody>
              { rows }
            </tbody>
          </table>
        </div>
      );
    }
  });

  return ListOfUpdates;
});

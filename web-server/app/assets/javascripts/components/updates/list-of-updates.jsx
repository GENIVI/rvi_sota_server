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
      this.props.Packages.removeWatch("poll-updates-packages");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({actionType: 'get-updates'});
      SotaDispatcher.dispatch({actionType: 'search-packages-by-regex', regex: "."});
      this.props.Updates.addWatch("poll-updates", _.bind(this.forceUpdate, this, null));
      this.props.Packages.addWatch("poll-updates-packages", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var rows = [];
      if(!_.isUndefined(this.props.Packages)) {
        rows = _.map(this.props.Updates.deref(), function(update) {
          var foundPackage = _.findWhere(this.props.Packages.deref(), {uuid: update.packageUuid});
          var startend = update.periodOfValidity.split("/");
          return (
            <tr key={update.id}>
              <td>
                {!_.isUndefined(foundPackage) ? 
                  foundPackage.id.name
                :
                  null
                }
              </td>
              <td>
                {!_.isUndefined(foundPackage) ? 
                  foundPackage.id.version
                :
                  null
                }
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
        }, this);
      }
     
      
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

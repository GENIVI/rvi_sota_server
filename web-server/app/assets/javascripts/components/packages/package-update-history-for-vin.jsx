define(function(require) {

  var React = require('react'),
      Router = require('react-router'),
      _ = require('underscore'),
      togglePanel = require('../../mixins/toggle-panel'),
      SotaDispatcher = require('sota-dispatcher');

  var PackageHistory = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [togglePanel],
    componentWillUnmount: function(){
      this.props.Packages.removeWatch("poll-package-history-for-vin");
    },
    componentWillMount: function(){
      this.props.Packages.addWatch("poll-package-history-for-vin", _.bind(this.forceUpdate, this, null));
    },
    refreshData: function() {
      SotaDispatcher.dispatch({actionType: 'get-package-history-for-vin', vin: this.props.Vin});
    },
    label: "History",
    panel: function() {
      var finishedRows = _.map(this.props.Packages.deref(), function(package) {
        if(package.success === true) {
          return (
            <tr key={package.packageId.name + '-' + package.packageId.version}>
              <td>
                <Router.Link to='package' params={{name: package.packageId.name, version: package.packageId.version}}>
                  { package.packageId.name }
                </Router.Link>
              </td>
              <td>
                { package.packageId.version }
              </td>
              <td>
                {package.completionTime}
              </td>
            </tr>
          );
        }
      });
      var failedRows = _.map(this.props.Packages.deref(), function(package) {
        if(package.success === false) {
          return (
            <tr key={package.packageId.name + '-' + package.packageId.version}>
              <td>
                <Router.Link to='package' params={{name: package.packageId.name, version: package.packageId.version}}>
                  { package.packageId.name }
                </Router.Link>
              </td>
              <td>
                { package.packageId.version }
              </td>
              <td>
                {package.completionTime}
              </td>
            </tr>
          );
        }
      });
      return (
        <div>
          <h2>Completed Updates</h2>
          <table className="table table-striped table-bordered">
            <thead>
              <tr>
                <td>
                  Package Name
                </td>
                <td>
                  Version
                </td>
                <td>
                  Completion Time
                </td>
              </tr>
            </thead>
            <tbody>
              { finishedRows }
            </tbody>
          </table>
          <h2>Failed Updates</h2>
          <table className="table table-striped table-bordered">
            <thead>
              <tr>
                <td>
                  Package Name
                </td>
                <td>
                  Version
                </td>
                <td>
                  Completion Time
                </td>
              </tr>
            </thead>
            <tbody>
              { failedRows }
            </tbody>
          </table>
        </div>
      );
    }
  });

  return PackageHistory;
});

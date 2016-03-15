define(function(require) {

  var React = require('react'),
      Router = require('react-router'),
      _ = require('underscore'),
      togglePanel = require('../../mixins/toggle-panel'),
      SotaDispatcher = require('sota-dispatcher');

  var QueuedPackages = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [togglePanel],
    componentWillUnmount: function(){
      this.props.Packages.removeWatch("poll-package-queue-for-vin");
    },
    componentWillMount: function(){
      this.props.Packages.addWatch("poll-package-queue-for-vin", _.bind(this.forceUpdate, this, null));
    },
    refreshData: function() {
      SotaDispatcher.dispatch({actionType: 'get-package-queue-for-vin', vin: this.props.Vin});
    },
    label: "Queue",
    panel: function() {
      var rows = _.map(this.props.Packages.deref(), function(package) {
        return (
          <tr key={package.name + '-' + package.version}>
            <td>
              <Router.Link to='package' params={{name: package.name, version: package.version}}>
                { package.name }
              </Router.Link>
            </td>
            <td>
              { package.version }
            </td>
          </tr>
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                Package Name
              </td>
              <td>
                Version
              </td>
            </tr>
          </thead>
          <tbody>
            { rows }
          </tbody>
        </table>
      );
    }
  });

  return QueuedPackages;
});

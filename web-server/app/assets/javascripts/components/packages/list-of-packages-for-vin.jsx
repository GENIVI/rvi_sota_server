define(function(require) {

  var React = require('react'),
      Router = require('react-router'),
      _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher');

  var Packages = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Packages.removeWatch("poll-packages");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({actionType: 'get-packages-for-vin', vin: this.props.Vin});
      this.props.Packages.addWatch("poll-packages", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var rows = _.map(this.props.Packages.deref(), function(package) {
        return (
          <tr>
            <td>
              <Router.Link to='package' params={{name: package.name, version: package.version}}>
                { package.name }
              </Router.Link>
            </td>
            <td>
              { package.version }
            </td>
            <td>
              <Router.Link to='new-campaign' params={{name: package.name, version: package.version}}>
                Create Campaign
              </Router.Link>
            </td>
          </tr>
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                Packages
              </td>
              <td>
                Version
              </td>
              <td>
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

  return Packages;
});

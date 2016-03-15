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
      this.props.Packages.removeWatch(this.props.PollEventName);
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch(this.props.DispatchObject);
      this.props.Packages.addWatch(this.props.PollEventName, _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var rows = _.map(this.props.Packages.deref(), function(package) {
        return (
          <tr key={package.id.name + '-' + package.id.version}>
            <td>
              <Router.Link to='package' params={{name: package.id.name, version: package.id.version}}>
                { package.id.name }
              </Router.Link>
            </td>
            <td>
              { package.id.version }
            </td>
            {this.props.DisplayCampaignLink ?
              <td>
                <Router.Link to='new-campaign' params={{name: package.id.name, version: package.id.version}}>
                  Create Campaign
                </Router.Link>
              </td>
            : ''}
          </tr>
        );
      }, this);
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
              {this.props.DisplayCampaignLink ? <td/> : ''}
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

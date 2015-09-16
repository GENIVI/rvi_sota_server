define(function(require) {

  var React = require('react'),
      Router = require('react-router'),
      Fluxbone = require('../../mixins/fluxbone'),
      SotaDispatcher =require('sota-dispatcher');

  var PackageComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Package', 'sync')
    ],
    render: function() {
      return (
        <tr>
          <td>
            <Router.Link to='package' params={{name: this.props.Package.get('name'), version: this.props.Package.get('version')}}>
              { this.props.Package.get('name') }
            </Router.Link>
          </td>
          <td>
            { this.props.Package.get('version') }
          </td>
          <td>
            <Router.Link to='new-campaign' params={{name: this.props.Package.get('name'), version: this.props.Package.get('version')}}>
              Create Campaign
            </Router.Link>
          </td>
        </tr>
      );
    }
  });

  return PackageComponent;
});

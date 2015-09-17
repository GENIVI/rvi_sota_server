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
            <Router.Link to='package' params={{name: this.props.Package.get('id').name, version: this.props.Package.get('id').version}}>
              { this.props.Package.get('id').name }
            </Router.Link>
          </td>
          <td>
            { this.props.Package.get('id').version }
          </td>
          <td>
            <Router.Link to='new-campaign' params={{name: this.props.Package.get('id').name, version: this.props.Package.get('id').version}}>
              Create Campaign
            </Router.Link>
          </td>
        </tr>
      );
    }
  });

  return PackageComponent;
});

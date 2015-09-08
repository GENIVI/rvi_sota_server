define(['react', 'react-router', '../mixins/fluxbone', 'sota-dispatcher'], function(React, Router, Fluxbone, SotaDispatcher) {

  var PackageComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Package', 'sync')
    ],
    render: function() {
      return (
        <Router.Link to='package' params={{name: this.props.Package.get('name'), version: this.props.Package.get('version')}}>
          <li className="list-group-item">
            { this.props.Package.get('name') } - { this.props.Package.get('version') }
          </li>
        </Router.Link>
      );
    }
  });

  return PackageComponent;
});

define(['react', 'react-router', '../mixins/fluxbone', 'sota-dispatcher'], function(React, Router, Fluxbone, SotaDispatcher) {

  var PackageComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Package', 'sync')
    ],
    handleUpdatePackage: function() {
      SotaDispatcher.dispatch({
        actionType: "package-updatePackage",
        package: this.props.Package
      });
    },
    render: function() {
      return (
        <Router.Link to='package' params={{name: this.props.Package.get('name'), version: this.props.Package.get('version')}}>
          <li className="list-group-item">
            <span className="badge" onClick={ this.handleUpdatePackage }>
              Update Package
            </span>
            { this.props.Package.get('name') } - { this.props.Package.get('version') }
          </li>
        </Router.Link>
      );
    }
  });

  return PackageComponent;
});

define(['jquery', 'react', '../mixins/fluxbone', 'sota-dispatcher'], function($, React, Fluxbone, SotaDispatcher) {

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
        <li className="list-group-item">
          <span className="badge" onClick={ this.handleUpdatePackage }>
            Update Package
          </span>
          { this.props.Package.get('name') }
        </li>
      );
    }
  });

  return PackageComponent;
});

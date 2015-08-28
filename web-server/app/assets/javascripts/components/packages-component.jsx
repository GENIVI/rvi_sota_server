define(['jquery', 'react', '../mixins/serialize-form', '../mixins/fluxbone', '../mixins/request-status', './package-component', 'sota-dispatcher'], function($, React, serializeForm, Fluxbone, RequestStatus, PackageComponent, SotaDispatcher) {

  var Packages = React.createClass({
    mixins: [
      Fluxbone.Mixin("PackageStore")
    ],
    render: function() {
      var packages = this.props.PackageStore.models.map(function(package) {
        return (
          <PackageComponent Package = { package }/>
        );
      });
      return (
        <div>
          <ul className="list-group">
            { packages }
          </ul>
        </div>
      );
    }
  });

  return Packages;
});

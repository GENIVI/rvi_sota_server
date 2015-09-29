define(function(require) {

  var _ = require('underscore'),
      React = require('react'),
      VehiclesToUpdateStore = require('stores/vehicles-to-update'),
      VehiclesToUpdate = require('components/vehicles-to-update-component'),
      showModel = require('../mixins/show-model'),
      CreateUpdate = require('components/create-update');

  var ShowPackageComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    render: function() {
      var params = this.context.router.getCurrentParams();
      return (
        <div>
          <h1>
            New Update Campaign for Package <br/>
            {params.name}
          </h1>
          <VehiclesToUpdate store={new VehiclesToUpdateStore({}, {pkgName: params.name, pkgVersion: params.version})}/>
          <CreateUpdate packageName={params.name} packageVersion={params.version}/>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});

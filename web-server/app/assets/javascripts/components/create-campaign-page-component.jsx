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
    mixins: [showModel],
    whereClause: function() {
      return {packageId: this.context.router.getCurrentParams().name + "/" + this.context.router.getCurrentParams().version};
    },
    showView: function() {
      return (
        <div>
          <h1>
            New Update Campaign for Package <br/>
            {this.state.Model.get('id').name}
          </h1>
          <VehiclesToUpdate store={new VehiclesToUpdateStore({}, {pkgName: this.state.Model.get('id').name, pkgVersion: this.state.Model.get('id').version})}/>
          <CreateUpdate packageName={this.state.Model.get('id').name} packageVersion={this.state.Model.get('id').version}/>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});

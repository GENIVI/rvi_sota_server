define(function(require) {

  var _ = require('underscore'),
      React = require('react'),
      showModel = require('../mixins/show-model'),
      VehiclesToUpdate = require('components/vehicles-to-update-component'),
      VehiclesToUpdateStore = require('stores/vehicles-to-update'),
      CreateUpdate = require('components/create-update'),
      AddPackageFilters = require('./package-filters/add-package-filters');

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
          <AddPackageFilters Package={this.state.Model}/>
          <VehiclesToUpdate store={new VehiclesToUpdateStore({}, {pkgName: this.state.Model.get('id').name, pkgVersion: this.state.Model.get('id').version})}/>
          <CreateUpdate packageName={this.state.Model.get('id').name} packageVersion={this.state.Model.get('id').version}/>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});

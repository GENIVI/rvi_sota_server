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
      return this.context.router.getCurrentParams();
    },
    showView: function() {
      return (
        <div>
          <h1>
            New Campaign
          </h1>
          <AddPackageFilters Package={this.state.Model}/>
          <VehiclesToUpdate store={new VehiclesToUpdateStore({}, {pkgName: this.state.Model.get('name'), pkgVersion: this.state.Model.get('version')})}/>
          <CreateUpdate packageName={this.state.Model.get('name')} packageVersion={this.state.Model.get('version')}/>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});

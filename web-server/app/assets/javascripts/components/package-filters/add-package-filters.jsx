define(['react', '../../stores/filters', '../../stores/filters-for-package', './filters-for-package-component'], function(React, FilterStore, FiltersForPackageStore, FiltersForPackageComponent) {

  var AddPackageFilters = React.createClass({
    render: function() {
      return (
        <div className="row">
          <div className="col-md-6">
            <h2>
              Package Filters
            </h2>
            <FiltersForPackageComponent Store={FiltersForPackageStore} Package={this.props.Package} eventName='delete-package-filter'/>
          </div>
          <div className="col-md-6">
            <h2>
              All Filters
            </h2>
            <FiltersForPackageComponent Store={FilterStore} Package={this.props.Package} eventName='create-package-filter'/>
          </div>
        </div>
      );
    }
  });

  return AddPackageFilters;
});

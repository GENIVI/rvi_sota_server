define(['react', '../search-bar', './filters-component', './create-filter', '../../stores/filters', 'sota-dispatcher'], function(React, SearchBar, FiltersComponent, CreateFilterComponent, FiltersStore, SotaDispatcher) {

  var FilterablePackageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <CreateFilterComponent url="/api/v1/filters"/>
        <FiltersComponent Store={FiltersStore}/>
      </div>
    );}
  });

  return FilterablePackageComponent;

});

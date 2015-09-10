define(function(require) {

  var React = require('react'),
    SearchBar = require('../search-bar'),
    FiltersComponent = require('./filters-component'),
    FilterFormComponent = require('./filter-form'),
    FiltersStore = require('../../stores/filters'),
    SotaDispatcher = require('sota-dispatcher');

  var FilterablePackageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <SearchBar label="Search filters by regex" event="search-filters"/>
        <FilterFormComponent Store={FiltersStore} event="create-filter"/>
        <FiltersComponent Store={FiltersStore}/>
      </div>
    );}
  });

  return FilterablePackageComponent;

});

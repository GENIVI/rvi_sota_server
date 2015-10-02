define(function(require) {

  var React = require('react'),
      FiltersHeader = require('./filters-header-component'),
      SearchBar = require('../search-bar'),
      ListOfFilters = require('./list-of-filters'),
      Errors = require('../errors'),
      db = require('stores/db');

  var FiltersPageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <FiltersHeader/>
        <Errors />
        <SearchBar label="Filter" event="search-filters-by-regex"/>
        <ListOfFilters Filters={db.searchableFilters}/>
      </div>
    );}
  });

  return FiltersPageComponent;
});

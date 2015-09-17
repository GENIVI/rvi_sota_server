define(function(require) {

  var React = require('react'),
      SearchBar = require('../search-bar'),
      FiltersComponent = require('./filters-component'),
      FiltersStore = require('../../stores/filters'),
      FiltersHeader = require('./filters-header-component');

  var FiltersPageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <FiltersHeader/>
        <SearchBar label="Filter" event="search-filters"/>
        <FiltersComponent Store={FiltersStore}/>
      </div>
    );}
  });

  return FiltersPageComponent;
});

define(function(require) {

  var React = require('react'),
      SearchBar = require('../search-bar'),
      ListOfPackages = require('./list-of-packages'),
      PackagesHeader = require('./packages-header-component'),
      Errors = require('../errors'),
      db = require('stores/db');

  var PackagesPageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <PackagesHeader/>
        <Errors />
        <SearchBar label="Filter" event="search-packages-by-regex"/>
        <ListOfPackages Packages={db.searchablePackages}/>
      </div>
    );}
  });

  return PackagesPageComponent;

});

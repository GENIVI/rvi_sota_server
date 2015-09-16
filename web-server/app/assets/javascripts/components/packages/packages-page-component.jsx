define(function(require) {

  var React = require('react'),
      SearchBar = require('../search-bar'),
      PackagesComponent = require('./packages-component'),
      PackageStore = require('../../stores/packages');
      PackagesHeader = require('./packages-header-component');

  var PackagesPageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <PackagesHeader/>
        <SearchBar label="Filter" event="packages-filter"/>
        <PackagesComponent PackageStore={PackageStore}/>
      </div>
    );}
  });

  return PackagesPageComponent;

});

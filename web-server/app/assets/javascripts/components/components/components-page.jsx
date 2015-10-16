define(function(require) {

  var React = require('react'),
      ComponentsHeader = require('./components-header'),
      SearchBar = require('../search-bar'),
      ListOfComponents = require('./list-of-components'),
      Errors = require('../errors'),
      db = require('stores/db');

  var ComponentsPage = React.createClass({
    render: function() {
      return (
      <div>
        <ComponentsHeader/>
        <Errors />
        <SearchBar label="Filter" event="search-components-by-regex"/>
        <ListOfComponents Components={db.searchableComponents}/>
      </div>
    );}
  });

  return ComponentsPage;
});

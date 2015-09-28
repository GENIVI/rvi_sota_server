define(function(require) {
  var _ = require('underscore'),
      React = require('react'),
      EditFilterComponent = require('./edit-filter-component'),
      PackagesForFilter = require('./packages-for-filter'),
      db = require('stores/db');

  var ShowUpdateComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Filter.removeWatch("poll-filter");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({
        actionType: 'get-filter',
        name: this.context.router.getCurrentParams().name
      });
      this.props.Filter.addWatch("poll-filter", _.bind(this.forceUpdate, this, null));
    },
    removeFilter: function() {
      SotaDispatcher.dispatch({
        actionType: 'destroy-filter',
        name: this.context.router.getCurrentParams().name
      });
    },
    render: function() {
      var listItems = _.map(this.props.Filter.deref(), function(value, key) {
        return (
          <li>
            {key}: {value}
          </li>
        );
      });
      return (
        <div>
          <h1>
            {this.props.Filter.deref().name}
          </h1>
          <ul>
            {listItems}
          </ul>
          <h2>
            Edit filter
          </h2>
          <EditFilterComponent Filter={this.props.Filter}/>
          <button type="button" className="btn btn-primary" onClick={this.removeFilter} name="delete-filter">Delete Filter</button>
          <PackagesForFilter Filter={this.props.Filter} Packages={db.packages} PackagesForFilter={db.packagesForFilter}/>
        </div>
      );
    }
  });

  return ShowUpdateComponent;
});

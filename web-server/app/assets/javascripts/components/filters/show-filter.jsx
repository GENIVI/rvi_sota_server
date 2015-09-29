define(function(require) {
  var _ = require('underscore'),
      React = require('react'),
      EditFilterComponent = require('./edit-filter-component'),
      PackageFilterAssociation = require('../package-filters/package-filter-association'),
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
      var params = this.context.router.getCurrentParams();
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
          <PackageFilterAssociation
            Resource={this.props.Filter}
            CreateList={db.packages}
            DeleteList={db.packagesForFilter}
            getCreateList="get-packages"
            createResourceName="Packages"
            getDeleteList={{actionType: 'get-packages-for-filter', filter: params.name}}/>
        </div>
      );
    }
  });

  return ShowUpdateComponent;
});

define(function(require) {

  var React = require('react'),
      PackagesForFilterItem = require('./packages-for-filter-item'),
      db = require('stores/db'),
      SotaDispatcher = require('sota-dispatcher');

  var PackagesForFilter = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.PackagesForFilter.removeWatch("watch-em-packages-for-filter");
      this.props.Filter.removeWatch("poll-packages-for-filter");
      this.props.Packages.removeWatch("watch-em-packages");
    },
    componentWillMount: function(){
      var filterName = this.context.router.getCurrentParams().name;
      SotaDispatcher.dispatch({actionType: 'get-packages-for-filter', filter: filterName});
      SotaDispatcher.dispatch({actionType: 'get-packages'});
      this.props.PackagesForFilter.addWatch("watch-em-packages-for-filter", _.bind(this.forceUpdate, this, null));
      this.props.Packages.addWatch("watch-em-packages", _.bind(this.forceUpdate, this, null));
      this.props.Filter.addWatch("poll-packages-for-filter", _.bind(this.forceUpdate, this, null));
    },

    payload: function(package) {
      return {
        filterName: this.props.Filter.deref().name,
        packageName: package.id.name,
        packageVersion: package.id.version
      };
    },

    listItems: function(package, eventName) {
      return (
        <PackagesForFilterItem Filter={this.props.Filter} Payload={this.payload(package)} eventName={eventName} label={package.id.name}/>
      );
    },
    createListItems: function(package, eventName) {
      return this.listItems(package, 'add-package-filter');
    },
    destroyListItems: function(package, eventName) {
      return this.listItems(package, 'destroy-package-filter');
    },

    render: function() {
      var currentList = _.map(db.packagesForFilter.deref(), this.destroyListItems, this);
      var createList = _.map(db.packages.deref(), this.createListItems, this);

      return (
        <div className="row">
          <div className="col-md-6">
            {currentList}
          </div>
          <div className="col-md-6">
            {createList}
          </div>
        </div>
      );
    }
  });

  return PackagesForFilter;
});

define(function(require) {

  var React = require('react'),
      PackageFilterListItem = require('./package-filter-list-item'),
      db = require('stores/db'),
      SotaDispatcher = require('sota-dispatcher');

  var PackageFilterAssociation = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    refreshLists: function() {
      SotaDispatcher.dispatch(this.props.getDeleteList);
      SotaDispatcher.dispatch({actionType: this.props.getCreateList});
    },
    componentWillUnmount: function(){
      this.props.DeleteList.removeWatch("watch-delete-list");
      this.props.CreateList.removeWatch("watch-create-list");
      this.props.Resource.removeWatch("watch-resource");
    },
    componentWillMount: function(){
      this.refreshLists();
      var filterName = this.context.router.getCurrentParams().name;
      this.props.DeleteList.addWatch("watch-delete-list", _.bind(this.forceUpdate, this, null));
      this.props.CreateList.addWatch("watch-create-list", _.bind(this.forceUpdate, this, null));
      this.props.Resource.addWatch("watch-resource", _.bind(this.forceUpdate, this, null));
    },
    componentDidMount: function(){
      this.refreshLists();
    },

    payload: function(model) {
      if (model.id) {
        return {
          filterName: this.props.Resource.deref().name,
          packageName: model.id.name,
          packageVersion: model.id.version
        };
      } else {
        return {
          filterName: model.name,
          packageName: this.props.Resource.deref().id.name,
          packageVersion: this.props.Resource.deref().id.version
        };
      }
    },

    listItems: function(model, eventName) {
      var label = model.id ? model.id.name + '-' + model.id.version : model.filterName;
      return (
        <PackageFilterListItem Payload={this.payload(model)} eventName={eventName} label={label}/>
      );
    },
    createListItems: function(model, eventName) {
      return this.listItems(model, 'add-package-filter');
    },
    destroyListItems: function(model, eventName) {
      return this.listItems(model, 'destroy-package-filter');
    },

    render: function() {
      var currentList = _.map(this.props.DeleteList.deref(), this.destroyListItems, this);
      var createList = _.map(this.props.CreateList.deref(), this.createListItems, this);

      return (
        <div className="row">
          <div className="col-md-6">
            <h3>
              Current { this.props.createResourceName }
            </h3>
            {currentList}
          </div>
          <div className="col-md-6">
            <h3>
              Add { this.props.createResourceName }
            </h3>
            {createList}
          </div>
        </div>
      );
    }
  });

  return PackageFilterAssociation;
});

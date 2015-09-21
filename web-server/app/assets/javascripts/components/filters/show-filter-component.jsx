define(function(require) {

  var _ = require('underscore'),
      React = require('react'),
      Router = require('react-router'),
      HandleFail = require('../../mixins/handle-fail'),
      showModel = require('../../mixins/show-model'),
      Fluxbone = require('../../mixins/fluxbone'),
      FiltersStore = require('../../stores/filters'),
      PackagesComponent = require('../packages/packages-component'),
      PackageStore = require('../../stores/packages');
      EditFilterComponent = require('./edit-filter-component');

  var ShowFilterComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [
      showModel,
      Fluxbone.Mixin('Store', 'sync'),
      Router.Navigation,
      HandleFail
    ],
    whereClause: function() {
      var params = this.context.router.getCurrentParams();
      return {name: params.name};
    },
    removeFilter: function() {
      this.sendDeleteRequest('/api/v1/filters/' + this.state.Model.get('name'));
    },
    onSuccess: function() {
      SotaDispatcher.dispatch({
        actionType: 'fetch-filters',
      });
      this.transitionTo('filters');
    },
    showView: function() {
      var listItems = _.map(this.state.Model.attributes, function(value, key) {
        return (
          <li>
            {key}: {value}
          </li>
        );
      });
      var store = PackageStore;
      store.fetchForFilter(this.state.Model.get('name'));
      return (
        <div>
          <h1>
            {this.state.Model.get('name')}
          </h1>
          <ul>
            {listItems}
          </ul>
          <h2>
            Edit filter
          </h2>
          <EditFilterComponent Store={FiltersStore} Model={this.state.Model}/>
          <button type="button" className="btn btn-primary" onClick={this.removeFilter}>Delete Filter</button>
          <h3>Packages with this filter</h3>
          <PackagesComponent PackageStore={store}/>
        </div>
      );
    }
  });

  return ShowFilterComponent;
});

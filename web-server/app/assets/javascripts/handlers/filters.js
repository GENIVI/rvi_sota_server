define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore'),
      db = require('../stores/db'),
      sendRequest = require('../mixins/send-request');

  var Handler = (function() {
      var updateFilter = function(payload) {
        sendRequest.doPut("/api/v1/filters/" + payload.filter.name, payload.filter)
          .success(function() {
            SotaDispatcher.dispatch({actionType: 'get-filter', name: payload.filter.name});
          });
      };
      this.dispatchCallback = function(payload) {
        switch(payload.actionType) {
          case 'get-filters':
            sendRequest.doGet('/api/v1/filters')
              .success(function(filters) {
                db.filters.reset(filters);
              });
          break;
          case 'search-filters-by-regex':
            var query = payload.regex ? '?regex=' + payload.regex : '';

            sendRequest.doGet('/api/v1/filters' + query)
              .success(function(filters) {
                db.searchableFilters.reset(filters);
              });
          break;
          case 'get-filter':
            sendRequest.doGet('/api/v1/filters')
              .success(function(filters) {
                var showFilter = _.find(filters, function(filter) {
                  return filter.name == payload.name;
                });
                db.showFilter.reset(showFilter);
              });
          break;
          case 'create-filter':
            sendRequest.doPost('/api/v1/filters', payload.filter)
              .success(function(filters) {
                SotaDispatcher.dispatch({actionType: 'get-filters'});
                SotaDispatcher.dispatch({actionType: 'search-filters-by-regex'});
              });
          break;
          case 'edit-filter':
            sendRequest.doPost('/api/v1/validate/filter', payload.filter)
              .success(_.bind(function(filter) {
                updateFilter(payload);
              }, this));
            break;
          case 'destroy-filter':
            sendRequest.doDelete('/api/v1/filters/' + payload.name)
              .success(_.bind(function(filter) {
                location.hash = '#/filters';
                SotaDispatcher.dispatch({actionType: 'get-filters'});
              }, this));
            break;
        }
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});

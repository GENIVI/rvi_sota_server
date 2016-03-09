define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore'),
      db = require('../stores/db'),
      sendRequest = require('../mixins/send-request');

  var handlers = {
    getPackagesForFilter: function(payload) {
      sendRequest.doGet('/api/v1/filters/' + payload.filter + '/package')
        .success(function(packages) {
          db.packagesForFilter.reset(packages);
        });
    },
    getFiltersForPackage: function(payload) {
      sendRequest.doGet('/api/v1/packages/' + payload.name + '/' + payload.version + '/filter')
        .success(function(filters) {
          db.filtersForPackage.reset(filters);
        });
    },
    addPackageFilter: function(payload) {
      sendRequest.doPut('/api/v1/packages/', payload.packageFilter.packageName +
        '/' + payload.packageFilter.packageVersion +
        '/filter/' + payload.packageFilter.filterName)
        .success(_.bind(this.refreshPackageFilters, this, payload));
    },
    destroyPackageFilter: function(payload){
      var packageFilter = payload.packageFilter;
      var deleteUrl = '/api/v1/packages' +
        '/' + packageFilter.packageName +
        '/' + packageFilter.packageVersion +
        '/filter/' + packageFilter.filterName;
      sendRequest.doDelete(deleteUrl)
        .success(_.bind(this.refreshPackageFilters, this, payload));
    },
    refreshPackageFilters: function(payload) {
      SotaDispatcher.dispatch({
        actionType: 'get-packages-for-filter',
        filter: payload.packageFilter.filterName
      });
      SotaDispatcher.dispatch({
        actionType: 'get-filters-for-package',
        name: payload.packageFilter.packageName,
        version: payload.packageFilter.packageVersion
      });
    }
  };

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
        switch(payload.actionType) {
          case 'get-packages-for-filter':
            handlers.getPackagesForFilter(payload);
          break;
          case 'get-filters-for-package':
            handlers.getFiltersForPackage(payload);
          break;
          case 'add-package-filter':
            handlers.addPackageFilter(payload);
          break;
          case 'destroy-package-filter':
            handlers.destroyPackageFilter(payload);
          break;
        }
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});

define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore'),
      db = require('../stores/db'),
      UpdatesHandler = require('./updates'),
      filtersHandler = require('./filters'),
      packageFiltersHandler = require('./package-filters'),
      packagesHandler = require('./packages');

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
        // global logging
        console.log(payload.actionType, payload);
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});

define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore'),
      db = require('../stores/db'),
      UpdatesHandler = require('./updates'),
      filtersHandler = require('./filters'),
      vehiclesHandler = require('./vehicles'),
      packageFiltersHandler = require('./package-filters'),
      packagesHandler = require('./packages');

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
        // global logging
        console.log(payload.actionType, payload);

        // clear error messages for next request
        db.postStatus.reset("");
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));

      $(document).ajaxError(function(event, xhr) {
         var ct = xhr.getResponseHeader("content-type") || "";
           var result = xhr.responseText;
           if (ct.indexOf('plain') > -1) {
             console.log('Plaintext error message');
             db.postStatus.reset(result);
           } else if (ct.indexOf('json') > -1) {
             db.postStatus.reset(JSON.parse(result).description);
           }
      });

  });

  return new Handler();

});

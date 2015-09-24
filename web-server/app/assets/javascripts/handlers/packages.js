define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore');
      db = require('../stores/db');
      sendRequest = require('../mixins/send-request');

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
        switch(payload.actionType) {
          case 'get-packages':
            sendRequest.doGet('/api/v1/packages')
              .success(function(packages) {
                db.packages.reset(packages);
              });
          break;
        }
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});

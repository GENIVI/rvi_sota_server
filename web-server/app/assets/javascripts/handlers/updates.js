define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore'),
      db = require('../stores/db'),
      sendRequest = require('../mixins/send-request');

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
        switch(payload.actionType) {
          case 'get-updates':
            sendRequest.doGet('/api/v1/updates')
              .success(function(updates) {
                db.updates.reset(updates);
              });
          break;
          case 'get-update':
            sendRequest.doGet('/api/v1/updates')
              .success(function(updates) {
                var showUpdate = _.find(updates, function(update) {
                  return update.id == payload.id;
                });
                db.showUpdate.reset(showUpdate);
              });
          break;
          case 'get-update-status':
            sendRequest.doGet('/api/v1/updates/' + payload.id)
              .success(function(updateStatus) {
                db.updateStatus.reset(updateStatus);
              });
          break;
        }
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});

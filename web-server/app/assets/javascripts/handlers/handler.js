define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore');
      db = require('../stores/db');
      UpdatesHandler = require('./updates');

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});

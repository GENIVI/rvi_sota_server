define(function(require) {

  var db = require('../stores/db'),
      errors = require('../handlers/errors');
      sendRequest = require('./send-request');

  return function(url, resourceName, callback) {
    sendRequest.doGet(url, {global: false})
      .error(function(xhr) {
        errors.renderRequestError(xhr);
      })
      .success(function(data) {
        if (data == []) {
          db.postStatus.reset(resourceName + " already exists");
        } else {
          callback();
        }
      });
  };
});

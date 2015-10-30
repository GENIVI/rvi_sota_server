define(function(require) {

  var db = require('../stores/db'),
      errors = require('../handlers/errors');
      sendRequest = require('./send-request');

  return function(url, resourceName, callback) {
    sendRequest.doGet(url, {global: false})
      .error(function(xhr, textStatus) {
        if (xhr.status == 404) {
          callback();
        } else {
          errors.renderRequestError(xhr);
        }
      })
      .success(function() {
        db.postStatus.reset(resourceName + " already exists");
      });
  };
});

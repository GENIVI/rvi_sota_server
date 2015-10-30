define(function(require) {
  var db = require('../stores/db');

  return {
    renderRequestError: function(xhr) {
      var ct = xhr.getResponseHeader("content-type") || "";
      var result = xhr.responseText;
      if (ct.indexOf('plain') > -1) {
        console.log('Plaintext error message');
        db.postStatus.reset(result);
      } else if (ct.indexOf('json') > -1) {
        db.postStatus.reset(JSON.parse(result).description);
      }
    }
  };

});

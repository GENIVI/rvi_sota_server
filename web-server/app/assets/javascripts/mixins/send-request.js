define(['jquery', 'underscore'], function($, _) {
  var sendRequest = {
    send: function(type, url, data, opts) {
      opts = opts || {};
      if (opts.form) {
        return this.formMultipart(type, url, data);
      } else {
        return this.jsonAjax(type, url, data, opts);
      }
    },
    jsonAjax: function(type, url, data, opts) {
      return $.ajax(_.extend({
        type: type,
        url: url,
        dataType: 'json',
        data: JSON.stringify(data),
        contentType: "application/json"
      }, opts));
    },
    formMultipart: function(type, url, data) {
      return $.ajax({
        type: type,
        url: url,
        cache: false,
        contentType: false,
        processData: false,
        data: data
      });
    },
    doGet: function(url, opts) {
      return this.send("GET", url, undefined, opts);
    },
    doPost: function(url, data, opts) {
      return this.send("POST", url, data, opts);
    },
    doPut: function(url, data, opts) {
      return this.send("PUT", url, data, opts);
    },
    doDelete: function(url, data, opts) {
      return this.send("DELETE", url, data, opts);
    }
  };

  return sendRequest;
});

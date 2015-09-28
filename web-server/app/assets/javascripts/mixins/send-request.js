define(['jquery'], function($) {
  var sendRequest = {
    send: function(type, url, data, opts) {
      opts = opts || {};
      if (opts.form) {
        return this.formMultipart(type, url, data);
      } else {
        return this.jsonAjax(type, url, data);
      }
    },
    jsonAjax: function(type, url, data) {
      return $.ajax({
        type: type,
        url: url,
        dataType: 'json',
        data: JSON.stringify(data),
        contentType: "application/json"
      });
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
      return $.get(url);
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

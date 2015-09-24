define(['jquery'], function($) {
  var sendRequest = {
    jsonAjax: function(type, url, data) {
      return $.ajax({
        type: type,
        url: url,
        dataType: 'json',
        data: JSON.stringify(data),
        contentType: "application/json"
      });
    },
    doGet: function(url) {
      return $.get(url);
    },
    doPost: function(url, data) {
      return this.jsonAjax("POST", url, data);
    },
    doDelete: function(url, data) {
      return this.jsonAjax("DELETE", url, data);
    }
  };

  return sendRequest;
});

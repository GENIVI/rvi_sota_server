define(['jquery'], function($) {
  var sendRequest = {
    doPost: function(url, data) {
      return $.ajax({
        type: "POST",
        url: url,
        dataType: 'json',
        data: JSON.stringify(data),
        contentType: "application/json"
      });
    }
  };

  return sendRequest;
});

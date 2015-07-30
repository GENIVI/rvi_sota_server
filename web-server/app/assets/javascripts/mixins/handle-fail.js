define(['jquery'], function($) {
  var HandleFailMixin = {
    getInitialState: function() {
      return {postStatus : ""};
    },
    sendPostRequest: function(url, data) {
      return $.ajax({
        type: "POST",
        url: url,
        dataType: 'json',
        data: JSON.stringify(data),
        contentType: "application/json"
      });
    },
    sendRequest: function(url, data) {
      this.sendPostRequest(url, data)
        .success(this.onSuccess.bind(this))
        .fail(this.onFail.bind(this));
    },
    onFail: function(data) {
      var res = JSON.parse(data.responseText);
      this.setState({postStatus: res.errorMsg});
    }
  };

  return HandleFailMixin;
});

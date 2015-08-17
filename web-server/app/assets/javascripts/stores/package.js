define(['backbone', 'underscore', '../lib/backbone-model-file-upload', '../mixins/send-request', 'sota-dispatcher'], function(Backbone, _, BackboneModelFileUpload, sendRequest, SotaDispatcher) {

  var Package = Backbone.Model.extend({
    fileAttribute: 'binary',
    initialize: function() {
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    url: function() {
      return '/api/v1/packages/' + this.get("name") + '/' + this.get("version");
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
        case 'package-updatePackage':
          this.updatePackage();
      }
    },
    updatePackage: function() {
      var timestamp = new Date();
      var payload = {
        packageId: this.get('id'),
        priority: 10,
        startAfter: this.formatDatetime(timestamp),
        endBefore: this.formatDatetime(new Date(timestamp.getTime() + 10*60000))
      };

      // TODO find a good place to configure this url
      sendRequest.doPost("/api/v1/install_campaigns", payload)
        .fail(_.bind(function(xhr) {
          this.trigger("error", this, xhr);
        }, this));
    },
    formatDatetime: function(now) {
      var tzo = -now.getTimezoneOffset();
      var dif = tzo >= 0 ? '+' : '-';

      var pad = function(num) {
        var norm = Math.abs(Math.floor(num));
        return (norm < 10 ? '0' : '') + norm;
      };

      return now.getFullYear() +
        '-' + pad(now.getMonth()+1) +
        '-' + pad(now.getDate()) +
        'T' + pad(now.getHours()) +
        ':' + pad(now.getMinutes()) +
        ':' + pad(now.getSeconds()) +
        dif + pad(tzo / 60) +
        ':' + pad(tzo % 60);
    }
  });

  return Package;

});

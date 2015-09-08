define(['backbone', 'underscore', '../lib/backbone-model-file-upload', '../mixins/send-request', 'sota-dispatcher'], function(Backbone, _, BackboneModelFileUpload, sendRequest, SotaDispatcher) {

  var Package = Backbone.Model.extend({
    fileAttribute: 'file',
    initialize: function() {
      this.set({ packageId: this.get("name") + '/' + this.get("version")});
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    url: function() {
      return '/api/v1/packages/' + this.get("name") + '/' + this.get("version");
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
        case 'package-updatePackage':
          this.updatePackage(payload.package);
      }
    },
    updatePackage: function(payload) {
      // TODO find a good place to configure this url
      sendRequest.doPost("/api/v1/install_campaigns", payload)
        .fail(_.bind(function(xhr) {
          this.trigger("error", this, xhr);
        }, this));
    }
  });

  return Package;

});

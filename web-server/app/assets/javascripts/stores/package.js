define(['backbone', 'underscore', '../lib/backbone-model-file-upload', '../mixins/send-request', 'sota-dispatcher'], function(Backbone, _, BackboneModelFileUpload, sendRequest, SotaDispatcher) {

  var Package = Backbone.Model.extend({
    fileAttribute: 'file',
    //The 'id' attribute is special to Backbone and can't be a complex object, which
    //is what core/resolver gives us, so reassign the id attribute to a name which
    //isn't used.
    idAttribute: '_none',
    initialize: function() {
      this.set({ packageId: this.get("id").name + '/' + this.get("id").version});
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    url: function() {
      return '/api/v1/packages/' + this.get("id").name + '/' + this.get("id").version;
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

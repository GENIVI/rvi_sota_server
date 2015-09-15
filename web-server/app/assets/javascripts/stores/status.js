define(['backbone', 'underscore', '../lib/backbone-model-file-upload', '../mixins/send-request', 'sota-dispatcher'], function(Backbone, _, BackboneModelFileUpload, sendRequest, SotaDispatcher) {

  var Status = Backbone.Model.extend({
    url: function() {
      return "/api/v1/updates/" + this.get('updateId') + "/status";
    },
    initialize: function(attrs, opts) {
      this.set({
        updateId: opts.updateId
      });
      this.fetch();
    }
  });

  return Status;

});

define(function(require) {

  var Backbone = require('backbone'),
      BackboneModelFileUpload = require('../lib/backbone-model-file-upload'),
      SotaDispatcher = require('sota-dispatcher');

  var Package = Backbone.Model.extend({
    fileAttribute: 'file',
    //The 'id' attribute is special to Backbone and can't be a complex object, which
    //is what core/resolver gives us, so reassign the id attribute to a name which
    //isn't used.
    idAttribute: '_none',
    initialize: function() {
      this.set({ packageId: this.get("id").name + '/' + this.get("id").version});
    },
    url: function() {
      return '/api/v1/packages/' + this.get("id").name + '/' + this.get("id").version;
    },
  });

  return Package;

});

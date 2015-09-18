define(['jquery', 'backbone', 'sota-dispatcher'], function($, Backbone, SotaDispatcher) {

  var VehiclesToUpdate = Backbone.Model.extend({
    url: "",
    initialize: function(attributes, options) {
      SotaDispatcher.register(this.dispatchCallback.bind(this));
      this.url = '/api/v1/resolve/' + options.pkgName + "/" + options.pkgVersion;
    },
    getVINs: function() {
      var vins = [];
      for (var vin in this.attributes) {
        vins.push(this.attributes[vin][0]);
      }
      return vins;
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
        case 'get-affected-vins':
          this.fetch();
          break;
        case 'sync':
          this.getVINs();
          break;
      }
    }
  });

  return VehiclesToUpdate;

});

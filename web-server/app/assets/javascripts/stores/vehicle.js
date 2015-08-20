define(['backbone', 'underscore', '../mixins/send-request', 'sota-dispatcher'], function(Backbone, _, sendRequest, SotaDispatcher) {

  var Vehicle = Backbone.Model.extend({
    url: function() {
      return '/api/v1/vehicles/' + this.get('vin');
    }
  });

  return Vehicle;

});

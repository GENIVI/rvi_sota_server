define(['backbone', 'sota-dispatcher', './package'], function(Backbone, SotaDispatcher, Package) {

  var Updates = Backbone.Collection.extend({
    url: '/api/v1/updates',
    initialize: function() {
      this.fetch();
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
      }
    }
  });

  return Updates;

});

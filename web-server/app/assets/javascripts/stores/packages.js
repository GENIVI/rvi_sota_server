define(['backbone', 'sota-dispatcher', './package'], function(Backbone, SotaDispatcher, Package) {

  var Packages = Backbone.Collection.extend({
    url: '/api/v1/packages',
    model: Package,
    initialize: function() {
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
        case 'package-add':
          this.createWithEvents(payload.package, { type: 'put' });
          break;
        case 'packages-filter':
          this.fetch({ data: $.param({regex: payload.regex}) });
          break;
        case 'initialize':
          this.fetch();
          break;
      }
    }
  });

  return new Packages();

});

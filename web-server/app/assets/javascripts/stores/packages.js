define(['backbone', 'sota-dispatcher', './package'], function(Backbone, SotaDispatcher, Package) {

  var Packages = Backbone.Collection.extend({
    url: '/api/v1/packages',
    model: Package,
    initialize: function() {
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    fetchForFilter: function(filterName) {
      var options = { url: "/api/v1/packageFilters?filter=" + filterName};
      return Backbone.Model.prototype.fetch.call(this, options);
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

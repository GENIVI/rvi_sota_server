define(['backbone', 'sota-dispatcher'], function(Backbone, SotaDispatcher) {

  var Filters = Backbone.Collection.extend({
    url: '/api/v1/filters',
    initialize: function() {
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
        case 'search-filters':
          this.fetch({ data: $.param({regex: payload.regex}) });
          break;
        case 'fetch-filters':
          this.fetch();
          break;
        case 'initialize':
          this.fetch();
          break;
      }
    }
  });

  return new Filters();

});

define(function(require) {

  var Backbone = require('backbone'),
      _ = require('underscore');
      SotaDispatcher = require('sota-dispatcher');

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
        case 'create-filter':
          this.createWithEvents(payload.filter);
          break;
        case 'update-filter':
          var opts = {
            type: 'PUT',
            url: this.url + "/" + payload.filter.name,
            success: _.bind(function(model, attrs, opts) {
              this.trigger('sync');
            }, this)
          };

          var filter = this.findWhere({name: payload.filter.name});
          filter.save(payload.filter, opts);
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

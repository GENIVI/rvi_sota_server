define(function(require) {

  var Backbone = require('backbone'),
      _ = require('underscore');
      sendRequest = require('../mixins/send-request')
      SotaDispatcher = require('sota-dispatcher');

  var Filters = Backbone.Collection.extend({
    url: '/api/v1/filters',
    initialize: function() {
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    updateFilter: function(payload) {
      var opts = {
        type: 'PUT',
        url: this.url + "/" + payload.filter.name,
        success: _.bind(function(model, attrs, opts) {
          this.trigger('sync');
        }, this)
      };

      var filter = this.findWhere({name: payload.filter.name});
      filter.save(payload.filter, opts);
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
        case 'search-filters':
          this.fetch({ data: $.param({regex: payload.regex}) });
          break;
        case 'filter-add':
          this.createWithEvents(payload.filter);
          break;
        case 'update-filter':
          sendRequest.doPost('/api/v1/validate/filter', payload.filter)
          .done(_.bind(function() {
            this.updateFilter(payload)
          }, this))
          .fail(_.bind(function(xhr) {
            var response = JSON.parse(xhr.responseText);
            this.trigger('notice', response.description)
          }, this));
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

define(['jquery', 'backbone', 'sota-dispatcher', './vehicle'], function($, Backbone, SotaDispatcher, Vehicle) {

  var Vehicles = Backbone.Collection.extend({
    url: '/api/v1/vehicles',
    model: Vehicle,
    initialize: function() {
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
        case 'vehicle-add':
          var exists = this.findWhere({vin: payload.vehicle.vin});
          if (exists) {
            this.trigger('notice', 'Already exists');
          } else {
            this.createWithEvents(payload.vehicle, {type: 'put'});
          }
          break;
        case 'vehicles-filter':
          this.fetch({ data: $.param({regex: payload.regex}) });
          break;
        case 'initialize':
          this.fetch();
          break;
      }
    }
  });

  return new Vehicles();

});

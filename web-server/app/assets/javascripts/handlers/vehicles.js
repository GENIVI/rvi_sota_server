define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore');
      db = require('../stores/db');
      sendRequest = require('../mixins/send-request');

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
        switch(payload.actionType) {
          case 'get-vehicles':
            sendRequest.doGet('/api/v1/vehicles')
              .success(function(vehicles) {
                db.vehicles.reset(vehicles);
              });
          break;
          case 'create-vehicle':
            var url = '/api/v1/vehicles/' + payload.vehicle.vin;
            sendRequest.doPut(url, payload.vehicle)
              .success(function(vehicles) {
                SotaDispatcher.dispatch({actionType: 'search-vehicles-by-regex'});
              });
          break;
          case 'search-vehicles-by-regex':
            var query = payload.regex ? '?regex=' + payload.regex : '';

            sendRequest.doGet('/api/v1/vehicles' + query)
              .success(function(vehicles) {
                db.searchableVehicles.reset(vehicles);
              });
          break;
          case 'fetch-affected-vins':
            var affectedVinsUrl = '/api/v1/resolve/' + payload.name + "/" + payload.version;

            sendRequest.doGet(affectedVinsUrl)
              .success(function(vehicles) {
                db.affectedVins.reset(vehicles);
              });
          break;
          case 'get-vehicles-for-package':
            sendRequest.doGet('/api/v1/vehicles?package=' + payload.name + '-' + payload.version)
              .success(function(vehicles) {
                db.vehiclesForPackage.reset(vehicles);
              });
          break;
        }
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});

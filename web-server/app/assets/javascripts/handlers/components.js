define(function(require) {
  var SotaDispatcher = require('sota-dispatcher'),
      _ = require('underscore'),
      db = require('../stores/db'),
      sendRequest = require('../mixins/send-request');

  var Handler = (function() {
      this.dispatchCallback = function(payload) {
        switch(payload.actionType) {
          case 'get-components':
            sendRequest.doGet('/api/v1/components')
              .success(function(components) {
                db.components.reset(components);
              });
          break;
          case 'search-components-by-regex':
            var query = payload.regex ? '?regex=' + payload.regex : '';

            sendRequest.doGet('/api/v1/components' + query)
              .success(function(components) {
                db.searchableComponents.reset(components);
              });
          break;
          case 'get-component':
            sendRequest.doGet('/api/v1/components')
              .success(function(components) {
                var showComponent = _.find(components, function(component) {
                  return component.partNumber == payload.partNumber;
                });
                db.showComponent.reset(showComponent);
              });
          break;
          case 'create-component':
            var url = '/api/v1/components/' + payload.component.partNumber;
            sendRequest.doPut(url, payload.component)
              .success(function() {
                SotaDispatcher.dispatch({actionType: 'search-components-by-regex'});
              });
          break;
          case 'destroy-component':
            sendRequest.doDelete('/api/v1/components/' + payload.partNumber)
              .success(_.bind(function(component) {
                location.hash = '#/components';
                SotaDispatcher.dispatch({actionType: 'get-components'});
              }, this));
            break;
          case 'get-vins-for-component':
            sendRequest.doGet('/api/v1/vehicles?component=' + payload.partNumber )
              .success(function(vehicles) {
                var formattedVehicles = _.map(vehicles, function(vehicle) {
                  return {vin: vehicle.vin};
                });
                db.vinsForComponent.reset(formattedVehicles);
              });
          break;
        }
      };
      SotaDispatcher.register(this.dispatchCallback.bind(this));
  });

  return new Handler();

});

define(['underscore', 'backbone', '../mixins/send-request', 'sota-dispatcher'], function(_, Backbone, sendRequest, SotaDispatcher) {

  var FiltersForPackage = Backbone.Collection.extend({
    url: '/api/v1/packageFilters',
    initialize: function() {
      SotaDispatcher.register(this.dispatchCallback.bind(this));
    },
    dispatchCallback: function(payload) {
      switch(payload.actionType) {
        case 'create-package-filter':
          this.createFilterForPackage(payload.package, payload.filter);
          break;
        case 'delete-package-filter':
          this.destroyFilterForPackage(payload.package, payload.filter);
          break;
        case 'filters-for-package':
          this.fetchFiltersForPackage(payload.package);
          break;
      }
    },

    fetchFiltersForPackage: function(package) {
      var options = { url: this.url + "?package=" + package.get('name') + "-" + package.get('version') };
      return Backbone.Model.prototype.fetch.call(this, options);
    },

    createFilterForPackage: function(package, filter) {
      var packageFilter = {
        packageName: package.get('name'),
        packageVersion: package.get('version'),
        filterName: filter.get('name')
      };

      var options = {
        success: _.bind(function() {
          this.fetchFiltersForPackage(package);
        }, this)
      };

      this.createWithEvents(packageFilter, options);
    },

    destroyFilterForPackage: function(package, filter) {
      var pathPrefix = "/api/v1/packageFilters/";
      var resourcePath = package.get('packageId') + "/" + filter.get('name');
      var url = pathPrefix + resourcePath;

      sendRequest.doDelete(url)
        .done(_.bind(function() {
          this.fetchFiltersForPackage(package);
        }, this))
        .fail(function(xhr) {
          console.log("Error", xhr.status, xhr.statusText);
        });
    }
  });

  return new FiltersForPackage();

});

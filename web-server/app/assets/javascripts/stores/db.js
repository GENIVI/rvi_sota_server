define(function(require) {
  var atom = require('../lib/atom');

  var DB = (function() {
    function DB() {
      this.updates = atom.createAtom([
      ]);
      this.showUpdate = atom.createAtom({});
      this.updateStatus = atom.createAtom({});
      this.operationResultsForVin = atom.createAtom({});

      this.packagesForFilter = atom.createAtom([]);
      this.packagesForVin = atom.createAtom([]);
      this.packageQueueForVin = atom.createAtom([]);
      this.packageHistoryForVin = atom.createAtom([]);
      this.componentsOnVin = atom.createAtom([]);
      this.firmwareOnVin = atom.createAtom([]);
      this.filtersForPackage = atom.createAtom([]);
      this.vehiclesForPackage = atom.createAtom([]);
      this.vehiclesQueuedForPackage = atom.createAtom([]);

      this.packages = atom.createAtom([]);
      this.showPackage = atom.createAtom({});
      this.searchablePackages = atom.createAtom([]);

      this.filters = atom.createAtom([]);
      this.searchableFilters = atom.createAtom([]);
      this.showFilter = atom.createAtom({});

      this.searchableComponents = atom.createAtom([]);
      this.showComponent = atom.createAtom({});
      this.vinsForComponent = atom.createAtom([]);

      this.affectedVins = atom.createAtom([]);
      this.searchableVehicles = atom.createAtom([]);
      this.postStatus = atom.createAtom([]);
    }

    return DB;
  })();

  return new DB();

});

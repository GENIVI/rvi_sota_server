define(function(require) {
  var atom = require('../lib/atom');

  var DB = (function() {
    function DB() {
      this.updates = atom.createAtom([
      ]);
      this.showUpdate = atom.createAtom({});
      this.updateStatus = atom.createAtom({});
    }

    return DB;
  })();

  return new DB();

});

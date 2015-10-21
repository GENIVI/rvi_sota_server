define(function(require) {

  var React = require('react'),
      _ = require('underscore'),
      db = require('stores/db'),
      togglePanel = require('../../mixins/toggle-panel'),
      SotaDispatcher = require('sota-dispatcher');

  var AffectedVins = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [togglePanel],
    componentWillUnmount: function(){
      this.props.AffectedVins.removeWatch("poll-affected-vins");
      _.each([db.packagesForFilter, db.filtersForPackage], function(atom) {
        atom.removeWatch('poll-package-filters');
      });
    },
    componentWillMount: function(){
      this.refreshData();
      this.props.AffectedVins.addWatch("poll-affected-vins", _.bind(this.forceUpdate, this, null));
      _.each([db.packagesForFilter, db.filtersForPackage], function(atom) {
        atom.addWatch('poll-package-filters', _.bind(this.refreshData, this, null));
      }, this);
    },
    refreshData: function() {
      var params = this.context.router.getCurrentParams();
      SotaDispatcher.dispatch({
        actionType: 'fetch-affected-vins',
        name: params.name,
        version: params.version
      });
    },
    label: "Affected Vins",
    panel: function() {
      var vehicles = _.map(this.props.AffectedVins.deref(), function(vin) {
        return (
          <li className="list-group-item" key={vin[0]}>
            { vin[0] }
          </li>
        );
      });
      return (
        <div>
          <ul className={'list-group ' + (this.state.collapsed ? 'hide' : '')}>
            { vehicles }
          </ul>
        </div>
      );
    }
  });

  return AffectedVins;
});

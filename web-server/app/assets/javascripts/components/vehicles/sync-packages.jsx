define(function(require) {
  var React = require('react'),
      SotaDispatcher = require('sota-dispatcher');

  var SyncPackagesComponent = React.createClass({
    handleSync: function(e) {
      e.preventDefault();

      SotaDispatcher.dispatch({
        actionType: 'sync-packages-for-vin',
        vin: this.props.Vin
      });
    },
    render: function() {
      return (
        <button type="button" className="btn btn-primary" onClick={this.handleSync}>Sync Packages</button>
      );
    }
  });

  return SyncPackagesComponent;
});

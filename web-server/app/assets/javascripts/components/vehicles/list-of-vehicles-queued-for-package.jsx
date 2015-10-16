define(function(require) {

  var React = require('react'),
      _ = require('underscore'),
      Router = require('react-router'),
      Fluxbone = require('../../mixins/fluxbone'),
      SotaDispatcher = require('sota-dispatcher');

  var ListOfVehiclesQueuedForPackage = React.createClass({
    componentWillUnmount: function(){
      this.props.Vehicles.removeWatch("poll-vehicles-queued-for-package");
    },
    componentWillMount: function(){
      this.props.Vehicles.addWatch("poll-vehicles-queued-for-package", _.bind(this.forceUpdate, this, null));
    },
    toggleQueuedVins: function() {
      SotaDispatcher.dispatch({actionType: 'get-vehicles-queued-for-package', name: this.props.PackageName, version: this.props.PackageVersion});
      this.setState({showQueuedVins: !this.state.showQueuedVins});
    },
    getInitialState: function() {
      return {showQueuedVins: false};
    },
    render: function() {
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <button className="btn btn-primary pull-right" onClick={this.toggleQueuedVins}>
                { this.state.showQueuedVins ? "HIDE" : "Show Queued Vehicles" }
              </button>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-md-12">
              { this.state.showQueuedVins ? this.showQueuedVins() : null }
            </div>
          </div>
        </div>
      )
    },
    showQueuedVins: function() {
      var vehicles = _.map(this.props.Vehicles.deref(), function(vehicle) {
        return (
          <tr key={vehicle}>
            <td>
              <Router.Link to='vehicle' params={{vin: vehicle}}>
              { vehicle }
              </Router.Link>
            </td>
          </tr>
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                VINs
              </td>
            </tr>
          </thead>
          <tbody>
            { vehicles }
          </tbody>
        </table>
      );
    }
  });

  return ListOfVehiclesQueuedForPackage;
});

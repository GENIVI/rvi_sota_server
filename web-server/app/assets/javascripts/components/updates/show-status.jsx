define(function(require) {
  var _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher'),
      React = require('react');

  var StatusComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.UpdateStatus.removeWatch("poll-update-status");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({
        actionType: 'get-update-status',
        id: this.context.router.getCurrentParams().id
      });
      this.props.UpdateStatus.addWatch("poll-update-status", _.bind(this.forceUpdate, this, null));
    },
    failedVINsTable: function() {
      var failedVINRows = _.map(this.props.UpdateStatus.deref(), function(value) {
        if(Array.isArray(value)) {
          if(value[2] === "Failed") {
            return (
              <tr key={value[1]}>
                <td>
                  {value[1]}
                </td>
              </tr>
            )
          }
        }
      });
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <h2>
                Failed Vehicles
              </h2>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-xs-12">
              <table className="table table-striped table-bordered">
                <tbody>
                  { failedVINRows }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      );
    },
    completedVINsTable: function() {
      var completedVINRows = _.map(this.props.UpdateStatus.deref(), function(value) {
        if(Array.isArray(value)) {
          if(value[2] === "Finished") {
            return (
              <tr key={value[1]}>
                <td>
                  {value[1]}
                </td>
              </tr>
            )
          }
        }
      });
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <h2>
                Completed Vehicles
              </h2>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-xs-12">
              <table className="table table-striped table-bordered">
                <tbody>
                  { completedVINRows }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      );
    },
    toggleFailedVINs: function() {
      this.setState({showFailedVINs: !this.state.showFailedVINs});
    },
    toggleCompletedVINs: function() {
      this.setState({showCompletedVINs: !this.state.showCompletedVINs});
    },
    getInitialState: function() {
      return {showFailedVINs: false,
              showCompletedVINs: false
      };
    },
    render: function() {
      var completedVINs = 0;
      var pendingVINs = 0;
      var failedVINs = 0;

      var rows = _.map(this.props.UpdateStatus.deref(), function(value) {
        if(Array.isArray(value)) {
          if(value[2] === "Pending") {
            pendingVINs++;
          } else if (value[2] === "Finished") {
            completedVINs++;
          } else if(value[2] === "Failed") {
            failedVINs++;
          }
          return (
            <tr key={value[1]}>
              <td>
                {value[1]}
              </td>
              <td>
                {value[2]}
              </td>
            </tr>
          );
        }
      });
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <h2>
                Status
              </h2>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-xs-12">
              <table className="table table-striped table-bordered">
              <tbody>
                <tr>
                  <td>Pending:</td>
                  <td>{pendingVINs}</td>
                </tr>
                <tr>
                  <td>Completed:</td>
                  <td>{completedVINs}</td>
                </tr>
                <tr>
                  <td>Failed:</td>
                  <td>{failedVINs}</td>
                </tr>
              </tbody>
              </table>
            </div>
          </div>
          <div className="row">
            <div className="col-md-12">
              <h2>
                All Vehicles
              </h2>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-xs-12">
              <table className="table table-striped table-bordered">
                <tbody>
                  { rows }
                </tbody>
              </table>
            </div>
          </div>
          <button className="btn btn-primary pull-right" onClick={this.toggleFailedVINs}>
            { this.state.showFailedVINs ? "HIDE" : "Show failed VINs" }
          </button>
          <button className="btn btn-primary pull-right" onClick={this.toggleCompletedVINs}>
            { this.state.showCompletedVINs ? "HIDE" : "Show completed VINs" }
          </button>
          { this.state.showFailedVINs ? this.failedVINsTable() : null }
          { this.state.showCompletedVINs ? this.completedVINsTable() : null }
        </div>
      );
    }
  });

  return StatusComponent;
});

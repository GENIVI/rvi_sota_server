define(function(require) {
  var _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher'),
      React = require('react');

  var ListOperationResultsForVin = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.OperationResultsForVin.removeWatch("poll-operation-results");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({
        actionType: 'get-operation-results-for-vin',
        vin: this.context.router.getCurrentParams().vin
      });
      this.props.OperationResultsForVin.addWatch("poll-operation-results", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var operationResults = _.map(this.props.OperationResultsForVin.deref(), function(value) {
        return (
          <tr key={value.id}>
            <td>
              {value.id}
            </td>
            <td>
              {value.updateId}
            </td>
            <td>
              {value.resultCode}
            </td>
            <td>
              {value.resultText}
            </td>
          </tr>
        );
      });
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <h2>
                Operation Results
              </h2>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-xs-12">
              <table className="table table-striped table-bordered">
              <tbody>
                <tr>
                  <td>ID</td>
                  <td>Update ID</td>
                  <td>Result Code</td>
                  <td>Result Text</td>
                </tr>
                {operationResults}
              </tbody>
              </table>
            </div>
          </div>
        </div>
      );
    }
  });

  return ListOperationResultsForVin;
});

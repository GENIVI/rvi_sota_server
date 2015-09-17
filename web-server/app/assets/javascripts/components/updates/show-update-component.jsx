define(function(require) {
  var _ = require('underscore'),
      React = require('react'),
      Status = require('../../stores/status'),
      StatusComponent = require('../../components/updates/status-component'),
      showModel = require('../../mixins/show-model');

  var ShowUpdateComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [showModel],
    whereClause: function() {
      var params = this.context.router.getCurrentParams();
      return {id: parseInt(params.id)};
    },
    showView: function() {
      var rows = _.map(this.state.Model.attributes, function(value, key) {
        return (
          <tr>
            <td>
              {key}
            </td>
            <td>
              {value}
            </td>
          </tr>
        );
      });
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <h1>
                Update ID: {this.state.Model.get('id')}
              </h1>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-md-12">
              <table className="table table-striped table-bordered">
                <tbody>
                  { rows }
                </tbody>
              </table>
            </div>
          </div>
          <StatusComponent Model={new Status({}, {updateId: this.state.Model.get('id')})} />
        </div>
      );
    }
  });

  return ShowUpdateComponent;
});

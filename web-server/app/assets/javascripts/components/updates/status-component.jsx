define(function(require) {
  var _ = require('underscore'),
      React = require('react'),
      Fluxbone = require('../../mixins/fluxbone');

  var StatusComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [
      Fluxbone.Mixin('Model', 'sync')
    ],
    render: function() {
      var rows = _.map(this.props.Model.attributes, function(value) {
        if(Array.isArray(value)) {
          return (
            <tr>
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
            <div className="col-md-12">
              <table className="table table-striped table-bordered">
                <tbody>
                  { rows }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      );
    }
  });

  return StatusComponent;
});

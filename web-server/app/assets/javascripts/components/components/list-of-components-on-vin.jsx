define(function(require) {
  var _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher'),
      Router = require('react-router'),
      React = require('react');

  var ComponentsOnVin = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Components.removeWatch("poll-components-on-vin");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({actionType: 'list-components-on-vin', vin: this.props.Vin});
      this.props.Components.addWatch("poll-components-on-vin", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var components = _.map(this.props.Components.deref(), function(component) {
        return (
          <tr key={component}>
              <td>
                <Router.Link to='component' params={ {partNumber: component} }>
                  {component}
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
                Part Number
              </td>
            </tr>
          </thead>
          <tbody>
            { components }
          </tbody>
        </table>
      );
    }
  });

  return ComponentsOnVin;
});

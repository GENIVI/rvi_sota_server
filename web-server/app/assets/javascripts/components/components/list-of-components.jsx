define(function(require) {
  var _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher'),
      Router = require('react-router'),
      React = require('react');

  var ListOfUpdates = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Components.removeWatch("poll-components");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({actionType: 'search-components-by-regex'});
      this.props.Components.addWatch("poll-components", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var components = _.map(this.props.Components.deref(), function(component) {
        return (
          <tr key={component.partNumber}>
              <td>
                <Router.Link to='component' params={ {partNumber: component.partNumber} }>
                  {component.partNumber}
                </Router.Link>
              </td>
              <td>
                {component.description}
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
              <td>
                Description
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

  return ListOfUpdates;
});

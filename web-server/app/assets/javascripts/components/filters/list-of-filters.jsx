define(function(require) {
  var _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher'),
      Router = require('react-router'),
      db = require('../../stores/db'),
      React = require('react');

  var ListOfUpdates = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Filters.removeWatch("poll-filters");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({actionType: 'search-filters-by-regex', regex: "."});
      this.props.Filters.addWatch("poll-filters", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var filters = _.map(this.props.Filters.deref(), function(filter) {
        return (
          <tr key={filter.name + filter.expression}>
              <td>
                <Router.Link to='filter' params={ {name: filter.name} }>
                  {filter.name}
                </Router.Link>
              </td>
              <td>
                {filter.expression}
              </td>
          </tr>
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                Name
              </td>
              <td>
          Expression
              </td>
            </tr>
          </thead>
          <tbody>
            { filters }
          </tbody>
        </table>
      );
    }
  });

  return ListOfUpdates;
});

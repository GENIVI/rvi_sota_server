define(['react', 'react-router', '../../mixins/fluxbone', '../../mixins/handle-fail', 'sota-dispatcher'], function(React, Router, Fluxbone, HandleFail, SotaDispatcher) {

  var floatRight = {
    float: 'right'
  };

  var FilterComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Filter'),
      HandleFail
    ],
    removeFilter: function() {
      this.sendDeleteRequest('/api/v1/filters/' + this.props.Filter.get('name'))
    },
    onSuccess: function() {
      SotaDispatcher.dispatch({
        actionType: 'fetch-filters'
      });
    },
    render: function() {
      return (
        <li className="list-group-item">
          <Router.Link to='filter' params={ {name: this.props.Filter.get('name')} }>
            { this.props.Filter.get('name') }
          </Router.Link>
          <span onClick={this.removeFilter} style={floatRight} className="glyphicon glyphicon-remove"></span>
        </li>
      );
    }
  });

  return FilterComponent;
});

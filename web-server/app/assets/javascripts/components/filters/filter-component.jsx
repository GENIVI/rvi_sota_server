define(['react', 'react-router', '../../mixins/fluxbone', 'sota-dispatcher'], function(React, Router, Fluxbone, SotaDispatcher) {

  var floatRight = {
    float: 'right'
  };

  var FilterComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Filter')
    ],
    render: function() {
      return (
        <li className="list-group-item">
          <Router.Link to='filter' params={ {name: this.props.Filter.get('name')} }>
            { this.props.Filter.get('name') }
          </Router.Link>
        </li>
      );
    }
  });

  return FilterComponent;
});

define(['react', 'react-router', '../../mixins/fluxbone', 'sota-dispatcher'], function(React, Router, Fluxbone, SotaDispatcher) {

  var FilterComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Filter')
    ],
    render: function() {
      return (
        <Router.Link to='filter' params={ {name: this.props.Filter.get('name')} }>
          <li className="list-group-item">
            { this.props.Filter.get('name') }
          </li>
        </Router.Link>
      );
    }
  });

  return FilterComponent;
});

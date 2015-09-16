define(['react', 'react-router', '../../mixins/fluxbone', 'sota-dispatcher'], function(React, Router, Fluxbone, SotaDispatcher) {

  var FilterComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Filter', 'sync')
    ],
    render: function() {
      return (
        <tr>
          <td>
            { this.props.Filter.get('name') }
          </td>
          <td>
            { this.props.Filter.get('expression') }
          </td>
        </tr>
      );
    }
  });

  return FilterComponent;
});

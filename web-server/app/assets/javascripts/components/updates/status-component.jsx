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
      var listItems = _.map(this.props.Model.attributes, function(value, key) {
        return (
          <li>
            {key}: {value}
          </li>
        );
      });
      return (
        <div>
          <h2>
            Status
          </h2>
          <ul>
            {listItems}
          </ul>
        </div>
      );
    }
  });

  return StatusComponent;
});

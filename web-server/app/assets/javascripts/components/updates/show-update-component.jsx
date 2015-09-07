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
      var listItems = _.map(this.state.Model.attributes, function(value, key) {
        return (
          <li>
            {key}: {value}
          </li>
        );
      });
      return (
        <div>
          <h1>
            Update {this.state.Model.get('id')}
          </h1>
          <ul>
            {listItems}
          </ul>
          <StatusComponent Model={new Status({}, {updateId: this.state.Model.get('id')})} />
        </div>
      );
    }
  });

  return ShowUpdateComponent;
});

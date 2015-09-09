define(['underscore', 'react', '../../mixins/show-model'], function(_, React, showModel) {

  var ShowFilterComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [showModel],
    whereClause: function() {
      var params = this.context.router.getCurrentParams();
      return {name: params.name};
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
            {this.state.Model.get('name')}
          </h1>
          <ul>
            {listItems}
          </ul>
        </div>
      );
    }
  });

  return ShowFilterComponent;
});

define(['underscore', 'react', '../mixins/show-model'], function(_, React, showModel) {

  var ShowPackageComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [showModel],
    showView: function() {
      var listItems = _.map(this.state.Model.attributes, function(value, key) {
        return (
          <li>
            {key}: {value}
          </li>
        )
      });
      return (
        <div>
          <h1>
            {this.state.Model.get('name') + " - " + this.state.Model.get('version')}
          </h1>
          <p>
            {this.state.Model.get('description')}
          </p>
          <ul>
            {listItems}
          </ul>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});

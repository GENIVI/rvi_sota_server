define(['underscore', 'react', '../mixins/show-model', 'components/create-update', './package-filters/add-package-filters'], function(_, React, showModel, CreateUpdate, AddPackageFilters) {

  var ShowPackageComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    mixins: [showModel],
    whereClause: function() {
      return this.context.router.getCurrentParams();
    },
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
          <AddPackageFilters Package={this.state.Model}/>
          <CreateUpdate packageName={this.state.Model.get('name')} packageVersion={this.state.Model.get('version')}/>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});

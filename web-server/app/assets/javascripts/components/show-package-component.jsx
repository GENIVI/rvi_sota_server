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
      var rows = _.map(this.state.Model.attributes, function(value, key) {
        return (
          <tr>
            <td>
              {key}
            </td>
            <td>
              {value}
            </td>
          </tr>
        );
      });
      return (
        <div>
          <h1>
            Package Details
          </h1>
          <p>
            {this.state.Model.get('description')}
          </p>
          <table className="table table-striped table-bordered">
            <thead>
              <tr>
                <td>
                  {this.state.Model.get('name')}
                </td>
                <td>
                </td>
              </tr>
            </thead>
            <tbody>
              { rows }
            </tbody>
          </table>
          <AddPackageFilters Package={this.state.Model}/>
          <CreateUpdate packageName={this.state.Model.get('name')} packageVersion={this.state.Model.get('version')}/>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});

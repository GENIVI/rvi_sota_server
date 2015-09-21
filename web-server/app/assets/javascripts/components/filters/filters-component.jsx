define(['react', '../../mixins/fluxbone', './filter-component', 'sota-dispatcher'],function(React, Fluxbone, FilterComponent, SotaDispatcher) {

  var Filters = React.createClass({
    mixins: [
      Fluxbone.Mixin("Store", "sync")
    ],
    render: function() {
      var filters = this.props.Store.models.map(function(filter) {
        return (
          <FilterComponent key={filter.get('name') + filter.get('expression')} Filter={filter} />
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                Name
              </td>
	      <td>
		Expression
	      </td>
              <td/>
            </tr>
          </thead>
          <tbody>
            { filters }
          </tbody>
        </table>
      );
    }
  });

  return Filters;
});

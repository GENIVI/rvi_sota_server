define(['react', '../../mixins/fluxbone', './filter-component', 'sota-dispatcher'], function(React, Fluxbone, FilterComponent, SotaDispatcher) {

  var FiltersForPackage = React.createClass({
    mixins: [
      Fluxbone.Mixin("Store", "sync")
    ],
    componentDidMount: function() {
      SotaDispatcher.dispatch({
        actionType: 'fetch-filters'
      });
    },
    render: function() {
      var filters = this.props.Store.models.map(function(filter) {
        return (
          <FilterComponent Store={this.props.Store} Filter={filter} />
        );
      }, this);
      return (
        <div>
          <br/>
          <ul className="list-group">
            { filters }
          </ul>
        </div>
      );
    }
  });

  return FiltersForPackage;
});

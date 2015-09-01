define(['react', '../../mixins/fluxbone', './filter-for-package-component', 'sota-dispatcher'], function(React, Fluxbone, FilterForPackageComponent, SotaDispatcher) {

  var FiltersForPackage = React.createClass({
    mixins: [
      Fluxbone.Mixin("Store", "sync")
    ],
    componentDidMount: function() {
      SotaDispatcher.dispatch({
        actionType: 'filters-for-package',
        package: this.props.Package
      });
    },
    render: function() {
      var filters = this.props.Store.models.map(function(filter) {
        return (
          <FilterForPackageComponent Filter={filter} Package={this.props.Package} eventName={this.props.eventName}/>
        );
      }, this);
      return (
        <div>
          <ul className="list-group">
            { filters }
          </ul>
        </div>
      );
    }
  });

  return FiltersForPackage;
});

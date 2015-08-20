define(['react', 'components/vehicles-component', 'components/add-vehicle-component', 'components/search-bar', 'stores/vehicles', 'sota-dispatcher'], function(React, VehiclesComponent, AddVehicleComponent, SearchBar, VehicleStore, SotaDispatcher) {

  var FilterableVehicleComponent = React.createClass({
    render: function() {
      return (
      <div>
        <SearchBar />
        <AddVehicleComponent VehicleStore={VehicleStore}/>
        <VehiclesComponent VehicleStore={VehicleStore}/>
      </div>
    );}
  });

  return FilterableVehicleComponent;

});

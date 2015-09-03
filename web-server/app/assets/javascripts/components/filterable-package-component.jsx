define(['react', 'components/search-bar', 'components/packages-component', 'components/add-package-component', 'stores/packages', 'sota-dispatcher'], function(React, SearchBar, PackagesComponent, AddPackageComponent, PackageStore, SotaDispatcher) {

  var FilterablePackageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <SearchBar label="Search packages by regex" event="packages-filter"/>
        <AddPackageComponent PackageStore={PackageStore}/>
        <PackagesComponent PackageStore={PackageStore}/>
      </div>
    );}
  });

  return FilterablePackageComponent;

});

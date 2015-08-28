define(['react', 'components/packages-component', 'components/add-package-component', 'stores/packages', 'sota-dispatcher'], function(React, PackagesComponent, AddPackageComponent, PackageStore, SotaDispatcher) {

  var FilterablePackageComponent = React.createClass({
    render: function() {
      return (
      <div>
        <AddPackageComponent PackageStore={PackageStore}/>
        <PackagesComponent PackageStore={PackageStore}/>
      </div>
    );}
  });

  return FilterablePackageComponent;

});

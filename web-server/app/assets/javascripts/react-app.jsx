require(['jquery', 'react', 'components/add-vin', 'components/packages-component', 'stores/packages', './sota-dispatcher'], function($, React, AddVin, PackagesComponent, PackageStore, SotaDispatcher) {

  var App = React.createClass({
    render: function() {
      return (
      <div>
        <div id="add-vins">
          <AddVin url="/api/v1/vins" />
        </div>
        <div id="packages">
          <PackagesComponent PackageStore = { PackageStore }/>
        </div>
      </div>
    );}
  });

  React.render(<App />, document.getElementById('app'));

});

require(['jquery', 'react', 'components/add-vin', 'components/add-package','./mixins/handle-fail'], function($, React, AddVin, AddPackage, HandleFailMixin) {

  var App = React.createClass({
    render: function() {
      return (
      <div>
        <div id="add-vins">
          <AddVin url="/api/v1/vins" />
        </div>
        <div id="add-package">
          <AddPackage url="/api/v1/packages"/>
        </div>
      </div>
    );}
  });

  React.render(<App />, document.getElementById('app'));

});

require(['react', 'components/add-vin', 'components/create-filter', 'components/packages-component', 'stores/packages', 'react-router'], function(React, AddVin, CreateFilter, PackagesComponent, PackageStore, Router) {

  var Link = Router.Link;
  var Route = Router.Route;
  var RouteHandler = Router.RouteHandler;

  var App = React.createClass({
    render: function() {
      return (
      <div>
        <ul className="nav nav-pills">
          <li role="presentation">
            <Link to="vins">Vehicles</Link>
          </li>
          <li role="presentation">
            <Link to="packages">Packages</Link>
          </li>
          <li role="presentation">
            <Link to="filters">Filters</Link>
          </li>
        </ul>
        <div>
          <RouteHandler />
        </div>
      </div>
    );}
  });

  var wrapComponent = function(Component, props) {
    return React.createClass({
      render: function() {
        return React.createElement(Component, props);
      }
    });
  };

  var routes = (
    <Route handler={App} path="/">
      <Route name="packages" handler={wrapComponent(PackagesComponent, {PackageStore:PackageStore})}/>
      <Route name="vins" handler={wrapComponent(AddVin, {url:"/api/v1/vehicles"})}/>
      <Route name="filters" handler={wrapComponent(CreateFilter, {url:"/api/v1/filters"})} />
    </Route>
  );

  Router.run(routes, function (Handler) {
    React.render(<Handler/>, document.getElementById('app'));
  });

});

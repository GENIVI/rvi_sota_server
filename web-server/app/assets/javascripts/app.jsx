define(function(require) {

  var React = require('react'),
      FilterableVehicleComponent = require('components/filterable-vehicle-component'),
      FilterablePackageComponent = require('components/filterable-package-component'),
      SearchableFilterComponent = require('components/filters/searchable-filter-component'),
      ShowPackage = require('components/show-package-component'),
      ShowFilter = require('components/filters/show-filter-component'),
      Packages = require('stores/packages'),
      Filters = require('stores/filters'),
      Router = require('react-router'),
      SotaDispatcher = require('sota-dispatcher');

  var Link = Router.Link;
  var Route = Router.Route;
  var RouteHandler = Router.RouteHandler;
  var DefaultRoute = Router.DefaultRoute;

  var App = React.createClass({
    render: function() {
      return (
      <div>
        <ul className="nav nav-pills">
          <li role="presentation">
            <Link to="vehicles">Vehicles</Link>
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
      <Route name="vehicles" handler={FilterableVehicleComponent}/>
      <Route name="packages">
        <Route name="package" path="/packages/:name/:version" handler={wrapComponent(ShowPackage, {Store: Packages})}/>
        <DefaultRoute handler={FilterablePackageComponent}/>
      </Route>
      <Route name="filters">
        <Route name="filter" path="/filters/:name" handler={wrapComponent(ShowFilter, {Store: Filters})}/>
        <DefaultRoute handler={SearchableFilterComponent} />
      </Route>
    </Route>
  );

  return {
    run: function() {
      Router.run(routes, function (Handler) {
        React.render(<Handler/>, document.getElementById('app'));
      });

      SotaDispatcher.dispatch({
        actionType: 'initialize'
      });
    }
  };

});

define(function(require) {

  var React = require('react'),
      VehiclesPageComponent = require('components/vehicles/vehicles-page-component'),
      FilterablePackageComponent = require('components/filterable-package-component'),
      SearchableFilterComponent = require('components/filters/searchable-filter-component'),
      ShowPackage = require('components/show-package-component'),
      ShowFilter = require('components/filters/show-filter-component'),
      Packages = require('stores/packages'),
      Filters = require('stores/filters'),
      Router = require('react-router'),
      Updates = require('stores/updates'),
      UpdatesComponent = require('components/updates/updates-component'),
      ShowUpdate = require('components/updates/show-update-component'),
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
          <li role="presentation">
            <Link to="updates">Updates</Link>
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
      <Route name="vehicles" handler={VehiclesPageComponent}/>
      <Route name="packages">
        <Route name="package" path="/packages/:name/:version" handler={wrapComponent(ShowPackage, {Store: Packages})}/>
        <DefaultRoute handler={FilterablePackageComponent}/>
      </Route>
      <Route name="filters">
        <Route name="filter" path="/filters/:name" handler={wrapComponent(ShowFilter, {Store: Filters})}/>
        <DefaultRoute handler={SearchableFilterComponent} />
      </Route>
      <Route name="updates">
        <Route name="update" path="/updates/:id" handler={wrapComponent(ShowUpdate, {Store: new Updates()})} />
        <DefaultRoute handler={wrapComponent(UpdatesComponent, {Store: new Updates()})} />
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

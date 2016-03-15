define(function(require) {

  var React = require('react'),
      VehiclesPageComponent = require('components/vehicles/vehicles-page-component'),
      PackagesPageComponent = require('components/packages/packages-page-component'),
      FiltersPageComponent = require('components/filters/filters-page-component'),
      ComponentsPage = require('components/components/components-page'),
      ShowPackage = require('components/packages/show-package'),
      ShowFilter = require('components/filters/show-filter'),
      ShowComponent = require('components/components/show-component'),
      VehiclePageComponent = require('components/vehicles/vehicle-page-component'),
      Router = require('react-router'),
      CreateCampaign = require('components/create-campaign-page-component'),
      ListOfUpdates = require('components/updates/list-of-updates'),
      ShowUpdate = require('components/updates/show-update-component'),
      SotaDispatcher = require('sota-dispatcher');

  // set up db
  var db = require('stores/db');
  // set up handlers
  var Handler = require('handlers/handler');

  var Link = Router.Link;
  var Route = Router.Route;
  var RouteHandler = Router.RouteHandler;
  var DefaultRoute = Router.DefaultRoute;

  var App = React.createClass({
    render: function() {
      return (
      <div>
 	    <nav className="navbar navbar-default navbar-fixed-top top-nav">
	      <div className="navbar-header">
            <div className="top-logo">
              <a href="#/" className="top-logo"></a>
            </div>
            <div className="top-icon">
              <h2 className="nav-label">SOTA</h2>
            </div>
	        <div className="navbar-collapse collapse">
              <ul className="nav side-nav">
                <li role="presentation">
                  <Link to="vehicles" className="vehicles"><i className="fa fa-car nav-link-btn"/>Vehicles</Link>
                </li>
                <li role="presentation">
                  <Link to="packages" className="packages"><i className="fa fa-envelope nav-link-btn"/>Packages</Link>
                </li>
                <li role="presentation">
                  <Link to="components" className="components"><i className="fa fa-cog nav-link-btn"/>Components</Link>
                </li>
                <li role="presentation">
                  <Link to="filters" className="filters"><i className="fa fa-filter nav-link-btn"/>Filters</Link>
                </li>
                <li role="presentation">
                  <Link to="updates" className="updates"><i className="fa fa-download nav-link-btn"/>Updates</Link>
                </li>
              </ul>
	        </div>
          </div>
        </nav>
        <div className="page wrapper container-fluid">
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
      <Route name="vehicle" path="vehicles/:vin" handler={VehiclePageComponent}/>
      <Route name="packages">
        <Route name="package" path="/packages/:name/:version" handler={wrapComponent(ShowPackage, {Package: db.showPackage})}/>
        <Route name="new-campaign" path="/packages/:name/:version/new-campaign" handler={wrapComponent(CreateCampaign)}/>
        <DefaultRoute handler={PackagesPageComponent}/>
      </Route>
      <Route name="filters">
        <Route name="filter" path="/filters/:name" handler={wrapComponent(ShowFilter, {Filter: db.showFilter})}/>
        <DefaultRoute handler={FiltersPageComponent} />
      </Route>
      <Route name="updates">
        <Route name="update" path="/updates/:id" handler={wrapComponent(ShowUpdate, {Update: db.showUpdate})} />
        <DefaultRoute handler={wrapComponent(ListOfUpdates, {Updates: db.updates})} />
      </Route>
      <Route name="components">
        <Route name="component" path="/component/:partNumber" handler={wrapComponent(ShowComponent, {Component: db.showComponent})} />
        <DefaultRoute handler={ComponentsPage}/>
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

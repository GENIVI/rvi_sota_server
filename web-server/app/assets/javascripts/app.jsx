define(function(require) {

  var React = require('react'),
      VehiclesPageComponent = require('components/vehicles/vehicles-page-component'),
      PackagesPageComponent = require('components/packages/packages-page-component'),
      FiltersPageComponent = require('components/filters/filters-page-component'),
      ShowPackage = require('components/show-package-component'),
      ShowFilter = require('components/filters/show-filter-component'),
      Packages = require('stores/packages'),
      Filters = require('stores/filters'),
      Router = require('react-router'),
      Updates = require('stores/updates'),
      UpdatesComponent = require('components/updates/updates-component'),
      CreateCampaign = require('components/create-campaign-page-component'),
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
 	<div className="navbar navbar-fixed-top top-nav">
	  <div className="navbar-header">
            <div className="top-logo">
              <a href="#/" className="top-logo"></a>
            </div>
            <div className="top-icon">
              <label>SOTA</label>
            </div>
	    <div className="navbar-collapse collapse">
              <ul className="nav side-nav">
                <li role="presentation">
   	          <Link to="vehicles" className="vehicles">Vehicles</Link>
   	        </li>
                <li role="presentation">
                  <Link to="packages" className="packages">Packages</Link>
                </li>
                <li role="presentation">
                  <Link to="filters" className="filters">Filters</Link>
                </li>
                <li role="presentation">
                  <Link to="updates" className="updates">Updates</Link>
                </li>
              </ul>
	    </div>
          </div>
        </div>
        <div className="page wrapper">
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
        <Route name="new-campaign" path="/packages/:name/:version/new-campaign" handler={wrapComponent(CreateCampaign, {Store: Packages})}/>
        <DefaultRoute handler={PackagesPageComponent}/>
      </Route>
      <Route name="filters">
        <Route name="filter" path="/filters/:name" handler={wrapComponent(ShowFilter, {Store: Filters})}/>
        <DefaultRoute handler={FiltersPageComponent} />
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

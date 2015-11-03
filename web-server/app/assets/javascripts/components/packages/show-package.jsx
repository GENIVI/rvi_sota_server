define(function(require) {

  var _ = require('underscore'),
      Router = require('react-router'),
      VehiclesListPanel = require('../vehicles/vehicles-list-panel'),
      PackageFilterAssociation = require('../package-filters/package-filter-association'),
      AffectedVins = require('../vehicles/affected-vins'),
      SotaDispatcher = require('sota-dispatcher'),
      db = require('stores/db'),
      React = require('react');

  var ShowPackageComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Package.removeWatch("poll-package");
    },
    componentWillMount: function(){
      var params = this.context.router.getCurrentParams();
      SotaDispatcher.dispatch({
        actionType: 'get-package',
        name: params.name,
        version: params.version
      });
      this.props.Package.addWatch("poll-package", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var params = this.context.router.getCurrentParams();
      var rows = _.map(this.props.Package.deref(), function(value, key) {
        if(key === "id") {
          var idString = value.name + '-' + value.version;
          return (
            <tr key={idString}>
              <td>
                {key}
              </td>
              <td>
                {idString}
              </td>
            </tr>
          );
        }
        return (
          <tr key={key}>
            <td>
              {key}
            </td>
            <td>
              {value}
            </td>
          </tr>
        );
      });
      var data = {name: params.name, version: params.version}
      return (
        <div>
          <h1>
            Packages &gt; {params.name + "-" + params.version}
          </h1>
          <div className="row">
            <div className="col-md-12">
              <Router.Link to='new-campaign' params={{name: params.name, version: params.version}}>
                <button className="btn btn-primary pull-right" name="new-campaign">
                  NEW CAMPAIGN
                </button>
              </Router.Link>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-md-12">
              <table className="table table-striped table-bordered">
                <thead>
                  <tr>
                    <td>
                      {params.name}
                    </td>
                    <td>
                    </td>
                  </tr>
                </thead>
                <tbody>
                  { rows }
                </tbody>
              </table>
            </div>
          </div>
          <PackageFilterAssociation
            Resource={this.props.Package}
            CreateList={db.filters}
            DeleteList={db.filtersForPackage}
            getCreateList="get-filters"
            createResourceName="Filters"
            getDeleteList={{actionType: 'get-filters-for-package', name: params.name, version: params.version}}/>
          <br/>
          <AffectedVins AffectedVins={db.affectedVins} />
          <h2>Vehicles</h2>
          <VehiclesListPanel
            Vehicles={db.vehiclesForPackage}
            PollEventName="poll-vehicles-for-package"
            DispatchObject={{actionType: "get-vehicles-for-package", name: params.name, version: params.version}}
            Label="Vehicles with this package installed"/>
          <VehiclesListPanel
            Vehicles={db.vehiclesQueuedForPackage}
            PollEventName="poll-vehicles-for-package"
            DispatchObject={{actionType: "get-vehicles-queued-for-package", name: params.name, version: params.version}}
            Label="Vehicles with this package queued for install"/>
        </div>
      );
    }
  });

  return ShowPackageComponent;
});

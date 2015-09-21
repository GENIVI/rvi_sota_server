define(function(require) {

  var React = require('react'),
      Fluxbone = require('../../mixins/fluxbone'),
      PackageComponent = require('./package-component'),
      SotaDispatcher = require('sota-dispatcher');

  var Packages = React.createClass({
    mixins: [
      Fluxbone.Mixin("PackageStore", "sync")
    ],
    render: function() {
      var packages = this.props.PackageStore.models.map(function(package) {
        return (
          <PackageComponent Package={ package } key={package.get('id').name + package.get('id').version}/>
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                Packages
              </td>
              <td>
                Version
              </td>
              <td>
              </td>
            </tr>
          </thead>
          <tbody>
            { packages }
          </tbody>
        </table>
      );
    }
  });

  return Packages;
});

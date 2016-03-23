define(function(require) {
  var _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher'),
      React = require('react');

  var FirmwareOnVin = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Firmware.removeWatch("poll-firmware-on-vin");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({actionType: 'list-firmware-on-vin', vin: this.props.Vin});
      this.props.Firmware.addWatch("poll-firmware-on-vin", _.bind(this.forceUpdate, this, null));
    },
    render: function() {
      var firmware = _.map(this.props.Firmware.deref(), function(firmware) {
        return (
          <tr key={firmware.module + '-' + firmware.version_id}>
              <td>
                  {firmware.module}
              </td>
              <td>
                  {firmware.firmwareId}
              </td>
          </tr>
        );
      });
      return (
        <table className="table table-striped table-bordered">
          <thead>
            <tr>
              <td>
                Module Name
              </td>
              <td>
                Version ID
              </td>
            </tr>
          </thead>
          <tbody>
            { firmware }
          </tbody>
        </table>
      );
    }
  });

  return FirmwareOnVin;
});

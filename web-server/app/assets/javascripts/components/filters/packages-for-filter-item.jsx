define(function(require) {

  var React = require('react'),
      _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher');

  var CurrentPackagesForFilter = React.createClass({
    handleClick: function(a, b) {
      SotaDispatcher.dispatch({
        actionType: this.props.eventName,
        packageFilter: this.props.Payload
      });
    },
    render: function() {
      return (
        <li className="list-group-item" onClick={this.handleClick}>
          {this.props.label}
        </li>
      );
    }
  });

  return CurrentPackagesForFilter;
});

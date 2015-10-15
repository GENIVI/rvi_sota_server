define(function(require) {
  var _ = require('underscore'),
      React = require('react'),
      SotaDispatcher = require('sota-dispatcher'),
      Errors = require('../errors'),
      db = require('stores/db');

  var ShowComponent = React.createClass({
    contextTypes: {
      router: React.PropTypes.func
    },
    componentWillUnmount: function(){
      this.props.Component.removeWatch("poll-component");
    },
    componentWillMount: function(){
      SotaDispatcher.dispatch({
        actionType: 'get-component',
        partNumber: this.context.router.getCurrentParams().partNumber
      });
      this.props.Component.addWatch("poll-component", _.bind(this.forceUpdate, this, null));
    },
    removeComponent: function() {
      SotaDispatcher.dispatch({
        actionType: 'destroy-component',
        partNumber: this.context.router.getCurrentParams().partNumber
      });
    },
    render: function() {
      var params = this.context.router.getCurrentParams();
      var listItems = _.map(this.props.Component.deref(), function(value, key) {
        return (
          <li>
            {key}: {value}
          </li>
        );
      });
      return (
        <div>
          <h1>
            {this.props.Component.deref().partNumber}
          </h1>
          <ul>
            {listItems}
          </ul>
          <button type="button" className="btn btn-primary" onClick={this.removeComponent} name="delete-component">Delete Component</button>
          <Errors />
        </div>
      );
    }
  });

  return ShowComponent;
});

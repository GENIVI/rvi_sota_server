define(function(require) {
  var React = require('react'),
      _ = require('underscore'),
      db = require('../stores/db');

  var Errors = React.createClass({
    getInitialState: function() {
      return {errorMessage: ""};
    },
    componentWillUnmount: function(){
      db.postStatus.removeWatch("poll-errors");
    },
    componentWillMount: function(){
      db.postStatus.addWatch("poll-errors", _.bind(function() {
        if (this.isMounted) {
          this.setState({errorMessage: db.postStatus.deref()});
        }
      }, this));
    },
    render: function() {
      return (
        <div>
          { this.state.errorMessage }
        </div>
      );
    }
  });

  return Errors;
});

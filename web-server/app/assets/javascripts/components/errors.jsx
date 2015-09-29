define(function(require) {
  var React = require('react'),
      db = require('../stores/db');

  var Errors = React.createClass({
    getInitialState: function() {
      return {errorMessage: ""};
    },
    componentWillMount: function(){
      db.postStatus.addWatch("poll-errors", _.bind(function() {
        this.setState({errorMessage: db.postStatus.deref()});
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

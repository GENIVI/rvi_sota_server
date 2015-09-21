define(['react', 'underscore', 'sota-dispatcher'], function(React, _, SotaDispatcher) {

  var SearchBar = React.createClass({
    handleChange: function() {
      SotaDispatcher.dispatch({
        actionType: this.props.event,
        regex: this.refs.filterTextInput.getDOMNode().value
      });
      return false;
    },
    render: function() {
      return (
        <form className="form-inline pull-right search-bar">
          <div className="form-group">
            <label htmlFor="regex">{this.props.label}</label>
            <input
              type="text"
              name="regex"
              value={this.props.filterText}
              ref="filterTextInput"
              className="form-control"
              onChange={this.handleChange}
            />
          </div>
        </form>
      );
    }
  });

  return SearchBar;
});

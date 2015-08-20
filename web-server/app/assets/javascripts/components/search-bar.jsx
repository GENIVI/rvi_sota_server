define(['react', 'underscore', 'sota-dispatcher'], function(React, _, SotaDispatcher) {

  var SearchBar = React.createClass({
    handleChange: function() {
      SotaDispatcher.dispatch({
        actionType: 'vehicles-filter',
        regex: this.refs.filterTextInput.getDOMNode().value
      });
      return false;
    },
    render: function() {
      return (
        <form>
          <div className="form-group">
            <label htmlFor="regex">Vehicle Vin Regex</label>
            <input
              type="text"
              name="regex"
              placeholder="Search..."
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

define(function(require) {

    var $ = require('jquery'),
        React = require('react'),
        SotaDispatcher = require('sota-dispatcher'),
        Fluxbone = require('../../mixins/fluxbone'),
        RequestStatus = require('../../mixins/request-status'),
        HandleFail = require('../../mixins/handle-fail');

    var EditFilterComponent = React.createClass({
	mixins: [
	  RequestStatus.Mixin("Store")
	],
    handleSubmit: function(e) {
        e.preventDefault();

        var exp = React.findDOMNode(this.refs.expression).value
        var payload = {name: this.props.Model.get('name'), expression: exp}
        SotaDispatcher.dispatch({
            actionType: 'update-filter',
            filter: payload
        });
    },
    render: function() {
	  return (
	    <div>
          <form ref='form' onSubmit={this.handleSubmit} encType="multipart/form-data">
            <div className="form-group">
              <label htmlFor="name">Filter Expression</label>
		      <input type="text" className="form-control" name="expression" ref="expression" defaultValue={this.props.Model.get('expression')}/>
  		    </div>
	        <div className="form-group">
              <button type="submit" className="btn btn-primary">Update Filter</button>
		    </div>
            <div className="form-group">
              {this.state.postStatus}
            </div>
          </form>
	    </div>
        );}
    });

    return EditFilterComponent;
});

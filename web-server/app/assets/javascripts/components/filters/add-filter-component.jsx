define(function(require) {

    var React = require('react'),
        serializeForm = require('../../mixins/serialize-form'),
        db = require('../../stores/db'),
        SotaDispatcher = require('sota-dispatcher'),
	    toggleForm = require('../../mixins/toggle-form'),
        RequestStatus = require('../../mixins/request-status');

    var AddFilterComponent = React.createClass({
	mixins: [
	  toggleForm
	],
    handleSubmit: function(e) {
        e.preventDefault();

        this.setState({postStatus: ""});

        var payload = serializeForm(this.refs.form);
        SotaDispatcher.dispatch({
            actionType: 'create-filter',
            filter: payload
        });
    },
	buttonLabel: "NEW FILTER",
    form: function() {
	  return (
	    <div>
          <form ref='form' onSubmit={this.handleSubmit} encType="multipart/form-data">
            <div className="form-group">
              <label htmlFor="name">Filter Name</label>
		      <input type="text" className="form-control" name="name" ref="name" placeholder="Filter Name"/>
  		    </div>
            <div className="form-group">
              <label htmlFor="expression">Filter Expression</label>
                <textarea type="text" className="form-control" id="expression" ref="expression" name="expression" placeholder='vin_matches "678$"' />
		    </div>
	        <div className="form-group">
              <button type="submit" className="btn btn-primary">Add Filter</button>
		    </div>
          </form>
	    </div>
        );}
    });

    return AddFilterComponent;
});

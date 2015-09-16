define(function(require) {

    var $ = require('jquery'),
        React = require('react'),
        serializeForm = require('../../mixins/serialize-form'),
        SotaDispatcher = require('sota-dispatcher'),
        Fluxbone = require('../../mixins/fluxbone'),
	FilterComponent = require('./filter-component'),
	toggleForm = require('../../mixins/toggle-form'),
        RequestStatus = require('../../mixins/request-status');

    var AddFilterComponent = React.createClass({
	mixins: [
	  toggleForm,
	  RequestStatus.Mixin("Store")
	],
        handleSubmit: function(e) {
            e.preventDefault();

            var payload = serializeForm(this.refs.form);
            SotaDispatcher.dispatch({
                actionType: 'filter-add',
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
                  <textarea type="text" className="form-control" id="expression" ref="expression" name="expression" placeholder="Vin(1234)" defaultValue={this.state.expression}/>
		</div>
	        <div className="form-group">
                  <button type="submit" className="btn btn-primary">Add Filter</button>
		</div>
                <div className="form-group">
                  {this.state.postStatus}
                </div>
              </form>
	    </div>
        );}
    });

    return AddFilterComponent;
});

define(function(require) {

    var $ = require('jquery'),
        React = require('react'),
        Backbone = require('backbone'),
        HandleFailMixin = require('../../mixins/handle-fail'),
        serializeForm = require('../../mixins/serialize-form'),
        SotaDispatcher = require('sota-dispatcher')

    var CreateFilter = React.createClass({
        mixins: [HandleFailMixin],
        handleSubmit: function(e) {
            e.preventDefault();
            this.setState({postStatus: ""});

            payload = serializeForm(this.refs.form);
            SotaDispatcher.dispatch({
                actionType: this.props.event,
                filter: payload
            });
        },
        componentWillMount: function() {
          if (this.props.Model) {
            this.setState(this.props.Model.attributes);
          }
        },
        render: function() {
          var nameForm;
          if (!this.props.Model) {
            nameForm = (
              <div>
                <label htmlFor="name">Filter Name</label>
                <input type="text" className="form-control" id="name" ref="name" name="name" placeholder="Filter Name" defaultValue={this.state.name}/>
              </div>
            );
          } else {
            nameForm = (
                <input type="text" hidden="true" id="name" ref="name" name="name" value={this.state.name}/>
            );
          }
          return (
            <form ref='form' onSubmit={this.handleSubmit}>
                <div className="form-group">
                    {nameForm}
                    <label htmlFor="expression">Filter Expression</label>
                    <textarea type="text" className="form-control" id="expression" ref="expression" name="expression" placeholder="Vin(1234)" defaultValue={this.state.expression}/>
                </div>
                <button type="submit" className="btn btn-primary">Add Filter</button>
                <span className="postStatus">
                    {this.state.postStatus}
                </span>
            </form>
        );}
    });

    return CreateFilter;
});

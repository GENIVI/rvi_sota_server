define(function(require) {

    var React = require('react'),
        _ = require('underscore'),
        serializeForm = require('../../mixins/serialize-form'),
        SotaDispatcher = require('sota-dispatcher');

    var TextInput = React.createClass({
      getInitialState: function() {
        return {value: this.props.value};
      },
      componentWillMount: function(){
        this.setState({value: this.props.value});
      },
      componentWillReceiveProps: function(){
        this.setState({value: this.props.value});
      },
      handleChange: function(event) {
        this.setState({value: event.target.value});
      },
      render: function() {
        var value = this.state.value;
        return <input type="text" className="form-control" name="expression" ref="expression" value={value} onChange={this.handleChange} />;
      }
    });

    var EditFilterComponent = React.createClass({
    componentWillUnmount: function(){
      this.props.Filter.removeWatch("poll-filterer");
    },
    componentWillMount: function(){
      this.props.Filter.addWatch("poll-filterer", _.bind(this.forceUpdate, this, null));
    },
    handleSubmit: function(e) {
        e.preventDefault();

        var expression = serializeForm(this.refs.form);
        var payload = _.extend({name: this.props.Filter.deref().name}, expression);
        SotaDispatcher.dispatch({
            actionType: 'edit-filter',
            filter: payload
        });
    },
    render: function() {
	  return (
	    <div>
          <form ref='form' onSubmit={this.handleSubmit} encType="multipart/form-data">
            <div className="form-group">
              <label htmlFor="name">Filter Expression</label>
              <TextInput value={this.props.Filter.deref().expression}/>
  		    </div>
	        <div className="form-group">
              <button type="submit" className="btn btn-primary">Update Filter</button>
		    </div>
          </form>
	    </div>
        );}
    });

    return EditFilterComponent;
});

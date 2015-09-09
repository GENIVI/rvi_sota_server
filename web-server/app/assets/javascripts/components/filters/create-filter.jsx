define(['jquery', 'react', '../../mixins/handle-fail', '../../mixins/serialize-form', 'sota-dispatcher'], function($, React, HandleFailMixin, serializeForm, SotaDispatcher) {

    var CreateFilter = React.createClass({
        mixins: [HandleFailMixin],
        handleSubmit: function(e) {
            e.preventDefault();
            this.setState({postStatus: ""});

            payload = serializeForm(this.refs.form);
            if (this.validate(payload.name, payload.expression) === false) {
                this.setState({postStatus: "Filter must validate"});
                return;
            }
            this.sendPostRequest(this.props.url, payload);
        },
        validate: function(name, expression) {
            if (name === '' || expression === '') {
                this.setState({postStatus: "Filter name and filter expression must not be empty"});
                return false;
            }
            return true;
        },
        onSuccess: function(data) {
            SotaDispatcher.dispatch({
                actionType: 'fetch-filters'
            });
            this.setState({postStatus: "Added filter \"" + data.name + "\" successfully"});
            React.findDOMNode(this.refs.name).value = '';
            React.findDOMNode(this.refs.expression).value = '';
        },
        render: function() { return (
            <form ref='form' onSubmit={this.handleSubmit}>
                <div className="form-group">
                    <label htmlFor="name">Filter Name</label>
                    <input type="text" className="form-control" id="name" ref="name" name="name" placeholder="Filter Name"/>
                    <label htmlFor="expression">Filter Expression</label>
                    <textarea type="text" className="form-control" id="expression" ref="expression" name="expression" placeholder="Vin(1234)" />
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

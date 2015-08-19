define(['jquery', 'react', '../mixins/handle-fail', '../mixins/serialize-form'], function($, React, HandleFailMixin, serializeForm) {

    var CreateAssociation = React.createClass({
        mixins: [HandleFailMixin],
        handleSubmit: function(e) {
            e.preventDefault();
            this.setState({postStatus: ""});

            payload = serializeForm(this.refs.form);
            if (this.validate(payload.packageName, payload.packageVersion, payload.filterName) === false) {
                this.setState({postStatus: "Association must validate"});
                return;
            }
            this.sendPostRequest(this.props.url, payload);
        },
        validate: function(pkgName, pkgVersion, filterName) {
            if (pkgName === '' || pkgVersion === '' || filterName === '') {
                this.setState({postStatus: "Package and filter name and package version must not be empty"});
                return false;
            }
            return true;
        },
        onSuccess: function(data) {
            this.setState({postStatus: "Created association successfully"});
            React.findDOMNode(this.refs.packageName).value = '';
            React.findDOMNode(this.refs.packageVersion).value = '';
            React.findDOMNode(this.refs.filterName).value = '';
        },
        render: function() { return (
            <form ref='form' onSubmit={this.handleSubmit}>
                <div className="form-group">
                    <label htmlFor="packageName">Package Name</label>
                    <input type="text" className="form-control" id="packageName" ref="packageName" name="packageName" placeholder="Package Name"/>
                    <label htmlFor="packageVersion">Package Version</label>
                    <textarea type="text" className="form-control" id="packageVersion" ref="packageVersion" name="packageVersion" placeholder="10" />
                    <label htmlFor="filterName">Filter Name</label>
                    <textarea type="text" className="form-control" id="filterName" ref="filterName" name="filterName" placeholder="Filter Name" />
                </div>
                <button type="submit" className="btn btn-primary">Create Association</button>
                {this.state.postStatus}
            </form>
        );}
    });

    return CreateAssociation;
});

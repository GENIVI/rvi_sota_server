define(['jquery', 'react'], function($, React) {
  var toggleForm = {
    toggleForm: function() {
      this.setState({showForm: !this.state.showForm});
    },
    getInitialState: function() {
      return {showForm: false};
    },
    render: function() {
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <button className="btn btn-primary pull-right" onClick={this.toggleForm}>
                { this.state.showForm ? "HIDE" : this.buttonLabel }
              </button>
            </div>
          </div>
          <br/>
          <div className="row">
            <div className="col-md-12">
              { this.state.showForm ? this.form() : null }
            </div>
          </div>
        </div>
      );
    }
  };

  return toggleForm;
});

define(['jquery', 'react'], function($, React) {
  var togglePanel = {
    togglePanel: function() {
      this.refreshData();
      this.setState({showPanel: !this.state.showPanel});
    },
    getInitialState: function() {
      return {showPanel: false};
    },
    render: function() {
      var panelBody = !this.state.showPanel ? null :
        (<div className="panel-body">
           { this.panel() }
        </div>);
      var labelText = typeof this.label === 'string' ? this.label : this.label()
      return (
        <div>
          <div className="row">
            <div className="col-md-12">
              <div className="panel panel-default" id="panel1">
                <div className="panel-heading pointer" onClick={this.togglePanel}>
                   <h4 className="panel-title">
                     { labelText }
                  </h4>
                </div>
                { panelBody }
              </div>
            </div>
          </div>
        </div>
      );
    }
  };

  return togglePanel;
});

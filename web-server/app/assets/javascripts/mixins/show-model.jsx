define(['jquery', 'react'], function($, React) {
  var showModel = {
    getStateFromStore: function () {
      this.props.Store.once('sync', function(collection) {
        this.stateFromStore();
      }, this);
      return this.whereClause();
    },
    componentWillReceiveProps: function(props, context) {
      this.stateFromStore();
    },
    componentDidMount: function(props, context) {
      this.stateFromStore();
    },
    stateFromStore: function () {
      var model = this.props.Store.findWhere(this.whereClause());
      this.setState({ Model: model });
    },
    getInitialState: function () {
      this.getStateFromStore();
      return { Model: undefined };
    },
    loadingView: function() {
      return (
        <p>
          loading
        </p>
      );
    },
    render: function() {
      var view;
      if (this.state.Model) {
        view = this.showView();
      } else {
        view = this.loadingView();
      }
      return (
        <dix>
          {view}
        </dix>
      );
    }
  };

  return showModel;
});

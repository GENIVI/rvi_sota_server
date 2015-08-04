define(['backbone', 'underscore'], function(Backbone, _) {

  // add to backbone collections so this works
  _.extend(Backbone.Collection.prototype, {
    createWithEvents: function(attrs) {
      var persisted = this.create(attrs, {
        wait: true,
        error: _.bind(function(model, err) {
          this.trigger("error", model, err);
        }, this)
      });
    }
  });

  var RequestStatus;
  RequestStatus = function(modelName) {
    var mixin;
    mixin = {
      getInitialState: function() {
        return {postStatus : ''};
      },
      componentDidMount: function() {
        this.props[modelName].on('sync', function(model, data) {
          this.setState({postStatus: ''});
        }, this);
        return this.props[modelName].on('error', function(model, err) {
          var res = JSON.parse(err.responseText);
          this.setState({postStatus: res.errorMsg});
        }, this);
      },
      componentWillUnmount: function() {
        return this.props[modelName].off('sync error');
      }
    };
    return mixin;
  };

  return { Mixin: RequestStatus };
});

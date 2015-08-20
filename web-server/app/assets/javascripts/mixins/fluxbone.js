define([], function() {
  var Mixin,
      __slice = [].slice;

  Mixin = function(model_name, event_name, cb_name) {
    var eventCbName, mixin;
    if (event_name == null) {
      event_name = "all";
    }
    eventCbName = "_eventCbs_" + model_name + "_" + event_name + "_" + (cb_name || '');
    mixin = {
      componentDidMount: function() {
        return this.props[model_name].on(event_name, this[eventCbName], this);
      },
      componentWillUnmount: function() {
        return this.props[model_name].off();
      }
    };
    mixin[eventCbName] = function() {
      var args;
      args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
      if ((cb_name != null) && (this[cb_name] != null)) {
        return this[cb_name].apply(this, args);
      } else {
        return this.forceUpdate();
      }
    };
    return mixin;
  };

  return {
    Mixin: Mixin,
    ModelMixin: Mixin,
    CollectionMixin: Mixin
  };
});

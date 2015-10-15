define(function(require) {
  var React = require('react'),
      AddComponent = require('./add-component');

  var ComponentsHeader= React.createClass({
    render: function() {
      return (
      <div>
        <div className="row">
          <div className="col-md-12">
            <h1>
              Components
            </h1>
          </div>
        </div>
        <div className="row">
          <div className="col-md-8">
            <p>
            </p>
          </div>
        </div>
        <AddComponent/>
      </div>
    );}
  });

  return ComponentsHeader;

});

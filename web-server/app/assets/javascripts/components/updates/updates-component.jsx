define(function(require) {

  var React = require('react'),
      Router = require('react-router'),
      Fluxbone = require('../../mixins/fluxbone'),
      SotaDispatcher = require('sota-dispatcher');

  var Updates = React.createClass({
    mixins: [
      Fluxbone.Mixin("Store", "sync")
    ],
    componentDidMount: function(props, context) {
      this.props.Store.fetch();
    },
    render: function() {
      var updates = this.props.Store.models.map(function(update) {
        return (
          <Router.Link to='update' params={{id: update.get('id'), Model: update}}>
            <li className="list-group-item">
              {update.get('packageId').name} - {update.get('packageId').version}
            </li>
          </Router.Link>
        );
      });

      return (
        <div>
          <h1>
            Updates
          </h1>
          <ul className="list-group">
            {updates}
          </ul>
        </div>
      );
    }
  });

  return Updates;
});

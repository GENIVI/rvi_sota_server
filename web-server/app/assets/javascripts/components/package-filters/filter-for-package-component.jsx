define(['react', '../../mixins/fluxbone', 'sota-dispatcher'], function(React, Fluxbone, SotaDispatcher) {

  var FilterForPackageComponent = React.createClass({
    mixins: [
      Fluxbone.Mixin('Filter')
    ],
    handleClick: function() {
      SotaDispatcher.dispatch({
        actionType: this.props.eventName,
        filter: this.props.Filter,
        package: this.props.Package
      });
    },
    render: function() {
      return (
        <li className="list-group-item" onClick={this.handleClick} name={this.props.Filter.get('name')}>
          { this.props.Filter.get('name') }
        </li>
      );
    }
  });

  return FilterForPackageComponent;
});

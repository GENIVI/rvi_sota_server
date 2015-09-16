define(['jquery', 'react', '../mixins/handle-fail', '../mixins/serialize-form', 'sota-dispatcher', 'components/vehicles-to-update-component', 'stores/vehicles-to-update'], function($, React, HandleFailMixin, serializeForm, SotaDispatcher, VehiclesToUpdate, VehiclesToUpdateStore) {

  var CreateUpdate = React.createClass({
    mixins: [HandleFailMixin],
    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({postStatus: ""});

      var startAfterDate = React.findDOMNode(this.refs.startAfterDate).value;
      var startAfterTime = React.findDOMNode(this.refs.startAfterTime).value;
      var startAfterTimeZone = React.findDOMNode(this.refs.startAfterTimeZone).value;
      var endBeforeDate = React.findDOMNode(this.refs.endBeforeDate).value;
      var endBeforeTime = React.findDOMNode(this.refs.endBeforeTime).value;
      var endBeforeTimeZone = React.findDOMNode(this.refs.endBeforeTimeZone).value;

      var payload = {
        packageId : { name: this.props.packageName,
        version: this.props.packageVersion },
        priority: Number(React.findDOMNode(this.refs.priority).value),
        startAfter: this.formatDate(startAfterDate, startAfterTime, startAfterTimeZone),
        endBefore: this.formatDate(endBeforeDate, endBeforeTime, endBeforeTimeZone)
      }
      SotaDispatcher.dispatch({
        actionType: "package-updatePackage",
        package: payload
      });
    },
    formatDate: function(date, time, timeZone) {
      //TODO: Get seconds working with ReactJS
      return date + "T" + time + ":00" + timeZone;
    },
    getTodaysDate: function() {
      var today = new Date();
      var dd = today.getDate();
      var mm = today.getMonth()+1; //January is 0!

      var yyyy = today.getFullYear();
      if(dd<10){
          dd='0'+dd
      }
      if(mm<10){
          mm='0'+mm
      }
      return yyyy+"-"+mm+"-"+dd;
    },
    componentDidMount: function() {
      React.findDOMNode(this.refs.startAfterDate).value = this.getTodaysDate();
      React.findDOMNode(this.refs.endBeforeDate).value = this.getTodaysDate();
      React.findDOMNode(this.refs.startAfterTime).value = "00:00";
      React.findDOMNode(this.refs.endBeforeTime).value = "00:00";
    },
    render: function() { return (
        <div>
          <h3>Create Install Campaign</h3>
          <form ref='form' onSubmit={this.handleSubmit}>
            <div className="form-group">
              <label htmlFor="priority">Update Priority</label>
              <input type="number" className="form-control" name="priority" ref="priority" placeholder="10" required/>
            </div>
            <div className="row">
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="startAfterDate">Start After Date</label>
                  <input type="date" className="form-control" name="startAfterDate" ref="startAfterDate" placeholder="Please specify a date" required/>
                </div>
              </div>
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="startAfterTime">Start After Time</label>
                  <input type="time" className="form-control" name="startAfterTime" ref="startAfterTime" placeholder="Please specify a time" required/>
                </div>
              </div>
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="startAfterTimeZone">Start After Time Zone</label>
                  <input type="text" className="form-control" name="startAfterTimeZone" ref="startAfterTimeZone" defaultValue="+00:00" pattern="[+-](2[0-3]|[01][0-9]):[0-5][0-9]" required/>
                </div>
              </div>
            </div>

            <div className="row">
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="endBeforeDate">End Before Date</label>
                  <input type="date" className="form-control" name="endBeforeDate" ref="endBeforeDate" placeholder="Please specify a date" required/>
                </div>
              </div>
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="endBeforeTime">End Before Time</label>
                  <input type="time" className="form-control" name="endBeforeTime" ref="endBeforeTime" placeholder="Please specify a time" required/>
                </div>
              </div>
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="endBeforeTimeZone">End Before Time Zone</label>
                  <input type="text" className="form-control" name="endBeforeTimeZone" ref="endBeforeTimeZone" defaultValue="+00:00" pattern="[+-](2[0-3]|[01][0-9]):[0-5][0-9]" required/>
                </div>
              </div>
            </div>
            <button type="submit" className="btn btn-primary">Create Update</button>
            <div className="form-group">
              { this.state.postStatus }
            </div>
          </form>
        </div>
      );}
  });

  return CreateUpdate;
});

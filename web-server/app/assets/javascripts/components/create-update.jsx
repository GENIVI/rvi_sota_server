define(function(require) {

  var $ = require('jquery'),
      React = require('react'),
      Router = require('react-router'),
      HandleFailMixin = require('../mixins/handle-fail'),
      serializeForm = require('../mixins/serialize-form'),
      SendRequest = require('../mixins/send-request'),
      _ = require('underscore'),
      SotaDispatcher = require('sota-dispatcher');

  function generateUUID(){
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (d + Math.random()*16)%16 | 0;
        d = Math.floor(d/16);
        return (c=='x' ? r : (r&0x3|0x8)).toString(16);
    });
    return uuid;
  }

  var CreateUpdate = React.createClass({

    mixins: [HandleFailMixin, Router.Navigation],

    populateUUID: function(e) {
      e.preventDefault();
      var uuid = generateUUID();
      React.findDOMNode(this.refs.updateId).value = uuid;
    },

    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({postStatus: ""});

      var startAfterDate = React.findDOMNode(this.refs.startAfterDate).value;
      var startAfterTime = React.findDOMNode(this.refs.startAfterTime).value;
      var startAfterTimeZone = React.findDOMNode(this.refs.startAfterTimeZone).value;
      var endBeforeDate = React.findDOMNode(this.refs.endBeforeDate).value;
      var endBeforeTime = React.findDOMNode(this.refs.endBeforeTime).value;
      var endBeforeTimeZone = React.findDOMNode(this.refs.endBeforeTimeZone).value;
      var updateId = React.findDOMNode(this.refs.updateId).value;
      var signature = React.findDOMNode(this.refs.signature).value;
      var description = React.findDOMNode(this.refs.description).value;
      var requestConfirmation = React.findDOMNode(this.refs.requestConfirmation).checked;

      var payload = {
        id: updateId,
        packageId : {
          name: this.props.packageName,
          version: this.props.packageVersion
        },
        description: description,
        requestConfirmation: requestConfirmation,
        priority: Number(React.findDOMNode(this.refs.priority).value),
        creationTime: this.formatDateForJSON(startAfterDate, startAfterTime, startAfterTimeZone),
        periodOfValidity:
          this.formatDateForJSON(startAfterDate, startAfterTime, startAfterTimeZone)
          + '/'
          + this.formatDateForJSON(endBeforeDate, endBeforeTime, endBeforeTimeZone),
        signature: signature
      }
      SendRequest.doPost("/api/v1/updates", payload)
        .done(_.bind(function() {
          this.transitionTo("/updates/" + updateId);
        }, this))
        .fail(_.bind(function(xhr) {
          this.trigger("error", this, xhr);
        }, this));
    },
    formatDateForJSON: function(date, time, timeZone) {
      //TODO: Get seconds working with ReactJS
      return date + "T" + time + ":00" + timeZone;
    },
    formatDateForInput: function(date) {
      var dd = date.getDate();
      var mm = date.getMonth()+1; //January is 0!

      var yyyy = date.getFullYear();
      if(dd<10){
          dd='0'+dd
      }
      if(mm<10){
          mm='0'+mm
      }
      return yyyy+"-"+mm+"-"+dd;
    },
    incrementDate: function(date) {
      Date.prototype.addDays = function(days) {
        var dat = new Date(this.valueOf());
        dat.setDate(dat.getDate() + days);
        return dat;
      }
      return date.addDays(1);
    },
    componentDidMount: function() {
      React.findDOMNode(this.refs.startAfterDate).value = this.formatDateForInput(new Date());
      React.findDOMNode(this.refs.endBeforeDate).value = this.formatDateForInput(this.incrementDate(new Date()));
      React.findDOMNode(this.refs.startAfterTime).value = "00:00";
      React.findDOMNode(this.refs.endBeforeTime).value = "00:00";
    },
    render: function() { return (
        <div>
          <h3>Create Install Campaign</h3>
          <form ref='form' onSubmit={this.handleSubmit}>
            <div className="row">
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="description">Description</label>
                  <textarea className="form-control" name="description" ref="description" />
                </div>
              </div>
            </div>
            <div className="row">
              <div className="form-group">
                <div className="col-xs-4">
                  <label htmlFor="priority">Update Priority</label>
                  <input type="number" className="form-control" name="priority" ref="priority" placeholder="10" required/>
                </div>
              </div>
            </div>
            <div className="row">
              <div className="form-group">
                <div className="col-xs-4">
                  <label>
                    <input type="checkbox" name="requestConfirmation"
                           ref="requestConfirmation" />
                    Request confirmation
                  </label>
                </div>
              </div>
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
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="updateId">Update Id</label>
                  <input type="text" className="form-control" name="updateId" ref="updateId" placeholder="XXXXXXXX-XXXX-4XXX-aXXX-XXXXXXXXXXXX" pattern="\w{8}-\w{4}-4\w{3}-[89aAbB]\w{3}-\w{12}" required/>
                </div>
              </div>
              <div className="col-xs-4">
                <div className="form-group">
                  <label htmlFor="signature">Update Signature</label>
                  <input type="text" className="form-control" name="signature" ref="signature" placeholder="" pattern="\w*={0,2}" required />
                </div>
              </div>
            </div>
            <button type="button" className="btn btn-secondary" onClick={this.populateUUID}>Generate Update Id</button>
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

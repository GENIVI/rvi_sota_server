package org.genivi.sota.data

/**
  * The states that an [[UpdateSpec]] may be in.
  * Updates start in Pending state, then go to InFlight, then either Failed or
  * Finished. At any point before the Failed or Finished state it may transfer
  * to the Canceled state when a user cancels the operation
  */
object UpdateStatus extends Enumeration with CirceEnum {
  type UpdateStatus = Value

  val Pending, InFlight, Canceled, Failed, Finished = Value
}


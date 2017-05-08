/**
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 * License: MPL-2.0
 */
package org.genivi.sota.marshalling

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport


object CirceMarshallingSupport extends FailFastCirceSupport with CirceInstances

package org.genivi.sota.device_registry.common

import org.genivi.sota.data.GroupInfo.Name
import org.genivi.sota.data.Uuid

final case class CreateGroupRequest(device1: Uuid, device2: Uuid, groupName: Name)

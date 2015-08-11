#!/bin/bash

mysql -u sota -ps0ta -e 'delete from sota_resolver.Filter where name="Testfilter";delete from sota_resolver.Vehicle where vin="TESTVIN0123456789";delete from sota_core.Vehicle where vin="TESTVIN0123456789";'

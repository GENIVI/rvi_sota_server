#!/bin/bash

# copy packages uploaded by a user to all other users

# needs the environment variables LDAP_URL and LDAP_PASSWORD
# where LDAP_URL has the form "ldap://$hostname:389", the port being
# optional.

# you will probably also need to set the variable CORE_URL.
: ${CORE_URL:=http://localhost:8080}

set -euo pipefail

endpoint=${CORE_URL}/api/v1/super/packages/copy

usage() {
    echo "usage: $0 <userid> <packageuuid1> <packageuuid2> ..."
}

list2json() {
    sep=""
    echo -n [
    for n in $*; do
      echo -n $sep'"'$n'"'
      sep=","
    done
    echo ]
}

# $1: packageUuid
# rest: namespaces
create_copy_request() {
    packageuuid=$1
    shift
    namespaces="$*"
    namespace_list=$(list2json "${namespaces}")
    echo '{"packageUuid":"'"$packageuuid"'", "namespaces":'"${namespace_list}"'}'
}

call_curl() {
    echo curl -H $namespace_header $endpoint -d $copy_request
}

ldap_get_all_namespaces() {
    ldapsearch -H $LDAP_URL -D cn=admin,dc=genivi,dc=org -b dc=members,dc=genivi,dc=org -s sub -w $LDAP_PASSWORD | sed -n 's/uid: \(.*\)/\1/p'
}

if [ "$#" -lt 2 ]; then
    usage
    exit 1
fi

namespace=$1
shift
packageuuids="$*"
targetnamespaces="$(ldap_get_all_namespaces)"
namespace_header="x-ats-namespace: $namespace"

for packageuuid in ${packageuuids}; do
    copy_request=$(create_copy_request "$packageuuid" "$targetnamespaces")
    curl -X PUT -H "$namespace_header" -H "Content-Type: application/json" $endpoint -d "$copy_request"
done

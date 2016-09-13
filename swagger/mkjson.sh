#!/bin/sh
ruby -ryaml -rjson -e 'puts JSON.pretty_generate(YAML.load(ARGF))' < sota-core.yml > sota-core.json
ruby -ryaml -rjson -e 'puts JSON.pretty_generate(YAML.load(ARGF))' < sota-resolver.yml > sota-resolver.json
ruby -ryaml -rjson -e 'puts JSON.pretty_generate(YAML.load(ARGF))' < sota-device_registry.yml > sota-device_registry.json
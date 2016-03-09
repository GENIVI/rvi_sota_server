#!/bin/bash

secret=`head -c 500 /dev/random | tr -dc 'a-zA-Z0-9~!@#$%^&*_-' | fold -w 64 | head -n 1`
echo $secret | tr -d '\n'

#!/bin/bash

set -ex

# Arguments
FILE=$1
KEYWORD=$2
NEW_TEXT=$3

python modify_text.py $FILE $KEYWORD $NEW_TEXT

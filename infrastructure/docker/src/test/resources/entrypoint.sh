#!/bin/bash

if [ $# -eq 0 ]
  then
    echo "No arguments supplied"
    exit 0
fi

mcs -out:executable.exe /home/runner/"$@"
mono executable.exe
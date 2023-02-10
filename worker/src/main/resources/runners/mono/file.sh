#!/bin/bash
mcs -out:executable.exe /home/newuser/"$@"
mono executable.exe
#!/bin/bash

sourceDir=$(cd `dirname $0`/../; pwd)

javac -encoding utf-8 -source 1.8 -target 1.8 -d "${sourceDir}/bin/windows"  WinUpgradeSilent.java

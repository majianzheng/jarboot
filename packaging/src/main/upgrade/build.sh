#!/bin/bash

sourceDir=$(cd `dirname $0`/../; pwd)

echo "Compiling WinUpgradeSilent.java..."
javac -encoding utf-8 -source 1.8 -target 1.8 -d "${sourceDir}/bin/windows"  WinUpgradeSilent.java
echo "Compile WinUpgradeSilent.java done."

#!/bin/bash

javac --module-source-path .. --module info.kgeorgiy.ja.panov.implementor -d classes --module-path ../../lib:../../artifacts
jar -c --file implementor.jar --manifest=META-INF/MANIFEST.MF -C classes .
#jar -c --file base.jar -C classes/info.kgeorgiy.java.advanced.base .
#jar -c --file advanced-implementor.jar -C classes/info.kgeorgiy.java.advanced.implementor .
#jar -c --file implementor.jar --manifest=META-INF/MANIFEST.MF -C classes/info.kgeorgiy.ja.panov.implementor .
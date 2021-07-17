#!/bin/bash

files=../info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor

javadoc -private -author -d docs info/kgeorgiy/ja/panov/implementor/Implementor.java $files/ImplerException.java \
  $files/Impler.java $files/JarImpler.java -link https://docs.oracle.com/en/java/javase/11/docs/api
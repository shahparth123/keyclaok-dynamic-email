#!/bin/bash

rm -rf files/
mkdir files
mvn clean install
cp */target/*.jar ./files/
rm -rf ../standalone/deployments/
mkdir ../standalone/deployments/
cp */target/*.jar ../standalone/deployments/

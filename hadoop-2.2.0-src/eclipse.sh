#!/bin/sh
mvn clean compile
mvn eclipse:clean
mvn eclipse:eclipse

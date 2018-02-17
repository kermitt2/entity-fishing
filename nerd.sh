#!/usr/bin/env bash

export MAVEN_OPTS="-Xms4g -Xmx10g"

mvn -Dmaven.test.skip=true jetty:run-war

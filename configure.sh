#!/bin/bash
#
#  Copyright 2022 Red Hat
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#


if [ $# -lt 2 ]
then
    echo "give an SSL value, example"
    exit 1
fi

SSL=$1
EXAMPLE=$2
HOME=build/target/wildfly-*-grpc-*-SNAPSHOT

echo SSL: $SSL
echo EXAMPLE: $EXAMPLE

cp examples/$EXAMPLE/service/target/wildfly-grpc-examples-$EXAMPLE-service-*.jar $HOME/standalone/deployments
if [ $SSL == "none" ];
then
    cp ssl/standalone.xml.none $HOME/standalone/configuration/standalone.xml
    echo cp ssl/standalone.xml.none $HOME/standalone/configuration/standalone.xml
elif [ $SSL == "oneway" ];
then
    cp ssl/server.keystore.jks $HOME/standalone/configuration
    cp ssl/standalone.xml.oneway $HOME/standalone/configuration/standalone.xml
elif [ $SSL == "twoway" ];
then
    cp ssl/server.keystore.jks $HOME/standalone/configuration
    cp ssl/server.truststore.jks $HOME/standalone/configuration
    cp ssl/standalone.xml.twoway $HOME/standalone/configuration/standalone.xml
else
	echo "Don't recognize $SSL"
	exit
fi

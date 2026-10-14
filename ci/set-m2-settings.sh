#!/bin/sh

#
# This file is part of SFManager.
#
# SFManager is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# SFManager is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with SFManager. If not, see <http://www.gnu.org/licenses/>.
#
# @package SFManager
# @author Marian Pollzien <map@wafriv.de>
# @copyright (c) 2026, Marian Pollzien
# @license https://www.gnu.org/licenses/lgpl.html LGPLv3
#

ACCESS_TOKEN="$1"
ROOT_FOLDER="$( pwd )/../"
M2_HOME="${HOME}/.m2"
M2_CACHE="${ROOT_FOLDER}/maven"

if [ -d "${M2_CACHE}" ] && [ ! -d "${M2_HOME}" ]
then
    echo "Generating symbolic link for cache"
    ln -s "${M2_CACHE}" "${M2_HOME}"
fi

mkdir -p "${M2_HOME}"

echo "Write m2 settings"
cat > "${M2_HOME}/settings.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <activeProfiles>
        <activeProfile>github</activeProfile>
    </activeProfiles>
    <profiles>
        <profile>
            <id>github</id>
            <repositories>
                <repository>
                    <id>central</id>
                    <url>https://repo1.maven.org/maven2</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>true</enabled></snapshots>
                </repository>
                <repository>
                    <id>github-event-dispatcher</id>
                    <name>GitHub Antafes Apache Maven Packages - Event Dispatcher</name>
                    <url>https://maven.pkg.github.com/antafes/eventdispatcher</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>true</enabled></snapshots>
                </repository>
            </repositories>
        </profile>
        <profile>
            <id>SUREFIRE-1588</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
            </properties>
        </profile>
    </profiles>

    <servers>
        <server>
            <id>github-event-dispatcher</id>
            <username>antafes</username>
            <password>${ACCESS_TOKEN}</password>
        </server>
    </servers>
</settings>
EOF

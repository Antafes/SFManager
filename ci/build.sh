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

echo "Generating symbolic link for cache"

if [ -d "${M2_CACHE}" ] && [ ! -d "${M2_HOME}" ]
then
    ln -s "${M2_CACHE}" "${M2_HOME}"
fi

VERSION=`cat VERSION`

# Setup maven settings
$( pwd )/ci/set-m2-settings.sh ${ACCESS_TOKEN}

# Start build without tests
mvn -Drevision=${VERSION} -Dmaven.test.skip=true clean package

cp target/*.zip ../dist

echo "v$VERSION" >> ../dist/name
echo "v$VERSION" >> ../dist/tag

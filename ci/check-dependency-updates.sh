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
OUTPUT_DIR="${ROOT_FOLDER}/target"
REPORT_FILE="${OUTPUT_DIR}/dependency-update-report.md"
SUMMARY_FILE="${OUTPUT_DIR}/dependency-update-summary.json"
DEP_LOG="${OUTPUT_DIR}/dependency-updates.log"
PLUGIN_LOG="${OUTPUT_DIR}/plugin-updates.log"

echo "Generating symbolic link for cache"

if [ -d "${M2_CACHE}" ] && [ ! -d "${M2_HOME}" ]
then
    ln -s "${M2_CACHE}" "${M2_HOME}"
fi

# Setup maven settings when token is available (required for private registries)
if [ -n "${ACCESS_TOKEN}" ]
then
    "$( pwd )"/ci/set-m2-settings.sh "${ACCESS_TOKEN}"
fi

mkdir -p "${OUTPUT_DIR}"

# Collect dependency and plugin update information.
mvn -B versions:display-dependency-updates > "${DEP_LOG}" 2>&1
DEP_SCAN_EXIT_CODE=$?
mvn -B versions:display-plugin-updates > "${PLUGIN_LOG}" 2>&1
PLUGIN_SCAN_EXIT_CODE=$?

SCAN_FAILED=false
if [ ${DEP_SCAN_EXIT_CODE} -ne 0 ] || [ ${PLUGIN_SCAN_EXIT_CODE} -ne 0 ]
then
    SCAN_FAILED=true
fi

DEPENDENCY_UPDATES="$(grep " -> " "${DEP_LOG}" || true)"
PLUGIN_UPDATES="$(grep " -> " "${PLUGIN_LOG}" || true)"

HAS_UPDATES=false
if [ -n "${DEPENDENCY_UPDATES}" ] || [ -n "${PLUGIN_UPDATES}" ]
then
    HAS_UPDATES=true
fi

{
    echo "# Dependency Update Report"
    echo
    echo "Generated: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo
    if [ "${SCAN_FAILED}" = true ]
    then
        echo "## Scan status"
        echo "- Dependency scan failed with exit code: ${DEP_SCAN_EXIT_CODE}"
        echo "- Plugin scan failed with exit code: ${PLUGIN_SCAN_EXIT_CODE}"
        echo
        echo "See logs for details:"
        echo "- Dependency log: ${DEP_LOG}"
        echo "- Plugin log: ${PLUGIN_LOG}"
        echo
    fi

    echo "## Dependency updates"
    if [ -n "${DEPENDENCY_UPDATES}" ]
    then
        echo "${DEPENDENCY_UPDATES}" | sed 's/^/- `/' | sed 's/$/`/'
    else
        echo "- No dependency updates found."
    fi
    echo
    echo "## Plugin updates"
    if [ -n "${PLUGIN_UPDATES}" ]
    then
        echo "${PLUGIN_UPDATES}" | sed 's/^/- `/' | sed 's/$/`/'
    else
        echo "- No plugin updates found."
    fi
} > "${REPORT_FILE}"

{
    echo "{"
    echo "  \"scanFailed\": ${SCAN_FAILED},"
    echo "  \"dependencyScanExitCode\": ${DEP_SCAN_EXIT_CODE},"
    echo "  \"pluginScanExitCode\": ${PLUGIN_SCAN_EXIT_CODE},"
    echo "  \"hasUpdates\": ${HAS_UPDATES},"
    echo "  \"reportFile\": \"${REPORT_FILE}\""
    echo "}"
} > "${SUMMARY_FILE}"

if [ "${SCAN_FAILED}" = true ]
then
    rm -f "${OUTPUT_DIR}/dependency-updates-found"
    echo "Dependency scan failed. See logs for details."
    exit 1
fi

if [ "${HAS_UPDATES}" = true ]
then
    touch "${OUTPUT_DIR}/dependency-updates-found"
    echo "Updates found."
else
    rm -f "${OUTPUT_DIR}/dependency-updates-found"
    echo "No updates found."
fi

exit 0

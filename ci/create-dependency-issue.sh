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

GITHUB_TOKEN="$1"
ROOT_FOLDER="$( pwd )/../"
OUTPUT_DIR="${ROOT_FOLDER}/target"
SUMMARY_FILE="${OUTPUT_DIR}/dependency-update-summary.json"
REPORT_FILE="${OUTPUT_DIR}/dependency-update-report.md"
ISSUE_TITLE_PREFIX="Dependency updates:"

GITHUB_OWNER="${GITHUB_OWNER:-Antafes}"
GITHUB_REPO="${GITHUB_REPO:-SFManager}"
GITHUB_API_BASE="${GITHUB_API_BASE:-https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}}"
GITHUB_SEARCH_API_BASE="${GITHUB_SEARCH_API_BASE:-https://api.github.com/search/issues}"

post_json_or_fail()
{
    URL="$1"
    PAYLOAD="$2"
    RESPONSE_FILE="$(mktemp)"
    HTTP_CODE="$(curl -sS \
        -o "${RESPONSE_FILE}" \
        -w "%{http_code}" \
        -X POST \
        -H "Accept: application/vnd.github+json" \
        -H "Authorization: Bearer ${GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        "${URL}" \
        -d "${PAYLOAD}")"

    case "${HTTP_CODE}" in
        2*)
            rm -f "${RESPONSE_FILE}"
            return 0
            ;;
        *)
            echo "GitHub API request failed with HTTP ${HTTP_CODE}: ${URL}" >&2
            cat "${RESPONSE_FILE}" >&2
            rm -f "${RESPONSE_FILE}"
            exit 1
            ;;
    esac
}

if [ -z "${GITHUB_TOKEN}" ]
then
    echo "Missing GitHub token."
    exit 1
fi

if [ ! -f "${SUMMARY_FILE}" ] || [ ! -f "${REPORT_FILE}" ]
then
    echo "Missing dependency scan artifacts."
    exit 1
fi

if ! grep -q '"hasUpdates": true' "${SUMMARY_FILE}"
then
    echo "No dependency updates found. Skipping issue automation."
    exit 0
fi

TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
ISSUE_TITLE="${ISSUE_TITLE_PREFIX} ${TIMESTAMP}"

REPORT_CONTENT="$(cat "${REPORT_FILE}")"
COMMENT_BODY="$(printf 'Automated dependency scan found updates at %s.\n\n%s' "${TIMESTAMP}" "${REPORT_CONTENT}")"

# JSON escaping for API payloads.
ESCAPED_COMMENT_BODY="$(printf '%s' "${COMMENT_BODY}" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | sed ':a;N;$!ba;s/\n/\\n/g')"
ESCAPED_ISSUE_BODY="$(printf '%s' "${REPORT_CONTENT}" | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | sed ':a;N;$!ba;s/\n/\\n/g')"

SEARCH_RESPONSE_FILE="$(mktemp)"
HTTP_CODE="$(curl -sS \
    -o "${SEARCH_RESPONSE_FILE}" \
    -w "%{http_code}" \
    -G \
    -H "Accept: application/vnd.github+json" \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    --data-urlencode "q=repo:${GITHUB_OWNER}/${GITHUB_REPO} is:issue is:open label:\"type:dependencies\" in:title \"${ISSUE_TITLE_PREFIX}\"" \
    --data-urlencode "per_page=1" \
    "${GITHUB_SEARCH_API_BASE}")"

case "${HTTP_CODE}" in
    2*)
        EXISTING_ISSUES_RESPONSE="$(cat "${SEARCH_RESPONSE_FILE}")"
        ;;
    *)
        echo "GitHub Search API request failed with HTTP ${HTTP_CODE}" >&2
        cat "${SEARCH_RESPONSE_FILE}" >&2
        rm -f "${SEARCH_RESPONSE_FILE}"
        exit 1
        ;;
esac

rm -f "${SEARCH_RESPONSE_FILE}"

EXISTING_ISSUE_NUMBER="$(printf '%s' "${EXISTING_ISSUES_RESPONSE}" | grep -o -m1 '"number":[[:space:]]*[0-9]\+' | sed 's/[^0-9]//g')"

if [ -n "${EXISTING_ISSUE_NUMBER}" ]
then
    echo "Updating existing dependency issue #${EXISTING_ISSUE_NUMBER}."
    post_json_or_fail \
        "${GITHUB_API_BASE}/issues/${EXISTING_ISSUE_NUMBER}/comments" \
        "{\"body\": \"${ESCAPED_COMMENT_BODY}\"}"
else
    echo "Creating new dependency issue."
    post_json_or_fail \
        "${GITHUB_API_BASE}/issues" \
        "{\"title\": \"${ISSUE_TITLE}\", \"body\": \"${ESCAPED_ISSUE_BODY}\", \"labels\": [\"type:dependencies\", \"dependencies\"]}"
fi

echo "Dependency issue automation completed."
exit 0


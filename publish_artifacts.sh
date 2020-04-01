#!/usr/bin/env bash

if [ -z "$GITHUB_TOKEN" ]
then
  echo "Please set GITHUB_TOKEN environment variable to contain your Github access token"
  exit 1
fi

gitRef=$1

if [ -z ${gitRef} ]
then
  echo "Please provide the existing Git tag/commit from which the artifacts should be released"
  exit 1
fi

echo "Sending repository_dispatch command to publish artifacts: gitRef='${gitRef}'"

curl -v -H "Accept: application/vnd.github.everest-preview+json" \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    --request POST \
    --data "{\"event_type\": \"release\", \"client_payload\": { \"gitRef\": \"${gitRef}\"}}" \
    https://api.github.com/repos/real-logic/agrona/dispatches
#!/bin/bash

set -ex

cd "$(dirname "$0")"

# Extract the current version from pom.xml
current_version=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" ./../../pom.xml)

# Remove the -SNAPSHOT suffix to get the release version
release_version=$(echo $current_version | sed 's/-SNAPSHOT//')

# Set the next development version (increment the minor version)
IFS='.' read -r -a version_parts <<< "$release_version"
major=${version_parts[0]}
minor=${version_parts[1]}
patch=${version_parts[2]}
next_dev_patch=$((patch + 1))
next_version="$major.$minor.$patch"
next_development_version="$major.$minor.$next_dev_patch-SNAPSHOT"

# Update the YAML file
./update_text.sh ./../workflows/publish.yml releaseVersion "$next_version"
./update_text.sh ./../workflows/publish.yml developmentVersion "$next_development_version"


git add ./../workflows/publish.yml
git commit -m "Updated publish.yml with release version: $release_version and development version: $next_development_version"

echo "Updated publish.yml with next patch release version: $release_version and patch development version: $next_development_version and commited"

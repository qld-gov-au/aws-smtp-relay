set -ex

echo GITHUB_TOKEN=$GITHUB_TOKEN
echo GITHUB_ORG=$GITHUB_ORG
echo GITHUB_REPO=$GITHUB_REPO
echo RELEASE_VERSION=$RELEASE_VERSION

echo "Listing all package versions for organization $GITHUB_ORG..."

# Get the list of packages
packages_response=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/orgs/$GITHUB_ORG/packages?package_type=maven&repository_id=$GITHUB_REPO_ID")

# Extract package names
package_names=$(echo "$packages_response" | jq -r '.[].name')


if [ -z "$package_names" ]; then
  echo "No packages found in repository $GITHUB_REPO"
  exit 0
fi

echo "Found packages: $package_names"

for package in $package_names; do
  echo "Processing package: $package"

  # Get the list of package versions
  response=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
               -H "Accept: application/vnd.github.v3+json" \
               "https://api.github.com/orgs/$GITHUB_ORG/packages/maven/$package/versions")

  # Extract version IDs for the specified release version
  version_ids=$(echo "$response" | jq -r --arg version "$RELEASE_VERSION" '.[] | select(.name == $version) | .id')

  if [ -z "$version_ids" ]; then
    echo "No versions found for $RELEASE_VERSION in package $package"
  else
    echo "Found versions for $RELEASE_VERSION in package $package: $version_ids"
    for version_id in $version_ids; do
      echo "Deleting version ID $version_id from package $package..."
      curl -X DELETE -H "Authorization: token $GITHUB_TOKEN" \
           -H "Accept: application/vnd.github.v3+json" \
           "https://api.github.com/orgs/$GITHUB_ORG/packages/maven/$package/versions/$version_id"
    done
  fi
done
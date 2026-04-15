param(
    [string]$Registry = $env:DOCKER_REGISTRY,
    [string]$Repository = $env:DOCKER_REPOSITORY,
    [string]$Tag = $(if ($env:DOCKER_IMAGE_TAG) { $env:DOCKER_IMAGE_TAG } else { "latest" })
)

if ([string]::IsNullOrWhiteSpace($Registry)) {
    throw "DOCKER_REGISTRY is required."
}

if ([string]::IsNullOrWhiteSpace($Repository)) {
    throw "DOCKER_REPOSITORY is required."
}

$image = "$Registry/$Repository`:$Tag"

./mvnw.cmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) {
    throw "Maven package failed."
}

docker build -t $image .
if ($LASTEXITCODE -ne 0) {
    throw "Docker build failed."
}

docker push $image
if ($LASTEXITCODE -ne 0) {
    throw "Docker push failed."
}

Write-Host "Pushed image: $image"

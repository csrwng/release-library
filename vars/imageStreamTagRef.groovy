#!/usr/bin/env groovy

def call(Object ctx, String imageStreamName, String tag) {
  def ref = null
  ctx.openshift.withCluster() {
    def imageStream = ctx.openshift.selector('is', imageStreamName)
    if (!imageStream.exists()) {
      ctx.error("image stream ${imageStreamName} does not exist")
    }
    def obj = imageStream.object()
    def repo = obj.status.dockerImageRepository
    if (obj.status.publicDockerImageRepository) {
      repo = obj.status.publicDockerImageRepository
    }
    ctx.echo "ImageStreamTag ${imageStreamName}:${tag} has repository ref ${repo}:${tag}"
    return "${repo}:${tag}"
}

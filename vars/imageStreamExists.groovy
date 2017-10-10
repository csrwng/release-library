#!/usr/bin/env groovy

def call(Object ctx, String imageStreamName) {
  def exists = false
  ctx.echo "ImageStreamExists called with ${imageStreamName}"
  ctx.openshift.withCluster() {
    ctx.echo "inside withCluster"
    try {
      def obj = ctx.openshift.selector('is', imageStreamName).object()
      exists = (obj != null)
    } catch(e) {
      exists = false
    }
  }
  ctx.echo "About to exit imageStreamExists"
  ctx.echo "ImageStream ${imageStreamName} exists: ${exists}"
  return exists
}

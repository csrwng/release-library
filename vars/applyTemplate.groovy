#!/usr/bin/groovy

@NonCPS
def invokeProcess(Object ctx, Object args) {
  return ctx.openshift.process(*args)
}

def call(Object ctx, String path, Object... params) {
  ctx.echo "apply template called"
  def args = ["-f", path]
  for (p in params) {
    args.add("-p")
    args.add(p)
  }

  ctx.openshift.withCluster() {
    ctx.echo "about to invoke process"
    def objects = invokeProcess(ctx, args)
    ctx.echo "about to apply objects"
    ctx.openshift.apply(objects)
  }
}

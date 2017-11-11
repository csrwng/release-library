#!/usr/bin/groovy

class DockerSourceImage implements java.io.Serializable {
  String ref
  String from
  String to
}

class DockerBuilder implements java.io.Serializable {
  Object ctx
  String fromTag
  String toTag
  String name
  java.util.ArrayList instructions
  java.util.ArrayList sourceImages
  java.util.ArrayList labels
  

  DockerBuilder(Object ctx) {
    this.ctx = ctx
    this.instructions = []
    this.sourceImages = []
    this.labels = []
  }

  def Name(String name) {
    this.name = name
    return this
  }

  def Labels(String ...labels) {
    for (label in labels) {
      def parts = label.tokenize("=")
      if (parts.size() != 2) {
        ctx.error "Invalid label: $label"
        return
      }
      def labelObj = [:]
      labelObj[parts[0]] = parts[1]
      this.labels << labelObj
    }
    return this
  }

  def From(String image) {
    this.instructions << "FROM ${image}"
    this.fromTag = image
    return this
  }
  
  def To(String tag) {
    this.toTag = tag
    return this
  }

  def Env(String ...vars) {
    def env = "ENV" << ""
    for (s in vars) {
      env << " " << s
    }
    this.instructions << env.toString()
    return this
  }

  def WorkDir(String dir) {
    this.instructions << "WORKDIR ${dir}"
    return this
  }

  def Copy(String imageRef, String from, String to) {
    def sourceFile = new java.io.File(from);
    // The directory used inside the build to hold the contents
    // of the image source will be named after the imamge source
    def buildDir = imageRef.tokenize(":")[0]
    def buildSource = new java.io.File(buildDir, sourceFile.getName())
    this.sourceImages << new DockerSourceImage(ref: imageRef, from: from, to: buildDir)
    this.instructions << "COPY ${buildSource.toString()} ${to}" 
    return this
  }

  def Run(String ...stmts) {
    def run = "RUN" << ""
    def concat = false
    for (s in stmts) {
      run << " "
      if (concat) {
        run << "&& \\\n    "
      } else {
        concat = true
      }
      run << s
    }
    this.instructions << run.toString()
    return this
  }

  def generateDockerfile() {
    def result = "" << ""
    for (i in this.instructions) {
      result << i << "\n"
    }
    return result.toString()
  }

  def generateBuildObject() {
    def imageSecret = this.ctx.dockerCfgSecret(this.ctx, "builder")
    def buildObject = [ 
      "kind": "Build",
      "metadata": [
        "generateName": "${this.name}",
        "labels": []
      ],
      "spec": [
        "source": [
          "dockerfile": "${this.generateDockerfile()}",
          "images": []
        ],
        "output": [
          "pushSecret": [
            "name": imageSecret
          ],
          "to": [
            "name":"${this.toTag}",
            "kind":"ImageStreamTag"
          ]
        ],
        "strategy": [
          "dockerStrategy": [
            "pullSecret": [
              "name": imageSecret
            ],
            "from": [
              "name": this.fromTag,
              "kind": "ImageStreamTag"
            ]
          ]
        ]
      ]
    ]
    for (l in this.labels) {
      buildObject.metadata.labels << l
    }
    for (image in this.sourceImages) {
      buildObject.source.images << [
        "from": [
          "kind": "ImageStreamTag",
          "name": "${image.ref}"
        ],
        "pullSecret": imageSecret,
        "paths": [
          [ "sourcePath": "${image.from}", "destinationDir": "${image.to}" ]
        ]
      ]
    }
    return buildObject
  }

  def Build(int timeOutSecs = 3600) {
    this.ctx.openshift.withCluster() {
      def build = openshift.create(this.generateBuildObject())
      waitForBuild(this.ctx, build.name(), timeOutSecs)
    }
  }
}

def call(Object ctx) {
  return new DockerBuilder(ctx)
}

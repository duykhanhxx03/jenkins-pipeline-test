pipeline {
  agent any

  environment {
    IMAGE_REPO = 'hello-local/jenkins-pipeline-test'
    APP_NAME   = 'hello-app'
    APP_PORT   = '9090'      // cổng app bên trong container (khớp Dockerfile EXPOSE 9090)
    HOST_PORT  = '9090'      // cổng publish ra host; đổi sang 19090 nếu 9090 đang bận
  }

  options {
    timestamps()
    skipDefaultCheckout(false)
  }

  stages {
    stage('Checkout') {
      steps {
        checkout([$class: 'GitSCM',
          branches: [[name: '*/main']],
          userRemoteConfigs: [[url: 'https://github.com/duykhanhxx03/jenkins-pipeline-test.git']]
        ])
        sh 'git --version'
      }
    }

    stage('Set Version') {
      steps {
        script {
          def sha = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
          env.APP_VERSION = "dev-${sha}"
          echo "App version: ${env.APP_VERSION}"
        }
      }
    }

    stage('Build JAR (Maven in Docker)') {
      steps {
        sh """
          docker run --rm \
            -v "\$PWD":/workspace -w /workspace \
            maven:3.9.8-eclipse-temurin-21 \
            mvn -B -e -DskipTests clean package
        """
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
          archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
      }
    }

    stage('Docker Build (local)') {
      steps {
        sh """
          docker version
          docker build -t ${IMAGE_REPO}:${APP_VERSION} -t ${IMAGE_REPO}:latest .
        """
      }
    }

    // ====== DEPLOY LOCAL ======
    stage('Deploy Local') {
      steps {
        sh """
          set -e
          # dừng & xoá container cũ (nếu có)
          docker rm -f ${APP_NAME} || true

          # chạy phiên bản mới
          docker run -d --name ${APP_NAME} --restart=always \\
            -p ${HOST_PORT}:${APP_PORT} \\
            ${IMAGE_REPO}:${APP_VERSION}

          # health-check sau deploy (tối đa ~30s)
          for i in \$(seq 1 30); do
            sleep 1
            if curl -sf http://127.0.0.1:${HOST_PORT}/hello >/dev/null; then
              echo "App is healthy."
              exit 0
            fi
            echo "waiting app after deploy..."
          done

          echo "Healthcheck failed" >&2
          exit 1
        """
      }
    }
  }

  post {
    success {
      echo "✅ Built local image: ${IMAGE_REPO}:${APP_VERSION} & deployed as ${APP_NAME} on port ${HOST_PORT}."
    }
    failure {
      echo "❌ Pipeline FAILED."
    }
  }
}

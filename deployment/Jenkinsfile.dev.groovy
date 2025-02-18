pipeline {
    agent any
    environment {
        // Credentials for jenkins to access private github repos
        GITHUB_CREDENTIALS = "8d51209e-434f-4761-bb3d-1f9e3974d0b1"

        //
        // VPS setup
        //
        REMOTE_USER = "ansible"
        REMOTE_IP = credentials("acide-elastika-01")
        // Folder where docker-compose and .env files are placed
        REMOTE_FOLDER = "/home/${REMOTE_USER}/docker/perucontrol-develop/"

        //
        // Build config
        //
        // prefix of the image to build, config triplet
        //  <project>-<service>-<stage>
        //  perucontrol-frontend-develop
        PROJECT_NAME = "perucontrol"
        PROJECT_SERVICE = "frontend"
        PROJECT_STAGE = "develop"
        PROJECT_TRIPLET = "${PROJECT_NAME}-${PROJECT_SERVICE}-${PROJECT_STAGE}"

        //
        // Docker registry setup
        //
        REGISTRY_CREDENTIALS = "dockerhub-digitalesacide-credentials"
        REGISTRY_URL = "docker.io"
        REGISTRY_USER = "digitalesacide"
        REGISTRY_REPO = "${PROJECT_TRIPLET}"
        // docker.io/digitalesacide/perucontrol-frontend-develop
        FULL_REGISTRY_URL = "${REGISTRY_URL}/${REGISTRY_USER}/${REGISTRY_REPO}"
        ESCAPED_REGISTRY_URL = "${REGISTRY_URL}\\/${REGISTRY_USER}\\/${REGISTRY_REPO}"

        // Docker buid arguments
        INTERNAL_BACKEND_URL = "http://trazo-dev-api:5000/api/v1"

        // SSH command
        SSH_COM = "ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_IP}"
    }

    stages {
        stage("Build & push image") {
            steps {
                sh "cp deployment/Dockerfile ."
                script {
                    withDockerRegistry(credentialsId: "${REGISTRY_CREDENTIALS}") {
                        def image = docker.build("${FULL_REGISTRY_URL}:${BUILD_NUMBER}", "--build-arg INTERNAL_BACKEND_URL=${INTERNAL_BACKEND_URL} .")
                        image.push()
                    }
                }
                sh "rm Dockerfile || true"
            }
        }
        stage("Restart frontend service") {
            steps {
                sshagent(['ssh-deploy']) {
                    // Replace docker image version
                    sh "${SSH_COM} 'cd ${REMOTE_FOLDER} && sed -i -E \"s/image: ${ESCAPED_REGISTRY_URL}:.+\$/image: ${ESCAPED_REGISTRY_URL}:${BUILD_NUMBER}/\" docker-compose.yml'"
                    sh "${SSH_COM} 'cd ${REMOTE_FOLDER} && docker compose up -d --no-deps ${PROJECT_TRIPLET}'"
                }
            }
        }
    }
}

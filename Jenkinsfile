pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        MAVEN_HOME = '/usr/share/maven'
        PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"
        ARTIFACT_NAME = 'user-authentication-1.0.0.jar'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code from repository...'
                checkout scm
                sh 'git rev-parse HEAD > .git/commit-id'
                sh 'cat .git/commit-id'
            }
        }

        stage('Build') {
            steps {
                echo 'Building the application...'
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application...'
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy to AWS') {
            steps {
                echo 'Deploying to AWS...'
                script {
                    // Upload artifact to S3 (optional)
                    // sh 'aws s3 cp target/${ARTIFACT_NAME} s3://your-bucket/artifacts/'
                    
                    // Deploy to EC2 or ECS
                    // This is a placeholder - customize based on your AWS setup
                    echo 'Deployment steps:'
                    echo '1. Copy JAR to EC2 instance'
                    echo '2. Stop existing service'
                    echo '3. Start new service with updated JAR'
                    echo '4. Health check'
                    
                    // Example EC2 deployment (uncomment and configure):
                    // sh '''
                    //     scp target/${ARTIFACT_NAME} ec2-user@your-ec2-instance:/opt/auth-service/
                    //     ssh ec2-user@your-ec2-instance 'sudo systemctl restart auth-service'
                    // '''
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
            // Optional: Send notification
            // emailext subject: "Build Success: ${env.JOB_NAME}",
            //            body: "Build ${env.BUILD_NUMBER} completed successfully.",
            //            to: "devops@example.com"
        }
        failure {
            echo 'Pipeline failed!'
            // Optional: Send notification
            // emailext subject: "Build Failed: ${env.JOB_NAME}",
            //            body: "Build ${env.BUILD_NUMBER} failed. Check console output.",
            //            to: "devops@example.com"
        }
        always {
            cleanWs()
        }
    }
}


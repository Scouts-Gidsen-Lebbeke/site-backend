# Site-backend
[![Deployment](https://github.com/Scouts-Gidsen-Lebbeke/site-backend/actions/workflows/deploy-to-cloud-run.yml/badge.svg)](https://github.com/Scouts-Gidsen-Lebbeke/site-backend/actions/workflows/deploy-to-cloud-run.yml)

### Prerequisites
Required:
- An SMTP mail client (a basic Gmail address will do)
- A domain name with dns (there exit many cheap registrars)

Optional:
- A database environment (by default an H2 file-based db is created, so it is strongly encouraged to have a better environment)
- A Mollie account (if you want to be able to receive payments)
- A Keycloak resource key (if you want to use an external authentication provider)
- Your Groepsadmin group id (if you want Groepsadmin to stay master of user data)

### Running the app

### Manual deployment
Install and initialize gcloud
```
sudo apt update
sudo apt install google-cloud-sdk
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init
```
One time gcloud setup
```
gcloud auth activate-service-account --key-file=<service-account-key>
gcloud config set project <project-id>
gcloud auth configure-docker gcr.io --quiet
```
Build and deploy
```
IMAGE_NAME=gcr.io/<project-id>/site-backend
docker build --build-arg VERSION=<project-version> -t $IMAGE_NAME <project-folder>
docker push $IMAGE_NAME
gcloud run deploy site-backend --image $IMAGE_NAME --platform managed --region europe-west1 \
    --allow-unauthenticated --update-env-vars <comma-separated-environment-vars>
```
